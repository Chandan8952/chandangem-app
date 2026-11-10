package com.chandankumar.chandangem.core.voice

import kotlin.math.max
import kotlin.math.min

/**
 * Normalizes and smooths audio amplitudes (RMS dB) for Jetpack Compose waveforms.
 */
class AudioWaveformVisualizer(
    private val bufferSize: Int = 16,
    private val minDb: Float = -10f,
    private val maxDb: Float = 10f
) {
    private val history = FloatArray(bufferSize) { 0.05f }
    private var index = 0

    /**
     * Pushes a new raw RMS dB value and returns the normalized float [0.0f..1.0f].
     */
    fun pushRmsDb(rmsDb: Float): Float {
        val clamped = max(minDb, min(maxDb, rmsDb))
        val normalized = (clamped - minDb) / (maxDb - minDb)
        val smoothed = 0.7f * normalized + 0.3f * history[index]

        history[index] = smoothed
        index = (index + 1) % bufferSize
        return smoothed.coerceIn(0.05f, 1.0f)
    }

    /**
     * Returns a snapshot of current amplitude bars for rendering multi-bar audio waves.
     */
    fun getBarsSnapshot(): List<Float> {
        val bars = mutableListOf<Float>()
        for (i in 0 until bufferSize) {
            val idx = (index + i) % bufferSize
            bars.add(history[idx])
        }
        return bars
    }

    fun reset() {
        history.fill(0.05f)
        index = 0
    }
}
