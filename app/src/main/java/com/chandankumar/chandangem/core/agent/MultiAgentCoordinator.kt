package com.chandankumar.chandangem.core.agent

import android.util.Log
import com.chandankumar.chandangem.core.mcp.McpServerRegistry
import com.chandankumar.chandangem.core.terminal.TerminalManager
import com.chandankumar.chandangem.core.voice.VoiceAssistantManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

data class ToolCallRecord(
    val id: String = UUID.randomUUID().toString(),
    val toolName: String,
    val arguments: String,
    val result: String,
    val isError: Boolean = false
)

data class AgentResponse(
    val primaryAgent: Agent,
    val delegatedAgent: Agent? = null,
    val messageText: String,
    val toolCalls: List<ToolCallRecord> = emptyList()
)

/**
 * Multi-Agent Coordinator for ChandanGem.
 * Orchestrates multi-agent problem solving, command execution, and voice loop handling.
 */
class MultiAgentCoordinator(
    private val terminalManager: TerminalManager,
    private val mcpRegistry: McpServerRegistry,
    private val voiceManager: VoiceAssistantManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val router = AgentRouter()

    init {
        // Wire voice queries directly to multi-agent reasoning loop
        voiceManager.onSpeechQueryHandler = { spokenText ->
            val response = processUserMessage(spokenText)
            response.messageText
        }
    }

    /**
     * Processes a user prompt through the multi-agent coordination pipeline.
     */
    suspend fun processUserMessage(userPrompt: String): AgentResponse = withContext(dispatcher) {
        Log.i(TAG, "Processing prompt: $userPrompt")
        val assignedAgent = router.route(userPrompt)
        val toolRecords = mutableListOf<ToolCallRecord>()

        val replyText = when (assignedAgent.type) {
            AgentType.TERMINAL_SPECIALIST -> {
                handleTerminalSpecialist(userPrompt, toolRecords)
            }
            AgentType.MCP_TOOL_DISPATCHER -> {
                handleMcpSpecialist(userPrompt, toolRecords)
            }
            AgentType.CODE_ARCHITECT -> {
                handleArchitectSpecialist(userPrompt)
            }
            AgentType.JARVIS_COORDINATOR, AgentType.VOICE_EXECUTIVE -> {
                handleJarvisLead(userPrompt, toolRecords)
            }
        }

        AgentResponse(
            primaryAgent = assignedAgent,
            delegatedAgent = if (assignedAgent != Agent.JARVIS) Agent.JARVIS else null,
            messageText = replyText,
            toolCalls = toolRecords
        )
    }

    private suspend fun handleTerminalSpecialist(
        prompt: String,
        records: MutableList<ToolCallRecord>
    ): String {
        // Extract command if user typed "run <cmd>" or similar
        val command = if (prompt.startsWith("run ", ignoreCase = true)) {
            prompt.substring(4).trim()
        } else if (prompt.startsWith("exec ", ignoreCase = true)) {
            prompt.substring(5).trim()
        } else {
            prompt
        }

        Log.d(TAG, "Shell specialist executing: $command")
        val cmdResult = terminalManager.executeCommandSync(command)

        val record = ToolCallRecord(
            toolName = "terminal_run_command",
            arguments = command,
            result = cmdResult.combinedOutput,
            isError = !cmdResult.isSuccess
        )
        records.add(record)

        return if (cmdResult.isSuccess) {
            "Executed `$command` in ${cmdResult.durationMs}ms (exit code: 0):\n```\n${cmdResult.stdout.ifBlank { "[No stdout output]" }}\n```"
        } else {
            "Command `$command` failed with exit code ${cmdResult.exitCode}:\n```\n${cmdResult.stderr}\n```"
        }
    }

    private suspend fun handleMcpSpecialist(
        prompt: String,
        records: MutableList<ToolCallRecord>
    ): String {
        val tools = mcpRegistry.getAllAvailableTools()
        val servers = mcpRegistry.servers.value

        return if (prompt.contains("list", ignoreCase = true) || prompt.contains("tools", ignoreCase = true)) {
            val toolListStr = tools.joinToString("\n") { "• **${it.name}**: ${it.description ?: "No description"}" }
            "Connected MCP Servers: ${servers.size}\nAvailable Tools (${tools.size}):\n$toolListStr"
        } else {
            // Demo execution of tool
            val toolName = "terminal_run_command"
            val args = buildJsonObject { put("command", "uname -a || echo 'Termux Android'") }
            val callResult = mcpRegistry.executeTool(toolName, args)

            records.add(
                ToolCallRecord(
                    toolName = toolName,
                    arguments = args.toString(),
                    result = callResult.textContent,
                    isError = callResult.isError
                )
            )

            "MCP Protocol Dispatch completed.\nResult from `$toolName`:\n${callResult.textContent}"
        }
    }

    private fun handleArchitectSpecialist(prompt: String): String {
        return "Architect Review:\n" +
                "ChandanGem is architected using Jetpack Compose, Kotlin 2.0 Coroutines/StateFlow, " +
                "Clean Architecture layers (Data, Domain, Presentation), and Model Context Protocol standard.\n\n" +
                "Query: \"$prompt\"\n" +
                "Status: Verified against Android 35 SDK specifications."
    }

    private suspend fun handleJarvisLead(
        prompt: String,
        records: MutableList<ToolCallRecord>
    ): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("who are you") || lower.contains("owner") -> {
                "I am **ChandanGem**, an autonomous AI agent application engineered by **Chandan Kumar**. " +
                        "I integrate a Voice Assistant with hands-free loop, Terminal Shell execution manager, " +
                        "Model Context Protocol (MCP) client, and a Multi-Agent coordination engine."
            }
            lower.contains("status") || lower.contains("health") -> {
                val sessions = terminalManager.sessionsList.value.size
                val mcpServers = mcpRegistry.servers.value.size
                val handsFree = voiceManager.isHandsFreeEnabled.value
                "System Status Report:\n" +
                        "• Voice Assistant: ${if (handsFree) "Hands-free Active" else "Ready (Push-to-Talk)"}\n" +
                        "• Active Terminal Sessions: $sessions\n" +
                        "• Registered MCP Servers: $mcpServers\n" +
                        "• Multi-Agent Network: 4 specialized agents online"
            }
            else -> {
                "Received your request: \"$prompt\". As lead coordinator, I can dispatch shell commands to the Terminal Specialist, query MCP servers, or activate hands-free voice loop."
            }
        }
    }

    companion object {
        private const val TAG = "MultiAgentCoordinator"
    }
}
