package com.chandankumar.chandangem.core.mcp

import com.chandankumar.chandangem.core.mcp.model.CallToolResult
import com.chandankumar.chandangem.core.mcp.model.JsonRpcError
import com.chandankumar.chandangem.core.mcp.model.JsonRpcRequest
import com.chandankumar.chandangem.core.mcp.model.JsonRpcResponse
import com.chandankumar.chandangem.core.mcp.model.McpContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testJsonRpcRequest_serialization() {
        val request = JsonRpcRequest(
            id = JsonPrimitive("42"),
            method = "ping"
        )
        val encoded = json.encodeToString(JsonRpcRequest.serializer(), request)
        assertTrue(encoded.contains("\"jsonrpc\":\"2.0\""))
        assertTrue(encoded.contains("\"id\":\"42\""))
        assertTrue(encoded.contains("\"method\":\"ping\""))

        val decoded = json.decodeFromString(JsonRpcRequest.serializer(), encoded)
        assertEquals("42", decoded.id.content)
        assertEquals("ping", decoded.method)
    }

    @Test
    fun testJsonRpcResponse_withError() {
        val response = JsonRpcResponse(
            id = JsonPrimitive("1"),
            error = JsonRpcError(code = -32601, message = "Method not found")
        )
        val encoded = json.encodeToString(JsonRpcResponse.serializer(), response)
        val decoded = json.decodeFromString(JsonRpcResponse.serializer(), encoded)

        assertNotNull(decoded.error)
        assertEquals(-32601, decoded.error?.code)
        assertEquals("Method not found", decoded.error?.message)
    }

    @Test
    fun testCallToolResult_textContentHelper() {
        val result = CallToolResult(
            content = listOf(
                McpContent(type = "text", text = "Line 1"),
                McpContent(type = "text", text = "Line 2")
            ),
            isError = false
        )
        assertEquals("Line 1\nLine 2", result.textContent)
    }
}
