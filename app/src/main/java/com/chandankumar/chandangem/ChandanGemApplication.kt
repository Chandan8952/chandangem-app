package com.chandankumar.chandangem

import android.app.Application
import android.util.Log
import com.chandankumar.chandangem.core.agent.MultiAgentCoordinator
import com.chandankumar.chandangem.core.mcp.McpServerRegistry
import com.chandankumar.chandangem.core.terminal.TerminalManager
import com.chandankumar.chandangem.core.voice.VoiceAssistantManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * ChandanGem Application Class
 * Owned and designed by Chandan Kumar.
 *
 * Bootstraps core singleton services:
 * - Terminal and Shell Execution Manager
 * - MCP Protocol Server Registry
 * - Voice Assistant & Hands-Free Audio Engine
 * - Multi-Agent Collaborative Coordinator
 */
class ChandanGemApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var terminalManager: TerminalManager
        private set

    lateinit var mcpServerRegistry: McpServerRegistry
        private set

    lateinit var voiceAssistantManager: VoiceAssistantManager
        private set

    lateinit var multiAgentCoordinator: MultiAgentCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "Initializing ChandanGem Application Suite (Owner: Chandan Kumar)...")

        // 1. Initialize Terminal & Shell Manager
        terminalManager = TerminalManager(this)

        // 2. Initialize MCP Server Registry
        mcpServerRegistry = McpServerRegistry(this, terminalManager)

        // 3. Initialize Voice Assistant Manager
        voiceAssistantManager = VoiceAssistantManager(this)

        // 4. Initialize Multi-Agent Coordinator
        multiAgentCoordinator = MultiAgentCoordinator(
            terminalManager = terminalManager,
            mcpRegistry = mcpServerRegistry,
            voiceManager = voiceAssistantManager
        )

        Log.i(TAG, "ChandanGem core infrastructure successfully initialized.")
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationScope.cancel()
        voiceAssistantManager.shutdown()
        terminalManager.shutdown()
    }

    companion object {
        const val TAG = "ChandanGemApp"

        @Volatile
        private var instance: ChandanGemApplication? = null

        fun getInstance(): ChandanGemApplication =
            instance ?: throw IllegalStateException("ChandanGemApplication not yet initialized")
    }
}
