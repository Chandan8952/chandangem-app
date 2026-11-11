package com.example.gemini.data.agent.swarm

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SwarmMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromRole: SwarmRole,
    val toRole: SwarmRole?, // null means broadcast to all swarm agents
    val subject: String,
    val content: String,
    val taskId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
