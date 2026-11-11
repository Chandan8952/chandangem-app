package com.example.gemini.data.agent.vision

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class VisionPerceptionEngine(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "VisionPerception"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
    }

    suspend fun decideNextAction(
        goal: String,
        currentStep: Int,
        maxSteps: Int,
        screenshotBase64: String?,
        screenWidth: Int,
        screenHeight: Int,
        uiTreeText: String,
        foregroundPackage: String,
        recentSteps: List<VisionStep>,
        apiKey: String? = null,
        authToken: String? = null
    ): Result<Pair<String, VisionAction>> = withContext(Dispatchers.IO) {
        try {
            val systemInstruction = """
                You are an Autonomous Android Screen Vision Agent powered by Gemini Flash Vision.
                Your task is to observe the current screen screenshot and accessibility tree, then decide the single next action to achieve the user's goal.

                Screen resolution: ${screenWidth}x${screenHeight}.
                Current Foreground Package: $foregroundPackage.
                User Goal: "$goal".
                Step: $currentStep of $maxSteps.

                Available Actions:
                1. TAP: {"type": "TAP", "x": <int>, "y": <int>, "elementId": "<id if known>", "reasoning": "<why>"}
                2. TYPE_TEXT: {"type": "TYPE_TEXT", "elementId": "<id>", "x": <int>, "y": <int>, "text": "<text>", "clearFirst": true/false, "pressEnter": true/false, "reasoning": "<why>"}
                3. SCROLL: {"type": "SCROLL", "direction": "down"|"up"|"left"|"right", "reasoning": "<why>"}
                4. SWIPE: {"type": "SWIPE", "startX": <int>, "startY": <int>, "endX": <int>, "endY": <int>, "durationMs": 350, "reasoning": "<why>"}
                5. PRESS_KEY: {"type": "PRESS_KEY", "key": "back"|"home"|"recents"|"enter", "reasoning": "<why>"}
                6. GLOBAL_ACTION: {"type": "GLOBAL_ACTION", "globalAction": "back"|"home"|"recents"|"notifications", "reasoning": "<why>"}
                7. LAUNCH_APP: {"type": "LAUNCH_APP", "packageName": "<package>", "reasoning": "<why>"}
                8. WAIT: {"type": "WAIT", "durationMs": 1000, "reasoning": "<why>"}
                9. COMPLETE: {"type": "COMPLETE", "reasoning": "<summary of how goal was accomplished>"}
                10. FAIL: {"type": "FAIL", "reasoning": "<why goal could not be accomplished>"}
                11. ASK_USER: {"type": "ASK_USER", "userPrompt": "<clarification question>", "reasoning": "<why>"}

                Output format: Return ONLY a valid JSON object with two fields:
                {
                  "thought": "Your internal visual reasoning about the current screen state and what to do next",
                  "action": { ...action object from above... }
                }
            """.trimIndent()

            val promptBuilder = StringBuilder()
            promptBuilder.append("Current UI Tree:\n").append(uiTreeText.take(3000)).append("\n\n")

            if (recentSteps.isNotEmpty()) {
                promptBuilder.append("Recent Action History:\n")
                recentSteps.takeLast(3).forEach { step ->
                    promptBuilder.append("Step ${step.stepNumber}: Action=${step.action.type}, Result=${step.resultMessage}, Thought=${step.thought}\n")
                }
                promptBuilder.append("\n")
            }
            promptBuilder.append("Analyze the screen and goal: '$goal'. Decide the next action now.")

            val requestJson = JSONObject()
            val contents = JSONArray()
            val userContent = JSONObject().put("role", "user")
            val parts = JSONArray()

            // 1. Text part
            parts.put(JSONObject().put("text", "$systemInstruction\n\n${promptBuilder}"))

            // 2. Image part if screenshot available
            if (!screenshotBase64.isNullOrBlank()) {
                val inlineData = JSONObject().apply {
                    put("mime_type", "image/jpeg")
                    put("data", screenshotBase64)
                }
                parts.put(JSONObject().put("inline_data", inlineData))
            }

            userContent.put("parts", parts)
            contents.put(userContent)
            requestJson.put("contents", contents)

            val generationConfig = JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            }
            requestJson.put("generationConfig", generationConfig)

            val url = if (!apiKey.isNullOrBlank()) {
                "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent?key=$apiKey"
            } else {
                "https://generativelanguage.googleapis.com/v1beta/models/$DEFAULT_MODEL:generateContent"
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))

            if (!authToken.isNullOrBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $authToken")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            val bodyString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error ${response.code}: $bodyString")
                return@withContext Result.failure(IllegalStateException("Gemini Vision API error ${response.code}: ${bodyString.take(150)}"))
            }

            val resObj = JSONObject(bodyString)
            val candidates = resObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext Result.failure(IllegalStateException("No candidates returned from Gemini Vision"))
            }
            val textContent = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            val parsedOutput = parseActionOutput(textContent)
            Result.success(parsedOutput)
        } catch (e: Exception) {
            Log.e(TAG, "Error in decideNextAction", e)
            Result.failure(e)
        }
    }

    private fun parseActionOutput(text: String): Pair<String, VisionAction> {
        val cleaned = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(cleaned)
        val thought = json.optString("thought", "Analyzing screen state")
        val actionObj = json.optJSONObject("action") ?: json

        val typeStr = actionObj.optString("type", "WAIT").uppercase()
        val type = runCatching { VisionActionType.valueOf(typeStr) }.getOrDefault(VisionActionType.WAIT)

        val action = VisionAction(
            type = type,
            x = if (actionObj.has("x")) actionObj.optDouble("x").toFloat() else null,
            y = if (actionObj.has("y")) actionObj.optDouble("y").toFloat() else null,
            elementId = actionObj.optString("elementId").takeIf { it.isNotBlank() },
            text = actionObj.optString("text").takeIf { it.isNotBlank() },
            clearFirst = actionObj.optBoolean("clearFirst", false),
            pressEnter = actionObj.optBoolean("pressEnter", false),
            direction = actionObj.optString("direction").takeIf { it.isNotBlank() },
            startX = if (actionObj.has("startX")) actionObj.optDouble("startX").toFloat() else null,
            startY = if (actionObj.has("startY")) actionObj.optDouble("startY").toFloat() else null,
            endX = if (actionObj.has("endX")) actionObj.optDouble("endX").toFloat() else null,
            endY = if (actionObj.has("endY")) actionObj.optDouble("endY").toFloat() else null,
            key = actionObj.optString("key").takeIf { it.isNotBlank() },
            globalAction = actionObj.optString("globalAction").takeIf { it.isNotBlank() },
            packageName = actionObj.optString("packageName").takeIf { it.isNotBlank() },
            intentUri = actionObj.optString("intentUri").takeIf { it.isNotBlank() },
            durationMs = actionObj.optLong("durationMs", 350L),
            reasoning = actionObj.optString("reasoning", thought),
            userPrompt = actionObj.optString("userPrompt").takeIf { it.isNotBlank() }
        )

        return Pair(thought, action)
    }
}
