package com.example.gemini.data.agent.swarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SwarmTaskDag(initialTasks: List<SwarmTask> = emptyList()) {
    private val _tasks = MutableStateFlow(initialTasks)
    val tasks: StateFlow<List<SwarmTask>> = _tasks.asStateFlow()

    fun setTasks(tasks: List<SwarmTask>) {
        _tasks.value = tasks
    }

    fun getTask(id: String): SwarmTask? {
        return _tasks.value.find { it.id == id }
    }

    @Synchronized
    fun getReadyTasks(): List<SwarmTask> {
        val current = _tasks.value
        val completedIds = current.filter { it.status == SwarmTaskStatus.DONE }.map { it.id }.toSet()

        return current.filter { task ->
            task.status == SwarmTaskStatus.PENDING &&
                    task.dependencies.all { it in completedIds }
        }
    }

    @Synchronized
    fun updateTask(
        taskId: String,
        status: SwarmTaskStatus? = null,
        workerId: String? = null,
        resultOutput: String? = null,
        errorMessage: String? = null,
        artifacts: Map<String, String>? = null
    ) {
        val current = _tasks.value
        val updated = current.map { task ->
            if (task.id == taskId) {
                task.copy(
                    status = status ?: task.status,
                    assignedWorkerId = workerId ?: task.assignedWorkerId,
                    resultOutput = resultOutput ?: task.resultOutput,
                    errorMessage = errorMessage ?: task.errorMessage,
                    artifacts = if (artifacts != null) task.artifacts + artifacts else task.artifacts,
                    startedAt = if (status == SwarmTaskStatus.RUNNING && task.startedAt == null) System.currentTimeMillis() else task.startedAt,
                    completedAt = if (status == SwarmTaskStatus.DONE || status == SwarmTaskStatus.FAILED) System.currentTimeMillis() else task.completedAt
                )
            } else {
                task
            }
        }
        _tasks.value = updated
    }

    fun isAllDone(): Boolean {
        val current = _tasks.value
        return current.isNotEmpty() && current.all { it.status == SwarmTaskStatus.DONE }
    }

    fun hasFailed(): Boolean {
        return _tasks.value.any { it.status == SwarmTaskStatus.FAILED }
    }

    fun progressPercentage(): Float {
        val current = _tasks.value
        if (current.isEmpty()) return 0f
        val doneCount = current.count { it.status == SwarmTaskStatus.DONE }
        return doneCount.toFloat() / current.size.toFloat()
    }
}
