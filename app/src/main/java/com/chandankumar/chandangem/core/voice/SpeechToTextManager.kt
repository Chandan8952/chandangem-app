package com.chandankumar.chandangem.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Speech-to-Text Manager wrapping Android SpeechRecognizer.
 * Supports continuous listening, live partial results, and amplitude streaming.
 */
class SpeechToTextManager(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _events = MutableSharedFlow<SpeechEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SpeechEvent> = _events.asSharedFlow()

    private val visualizer = AudioWaveformVisualizer()

    var language: String = Locale.getDefault().toLanguageTag()

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device")
            _events.tryEmit(SpeechEvent.Error(-1, "Speech recognition unavailable"))
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "onReadyForSpeech")
                    _isListening.value = true
                    _events.tryEmit(SpeechEvent.StateChanged(VoiceState.LISTENING))
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = visualizer.pushRmsDb(rmsdB)
                    _events.tryEmit(SpeechEvent.AmplitudeChanged(rmsdB, normalized))
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech")
                    _isListening.value = false
                    _events.tryEmit(SpeechEvent.StateChanged(VoiceState.PROCESSING))
                }

                override fun onError(error: Int) {
                    val errorMessage = getErrorDescription(error)
                    Log.w(TAG, "SpeechRecognizer error: $error ($errorMessage)")
                    _isListening.value = false
                    _events.tryEmit(SpeechEvent.Error(error, errorMessage))
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val resultText = matches?.firstOrNull() ?: ""
                    Log.d(TAG, "onResults: $resultText")
                    _isListening.value = false
                    _partialText.value = ""
                    if (resultText.isNotBlank()) {
                        _events.tryEmit(SpeechEvent.FinalResult(resultText))
                    } else {
                        _events.tryEmit(SpeechEvent.StateChanged(VoiceState.IDLE))
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val currentText = matches?.firstOrNull() ?: ""
                    if (currentText.isNotBlank()) {
                        _partialText.value = currentText
                        _events.tryEmit(SpeechEvent.PartialResult(currentText))
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Starts listening for user speech on Main thread.
     */
    fun startListening() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                _isListening.value = true
                Log.d(TAG, "Started listening with language $language")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                _events.tryEmit(SpeechEvent.Error(-2, e.localizedMessage ?: "Failed to start listening"))
            }
        }
    }

    /**
     * Stops active speech recognition.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _isListening.value = false
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping speech recognition", e)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                _isListening.value = false
                _partialText.value = ""
            } catch (e: Exception) {
                Log.w(TAG, "Error canceling speech recognition", e)
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying speech recognizer", e)
            }
        }
    }

    private fun getErrorDescription(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions (RECORD_AUDIO required)"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input timeout"
        else -> "Speech recognition error #$errorCode"
    }

    companion object {
        private const val TAG = "SpeechToTextManager"
    }
}
