package com.example.gemini.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Global RGB Chroma Lighting Manager: provides real-time animated RGB rainbow
 * lighting effects across the entire UI, accents, borders, and text.
 */
object RgbLightingManager {
    private const val PREFS = "rgb_lighting_prefs"
    private const val KEY_ENABLED = "rgb_enabled"
    private const val KEY_SPEED = "rgb_speed"
    private const val KEY_INTENSITY = "rgb_intensity"
    private const val KEY_TEXT_GLOW = "rgb_text_glow"
    private const val KEY_BORDER_GLOW = "rgb_border_glow"

    private var prefs: SharedPreferences? = null

    var isRgbEnabled by mutableStateOf(false)
        private set
    var speed by mutableFloatStateOf(1.0f)
        private set
    var intensity by mutableFloatStateOf(1.0f)
        private set
    var textGlow by mutableStateOf(true)
        private set
    var borderGlow by mutableStateOf(true)
        private set

    // Real-time animated hue phase (0..360)
    var currentHue by mutableFloatStateOf(0f)

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        isRgbEnabled = p.getBoolean(KEY_ENABLED, false)
        speed = p.getFloat(KEY_SPEED, 1.0f).coerceIn(0.2f, 3.0f)
        intensity = p.getFloat(KEY_INTENSITY, 1.0f).coerceIn(0.2f, 1.0f)
        textGlow = p.getBoolean(KEY_TEXT_GLOW, true)
        borderGlow = p.getBoolean(KEY_BORDER_GLOW, true)
    }

    fun setEnabled(enabled: Boolean) {
        isRgbEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_ENABLED, enabled)?.apply()
    }

    fun setSpeedValue(s: Float) {
        val clamped = s.coerceIn(0.2f, 3.0f)
        speed = clamped
        prefs?.edit()?.putFloat(KEY_SPEED, clamped)?.apply()
    }

    fun setIntensityValue(i: Float) {
        val clamped = i.coerceIn(0.2f, 1.0f)
        intensity = clamped
        prefs?.edit()?.putFloat(KEY_INTENSITY, clamped)?.apply()
    }

    fun setTextGlowEnabled(enabled: Boolean) {
        textGlow = enabled
        prefs?.edit()?.putBoolean(KEY_TEXT_GLOW, enabled)?.apply()
    }

    fun setBorderGlowEnabled(enabled: Boolean) {
        borderGlow = enabled
        prefs?.edit()?.putBoolean(KEY_BORDER_GLOW, enabled)?.apply()
    }

    // Dynamic animated color getters
    val primaryRgbColor: Color
        get() {
            if (!isRgbEnabled) return AppColors.primary
            return Color.hsv(currentHue, 0.85f * intensity, 1.0f)
        }

    val secondaryRgbColor: Color
        get() {
            if (!isRgbEnabled) return AppColors.secondary
            return Color.hsv((currentHue + 60f) % 360f, 0.85f * intensity, 0.95f)
        }

    val tertiaryRgbColor: Color
        get() {
            if (!isRgbEnabled) return AppColors.tertiary
            return Color.hsv((currentHue + 120f) % 360f, 0.85f * intensity, 0.90f)
        }

    fun rainbowColors(): List<Color> {
        val sat = 0.85f * intensity
        return listOf(
            Color.hsv((currentHue) % 360f, sat, 1f),
            Color.hsv((currentHue + 45f) % 360f, sat, 1f),
            Color.hsv((currentHue + 90f) % 360f, sat, 1f),
            Color.hsv((currentHue + 135f) % 360f, sat, 1f),
            Color.hsv((currentHue + 180f) % 360f, sat, 1f),
            Color.hsv((currentHue + 225f) % 360f, sat, 1f),
            Color.hsv((currentHue + 270f) % 360f, sat, 1f),
            Color.hsv((currentHue + 315f) % 360f, sat, 1f),
            Color.hsv((currentHue) % 360f, sat, 1f)
        )
    }

    val rainbowBrush: Brush
        get() = Brush.linearGradient(rainbowColors())
}

val LocalRgbLighting = staticCompositionLocalOf { false }

/**
 * Animation driver host that smoothly animates RGB hue when RGB mode is active.
 */
@Composable
fun RgbLightingHost(content: @Composable () -> Unit) {
    if (RgbLightingManager.isRgbEnabled) {
        val infiniteTransition = rememberInfiniteTransition(label = "RgbHueTransition")
        val animatedHue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (6000 / RgbLightingManager.speed.coerceAtLeast(0.2f)).toInt(),
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            ),
            label = "RgbHue"
        )

        LaunchedEffect(animatedHue) {
            RgbLightingManager.currentHue = animatedHue
        }
    }

    CompositionLocalProvider(LocalRgbLighting provides RgbLightingManager.isRgbEnabled) {
        content()
    }
}

/**
 * Modifier to apply animated RGB border to any surface or card.
 */
fun Modifier.rgbChromaBorder(
    width: Dp = 1.5.dp,
    shape: Shape = RoundedCornerShape(12.dp)
): Modifier = this.then(
    if (RgbLightingManager.isRgbEnabled && RgbLightingManager.borderGlow) {
        Modifier.border(width, RgbLightingManager.rainbowBrush, shape)
    } else {
        Modifier
    }
)

/**
 * Modifier to apply glowing RGB rainbow shader to text or icons.
 */
fun Modifier.rgbChromaText(): Modifier = this.then(
    if (RgbLightingManager.isRgbEnabled && RgbLightingManager.textGlow) {
        Modifier.drawWithContent {
            drawContent()
        }
    } else {
        Modifier
    }
)
