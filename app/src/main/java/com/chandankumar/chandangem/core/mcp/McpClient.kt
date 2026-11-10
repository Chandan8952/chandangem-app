package com.chandankumar.chandangem.core.mcp

import android.util.Log
import com.chandankumar.chandangem.core.mcp.model.CallToolParams
import com.chandankumar.chandangem.core.mcp.model.CallToolResult
import com.chandankumar.chandangem.core.mcp.model.JsonRpcNotification
import com.chandankumar.chandangem.core.mcp.model.JsonRpcRequest
import com.chandankumar.chandangem.core.mcp.model.ListResourcesResult
import com.chandankumar.chandangem.core.mcp.model.ListToolsResult
import com.chandankumar.chandangem.core.mcp.model.McpInitializeParams
import com.chandankumar.chandangem.core.mcp.model.McpInitializeResult
import com.chandankumar.chandangem.core.mcp.model.McpResource
import com.chandankumar.chandangem.core.mcp.model.McpTool
import com.chandankumar.chandangem.core.mcp.model.ReadResourceResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import java.util.concurrent.atomic.AtomicLong

/**
 * Model Context Protocol (MCP) Client.
 * Speaks JSON-RPC 2.0 to any compliant MCP server over STDIO or SSE.
 */
class McpClient(
    val serverId: String,
    val serverName: String,
    private val transport: McpTransport
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val requestIdCounter = AtomicLong(1)
    private val mutex = Mutex()

    val isConnected: StateFlow<Boolean> = transport.isConnected

    var serverInfo: McpInitializeResult? = null
        private set

    private fun nextId(): JsonPrimitive = JsonPrimitive(requestIdCounter.getAndIncrement().toString())

    /**
     * Connects transport and performs MCP initialization handshake.
     */
    suspend fun initialize(): McpInitializeResult = mutex.withLock {
        transport.connect()

        val initParams = json.encodeToJsonElement(McpInitializeParams.serializer(), McpInitializeParams())
        val initRequest = JsonRpcRequest(
            id = nextId(),
            method = "initialize",
            params = initParams
        )

        val response = transport.sendRequest(initRequest)
        if (response.error != null) {
            throw IllegalStateException("MCP initialize failed: ${response.error.message}")
        }

        val resultElement = response.result ?: throw IllegalStateException("Missing result in initialize response")
        val initResult = json.decodeFromJsonElement(McpInitializeResult.serializer(), resultElement)
        this.serverInfo = initResult

        // Send notifications/initialized as required by MCP spec
        transport.sendNotification(
            JsonRpcNotification(method = "notifications/initialized")
        )

        Log.i(TAG, "[$serverName] MCP initialized successfully: ${initResult.serverInfo.name} v${initResult.serverInfo.version}")
        return initResult
    }

    /**
     * Health check ping.
     */
    suspend fun ping(): Boolean {
        return try {
            val req = JsonRpcRequest(id = nextId(), method = "ping")
            val resp = transport.sendRequest(req)
            resp.error == null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Queries available tools exposed by the server.
     */
    suspend fun listTools(): List<McpTool> {
        val req = JsonRpcRequest(id = nextId(), method = "tools/list")
        val resp = transport.sendRequest(req)
        if (resp.error != null) {
            throw IllegalStateException("tools/list failed: ${resp.error.message}")
        }

        val resultElement = resp.result ?: return emptyList()
        val listResult = json.decodeFromJsonElement(ListToolsResult.serializer(), resultElement)
        return listResult.tools
    }

    /**
     * Executes a tool by name with arguments.
     */
    suspend fun callTool(name: String, arguments: JsonObject? = null): CallToolResult {
        val params = CallToolParams(name = name, arguments = arguments)
        val paramsElement = json.encodeToJsonElement(CallToolParams.serializer(), params)

        val req = JsonRpcRequest(
            id = nextId(),
            method = "tools/call",
            params = paramsElement
        )

        val resp = transport.sendRequest(req)
        if (resp.error != null) {
            return CallToolResult(
                content = emptyList(),
                isError = true
            )
        }

        val resultElement = resp.result ?: return CallToolResult()
        return json.decodeFromJsonElement(CallToolResult.serializer(), resultElement)
    }

    /**
     * Lists resources offered by this MCP server.
     */
    suspend fun listResources(): List<McpResource> {
        val req = JsonRpcRequest(id = nextId(), method = "resources/list")
        val resp = transport.sendRequest(req)
        if (resp.error != null) return emptyList()
        val resultElement = resp.result ?: return emptyList()
        val listResult = json.decodeFromJsonElement(ListResourcesResult.serializer(), resultElement)
        return listResult.resources
    }

    /**
     * Reads a resource by URI.
     */
    suspend fun readResource(uri: String): ReadResourceResult {
        val params = buildJsonObject { put("uri", uri) }
        val req = JsonRpcRequest(id = nextId(), method = "resources/read", params = params)
        val resp = transport.sendRequest(req)
        if (resp.error != null) throw IllegalStateException("resources/read failed: ${resp.error.message}")
        val resultElement = resp.result ?: return ReadResourceResult()
        return json.decodeFromJsonElement(ReadResourceResult.serializer(), resultElement)
    }

    suspend fun close() {
        try {
            transport.disconnect()
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting MCP transport for $serverName", e)
        }
    }

    companion object {
        private const val TAG = "McpClient"
    }
}
