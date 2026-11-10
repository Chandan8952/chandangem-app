package com.chandankumar.chandangem.ui.mcp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chandankumar.chandangem.core.mcp.McpServerStatus
import com.chandankumar.chandangem.core.mcp.RegisteredServer
import com.chandankumar.chandangem.ui.theme.BackgroundDark
import com.chandankumar.chandangem.ui.theme.BorderSubtle
import com.chandankumar.chandangem.ui.theme.CrimsonError
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
fun McpServerSheet(
    viewModel: McpViewModel,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.servers.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = null,
                    tint = CyberPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MCP Protocol Registry",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Servers List
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers) { server ->
                    ServerCard(
                        server = server,
                        onConnectClick = { viewModel.connectServer(server.id) },
                        onTestToolClick = { viewModel.testTool(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: RegisteredServer,
    onConnectClick: () -> Unit,
    onTestToolClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantDark)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = server.description,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Status chip
            StatusBadge(status = server.status)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Endpoint: ${server.endpointOrCommand}",
            color = NeonCyan,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp
        )

        if (server.errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Error: ${server.errorMessage}",
                color = CrimsonError,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Connect button if not connected
        if (server.status != McpServerStatus.CONNECTED) {
            Button(
                onClick = onConnectClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Connect Server", fontSize = 12.sp)
            }
        } else {
            // Tools list
            Text(
                text = "Exposed Tools (${server.tools.size}):",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            server.tools.forEach { tool ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceElevatedDark)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tool.name,
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        tool.description?.let {
                            Text(text = it, color = TextSecondary, fontSize = 10.sp)
                        }
                    }

                    IconButton(
                        onClick = { onTestToolClick(tool.name) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Test Tool",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: McpServerStatus) {
    val (color, label) = when (status) {
        McpServerStatus.CONNECTED -> EmeraldGreen to "CONNECTED"
        McpServerStatus.CONNECTING -> NeonCyan to "CONNECTING"
        McpServerStatus.ERROR -> CrimsonError to "ERROR"
        McpServerStatus.DISCONNECTED -> TextSecondary to "OFFLINE"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
