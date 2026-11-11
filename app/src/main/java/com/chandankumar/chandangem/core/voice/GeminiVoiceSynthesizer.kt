package com.chandankumar.chandangem.core.voice

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Gemini Live Neural Audio Voice Synthesizer.
 * Provides hyper-realistic, human-like voice responses using Google's Gemini 2.0 Flash / Live voices
 * (Puck, Charon, Aoede, Fenrir, Kore) replacing robotic Android TTS.
 */
class GeminiVoiceSynthesizer(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Synthesizes audio using Gemini API neural audio output.
     *
     * @param text Text to speak.
     * @param apiKey Gemini API Key.
     * @param voiceName Prebuilt voice: "Charon", "Puck", "Aoede", "Fenrir", or "Kore".
     * @param model Gemini model (default: "gemini-2.0-flash").
     */
    suspend fun synthesizeSpeech(
        text: String,
        apiKey: String,
        voiceName: String = "Charon",
        model: String = "gemini-2.0-flash"
    ): ByteArray? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || text.isBlank()) return@withContext null

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", text)
                    ))
                ))
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    put("speechConfig", JSONObject().put(
                        "voiceConfig", JSONObject().put(
                            "prebuiltVoiceConfig", JSONObject().put("voiceName", voiceName)
                        )
                    ))
                })
            }.toString()

            val request = Request.Builder()
                .url(url)
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val resString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini audio synthesis failed (${response.code}): $resString")
                    return@withContext null
                }

                val json = JSONObject(resString)
                val candidates = json.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                for (i in 0 until (parts?.length() ?: 0)) {
                    val part = parts?.optJSONObject(i)
                    val inlineData = part?.optJSONObject("inlineData")
                    val base64Data = inlineData?.optString("data")
                    if (!base64Data.isNullOrBlank()) {
                        return@withContext Base64.decode(base64Data, Base64.DEFAULT)
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Gemini speech synthesis", e)
            null
        }
    }

    /**
     * Plays the synthesized audio bytes using Android MediaPlayer.
     */
    suspend fun playAudio(audioBytes: ByteArray, onComplete: (() -> Unit)? = null) = withContext(Dispatchers.Main) {
        try {
            stopAudio()
            val tempFile = File.createTempFile("gemini_voice_", ".mp3", context.cacheDir)
            withContext(Dispatchers.IO) {
                FileOutputStream(tempFile).use { it.write(audioBytes) }
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    tempFile.delete()
                    onComplete?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: $what, $extra")
                    mp.release()
                    mediaPlayer = null
                    tempFile.delete()
                    onComplete?.invoke()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play audio", e)
            onComplete?.invoke()
        }
    }

    fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (ignored: Exception) {}
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "GeminiVoiceSynthesizer"
    }
}
