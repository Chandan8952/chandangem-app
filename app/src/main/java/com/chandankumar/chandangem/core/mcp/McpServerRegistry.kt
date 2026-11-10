package com.chandankumar.chandangem.core.mcp

import android.content.Context
import android.util.Log
import com.chandankumar.chandangem.core.mcp.model.CallToolResult
import com.chandankumar.chandangem.core.mcp.model.McpContent
import com.chandankumar.chandangem.core.mcp.model.McpTool
import com.chandankumar.chandangem.core.terminal.TerminalManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.concurrent.ConcurrentHashMap

enum class McpServerStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class RegisteredServer(
    val id: String,
    val name: String,
    val description: String,
    val isSse: Boolean,
    val endpointOrCommand: String,
    val status: McpServerStatus = McpServerStatus.DISCONNECTED,
    val tools: List<McpTool> = emptyList(),
    val errorMessage: String? = null
)

/**
 * Registry and dispatcher for Model Context Protocol servers in ChandanGem.
 */
class McpServerRegistry(
    private val context: Context,
    private val terminalManager: TerminalManager,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    private val clients = ConcurrentHashMap<String, McpClient>()
    private val _servers = MutableStateFlow<Map<String, RegisteredServer>>(emptyMap())
    val servers: StateFlow<Map<String, RegisteredServer>> = _servers.asStateFlow()

    init {
        registerBuiltInServers()
    }

    private fun registerBuiltInServers() {
        val defaultServers = listOf(
            RegisteredServer(
                id = "builtin-terminal",
                name = "Terminal & Shell Automation",
                description = "Native local shell execution via Termux bash/sh",
                isSse = false,
                endpointOrCommand = "native:terminal"
            ),
            RegisteredServer(
                id = "gemini-api-docs",
                name = "Gemini API Documentation",
                description = "Documentation and SDK search tool provider",
                isSse = true,
                endpointOrCommand = "http://127.0.0.1:8765/sse"
            )
        )

        _servers.value = defaultServers.associateBy { it.id }
    }

    /**
     * Connects to a registered server and retrieves its tool catalog.
     */
    fun connectServer(serverId: String) {
        val server = _servers.value[serverId] ?: return
        updateServerStatus(serverId, McpServerStatus.CONNECTING)

        scope.launch {
            try {
                if (server.endpointOrCommand == "native:terminal") {
                    // Native built-in tool provider
                    val nativeTools = listOf(
                        McpTool(
                            name = "terminal_run_command",
                            description = "Runs a shell command in Termux or Android environment",
                            inputSchema = buildJsonObject {
                                put("type", "object")
                            }
                        )
                    )
                    updateServer(serverId) {
                        it.copy(
                            status = McpServerStatus.CONNECTED,
                            tools = nativeTools,
                            errorMessage = null
                        )
                    }
                    Log.i(TAG, "Native terminal MCP server registered.")
                    return@launch
                }

                val transport: McpTransport = if (server.isSse) {
                    SseMcpTransport(server.endpointOrCommand)
                } else {
                    val cmd = server.endpointOrCommand.split(" ")
                    StdioMcpTransport(context, cmd)
                }

                val client = McpClient(server.id, server.name, transport)
                client.initialize()
                val tools = client.listTools()

                clients[serverId] = client
                updateServer(serverId) {
                    it.copy(
                        status = McpServerStatus.CONNECTED,
                        tools = tools,
                        errorMessage = null
                    )
                }
                Log.i(TAG, "Server ${server.name} connected with ${tools.size} tools.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect MCP server ${server.name}", e)
                updateServer(serverId) {
                    it.copy(
                        status = McpServerStatus.ERROR,
                        errorMessage = e.localizedMessage ?: "Connection failure"
                    )
                }
            }
        }
    }

    /**
     * Dispatches a tool call across registered servers.
     */
    suspend fun executeTool(toolName: String, arguments: JsonObject?): CallToolResult {
        // Special case: Native terminal tool
        if (toolName == "terminal_run_command") {
            val command = arguments?.get("command")?.jsonPrimitive?.content ?: "echo 'No command specified'"
            val result = terminalManager.executeCommandSync(command)
            return CallToolResult(
                content = listOf(
                    McpContent(type = "text", text = result.combinedOutput)
                ),
                isError = !result.isSuccess
            )
        }

        // Search in connected MCP clients
        for ((_, client) in clients) {
            try {
                val tools = client.listTools()
                if (tools.any { it.name == toolName }) {
                    return client.callTool(toolName, arguments)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Tool check failed on server ${client.serverName}: ${e.message}")
            }
        }

        return CallToolResult(
            content = listOf(McpContent(type = "text", text = "Tool '$toolName' not found on any connected server")),
            isError = true
        )
    }

    private fun updateServerStatus(serverId: String, status: McpServerStatus) {
        updateServer(serverId) { it.copy(status = status) }
    }

    private fun updateServer(serverId: String, transform: (RegisteredServer) -> RegisteredServer) {
        val current = _servers.value.toMutableMap()
        current[serverId]?.let {
            current[serverId] = transform(it)
            _servers.value = current
        }
    }

    fun getAllAvailableTools(): List<McpTool> {
        return _servers.value.values.flatMap { it.tools }
    }

    companion object {
        private const val TAG = "McpServerRegistry"
    }
}
