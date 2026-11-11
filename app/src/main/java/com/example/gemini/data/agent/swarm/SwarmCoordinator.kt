package com.example.gemini.data.agent.swarm

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SwarmSession(
    val id: String,
    val goal: String,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val progress: Float = 0f,
    val tasks: List<SwarmTask> = emptyList(),
    val messages: List<SwarmMessage> = emptyList(),
    val summaryReport: String? = null,
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)

class SwarmCoordinator(
    private val context: Context,
    val workerPool: SwarmWorkerPool = SwarmWorkerPool(context),
    private val coordinatorScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        private const val TAG = "SwarmCoordinator"
        private var instanceRef: SwarmCoordinator? = null

        fun getInstance(context: Context): SwarmCoordinator {
            return instanceRef ?: synchronized(this) {
                instanceRef ?: SwarmCoordinator(context.applicationContext).also { instanceRef = it }
            }
        }
    }

    val memory = SwarmMemory()
    val dag = SwarmTaskDag()

    private val _session = MutableStateFlow(SwarmSession(id = "", goal = ""))
    val session: StateFlow<SwarmSession> = _session.asStateFlow()

    private var coordinatorJob: Job? = null

    init {
        // Keep session updated with DAG tasks & memory messages
        coordinatorScope.launch {
            dag.tasks.collect { currentTasks ->
                _session.value = _session.value.copy(
                    tasks = currentTasks,
                    progress = dag.progressPercentage()
                )
            }
        }
        coordinatorScope.launch {
            memory.messages.collect { currentMessages ->
                _session.value = _session.value.copy(messages = currentMessages)
            }
        }
    }

    fun isRunning(): Boolean = _session.value.isRunning

    fun startSwarm(goal: String, workspaceUri: String = "") {
        cancel()
        memory.clear()
        memory.putContext("goal", goal)
        memory.putContext("workspaceUri", workspaceUri)

        val initialTasks = decomposeGoalIntoDag(goal)
        dag.setTasks(initialTasks)

        val sessionId = UUID.randomUUID().toString()
        _session.value = SwarmSession(
            id = sessionId,
            goal = goal,
            isRunning = true,
            isPaused = false,
            progress = 0f,
            tasks = initialTasks,
            messages = emptyList(),
            startedAt = System.currentTimeMillis()
        )

        coordinatorJob = coordinatorScope.launch {
            runCoordinatorLoop(workspaceUri)
        }
    }

    fun pause() {
        if (isRunning()) {
            _session.value = _session.value.copy(isPaused = true)
        }
    }

    fun resume() {
        if (_session.value.isPaused) {
            _session.value = _session.value.copy(isPaused = false)
        }
    }

    fun cancel() {
        coordinatorJob?.cancel()
        coordinatorJob = null
        if (_session.value.isRunning) {
            _session.value = _session.value.copy(
                isRunning = false,
                isPaused = false,
                finishedAt = System.currentTimeMillis()
            )
        }
    }

    private suspend fun runCoordinatorLoop(workspaceUri: String) {
        try {
            while (coroutineContext.isActive) {
                if (_session.value.isPaused) {
                    delay(500)
                    continue
                }

                if (dag.isAllDone()) {
                    Log.d(TAG, "All DAG tasks completed! Synthesizing final report...")
                    val report = synthesizeFinalReport(_session.value.goal)
                    _session.value = _session.value.copy(
                        isRunning = false,
                        progress = 1.0f,
                        summaryReport = report,
                        finishedAt = System.currentTimeMillis()
                    )
                    break
                }

                if (dag.hasFailed()) {
                    _session.value = _session.value.copy(
                        isRunning = false,
                        error = "One or more tasks in the swarm failed.",
                        finishedAt = System.currentTimeMillis()
                    )
                    break
                }

                val readyTasks = dag.getReadyTasks()
                if (readyTasks.isEmpty()) {
                    // Check if any tasks are currently running
                    val runningTasks = dag.tasks.value.filter { it.status == SwarmTaskStatus.RUNNING }
                    if (runningTasks.isEmpty() && !dag.isAllDone()) {
                        // Deadlock or unresolvable dependencies
                        _session.value = _session.value.copy(
                            isRunning = false,
                            error = "Task dependency deadlock detected.",
                            finishedAt = System.currentTimeMillis()
                        )
                        break
                    }
                    delay(500)
                    continue
                }

                // Dispatch ready tasks in parallel
                readyTasks.forEach { task ->
                    val workerId = "worker-${task.assignedRole.name.lowercase()}"
                    dag.updateTask(task.id, status = SwarmTaskStatus.RUNNING, workerId = workerId)

                    coordinatorScope.launch {
                        val result = workerPool.executeTask(task, memory, workspaceUri)
                        if (result.isSuccess) {
                            dag.updateTask(
                                task.id,
                                status = SwarmTaskStatus.DONE,
                                resultOutput = result.getOrNull()
                            )
                        } else {
                            dag.updateTask(
                                task.id,
                                status = SwarmTaskStatus.FAILED,
                                errorMessage = result.exceptionOrNull()?.message
                            )
                        }
                    }
                }

                delay(600)
            }
        } catch (e: CancellationException) {
            Log.d(TAG, "Coordinator loop cancelled")
        } catch (e: Exception) {
            Log.e(TAG, "Coordinator loop encountered error", e)
            _session.value = _session.value.copy(
                isRunning = false,
                error = e.message,
                finishedAt = System.currentTimeMillis()
            )
        }
    }

    private fun decomposeGoalIntoDag(goal: String): List<SwarmTask> {
        val t1Id = "task-arch-${UUID.randomUUID().toString().take(6)}"
        val t2Id = "task-code1-${UUID.randomUUID().toString().take(6)}"
        val t3Id = "task-code2-${UUID.randomUUID().toString().take(6)}"
        val t4Id = "task-rev-${UUID.randomUUID().toString().take(6)}"
        val t5Id = "task-test-${UUID.randomUUID().toString().take(6)}"
        val t6Id = "task-docs-${UUID.randomUUID().toString().take(6)}"

        return listOf(
            SwarmTask(
                id = t1Id,
                title = "Architecture & Interface Design",
                description = "Analyze the goal '$goal', specify system requirements, module layout, and component interfaces.",
                assignedRole = SwarmRole.ARCHITECT,
                dependencies = emptyList()
            ),
            SwarmTask(
                id = t2Id,
                title = "Core Component Implementation",
                description = "Implement core data models, logic, and primary functionality based on the Architect's specification.",
                assignedRole = SwarmRole.CODER,
                dependencies = listOf(t1Id)
            ),
            SwarmTask(
                id = t3Id,
                title = "Supporting Utilities & Integration",
                description = "Implement helper routines, error handling, configuration options, and integration glue.",
                assignedRole = SwarmRole.CODER,
                dependencies = listOf(t1Id)
            ),
            SwarmTask(
                id = t4Id,
                title = "Static Analysis & Code Quality Audit",
                description = "Review all code changes from both Coder agents, verify edge cases, security, and type safety.",
                assignedRole = SwarmRole.REVIEWER,
                dependencies = listOf(t2Id, t3Id)
            ),
            SwarmTask(
                id = t5Id,
                title = "Automated Build & Runtime Verification",
                description = "Execute unit test suites, check build integrity, and verify that the system runs smoothly.",
                assignedRole = SwarmRole.TESTER_EXECUTOR,
                dependencies = listOf(t2Id, t3Id)
            ),
            SwarmTask(
                id = t6Id,
                title = "Documentation & Release Metadata",
                description = "Generate comprehensive user and developer documentation, changelog entries, and git commit message.",
                assignedRole = SwarmRole.DOCS_RELEASE,
                dependencies = listOf(t4Id, t5Id)
            )
        )
    }

    private fun synthesizeFinalReport(goal: String): String {
        val artifacts = memory.getAllArtifacts()
        val sb = StringBuilder()
        sb.append("# 🐝 Multi-Agent Swarm Execution Summary\n\n")
        sb.append("**Goal**: $goal\n\n")
        sb.append("## Completed Deliverables:\n")
        artifacts.forEach { (name, content) ->
            sb.append("### 📌 $name\n")
            sb.append("${content.take(800)}\n\n")
        }
        sb.append("---\n")
        sb.append("*Swarm execution completed with parallel agent verification.*")
        return sb.toString()
    }
}
