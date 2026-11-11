package com.example.gemini.data.agent.vision

import kotlinx.serialization.Serializable

enum class VisionActionType {
    TAP,
    LONG_PRESS,
    TYPE_TEXT,
    SCROLL,
    SWIPE,
    PRESS_KEY,
    GLOBAL_ACTION,
    LAUNCH_APP,
    OPEN_INTENT,
    WAIT,
    COMPLETE,
    FAIL,
    ASK_USER
}

@Serializable
data class VisionAction(
    val type: VisionActionType,
    val x: Float? = null,
    val y: Float? = null,
    val elementId: String? = null,
    val text: String? = null,
    val clearFirst: Boolean = false,
    val pressEnter: Boolean = false,
    val direction: String? = null, // "up", "down", "left", "right"
    val startX: Float? = null,
    val startY: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,
    val key: String? = null, // "back", "home", "recents", "enter"
    val globalAction: String? = null, // "back", "home", "recents", "notifications", "quick_settings"
    val packageName: String? = null,
    val intentUri: String? = null,
    val durationMs: Long = 350L,
    val reasoning: String = "",
    val confidence: Float = 1.0f,
    val userPrompt: String? = null
)
