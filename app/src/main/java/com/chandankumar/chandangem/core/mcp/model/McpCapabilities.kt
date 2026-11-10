package com.chandankumar.chandangem.core.mcp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Model Context Protocol capabilities declaration.
 */
@Serializable
data class McpClientCapabilities(
    val roots: JsonObject? = null,
    val sampling: JsonObject? = null,
    val experimental: JsonObject? = null
)

@Serializable
data class McpServerCapabilities(
    val tools: JsonObject? = null,
    val resources: JsonObject? = null,
    val prompts: JsonObject? = null,
    val logging: JsonObject? = null,
    val experimental: JsonObject? = null
)
