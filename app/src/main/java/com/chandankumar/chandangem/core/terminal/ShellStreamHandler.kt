package com.chandankumar.chandangem.core.terminal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Handles async non-blocking line streaming from process I/O streams.
 */
class ShellStreamHandler(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Reads lines from an InputStream and emits them into a cold Flow.
     */
    fun streamLines(inputStream: InputStream): Flow<String> = flow {
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                emit(line)
                line = reader.readLine()
            }
        }
    }.flowOn(dispatcher)

    /**
     * Reads all text from an InputStream on IO dispatcher.
     */
    suspend fun readEntireStream(inputStream: InputStream): String = withContext(dispatcher) {
        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            reader.readText()
        }
    }
}
