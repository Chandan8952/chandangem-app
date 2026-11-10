package com.chandankumar.chandangem.core.agent

/**
 * Intelligent Router that classifies user queries and assigns the best specialist agent.
 */
class AgentRouter {

    fun route(prompt: String): Agent {
        val lower = prompt.lowercase()

        return when {
            isTerminalQuery(lower) -> Agent.TERMINAL
            isMcpQuery(lower) -> Agent.MCP
            isArchitectureQuery(lower) -> Agent.ARCHITECT
            else -> Agent.JARVIS
        }
    }

    private fun isTerminalQuery(text: String): Boolean {
        val shellKeywords = listOf(
            "run", "exec", "shell", "bash", "terminal", "command",
            "ls", "pwd", "grep", "cat", "chmod", "mkdir", "kill", "ps", "top"
        )
        return shellKeywords.any { keyword ->
            text.contains("\\b$keyword\\b".toRegex()) || text.startsWith("$keyword ")
        }
    }

    private fun isMcpQuery(text: String): Boolean {
        val mcpKeywords = listOf("mcp", "tool", "protocol", "json-rpc", "resource", "server", "sse")
        return mcpKeywords.any { text.contains(it) }
    }

    private fun isArchitectureQuery(text: String): Boolean {
        val archKeywords = listOf("compose", "gradle", "kotlin", "architecture", "android", "jetpack", "viewmodel", "coroutine")
        return archKeywords.any { text.contains(it) }
    }
}
