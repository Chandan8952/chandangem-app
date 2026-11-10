package com.chandankumar.chandangem.ui.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chandankumar.chandangem.core.agent.Agent
import com.chandankumar.chandangem.ui.theme.SurfaceElevatedDark
import com.chandankumar.chandangem.ui.theme.TextPrimary
import com.chandankumar.chandangem.ui.theme.TextSecondary

@Composable
fun AgentBadge(
    agent: Agent,
    modifier: Modifier = Modifier
) {
    val agentColor = Color(agent.colorHex)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevatedDark)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(agentColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = agent.name,
            color = agentColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "• ${agent.title}",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}
