package com.chandankumar.chandangem.core.agent

/**
 * Metadata and configuration for an autonomous Agent.
 */
data class Agent(
    val id: String,
    val type: AgentType,
    val name: String,
    val title: String,
    val colorHex: Long,
    val systemPrompt: String
) {
    companion object {
        val JARVIS = Agent(
            id = "agent_jarvis",
            type = AgentType.JARVIS_COORDINATOR,
            name = "Jarvis",
            title = "Lead AI Coordinator",
            colorHex = 0xFF00E5FF, // Neon Cyan
            systemPrompt = "You are Jarvis, the primary orchestrator of ChandanGem. Coordinate tasks and synthesize answers."
        )

        val TERMINAL = Agent(
            id = "agent_terminal",
            type = AgentType.TERMINAL_SPECIALIST,
            name = "Shell Specialist",
            title = "Termux & Linux Engineer",
            colorHex = 0xFF10B981, // Emerald Green
            systemPrompt = "You are the Terminal Specialist. You generate precise bash/sh commands and analyze shell outputs."
        )

        val MCP = Agent(
            id = "agent_mcp",
            type = AgentType.MCP_TOOL_DISPATCHER,
            name = "MCP Dispatcher",
            title = "Protocol & Tool Connector",
            colorHex = 0xFFA855F7, // Cyber Purple
            systemPrompt = "You are the MCP Dispatcher. You query Model Context Protocol servers and execute tools."
        )

        val ARCHITECT = Agent(
            id = "agent_architect",
            type = AgentType.CODE_ARCHITECT,
            name = "Code Architect",
            title = "Android & Kotlin Lead",
            colorHex = 0xFFF59E0B, // Amber
            systemPrompt = "You are the Code Architect. You design clean Android Kotlin & Jetpack Compose solutions."
        )

        val ALL_AGENTS = listOf(JARVIS, TERMINAL, MCP, ARCHITECT)
    }
}
