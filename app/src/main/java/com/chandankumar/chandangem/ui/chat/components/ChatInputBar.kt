package com.chandankumar.chandangem.ui.chat.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chandankumar.chandangem.core.voice.VoiceState
import com.chandankumar.chandangem.ui.theme.BackgroundDark
import com.chandankumar.chandangem.ui.theme.BorderSubtle
import com.chandankumar.chandangem.ui.theme.NeonCyan
import com.chandankumar.chandangem.ui.theme.SurfaceElevatedDark
import com.chandankumar.chandangem.ui.theme.SurfaceVariantDark
import com.chandankumar.chandangem.ui.theme.TextPrimary
import com.chandankumar.chandangem.ui.theme.TextSecondary

@Composable
fun ChatInputBar(
    inputDraft: String,
    voiceState: VoiceState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMicClick: () -> Unit,
    onTerminalClick: () -> Unit,
    onMcpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState == VoiceState.LISTENING

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Terminal Sheet Toggle
        IconButton(
            onClick = onTerminalClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = "Open Terminal Manager",
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        // MCP Sheet Toggle
        IconButton(
            onClick = onMcpClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Hub,
                contentDescription = "Open MCP Protocols",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Text Input
        OutlinedTextField(
            value = inputDraft,
            onValueChange = onInputChange,
            placeholder = {
                Text(
                    text = if (isListening) "Listening to voice..." else "Ask Jarvis or type 'run <command>'...",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BackgroundDark,
                unfocusedContainerColor = BackgroundDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = NeonCyan,
                unfocusedBorderColor = Color.Transparent
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 2.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Mic Button (with animated pulsating halo when listening)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(42.dp)
        ) {
            if (isListening) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.25f))
                )
            }

            IconButton(
                onClick = onMicClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isListening) NeonCyan else SurfaceElevatedDark)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Voice Input",
                    tint = if (isListening) BackgroundDark else NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Send Button
        IconButton(
            onClick = onSendClick,
            enabled = inputDraft.isNotBlank(),
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (inputDraft.isNotBlank()) NeonCyan else SurfaceElevatedDark)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message",
                tint = if (inputDraft.isNotBlank()) BackgroundDark else TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
