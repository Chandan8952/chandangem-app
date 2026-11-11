package com.example.gemini.ui.swarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gemini.data.agent.swarm.*

@Composable
fun SwarmCoordinatorPanel(
    coordinator: SwarmCoordinator,
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {}
) {
    val session by coordinator.session.collectAsState()
    var inputGoal by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = "Swarm Coordinator",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Multi-Agent Swarm Coordinator",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Parallel Specialized Agent Collaboration",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(Modifier.height(12.dp))

            // Goal Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Swarm Coding Objective",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = if (coordinator.isRunning()) session.goal else inputGoal,
                        onValueChange = { inputGoal = it },
                        placeholder = { Text("e.g. Refactor auth service, implement unit tests, verify build and write README") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !coordinator.isRunning(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (coordinator.isRunning()) {
                            if (session.isPaused) {
                                Button(
                                    onClick = { coordinator.resume() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Resume Swarm")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { coordinator.pause() },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Pause, contentDescription = null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { coordinator.cancel() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Cancel")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (inputGoal.isNotBlank()) {
                                        coordinator.startSwarm(inputGoal.trim())
                                    }
                                },
                                enabled = inputGoal.isNotBlank(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Launch Swarm")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Progress bar & Status
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when {
                        session.isRunning && session.isPaused -> "Paused"
                        session.isRunning -> "Running in parallel"
                        session.summaryReport != null -> "Completed"
                        session.error != null -> "Error: ${session.error}"
                        else -> "Ready"
                    }
                    Text("Swarm Progress: $statusText", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text("${(session.progress * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { session.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFFF59E0B)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Task DAG Board
            Text(
                "Task Dependency Graph (DAG)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))

            if (session.tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No active tasks. Enter an objective to spawn the agent swarm.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(session.tasks) { task ->
                        SwarmTaskCard(task)
                    }
                }
            }
        }
    }
}

@Composable
fun SwarmTaskCard(task: SwarmTask) {
    val (statusColor, statusBg) = when (task.status) {
        SwarmTaskStatus.DONE -> Color(0xFF10B981) to Color(0xFF10B981).copy(alpha = 0.12f)
        SwarmTaskStatus.RUNNING -> Color(0xFFF59E0B) to Color(0xFFF59E0B).copy(alpha = 0.12f)
        SwarmTaskStatus.FAILED -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        SwarmTaskStatus.READY -> Color(0xFF3B82F6) to Color(0xFF3B82F6).copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.outline to MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusBg,
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = task.assignedRole.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = task.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = task.status.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = task.description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.dependencies.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Dependencies: ${task.dependencies.joinToString(", ")}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (!task.resultOutput.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.resultOutput.take(200) + if (task.resultOutput.length > 200) "..." else "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
