package com.chandankumar.chandangem.core.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceLoopTest {

    @Test
    fun testAudioWaveformVisualizer_normalization() {
        val visualizer = AudioWaveformVisualizer(bufferSize = 8, minDb = -10f, maxDb = 10f)

        // Min clamp
        val minNormalized = visualizer.pushRmsDb(-20f)
        assertTrue("Normalized value should be >= 0.05", minNormalized >= 0.05f)

        // Max clamp
        val maxNormalized = visualizer.pushRmsDb(20f)
        assertTrue("Normalized value should be <= 1.0", maxNormalized <= 1.0f)

        // Snapshot length
        val snapshot = visualizer.getBarsSnapshot()
        assertEquals(8, snapshot.size)
    }

    @Test
    fun testVoiceLoopState_enumAndConfig() {
        val config = VoiceConfig(isHandsFreeEnabled = true)
        assertTrue(config.isHandsFreeEnabled)
        assertEquals("en-US", config.languageCode)

        val states = VoiceState.entries
        assertTrue(states.contains(VoiceState.IDLE))
        assertTrue(states.contains(VoiceState.LISTENING))
        assertTrue(states.contains(VoiceState.SPEAKING))
    }
}
