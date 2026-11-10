package com.chandankumar.chandangem.ui.chat.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chandankumar.chandangem.core.voice.VoiceState
import com.chandankumar.chandangem.ui.theme.CyberPurple
import com.chandankumar.chandangem.ui.theme.EmeraldGreen
import com.chandankumar.chandangem.ui.theme.NeonCyan
import com.chandankumar.chandangem.ui.theme.SurfaceVariantDark
import com.chandankumar.chandangem.ui.theme.TextSecondary
import kotlin.math.sin

@Composable
fun VoiceWaveformBar(
    voiceState: VoiceState,
    amplitude: Float,
    partialSpokenText: String,
    modifier: Modifier = Modifier
) {
    if (voiceState == VoiceState.IDLE) return

    val barColor = when (voiceState) {
        VoiceState.LISTENING -> NeonCyan
        VoiceState.SPEAKING -> EmeraldGreen
        VoiceState.PROCESSING -> CyberPurple
        else -> NeonCyan
    }

    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Text status or live transcription
        Text(
            text = when {
                partialSpokenText.isNotBlank() -> "\"$partialSpokenText\""
                voiceState == VoiceState.LISTENING -> "Listening..."
                voiceState == VoiceState.SPEAKING -> "Speaking..."
                voiceState == VoiceState.PROCESSING -> "Processing query..."
                else -> ""
            },
            color = if (partialSpokenText.isNotBlank()) barColor else TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )

        // Pulsing Waveform Bars
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val barCount = 9
            for (i in 0 until barCount) {
                val waveFactor = ((sin(phase + i * 0.7f) + 1f) / 2f) * 0.4f
                val dynamicHeight = (8.dp + ((amplitude + waveFactor) * 22).dp).coerceIn(4.dp, 28.dp)

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(dynamicHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
            }
        }
    }
}
