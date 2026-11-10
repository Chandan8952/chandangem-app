package com.chandankumar.chandangem.core.mcp

import android.content.Context
import android.util.Log
import com.chandankumar.chandangem.core.mcp.model.JsonRpcError
import com.chandankumar.chandangem.core.mcp.model.JsonRpcNotification
import com.chandankumar.chandangem.core.mcp.model.JsonRpcRequest
import com.chandankumar.chandangem.core.mcp.model.JsonRpcResponse
import com.chandankumar.chandangem.core.terminal.TermuxEnvironment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * Standard I/O MCP Transport.
 * Launches a local command (e.g., node, python, or shell script) and communicates via stdio lines.
 */
class StdioMcpTransport(
    private val context: Context,
    private val command: List<String>,
    private val workingDir: File? = null,
    private val envVars: Map<String, String> = emptyMap(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : McpTransport {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var process: Process? = null
    private var writer: BufferedWriter? = null

    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonRpcResponse>>()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _notifications = MutableSharedFlow<JsonRpcNotification>(extraBufferCapacity = 64)
    override val notifications: Flow<JsonRpcNotification> = _notifications.asSharedFlow()

    private var readJob: Job? = null

    override suspend fun connect() = withContext(dispatcher) {
        if (_isConnected.value) return@withContext

        try {
            val processBuilder = ProcessBuilder(command).apply {
                val cwd = workingDir ?: TermuxEnvironment.getDefaultWorkingDirectory(context)
                directory(cwd)
                val env = environment()
                env.clear()
                env.putAll(TermuxEnvironment.getEnvironmentMap(context))
                env.putAll(envVars)
            }

            val proc = processBuilder.start()
            this@StdioMcpTransport.process = proc
            this@StdioMcpTransport.writer = BufferedWriter(OutputStreamWriter(proc.outputStream, Charsets.UTF_8))

            _isConnected.value = true
            startStdoutReader(proc)
            Log.i(TAG, "Stdio MCP Transport started for command: ${command.joinToString(" ")}")
        } catch (e: Exception) {
            _isConnected.value = false
            Log.e(TAG, "Failed to start stdio MCP process", e)
            throw e
        }
    }

    private fun startStdoutReader(proc: Process) {
        readJob = scope.launch {
            try {
                BufferedReader(InputStreamReader(proc.inputStream, Charsets.UTF_8)).use { reader ->
                    var line: String? = reader.readLine()
                    while (line != null) {
                        if (line.isNotBlank()) {
                            handleIncomingLine(line)
                        }
                        line = reader.readLine()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Stdout reader ended: ${e.message}")
            } finally {
                _isConnected.value = false
            }
        }
    }

    private fun handleIncomingLine(line: String) {
        try {
            val element = json.parseToJsonElement(line)
            val jsonObject = element.jsonObject

            if (jsonObject.containsKey("id") && jsonObject["id"] != null) {
                // Response
                val idStr = jsonObject["id"]!!.jsonPrimitive.content
                val response = json.decodeFromJsonElement(JsonRpcResponse.serializer(), element)
                val deferred = pendingRequests.remove(idStr)
                deferred?.complete(response)
            } else if (jsonObject.containsKey("method")) {
                // Notification
                val notification = json.decodeFromJsonElement(JsonRpcNotification.serializer(), element)
                _notifications.tryEmit(notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse incoming MCP line: $line", e)
        }
    }

    override suspend fun sendRequest(request: JsonRpcRequest): JsonRpcResponse = withContext(dispatcher) {
        if (!_isConnected.value || writer == null) {
            throw IllegalStateException("Transport not connected")
        }

        val idStr = request.id.content
        val deferred = CompletableDeferred<JsonRpcResponse>()
        pendingRequests[idStr] = deferred

        val jsonStr = json.encodeToString(JsonRpcRequest.serializer(), request)
        writer?.write(jsonStr)
        writer?.newLine()
        writer?.flush()

        try {
            deferred.await()
        } catch (e: Exception) {
            pendingRequests.remove(idStr)
            throw e
        }
    }

    override suspend fun sendNotification(notification: JsonRpcNotification) = withContext(dispatcher) {
        if (!_isConnected.value || writer == null) return@withContext
        val jsonStr = json.encodeToString(JsonRpcNotification.serializer(), notification)
        writer?.write(jsonStr)
        writer?.newLine()
        writer?.flush()
    }

    override suspend fun disconnect() = withContext(dispatcher) {
        _isConnected.value = false
        readJob?.cancel()
        pendingRequests.values.forEach {
            it.complete(
                JsonRpcResponse(
                    error = JsonRpcError(-32000, "Connection closed")
                )
            )
        }
        pendingRequests.clear()
        try {
            writer?.close()
            process?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing stdio MCP process", e)
        }
        process = null
        writer = null
    }

    companion object {
        private const val TAG = "StdioMcpTransport"
    }
}
