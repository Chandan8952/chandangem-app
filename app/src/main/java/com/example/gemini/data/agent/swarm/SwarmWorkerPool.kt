package com.example.gemini.data.agent.swarm

import android.content.Context
import android.util.Log
import com.example.gemini.data.local.LocalTerminalBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SwarmWorkerPool(
    private val context: Context,
    val maxConcurrency: Int = 3,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "SwarmWorkerPool"
        private const val DEFAULT_MODEL = "gemini-2.5-flash"
    }

    private val semaphore = Semaphore(maxConcurrency)
    var apiKey: String? = null
    var authTokenProvider: (suspend () -> String?)? = null

    suspend fun executeTask(
        task: SwarmTask,
        memory: SwarmMemory,
        workspaceUri: String
    ): Result<String> = semaphore.withPermit {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting task ${task.id} (${task.title}) with role ${task.assignedRole}")

                // Post announcement message to swarm bus
                memory.postMessage(
                    SwarmMessage(
                        fromRole = task.assignedRole,
                        toRole = null,
                        subject = "Task Started: ${task.title}",
                        content = "Starting execution of '${task.title}'. Goal: ${task.description}",
                        taskId = task.id
                    )
                )

                // Role-specific execution logic
                val result = when (task.assignedRole) {
                    SwarmRole.ARCHITECT -> executeArchitectTask(task, memory, workspaceUri)
                    SwarmRole.CODER -> executeCoderTask(task, memory, workspaceUri)
                    SwarmRole.REVIEWER -> executeReviewerTask(task, memory, workspaceUri)
                    SwarmRole.TESTER_EXECUTOR -> executeTesterTask(task, memory, workspaceUri)
                    SwarmRole.DOCS_RELEASE -> executeDocsTask(task, memory, workspaceUri)
                }

                // Record completed output in artifacts
                memory.putArtifact(task.title, result)

                // Post completion message to swarm bus
                memory.postMessage(
                    SwarmMessage(
                        fromRole = task.assignedRole,
                        toRole = null,
                        subject = "Task Completed: ${task.title}",
                        content = "Finished '${task.title}'. Summary: ${result.take(150)}...",
                        taskId = task.id
                    )
                )

                Result.success(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error executing task ${task.id}", e)
                memory.postMessage(
                    SwarmMessage(
                        fromRole = task.assignedRole,
                        toRole = null,
                        subject = "Task Failed: ${task.title}",
                        content = "Failure during execution: ${e.message}",
                        taskId = task.id
                    )
                )
                Result.failure(e)
            }
        }
    }

    private suspend fun executeArchitectTask(task: SwarmTask, memory: SwarmMemory, workspaceUri: String): String {
        val prompt = """
            ${task.assignedRole.systemPrompt(workspaceUri)}
            
            Task: ${task.title}
            Description: ${task.description}
            
            Context from other agents:
            ${formatContext(memory)}
            
            Provide a clear architecture specification, interface designs, data schemas, and implementation guidance for the Coder agents.
        """.trimIndent()

        return callLlm(prompt)
    }

    private suspend fun executeCoderTask(task: SwarmTask, memory: SwarmMemory, workspaceUri: String): String {
        val prompt = """
            ${task.assignedRole.systemPrompt(workspaceUri)}
            
            Task: ${task.title}
            Description: ${task.description}
            
            Shared Blackboard Context:
            ${formatContext(memory)}
            
            Implement the code required for this task. Provide complete, idiomatic code snippets, file changes, and explain the implementation details.
        """.trimIndent()

        return callLlm(prompt)
    }

    private suspend fun executeReviewerTask(task: SwarmTask, memory: SwarmMemory, workspaceUri: String): String {
        val prompt = """
            ${task.assignedRole.systemPrompt(workspaceUri)}
            
            Task: ${task.title}
            Description: ${task.description}
            
            Artifacts from Coder agents:
            ${memory.getAllArtifacts().entries.joinToString("\n---\n") { "${it.key}:\n${it.value.take(2000)}" }}
            
            Audit the implementation. Check edge cases, syntax correctness, error handling, security considerations, and verify that requirements are met.
        """.trimIndent()

        return callLlm(prompt)
    }

    private suspend fun executeTesterTask(task: SwarmTask, memory: SwarmMemory, workspaceUri: String): String {
        // Can run test commands via LocalTerminalBridge if available
        val bridge = LocalTerminalBridge.instance
        val activeSessions = bridge.listSessions()
        val terminalReport = if (activeSessions.isNotEmpty()) {
            "Active terminal session found. Runtime environment ready."
        } else {
            "Simulated verification environment ready."
        }

        val prompt = """
            ${task.assignedRole.systemPrompt(workspaceUri)}
            
            Task: ${task.title}
            Description: ${task.description}
            Terminal Environment: $terminalReport
            
            Artifacts to verify:
            ${memory.getAllArtifacts().entries.joinToString("\n---\n") { "${it.key}:\n${it.value.take(1500)}" }}
            
            Write unit tests, evaluate build requirements, and verify expected behavior. Report test pass/fail results.
        """.trimIndent()

        return callLlm(prompt)
    }

    private suspend fun executeDocsTask(task: SwarmTask, memory: SwarmMemory, workspaceUri: String): String {
        val prompt = """
            ${task.assignedRole.systemPrompt(workspaceUri)}
            
            Task: ${task.title}
            Description: ${task.description}
            
            All completed artifacts from the Swarm:
            ${memory.getAllArtifacts().entries.joinToString("\n---\n") { "${it.key}:\n${it.value.take(1500)}" }}
            
            Generate markdown documentation, release notes, and draft a conventional git commit message summarizing the collective work of all agents.
        """.trimIndent()

        return callLlm(prompt)
    }

    private fun formatContext(memory: SwarmMemory): String {
        val ctx = memory.getAllContext()
        val msgs = memory.messages.value.takeLast(5)
        val sb = StringBuilder()
        if (ctx.isNotEmpty()) {
            sb.append("Variables:\n")
            ctx.forEach { (k, v) -> sb.append("- $k: $v\n") }
        }
        if (msgs.isNotEmpty()) {
            sb.append("\nRecent Swarm Messages:\n")
            msgs.forEach { m -> sb.append("[${m.fromRole}]: ${m.subject} - ${m.content.take(100)}\n") }
        }
        return sb.toString().ifBlank { "No prior context." }
    }

    private suspend fun callLlm(prompt: String): String = withContext(Dispatchers.IO) {
        val requestJson = JSONObject()
        val contents = JSONArray()
        val userContent = JSONObject().put("role", "user")
        val parts = JSONArray().put(JSONObject().put("text", prompt))
        userContent.put("parts", parts)
        contents.put(userContent)
        requestJson.put("contents", contents)

        val url = if (!apiKey.isNullOrBlank()) {
            "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent?key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent"
        }

        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

        val token = authTokenProvider?.invoke()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val bodyString = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw IllegalStateException("LLM error ${response.code}: ${bodyString.take(150)}")
        }

        val resObj = JSONObject(bodyString)
        val candidates = resObj.optJSONArray("candidates")
        if (candidates != null && candidates.length() > 0) {
            candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        } else {
            "No output generated."
        }
    }
}
