package com.example.gemini.data.agent.vision

import android.content.Context

object ScreenVisionSafetyGuard {

    private val DANGEROUS_PACKAGES = setOf(
        "com.google.android.apps.walletnfcrel",
        "com.phonepe.app",
        "net.one97.paytm",
        "com.google.android.apps.nbu.paisa.user",
        "com.android.vending" // Google Play Store purchase screens
    )

    private val SENSITIVE_KEYWORDS = listOf(
        "delete", "uninstall", "format", "transfer", "pay", "buy",
        "purchase", "subscribe", "password", "pin", "otp", "send money"
    )

    fun evaluateSafety(
        context: Context,
        action: VisionAction,
        foregroundPackage: String,
        screenText: String
    ): SafetyResult {
        // 1. Payment or Wallet apps
        if (foregroundPackage in DANGEROUS_PACKAGES) {
            if (action.type == VisionActionType.TAP || action.type == VisionActionType.TYPE_TEXT) {
                return SafetyResult.RequiresApproval("Action touches a financial/payment app ($foregroundPackage). Confirm to proceed.")
            }
        }

        // 2. Sensitive text or delete/uninstall
        val combined = "${action.reasoning} ${action.text.orEmpty()} ${action.packageName.orEmpty()}".lowercase()
        for (kw in SENSITIVE_KEYWORDS) {
            if (combined.contains(kw) || screenText.lowercase().contains(kw)) {
                if (action.type == VisionActionType.TAP || action.type == VisionActionType.TYPE_TEXT) {
                    return SafetyResult.RequiresApproval("Action involves sensitive operation ('$kw'). Confirm to execute.")
                }
            }
        }

        // 3. Destructive key combos or global actions
        if (action.type == VisionActionType.GLOBAL_ACTION && action.globalAction == "power_dialog") {
            return SafetyResult.Blocked("Triggering power dialog is blocked for safety.")
        }

        return SafetyResult.Allowed
    }

    sealed class SafetyResult {
        object Allowed : SafetyResult()
        data class RequiresApproval(val prompt: String) : SafetyResult()
        data class Blocked(val reason: String) : SafetyResult()
    }
}
