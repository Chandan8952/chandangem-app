package com.chandankumar.chandangem.ui.mcp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandankumar.chandangem.core.mcp.McpServerRegistry
import com.chandankumar.chandangem.core.mcp.RegisteredServer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class McpViewModel(
    private val mcpRegistry: McpServerRegistry
) : ViewModel() {

    val servers: StateFlow<List<RegisteredServer>> = mcpRegistry.servers
        .map { it.values.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun connectServer(serverId: String) {
        mcpRegistry.connectServer(serverId)
    }

    fun testTool(toolName: String) {
        viewModelScope.launch {
            val args = buildJsonObject {
                put("command", "echo 'Testing MCP tool execution'")
            }
            mcpRegistry.executeTool(toolName, args)
        }
    }
}
