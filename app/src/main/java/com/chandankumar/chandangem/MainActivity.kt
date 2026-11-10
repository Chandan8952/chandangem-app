package com.chandankumar.chandangem

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.chandankumar.chandangem.ui.chat.ChatScreen
import com.chandankumar.chandangem.ui.chat.ChatViewModel
import com.chandankumar.chandangem.ui.mcp.McpViewModel
import com.chandankumar.chandangem.ui.terminal.TerminalViewModel
import com.chandankumar.chandangem.ui.theme.BackgroundDark
import com.chandankumar.chandangem.ui.theme.ChandanGemTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ChandanGemApplication

        val chatViewModel = ChatViewModel(
            coordinator = app.multiAgentCoordinator,
            voiceManager = app.voiceAssistantManager
        )

        val terminalViewModel = TerminalViewModel(
            terminalManager = app.terminalManager
        )

        val mcpViewModel = McpViewModel(
            mcpRegistry = app.mcpServerRegistry
        )

        setContent {
            ChandanGemTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        Toast.makeText(
                            this,
                            "Microphone permission is required for Voice Assistant hands-free loop",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    ChatScreen(
                        chatViewModel = chatViewModel,
                        terminalViewModel = terminalViewModel,
                        mcpViewModel = mcpViewModel
                    )
                }
            }
        }
    }
}
