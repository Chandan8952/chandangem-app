package com.chandankumar.chandangem.core.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalManagerTest {

    @Test
    fun testCommandResult_successEvaluation() {
        val successResult = CommandResult(
            command = "echo 'hello'",
            exitCode = 0,
            stdout = "hello",
            stderr = "",
            durationMs = 15L
        )

        assertTrue(successResult.isSuccess)
        assertEquals("hello", successResult.combinedOutput)

        val failureResult = CommandResult(
            command = "cat nonexistent.txt",
            exitCode = 1,
            stdout = "",
            stderr = "No such file",
            durationMs = 20L
        )

        assertFalse(failureResult.isSuccess)
        assertTrue(failureResult.combinedOutput.contains("No such file"))
    }

    @Test
    fun testTermuxEnvironment_constants() {
        assertEquals("/data/data/com.termux/files/usr", TermuxEnvironment.TERMUX_PREFIX)
        assertEquals("/data/data/com.termux/files/home", TermuxEnvironment.TERMUX_HOME)
        assertEquals("/data/data/com.termux/files/usr/bin/bash", TermuxEnvironment.TERMUX_BASH)
    }
}
