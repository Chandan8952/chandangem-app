package com.example.gemini.data.agent.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.gemini.data.automation.AndroidAutomation
import com.example.gemini.data.automation.AutomationAccessibilityService
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.UUID

class ScreenVisionAgent(
    private val context: Context,
    private val perceptionEngine: VisionPerceptionEngine = VisionPerceptionEngine(context),
    private val agentScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    companion object {
        private const val TAG = "ScreenVisionAgent"
        private var instanceRef: ScreenVisionAgent? = null

        fun getInstance(context: Context): ScreenVisionAgent {
            return instanceRef ?: synchronized(this) {
                instanceRef ?: ScreenVisionAgent(context.applicationContext).also { instanceRef = it }
            }
        }
    }

    private val _session = MutableStateFlow(ScreenVisionSession(id = "", goal = ""))
    val session: StateFlow<ScreenVisionSession> = _session.asStateFlow()

    private var loopJob: Job? = null
    private var pendingActionToApprove: VisionAction? = null

    var customApiKey: String? = null
    var oauthTokenProvider: (suspend () -> String?)? = null

    fun isRunning(): Boolean {
        val s = _session.value.status
        return s != VisionAgentStatus.IDLE && s != VisionAgentStatus.COMPLETED &&
                s != VisionAgentStatus.FAILED && s != VisionAgentStatus.PAUSED
    }

    fun startGoal(goal: String, maxSteps: Int = 25) {
        stop()
        val sessionId = UUID.randomUUID().toString()
        _session.value = ScreenVisionSession(
            id = sessionId,
            goal = goal,
            status = VisionAgentStatus.CAPTURING_SCREEN,
            currentStep = 0,
            maxSteps = maxSteps,
            steps = emptyList(),
            startedAt = System.currentTimeMillis()
        )

        loopJob = agentScope.launch {
            runAgentLoop(goal, maxSteps)
        }
    }

    fun pause() {
        if (isRunning()) {
            loopJob?.cancel()
            _session.value = _session.value.copy(status = VisionAgentStatus.PAUSED)
        }
    }

    fun resume() {
        if (_session.value.status == VisionAgentStatus.PAUSED) {
            val s = _session.value
            _session.value = s.copy(status = VisionAgentStatus.CAPTURING_SCREEN)
            loopJob = agentScope.launch {
                runAgentLoop(s.goal, s.maxSteps)
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
        pendingActionToApprove = null
        if (_session.value.status != VisionAgentStatus.IDLE) {
            _session.value = _session.value.copy(
                status = VisionAgentStatus.IDLE,
                finishedAt = System.currentTimeMillis()
            )
        }
    }

    fun approvePendingAction() {
        val action = pendingActionToApprove ?: return
        pendingActionToApprove = null
        _session.value = _session.value.copy(
            status = VisionAgentStatus.EXECUTING_ACTION,
            pendingApprovalMessage = null
        )
        loopJob = agentScope.launch {
            executeAndContinue(action)
        }
    }

    private suspend fun runAgentLoop(goal: String, maxSteps: Int) {
        val notReady = AndroidAutomation.notReadyMessage(context)
        if (notReady != null) {
            _session.value = _session.value.copy(
                status = VisionAgentStatus.FAILED,
                error = "Android Automation not ready: $notReady",
                finishedAt = System.currentTimeMillis()
            )
            return
        }

        while (coroutineContext.isActive) {
            val current = _session.value
            if (current.currentStep >= maxSteps) {
                _session.value = current.copy(
                    status = VisionAgentStatus.COMPLETED,
                    error = "Max steps limit ($maxSteps) reached.",
                    finishedAt = System.currentTimeMillis()
                )
                break
            }

            val nextStepIndex = current.currentStep + 1
            _session.value = current.copy(
                currentStep = nextStepIndex,
                status = VisionAgentStatus.CAPTURING_SCREEN
            )

            // 1. Capture screen & UI tree
            val svc = AutomationAccessibilityService.instance
            val (base64Screenshot, screenW, screenH) = captureScreen(svc)
            val uiTree = svc?.let { runCatching { AndroidAutomation.call(context, "android_get_ui_tree", JSONObject().put("max", 50)) }.getOrNull() }
                ?.optJSONArray("content")?.optJSONObject(0)?.optString("text").orEmpty()
            val fgPkg = svc?.foregroundPackage ?: "unknown"

            // 2. Vision reasoning via Gemini Flash
            _session.value = _session.value.copy(status = VisionAgentStatus.ANALYZING_VISION)
            val token = oauthTokenProvider?.invoke()
            val decisionResult = perceptionEngine.decideNextAction(
                goal = goal,
                currentStep = nextStepIndex,
                maxSteps = maxSteps,
                screenshotBase64 = base64Screenshot,
                screenWidth = screenW,
                screenHeight = screenH,
                uiTreeText = uiTree,
                foregroundPackage = fgPkg,
                recentSteps = current.steps,
                apiKey = customApiKey,
                authToken = token
            )

            if (decisionResult.isFailure) {
                val err = decisionResult.exceptionOrNull()?.message ?: "Vision perception failed"
                _session.value = _session.value.copy(
                    status = VisionAgentStatus.FAILED,
                    error = err,
                    finishedAt = System.currentTimeMillis()
                )
                break
            }

            val (thought, action) = decisionResult.getOrThrow()

            // 3. Safety Check
            val safety = ScreenVisionSafetyGuard.evaluateSafety(context, action, fgPkg, uiTree)
            if (safety is ScreenVisionSafetyGuard.SafetyResult.Blocked) {
                _session.value = _session.value.copy(
                    status = VisionAgentStatus.FAILED,
                    error = "Safety Blocked: ${safety.reason}",
                    finishedAt = System.currentTimeMillis()
                )
                break
            } else if (safety is ScreenVisionSafetyGuard.SafetyResult.RequiresApproval) {
                pendingActionToApprove = action
                _session.value = _session.value.copy(
                    status = VisionAgentStatus.AWAITING_USER_APPROVAL,
                    pendingApprovalMessage = safety.prompt
                )
                break
            }

            // 4. Handle Terminal actions
            if (action.type == VisionActionType.COMPLETE) {
                recordStep(nextStepIndex, base64Screenshot, screenW, screenH, fgPkg, thought, action, "Goal achieved: ${action.reasoning}", true)
                _session.value = _session.value.copy(
                    status = VisionAgentStatus.COMPLETED,
                    finishedAt = System.currentTimeMillis()
                )
                break
            } else if (action.type == VisionActionType.FAIL) {
                recordStep(nextStepIndex, base64Screenshot, screenW, screenH, fgPkg, thought, action, "Goal failed: ${action.reasoning}", false)
                _session.value = _session.value.copy(
                    status = VisionAgentStatus.FAILED,
                    error = action.reasoning,
                    finishedAt = System.currentTimeMillis()
                )
                break
            } else if (action.type == VisionActionType.ASK_USER) {
                recordStep(nextStepIndex, base64Screenshot, screenW, screenH, fgPkg, thought, action, "Awaiting user input: ${action.userPrompt}", true)
                _session.value = _session.value.copy(
                    status = VisionAgentStatus.AWAITING_USER_APPROVAL,
                    pendingApprovalMessage = action.userPrompt ?: action.reasoning
                )
                break
            }

            // 5. Execute action and await idle
            val stepResult = executeAction(action)
            recordStep(nextStepIndex, base64Screenshot, screenW, screenH, fgPkg, thought, action, stepResult.first, stepResult.second)

            _session.value = _session.value.copy(status = VisionAgentStatus.AWAITING_IDLE)
            svc?.awaitIdle(quietMs = 500, maxMs = 3000)
            delay(400) // Visual breathing room
        }
    }

    private suspend fun executeAndContinue(action: VisionAction) {
        val current = _session.value
        val svc = AutomationAccessibilityService.instance
        val stepResult = executeAction(action)
        val fgPkg = svc?.foregroundPackage ?: "unknown"
        recordStep(current.currentStep, null, 1080, 2400, fgPkg, action.reasoning, action, stepResult.first, stepResult.second)
        svc?.awaitIdle()

        runAgentLoop(current.goal, current.maxSteps)
    }

    private suspend fun executeAction(action: VisionAction): Pair<String, Boolean> {
        _session.value = _session.value.copy(status = VisionAgentStatus.EXECUTING_ACTION)
        return try {
            when (action.type) {
                VisionActionType.TAP -> {
                    val args = JSONObject()
                    action.elementId?.let { args.put("id", it) }
                    action.x?.let { args.put("x", it.toDouble()) }
                    action.y?.let { args.put("y", it.toDouble()) }
                    val res = AndroidAutomation.call(context, "android_tap", args)
                    val isErr = res.optBoolean("isError", false)
                    val text = res.optJSONArray("content")?.optJSONObject(0)?.optString("text").orEmpty()
                    Pair(text.ifBlank { "Tapped at (${action.x}, ${action.y})" }, !isErr)
                }
                VisionActionType.LONG_PRESS -> {
                    val args = JSONObject().put("long", true)
                    action.elementId?.let { args.put("id", it) }
                    action.x?.let { args.put("x", it.toDouble()) }
                    action.y?.let { args.put("y", it.toDouble()) }
                    val res = AndroidAutomation.call(context, "android_tap", args)
                    Pair("Long pressed", !res.optBoolean("isError", false))
                }
                VisionActionType.TYPE_TEXT -> {
                    val args = JSONObject()
                    action.elementId?.let { args.put("id", it) }
                    action.x?.let { args.put("x", it.toDouble()) }
                    action.y?.let { args.put("y", it.toDouble()) }
                    args.put("text", action.text.orEmpty())
                    args.put("clear", action.clearFirst)
                    args.put("pressEnter", action.pressEnter)
                    val res = AndroidAutomation.call(context, "android_type_text", args)
                    Pair("Typed '${action.text}'", !res.optBoolean("isError", false))
                }
                VisionActionType.SCROLL -> {
                    val args = JSONObject().put("direction", action.direction ?: "down")
                    action.elementId?.let { args.put("id", it) }
                    val res = AndroidAutomation.call(context, "android_scroll", args)
                    Pair("Scrolled ${action.direction ?: "down"}", !res.optBoolean("isError", false))
                }
                VisionActionType.SWIPE -> {
                    val args = JSONObject()
                    action.startX?.let { args.put("startX", it.toDouble()) }
                    action.startY?.let { args.put("startY", it.toDouble()) }
                    action.endX?.let { args.put("endX", it.toDouble()) }
                    action.endY?.let { args.put("endY", it.toDouble()) }
                    args.put("durationMs", action.durationMs)
                    val res = AndroidAutomation.call(context, "android_swipe", args)
                    Pair("Swiped", !res.optBoolean("isError", false))
                }
                VisionActionType.PRESS_KEY -> {
                    val args = JSONObject().put("key", action.key ?: "back")
                    val res = AndroidAutomation.call(context, "android_press_key", args)
                    Pair("Pressed key ${action.key}", !res.optBoolean("isError", false))
                }
                VisionActionType.GLOBAL_ACTION -> {
                    val args = JSONObject().put("action", action.globalAction ?: "back")
                    val res = AndroidAutomation.call(context, "android_global_action", args)
                    Pair("Global action ${action.globalAction}", !res.optBoolean("isError", false))
                }
                VisionActionType.LAUNCH_APP -> {
                    val args = JSONObject().put("package", action.packageName ?: "")
                    val res = AndroidAutomation.call(context, "android_launch_app", args)
                    Pair("Launched app ${action.packageName}", !res.optBoolean("isError", false))
                }
                VisionActionType.OPEN_INTENT -> {
                    val args = JSONObject().put("uri", action.intentUri ?: "")
                    val res = AndroidAutomation.call(context, "android_open_intent", args)
                    Pair("Opened intent ${action.intentUri}", !res.optBoolean("isError", false))
                }
                VisionActionType.WAIT -> {
                    delay(action.durationMs.coerceAtLeast(500L))
                    Pair("Waited ${action.durationMs}ms", true)
                }
                else -> Pair("No-op", true)
            }
        } catch (e: Exception) {
            Pair("Execution failed: ${e.message}", false)
        }
    }

    private suspend fun captureScreen(svc: AutomationAccessibilityService?): Triple<String?, Int, Int> {
        return try {
            val res = AndroidAutomation.call(context, "android_screenshot", JSONObject().put("maxWidth", 1080).put("quality", 80))
            val contents = res.optJSONArray("content")
            var b64: String? = null
            if (contents != null) {
                for (i in 0 until contents.length()) {
                    val item = contents.getJSONObject(i)
                    if (item.optString("type") == "image") {
                        b64 = item.optString("data")
                        break
                    }
                }
            }
            Triple(b64, 1080, 2400)
        } catch (e: Exception) {
            Log.w(TAG, "Screen capture fallback", e)
            Triple(null, 1080, 2400)
        }
    }

    private fun recordStep(
        stepNumber: Int,
        screenshotBase64: String?,
        w: Int,
        h: Int,
        fgPkg: String,
        thought: String,
        action: VisionAction,
        resultMsg: String,
        isSuccess: Boolean
    ) {
        val step = VisionStep(
            stepNumber = stepNumber,
            screenshotBase64 = screenshotBase64,
            screenshotWidth = w,
            screenshotHeight = h,
            foregroundPackage = fgPkg,
            thought = thought,
            action = action,
            resultMessage = resultMsg,
            isSuccess = isSuccess
        )
        val current = _session.value
        _session.value = current.copy(steps = current.steps + step)
    }
}
