package com.chandankumar.chandangem.core.voice

/**
 * State representation for Voice Assistant and hands-free conversational loop.
 */
enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

/**
 * Configuration parameters for Voice Assistant.
 */
data class VoiceConfig(
    val languageCode: String = "en-US",
    val speechRate: Float = 1.05f,
    val pitch: Float = 1.0f,
    val isHandsFreeEnabled: Boolean = false,
    val silenceTimeoutMs: Long = 3000L,
    val autoResumeAfterSpeech: Boolean = true
)

/**
 * Reactive speech events dispatched by STT and TTS engines.
 */
sealed class SpeechEvent {
    data class PartialResult(val text: String) : SpeechEvent()
    data class FinalResult(val text: String) : SpeechEvent()
    data class AmplitudeChanged(val rmsDb: Float, val normalized: Float) : SpeechEvent()
    data class StateChanged(val newState: VoiceState) : SpeechEvent()
    data class Error(val code: Int, val message: String) : SpeechEvent()
}
