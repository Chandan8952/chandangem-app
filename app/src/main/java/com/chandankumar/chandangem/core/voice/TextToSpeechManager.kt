package com.chandankumar.chandangem.core.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Text-to-Speech Manager wrapping Android TextToSpeech engine.
 * Provides coroutine suspend-based speech synthesis and reactive speaking state.
 */
class TextToSpeechManager(
    private val context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language US not supported or missing data")
                isInitialized = false
                onInitComplete?.invoke(false)
            } else {
                isInitialized = true
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(attrs)
                tts?.setSpeechRate(1.05f)
                tts?.setPitch(1.0f)
                setupProgressListener()
                Log.i(TAG, "TTS engine successfully initialized")
                onInitComplete?.invoke(true)
            }
        } else {
            Log.e(TAG, "Failed to initialize TTS engine. Status: $status")
            isInitialized = false
            onInitComplete?.invoke(false)
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
                Log.d(TAG, "TTS started utterance: $utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                Log.d(TAG, "TTS completed utterance: $utteranceId")
                onSpeechDoneCallback?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                Log.e(TAG, "TTS error on utterance: $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
                Log.e(TAG, "TTS error on utterance: $utteranceId, code: $errorCode")
            }
        })
    }

    /**
     * Speaks the given text asynchronously.
     */
    fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_FLUSH,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not initialized, cannot speak")
            onDone?.invoke()
            return
        }

        this.onSpeechDoneCallback = onDone
        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, queueMode, params, utteranceId)
    }

    /**
     * Suspend version: suspends until speech synthesis completes.
     */
    suspend fun speakSuspend(text: String): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isInitialized || tts == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val originalListener = tts

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(id: String?) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    setupProgressListener() // Restore default listener
                    if (continuation.isActive) continuation.resume(true)
                }
            }

            override fun onError(id: String?) {
                if (id == utteranceId) {
                    _isSpeaking.value = false
                    setupProgressListener()
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        })

        continuation.invokeOnCancellation {
            stop()
            setupProgressListener()
        }

        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            setupProgressListener()
            if (continuation.isActive) continuation.resume(false)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun setSpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    companion object {
        private const val TAG = "TextToSpeechManager"
    }
}
