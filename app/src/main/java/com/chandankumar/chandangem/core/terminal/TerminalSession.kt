package com.chandankumar.chandangem.core.terminal

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.File
import java.io.OutputStreamWriter
import java.util.UUID

/**
 * Represents an active interactive terminal session.
 */
class TerminalSession(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Session-${id.take(4)}",
    val workingDir: File,
    private val process: Process,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val streamHandler = ShellStreamHandler()
    private val writer = BufferedWriter(OutputStreamWriter(process.outputStream, Charsets.UTF_8))

    private val _outputLines = MutableStateFlow<List<String>>(emptyList())
    val outputLines: StateFlow<List<String>> = _outputLines.asStateFlow()

    private val _isAlive = MutableStateFlow(true)
    val isAlive: StateFlow<Boolean> = _isAlive.asStateFlow()

    private var stdoutJob: Job? = null
    private var stderrJob: Job? = null

    init {
        startPumping()
    }

    private fun startPumping() {
        stdoutJob = scope.launch {
            try {
                streamHandler.streamLines(process.inputStream).collect { line ->
                    appendLine(line)
                }
            } catch (e: Exception) {
                Log.w(TAG, "[$name] Stdout stream ended: ${e.message}")
            } finally {
                checkAlive()
            }
        }

        stderrJob = scope.launch {
            try {
                streamHandler.streamLines(process.errorStream).collect { line ->
                    appendLine("[err] $line")
                }
            } catch (e: Exception) {
                Log.w(TAG, "[$name] Stderr stream ended: ${e.message}")
            } finally {
                checkAlive()
            }
        }
    }

    private fun appendLine(line: String) {
        _outputLines.update { current ->
            // Keep at most 1000 lines of scrollback
            val updated = current + line
            if (updated.size > 1000) updated.takeLast(1000) else updated
        }
    }

    /**
     * Sends input command string to the session stdin.
     */
    fun sendInput(input: String) {
        scope.launch {
            try {
                if (isAlive.value) {
                    writer.write(input)
                    if (!input.endsWith("\n")) {
                        writer.newLine()
                    }
                    writer.flush()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to process stdin", e)
                appendLine("[system error: failed to write stdin: ${e.message}]")
            }
        }
    }

    fun clearScrollback() {
        _outputLines.value = emptyList()
    }

    private fun checkAlive() {
        val alive = try {
            process.exitValue()
            false
        } catch (e: IllegalThreadStateException) {
            true
        }
        _isAlive.value = alive
    }

    fun terminate() {
        try {
            _isAlive.value = false
            stdoutJob?.cancel()
            stderrJob?.cancel()
            writer.close()
            process.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error terminating session $id", e)
        }
    }

    companion object {
        private const val TAG = "TerminalSession"
    }
}
