package com.chandankumar.chandangem.core.mcp

import com.chandankumar.chandangem.core.mcp.model.JsonRpcNotification
import com.chandankumar.chandangem.core.mcp.model.JsonRpcRequest
import com.chandankumar.chandangem.core.mcp.model.JsonRpcResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport interface for communicating with Model Context Protocol servers.
 */
interface McpTransport {
    val isConnected: StateFlow<Boolean>
    val notifications: Flow<JsonRpcNotification>

    suspend fun connect()
    suspend fun sendRequest(request: JsonRpcRequest): JsonRpcResponse
    suspend fun sendNotification(notification: JsonRpcNotification)
    suspend fun disconnect()
}
