package com.chandankumar.chandangem.core.mcp

import android.util.Log
import com.chandankumar.chandangem.core.mcp.model.JsonRpcError
import com.chandankumar.chandangem.core.mcp.model.JsonRpcNotification
import com.chandankumar.chandangem.core.mcp.model.JsonRpcRequest
import com.chandankumar.chandangem.core.mcp.model.JsonRpcResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Server-Sent Events (SSE) + HTTP POST MCP Transport.
 * Implements the standard MCP remote web transport specification.
 */
class SseMcpTransport(
    private val sseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .build(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : McpTransport {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _notifications = MutableSharedFlow<JsonRpcNotification>(extraBufferCapacity = 64)
    override val notifications: Flow<JsonRpcNotification> = _notifications.asSharedFlow()

    private var eventSource: EventSource? = null
    private var postEndpointUrl: String? = null
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonRpcResponse>>()
    private var connectionDeferred = CompletableDeferred<Boolean>()

    override suspend fun connect(): Unit = withContext(dispatcher) {
        if (_isConnected.value) return@withContext

        connectionDeferred = CompletableDeferred()
        val request = Request.Builder()
            .url(sseUrl)
            .header("Accept", "text/event-stream")
            .build()

        val factory = EventSources.createFactory(client)
        eventSource = factory.newEventSource(request, object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.i(TAG, "SSE connection opened to $sseUrl")
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                Log.d(TAG, "SSE event [$type]: $data")
                when (type) {
                    "endpoint" -> {
                        // MCP SSE initial message contains the relative or absolute POST URL
                        postEndpointUrl = resolvePostUrl(data)
                        _isConnected.value = true
                        connectionDeferred.complete(true)
                    }
                    "message", null -> {
                        handleIncomingPayload(data)
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.i(TAG, "SSE connection closed")
                _isConnected.value = false
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE failure: ${t?.message}, response code: ${response?.code}")
                _isConnected.value = false
                if (!connectionDeferred.isCompleted) {
                    connectionDeferred.complete(false)
                }
            }
        })

        val connected = connectionDeferred.await()
        if (!connected) {
            throw IllegalStateException("Failed to establish SSE MCP transport to $sseUrl")
        }
    }

    private fun resolvePostUrl(data: String): String {
        return if (data.startsWith("http://") || data.startsWith("https://")) {
            data.trim()
        } else {
            // Relative path
            val base = sseUrl.substringBeforeLast('/')
            "$base/${data.trim().removePrefix("/")}"
        }
    }

    private fun handleIncomingPayload(data: String) {
        try {
            val element = json.parseToJsonElement(data)
            val jsonObject = element.jsonObject

            if (jsonObject.containsKey("id") && jsonObject["id"] != null) {
                val idStr = jsonObject["id"]!!.jsonPrimitive.content
                val response = json.decodeFromJsonElement(JsonRpcResponse.serializer(), element)
                val deferred = pendingRequests.remove(idStr)
                deferred?.complete(response)
            } else if (jsonObject.containsKey("method")) {
                val notification = json.decodeFromJsonElement(JsonRpcNotification.serializer(), element)
                _notifications.tryEmit(notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing incoming SSE payload", e)
        }
    }

    override suspend fun sendRequest(request: JsonRpcRequest): JsonRpcResponse = withContext(dispatcher) {
        val targetUrl = postEndpointUrl ?: throw IllegalStateException("Post endpoint URL not resolved")
        val idStr = request.id.content
        val deferred = CompletableDeferred<JsonRpcResponse>()
        pendingRequests[idStr] = deferred

        try {
            val body = json.encodeToString(JsonRpcRequest.serializer(), request)
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url(targetUrl)
                .post(body)
                .build()

            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("HTTP POST to $targetUrl failed with code ${response.code}")
                }
            }

            deferred.await()
        } catch (e: Exception) {
            pendingRequests.remove(idStr)
            throw e
        }
    }

    override suspend fun sendNotification(notification: JsonRpcNotification) = withContext(dispatcher) {
        val targetUrl = postEndpointUrl ?: return@withContext
        val body = json.encodeToString(JsonRpcNotification.serializer(), notification)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(targetUrl)
            .post(body)
            .build()

        client.newCall(httpRequest).execute().close()
    }

    override suspend fun disconnect() = withContext(dispatcher) {
        _isConnected.value = false
        eventSource?.cancel()
        eventSource = null
        pendingRequests.values.forEach {
            it.complete(JsonRpcResponse(error = JsonRpcError(-32000, "Transport disconnected")))
        }
        pendingRequests.clear()
    }

    companion object {
        private const val TAG = "SseMcpTransport"
    }
}
