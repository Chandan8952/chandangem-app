package com.chandankumar.chandangem.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chandankumar.chandangem.core.agent.Agent
import com.chandankumar.chandangem.core.agent.MultiAgentCoordinator
import com.chandankumar.chandangem.core.voice.VoiceAssistantManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val coordinator: MultiAgentCoordinator,
    private val voiceManager: VoiceAssistantManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    text = "Welcome to ChandanGem! I am Jarvis, your lead AI orchestrator. " +
                            "I can execute shell commands via the Terminal Specialist, query MCP servers, " +
                            "or interact hands-free with voice.",
                    isUser = false,
                    agent = Agent.JARVIS
                )
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeVoiceEngine()
    }

    private fun observeVoiceEngine() {
        viewModelScope.launch {
            voiceManager.voiceState.collect { state ->
                _uiState.update { it.copy(voiceState = state) }
            }
        }

        viewModelScope.launch {
            voiceManager.amplitude.collect { amp ->
                _uiState.update { it.copy(voiceAmplitude = amp) }
            }
        }

        viewModelScope.launch {
            voiceManager.isHandsFreeEnabled.collect { handsFree ->
                _uiState.update { it.copy(isHandsFreeEnabled = handsFree) }
            }
        }

        viewModelScope.launch {
            voiceManager.sttManager.partialText.collect { partial ->
                _uiState.update { it.copy(partialSpokenText = partial) }
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputDraft = text) }
    }

    fun selectAgent(agent: Agent) {
        _uiState.update { it.copy(activeAgent = agent) }
    }

    fun sendMessage(customPrompt: String? = null) {
        val prompt = customPrompt ?: _uiState.value.inputDraft
        if (prompt.isBlank()) return

        val userMessage = ChatMessage(text = prompt, isUser = true)
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputDraft = "",
                isProcessing = true
            )
        }

        viewModelScope.launch {
            try {
                val response = coordinator.processUserMessage(prompt)
                val assistantMessage = ChatMessage(
                    text = response.messageText,
                    isUser = false,
                    agent = response.primaryAgent,
                    toolCalls = response.toolCalls
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isProcessing = false
                    )
                }

                // If hands-free is enabled or user asked via voice, speak it
                if (_uiState.value.isHandsFreeEnabled) {
                    voiceManager.speakResponse(response.messageText)
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = "Coordination error: ${e.localizedMessage ?: "Unknown error"}",
                    isUser = false,
                    agent = Agent.JARVIS
                )
                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isProcessing = false
                    )
                }
            }
        }
    }

    fun toggleListening() {
        voiceManager.toggleListening()
    }

    fun toggleHandsFree() {
        voiceManager.toggleHandsFreeMode()
    }

    fun speakText(text: String) {
        voiceManager.ttsManager.speak(text)
    }

    fun openTerminalSheet(open: Boolean) {
        _uiState.update { it.copy(isTerminalSheetOpen = open) }
    }

    fun openMcpSheet(open: Boolean) {
        _uiState.update { it.copy(isMcpSheetOpen = open) }
    }
}
