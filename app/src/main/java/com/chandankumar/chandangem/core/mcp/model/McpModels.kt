package com.chandankumar.chandangem.core.mcp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class McpImplementation(
    val name: String,
    val version: String
)

@Serializable
data class McpInitializeParams(
    val protocolVersion: String = "2024-11-05",
    val capabilities: McpClientCapabilities = McpClientCapabilities(),
    val clientInfo: McpImplementation = McpImplementation("ChandanGem-Client", "1.0.0")
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: McpServerCapabilities,
    val serverInfo: McpImplementation,
    val instructions: String? = null
)

@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonObject
)

@Serializable
data class ListToolsResult(
    val tools: List<McpTool> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class CallToolParams(
    val name: String,
    val arguments: JsonObject? = null
)

@Serializable
data class CallToolResult(
    val content: List<McpContent> = emptyList(),
    val isError: Boolean = false
) {
    val textContent: String
        get() = content.filter { it.type == "text" }.joinToString("\n") { it.text ?: "" }
}

@Serializable
data class McpContent(
    val type: String, // "text" | "image" | "resource"
    val text: String? = null,
    val data: String? = null,
    val mimeType: String? = null
)

@Serializable
data class McpResource(
    val uri: String,
    val name: String,
    val description: String? = null,
    val mimeType: String? = null
)

@Serializable
data class ListResourcesResult(
    val resources: List<McpResource> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class ReadResourceResult(
    val contents: List<McpResourceContent> = emptyList()
)

@Serializable
data class McpResourceContent(
    val uri: String,
    val mimeType: String? = null,
    val text: String? = null,
    val blob: String? = null
)
