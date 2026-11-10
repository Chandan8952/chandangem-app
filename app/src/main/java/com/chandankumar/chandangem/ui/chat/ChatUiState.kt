package com.chandankumar.chandangem.ui.chat

import com.chandankumar.chandangem.core.agent.Agent
import com.chandankumar.chandangem.core.agent.ToolCallRecord
import com.chandankumar.chandangem.core.voice.VoiceState
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val agent: Agent? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val toolCalls: List<ToolCallRecord> = emptyList(),
    val isSpoken: Boolean = false
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputDraft: String = "",
    val activeAgent: Agent = Agent.JARVIS,
    val voiceState: VoiceState = VoiceState.IDLE,
    val voiceAmplitude: Float = 0.05f,
    val isHandsFreeEnabled: Boolean = false,
    val isTerminalSheetOpen: Boolean = false,
    val isMcpSheetOpen: Boolean = false,
    val isProcessing: Boolean = false,
    val partialSpokenText: String = ""
)
