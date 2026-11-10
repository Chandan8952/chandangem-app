package com.chandankumar.chandangem.core.voice

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Voice Assistant Manager providing full-duplex hands-free voice loop.
 * Coordinates Speech-to-Text (STT) and Text-to-Speech (TTS), audio amplitude streaming,
 * and automatic turn-taking for continuous voice interactions.
 */
class VoiceAssistantManager(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) {

    val sttManager = SpeechToTextManager(context)
    val ttsManager = TextToSpeechManager(context)

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _amplitude = MutableStateFlow(0.05f)
    val amplitude: StateFlow<Float> = _amplitude.asStateFlow()

    private val _isHandsFreeEnabled = MutableStateFlow(false)
    val isHandsFreeEnabled: StateFlow<Boolean> = _isHandsFreeEnabled.asStateFlow()

    private var voiceLoopJob: Job? = null
    var onSpeechQueryHandler: (suspend (String) -> String)? = null

    init {
        observeSttEvents()
    }

    private fun observeSttEvents() {
        scope.launch {
            sttManager.events.collect { event ->
                when (event) {
                    is SpeechEvent.StateChanged -> {
                        if (_voiceState.value != VoiceState.SPEAKING) {
                            _voiceState.value = event.newState
                        }
                    }
                    is SpeechEvent.AmplitudeChanged -> {
                        _amplitude.value = event.normalized
                    }
                    is SpeechEvent.PartialResult -> {
                        _voiceState.value = VoiceState.LISTENING
                    }
                    is SpeechEvent.FinalResult -> {
                        handleFinalSpeech(event.text)
                    }
                    is SpeechEvent.Error -> {
                        Log.w(TAG, "STT error: ${event.message}")
                        if (_isHandsFreeEnabled.value && _voiceState.value != VoiceState.SPEAKING) {
                            // Retry listening after short backoff in hands-free mode
                            delay(1000)
                            if (_isHandsFreeEnabled.value) {
                                sttManager.startListening()
                            }
                        } else {
                            _voiceState.value = VoiceState.IDLE
                        }
                    }
                }
            }
        }
    }

    private fun handleFinalSpeech(text: String) {
        if (text.isBlank()) {
            if (_isHandsFreeEnabled.value) {
                sttManager.startListening()
            } else {
                _voiceState.value = VoiceState.IDLE
            }
            return
        }

        _voiceState.value = VoiceState.PROCESSING
        scope.launch {
            try {
                Log.d(TAG, "Processing spoken input: $text")
                val response = onSpeechQueryHandler?.invoke(text) ?: "Received: $text"

                // Assistant speaks the response
                speakResponse(response)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing spoken query", e)
                speakResponse("An error occurred while processing your request.")
            }
        }
    }

    /**
     * Speaks the assistant response and triggers hands-free loop next turn if enabled.
     */
    fun speakResponse(response: String) {
        _voiceState.value = VoiceState.SPEAKING
        ttsManager.speak(response) {
            _voiceState.value = VoiceState.IDLE
            if (_isHandsFreeEnabled.value) {
                scope.launch {
                    delay(500) // Brief conversational pause before listening again
                    if (_isHandsFreeEnabled.value) {
                        Log.d(TAG, "Hands-free loop: Resuming listening...")
                        sttManager.startListening()
                    }
                }
            }
        }
    }

    /**
     * Toggles hands-free continuous loop mode.
     */
    fun toggleHandsFreeMode(enabled: Boolean? = null) {
        val target = enabled ?: !_isHandsFreeEnabled.value
        _isHandsFreeEnabled.value = target
        Log.i(TAG, "Hands-free mode set to: $target")

        if (target) {
            if (_voiceState.value == VoiceState.IDLE) {
                sttManager.startListening()
            }
        } else {
            sttManager.stopListening()
            ttsManager.stop()
            _voiceState.value = VoiceState.IDLE
        }
    }

    /**
     * Manual push-to-talk start/stop trigger.
     */
    fun toggleListening() {
        if (_voiceState.value == VoiceState.LISTENING) {
            sttManager.stopListening()
            _voiceState.value = VoiceState.IDLE
        } else {
            if (_voiceState.value == VoiceState.SPEAKING) {
                // Barge-in: interrupt speech immediately
                ttsManager.stop()
            }
            sttManager.startListening()
        }
    }

    fun stopAll() {
        _isHandsFreeEnabled.value = false
        sttManager.stopListening()
        ttsManager.stop()
        _voiceState.value = VoiceState.IDLE
    }

    fun shutdown() {
        stopAll()
        sttManager.destroy()
        ttsManager.shutdown()
    }

    companion object {
        private const val TAG = "VoiceAssistantManager"
    }
}
