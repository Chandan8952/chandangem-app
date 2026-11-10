package com.chandankumar.chandangem.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chandankumar.chandangem.core.agent.Agent
import com.chandankumar.chandangem.ui.chat.components.ChatInputBar
import com.chandankumar.chandangem.ui.chat.components.MessageBubble
import com.chandankumar.chandangem.ui.chat.components.VoiceWaveformBar
import com.chandankumar.chandangem.ui.mcp.McpServerSheet
import com.chandankumar.chandangem.ui.mcp.McpViewModel
import com.chandankumar.chandangem.ui.terminal.TerminalSheet
import com.chandankumar.chandangem.ui.terminal.TerminalViewModel
import com.chandankumar.chandangem.ui.theme.BackgroundDark
import com.chandankumar.chandangem.ui.theme.BorderSubtle
import com.chandankumar.chandangem.ui.theme.CyberPurple
import com.chandankumar.chandangem.ui.theme.EmeraldGreen
import com.chandankumar.chandangem.ui.theme.NeonCyan
import com.chandankumar.chandangem.ui.theme.SurfaceDark
import com.chandankumar.chandangem.ui.theme.SurfaceElevatedDark
import com.chandankumar.chandangem.ui.theme.SurfaceVariantDark
import com.chandankumar.chandangem.ui.theme.TextPrimary
import com.chandankumar.chandangem.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    terminalViewModel: TerminalViewModel,
    mcpViewModel: McpViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    val terminalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mcpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Auto-scroll on new messages
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ChandanGem",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Chandan Kumar • Multi-Agent Suite",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                },
                actions = {
                    // Hands-free Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(SurfaceElevatedDark)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HeadsetMic,
                            contentDescription = "Hands-free loop",
                            tint = if (uiState.isHandsFreeEnabled) EmeraldGreen else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hands-free",
                            fontSize = 11.sp,
                            color = if (uiState.isHandsFreeEnabled) EmeraldGreen else TextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Switch(
                            checked = uiState.isHandsFreeEnabled,
                            onCheckedChange = { chatViewModel.toggleHandsFree() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BackgroundDark,
                                checkedTrackColor = EmeraldGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceVariantDark
                            ),
                            modifier = Modifier.size(width = 38.dp, height = 24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceDark
                )
            )
        },
        containerColor = BackgroundDark,
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Agent Switcher Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Agent.ALL_AGENTS.forEach { agent ->
                    val isSelected = agent.id == uiState.activeAgent.id
                    val agentColor = Color(agent.colorHex)

                    FilterChip(
                        selected = isSelected,
                        onClick = { chatViewModel.selectAgent(agent) },
                        label = {
                            Text(
                                text = agent.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = agentColor.copy(alpha = 0.2f),
                            selectedLabelColor = agentColor,
                            containerColor = SurfaceElevatedDark,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            // Voice Waveform Activity Banner
            VoiceWaveformBar(
                voiceState = uiState.voiceState,
                amplitude = uiState.voiceAmplitude,
                partialSpokenText = uiState.partialSpokenText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Chat Messages Viewport
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(
                        message = message,
                        onSpeakClick = { chatViewModel.speakText(it) }
                    )
                }

                if (uiState.isProcessing) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Agents coordinating response...",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Floating Input Dock
            ChatInputBar(
                inputDraft = uiState.inputDraft,
                voiceState = uiState.voiceState,
                onInputChange = { chatViewModel.updateInput(it) },
                onSendClick = { chatViewModel.sendMessage() },
                onMicClick = { chatViewModel.toggleListening() },
                onTerminalClick = { chatViewModel.openTerminalSheet(true) },
                onMcpClick = { chatViewModel.openMcpSheet(true) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }

    // Interactive Terminal Bottom Sheet
    if (uiState.isTerminalSheetOpen) {
        TerminalSheet(
            viewModel = terminalViewModel,
            sheetState = terminalSheetState,
            onDismissRequest = { chatViewModel.openTerminalSheet(false) }
        )
    }

    // MCP Protocol Management Bottom Sheet
    if (uiState.isMcpSheetOpen) {
        McpServerSheet(
            viewModel = mcpViewModel,
            sheetState = mcpSheetState,
            onDismissRequest = { chatViewModel.openMcpSheet(false) }
        )
    }
}
