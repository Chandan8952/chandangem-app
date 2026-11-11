package com.example.gemini.data.agent.vision

import kotlinx.serialization.Serializable

enum class VisionAgentStatus {
    IDLE,
    CAPTURING_SCREEN,
    ANALYZING_VISION,
    EXECUTING_ACTION,
    AWAITING_IDLE,
    PAUSED,
    AWAITING_USER_APPROVAL,
    COMPLETED,
    FAILED
}

@Serializable
data class VisionStep(
    val stepNumber: Int,
    val screenshotBase64: String? = null,
    val screenshotWidth: Int = 0,
    val screenshotHeight: Int = 0,
    val foregroundPackage: String = "",
    val thought: String = "",
    val action: VisionAction,
    val resultMessage: String = "",
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ScreenVisionSession(
    val id: String,
    val goal: String,
    val status: VisionAgentStatus = VisionAgentStatus.IDLE,
    val currentStep: Int = 0,
    val maxSteps: Int = 25,
    val steps: List<VisionStep> = emptyList(),
    val error: String? = null,
    val pendingApprovalMessage: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null
)
