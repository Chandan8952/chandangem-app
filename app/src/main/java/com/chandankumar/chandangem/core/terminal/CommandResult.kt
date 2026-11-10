package com.chandankumar.chandangem.core.terminal

/**
 * Result data class for shell command execution.
 */
data class CommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val durationMs: Long
) {
    val isSuccess: Boolean
        get() = exitCode == 0

    val combinedOutput: String
        get() = buildString {
            if (stdout.isNotBlank()) append(stdout)
            if (stderr.isNotBlank()) {
                if (isNotEmpty()) append("\n")
                append("[stderr]\n").append(stderr)
            }
        }
}
