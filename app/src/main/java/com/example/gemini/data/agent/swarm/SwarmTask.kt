package com.example.gemini.data.agent.swarm

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class SwarmTaskStatus {
    PENDING,
    READY,
    RUNNING,
    DONE,
    FAILED,
    BLOCKED
}

@Serializable
data class SwarmTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val assignedRole: SwarmRole,
    val dependencies: List<String> = emptyList(), // task IDs that must be DONE before this starts
    val status: SwarmTaskStatus = SwarmTaskStatus.PENDING,
    val assignedWorkerId: String? = null,
    val resultOutput: String? = null,
    val errorMessage: String? = null,
    val artifacts: Map<String, String> = emptyMap(), // name -> path or content
    val startedAt: Long? = null,
    val completedAt: Long? = null
)
