package com.chandankumar.chandangem.core.terminal

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

/**
 * Terminal & Shell Execution Manager for ChandanGem.
 * Dispatches commands to Termux bash/sh or Android sh, streams output reactively,
 * and manages interactive terminal sessions.
 */
class TerminalManager(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val sessions = ConcurrentHashMap<String, TerminalSession>()
    private val _sessionsList = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessionsList: StateFlow<List<TerminalSession>> = _sessionsList.asStateFlow()

    private val streamHandler = ShellStreamHandler(dispatcher)

    /**
     * Executes a command synchronously (suspending) and returns complete CommandResult.
     */
    suspend fun executeCommandSync(
        command: String,
        workingDir: File? = null,
        timeoutMs: Long = 60_000L
    ): CommandResult = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        val shell = TermuxEnvironment.getShellBinary()
        val cwd = workingDir ?: TermuxEnvironment.getDefaultWorkingDirectory(context)

        val processBuilder = ProcessBuilder(shell, "-c", command).apply {
            directory(cwd)
            val env = environment()
            env.clear()
            env.putAll(TermuxEnvironment.getEnvironmentMap(context))
        }

        try {
            val process = processBuilder.start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val readOutputJob = withTimeoutOrNull(timeoutMs) {
                val outReader = BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8))
                val errReader = BufferedReader(InputStreamReader(process.errorStream, Charsets.UTF_8))

                outReader.forEachLine { line -> stdoutBuilder.appendLine(line) }
                errReader.forEachLine { line -> stderrBuilder.appendLine(line) }

                process.waitFor()
            }

            val exitCode = if (readOutputJob != null) {
                process.exitValue()
            } else {
                process.destroyForcibly()
                -999 // Timeout code
            }

            val duration = System.currentTimeMillis() - startTime
            val stdout = stdoutBuilder.toString().trimEnd()
            val stderr = if (readOutputJob == null) "Command timed out after ${timeoutMs}ms" else stderrBuilder.toString().trimEnd()

            CommandResult(
                command = command,
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                durationMs = duration
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error executing shell command: $command", e)
            val duration = System.currentTimeMillis() - startTime
            CommandResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = e.localizedMessage ?: "Unknown execution error",
                durationMs = duration
            )
        }
    }

    /**
     * Streams command output lines reactively via a Cold Flow.
     */
    fun executeCommand(
        command: String,
        workingDir: File? = null
    ): Flow<String> = flow {
        val shell = TermuxEnvironment.getShellBinary()
        val cwd = workingDir ?: TermuxEnvironment.getDefaultWorkingDirectory(context)

        val processBuilder = ProcessBuilder(shell, "-c", command).apply {
            directory(cwd)
            val env = environment()
            env.clear()
            env.putAll(TermuxEnvironment.getEnvironmentMap(context))
            redirectErrorStream(true) // Merge stdout and stderr for linear terminal stream
        }

        val process = processBuilder.start()
        try {
            BufferedReader(InputStreamReader(process.inputStream, Charsets.UTF_8)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    emit(line)
                    line = reader.readLine()
                }
            }
            process.waitFor()
        } finally {
            process.destroy()
        }
    }.flowOn(dispatcher)

    /**
     * Creates and registers a new interactive TerminalSession.
     */
    fun createSession(name: String? = null, workingDir: File? = null): TerminalSession {
        val shell = TermuxEnvironment.getShellBinary()
        val cwd = workingDir ?: TermuxEnvironment.getDefaultWorkingDirectory(context)

        val processBuilder = ProcessBuilder(shell).apply {
            directory(cwd)
            val env = environment()
            env.clear()
            env.putAll(TermuxEnvironment.getEnvironmentMap(context))
        }

        val process = processBuilder.start()
        val sessionName = name ?: "Shell-${sessions.size + 1}"
        val session = TerminalSession(
            name = sessionName,
            workingDir = cwd,
            process = process
        )

        sessions[session.id] = session
        updateSessionsList()
        Log.i(TAG, "Created terminal session ${session.id} ($sessionName)")
        return session
    }

    fun getSession(sessionId: String): TerminalSession? = sessions[sessionId]

    fun closeSession(sessionId: String) {
        sessions.remove(sessionId)?.let { session ->
            session.terminate()
            updateSessionsList()
            Log.i(TAG, "Closed terminal session $sessionId")
        }
    }

    private fun updateSessionsList() {
        _sessionsList.value = sessions.values.toList()
    }

    fun shutdown() {
        sessions.values.forEach { it.terminate() }
        sessions.clear()
        _sessionsList.value = emptyList()
    }

    companion object {
        private const val TAG = "TerminalManager"
    }
}
