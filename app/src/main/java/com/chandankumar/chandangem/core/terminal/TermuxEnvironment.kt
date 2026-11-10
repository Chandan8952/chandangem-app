package com.chandankumar.chandangem.core.terminal

import android.content.Context
import java.io.File

/**
 * Detects and configures the Termux shell environment and system paths.
 * Allows ChandanGem to seamlessly execute inside Termux or standard Android sandbox.
 */
object TermuxEnvironment {

    const val TERMUX_PACKAGE = "com.termux"
    const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"
    const val TERMUX_HOME = "/data/data/com.termux/files/home"
    const val TERMUX_TMP = "/data/data/com.termux/files/usr/tmp"
    const val TERMUX_BIN = "$TERMUX_PREFIX/bin"
    const val TERMUX_BASH = "$TERMUX_BIN/bash"
    const val TERMUX_SH = "$TERMUX_BIN/sh"
    const val SYSTEM_SH = "/system/bin/sh"

    /**
     * Checks if Termux prefix and binaries are accessible on the current device.
     */
    fun isTermuxAvailable(): Boolean {
        return File(TERMUX_BASH).canExecute() || File(TERMUX_SH).canExecute()
    }

    /**
     * Resolves the primary shell binary to use for process execution.
     */
    fun getShellBinary(): String {
        return when {
            File(TERMUX_BASH).canExecute() -> TERMUX_BASH
            File(TERMUX_SH).canExecute() -> TERMUX_SH
            File(SYSTEM_SH).canExecute() -> SYSTEM_SH
            else -> "sh"
        }
    }

    /**
     * Resolves default working directory.
     */
    fun getDefaultWorkingDirectory(context: Context): File {
        val termuxHome = File(TERMUX_HOME)
        return if (termuxHome.exists() && termuxHome.canWrite()) {
            termuxHome
        } else {
            context.filesDir
        }
    }

    /**
     * Builds comprehensive environment variables including Termux PATH and library paths.
     */
    fun getEnvironmentMap(context: Context): MutableMap<String, String> {
        val env = HashMap(System.getenv())

        val currentPath = env["PATH"] ?: "/system/bin:/system/xbin"
        val termuxPath = "$TERMUX_BIN:$TERMUX_PREFIX/bin/applets"

        env["PATH"] = if (isTermuxAvailable()) {
            "$termuxPath:$currentPath"
        } else {
            currentPath
        }

        if (isTermuxAvailable()) {
            env["PREFIX"] = TERMUX_PREFIX
            env["HOME"] = TERMUX_HOME
            env["TMPDIR"] = TERMUX_TMP
            env["LD_LIBRARY_PATH"] = "$TERMUX_PREFIX/lib"
        } else {
            env["HOME"] = context.filesDir.absolutePath
            env["TMPDIR"] = context.cacheDir.absolutePath
        }

        env["TERM"] = "xterm-256color"
        env["LANG"] = "en_US.UTF-8"
        env["CHANDANGEM_AGENT"] = "Jarvis-AntiGem"

        return env
    }
}
