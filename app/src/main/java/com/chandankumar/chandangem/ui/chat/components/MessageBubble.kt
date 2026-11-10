package com.chandankumar.chandangem.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chandankumar.chandangem.ui.chat.ChatMessage
import com.chandankumar.chandangem.ui.theme.BackgroundDark
import com.chandankumar.chandangem.ui.theme.BorderSubtle
import com.chandankumar.chandangem.ui.theme.NeonCyan
import com.chandankumar.chandangem.ui.theme.SurfaceElevatedDark
import com.chandankumar.chandangem.ui.theme.SurfaceVariantDark
import com.chandankumar.chandangem.ui.theme.TextPrimary
import com.chandankumar.chandangem.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageBubble(
    message: ChatMessage,
    onSpeakClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.isUser
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Agent Header if assistant message
        if (!isUser && message.agent != null) {
            AgentBadge(agent = message.agent)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Bubble Container
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 16.dp
                    )
                )
                .background(if (isUser) SurfaceElevatedDark else SurfaceVariantDark)
                .border(
                    width = 1.dp,
                    color = if (isUser) NeonCyan.copy(alpha = 0.5f) else BorderSubtle,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(12.dp)
        ) {
            Column {
                // If message has tool executions, show them above or embedded
                message.toolCalls.forEach { record ->
                    ToolExecutionCard(record = record)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Main Message Content
                RenderFormattedMessage(text = message.text)

                Spacer(modifier = Modifier.height(4.dp))

                // Footer with time & speech replay button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser) {
                        IconButton(
                            onClick = { onSpeakClick(message.text) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Play via TTS",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Text(
                        text = timeStr,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RenderFormattedMessage(text: String) {
    // Basic Markdown split for code blocks (```)
    if (text.contains("```")) {
        val parts = text.split("```")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(BackgroundDark)
                        .padding(8.dp)
                ) {
                    Text(
                        text = part.trim(),
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                if (part.isNotBlank()) {
                    Text(
                        text = part,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    } else {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}
