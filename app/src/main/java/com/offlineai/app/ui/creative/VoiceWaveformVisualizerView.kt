package com.offlineai.app.ui.creative

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

class VoiceWaveformVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()
    private val targetWaveform = FloatArray(32) { 0.08f }
    private val currentWaveform = FloatArray(32) { 0.08f }
    private var primaryColor: Int = Color.parseColor("#7C3AED")
    private var secondaryColor: Int = Color.parseColor("#06B6D4")
    private var idlePhase: Float = 0f
    private var hasActiveAudio: Boolean = false

    fun setWaveform(data: FloatArray) {
        var anyActive = false
        if (data.size == targetWaveform.size) {
            for (i in data.indices) {
                targetWaveform[i] = data[i].coerceIn(0.04f, 1.0f)
                if (data[i] > 0.12f) anyActive = true
            }
        } else {
            for (i in targetWaveform.indices) {
                val srcIdx = (i * data.size) / targetWaveform.size
                val v = if (srcIdx in data.indices) data[srcIdx] else 0.08f
                targetWaveform[i] = v.coerceIn(0.04f, 1.0f)
                if (v > 0.12f) anyActive = true
            }
        }
        hasActiveAudio = anyActive
        postInvalidateOnAnimation()
    }

    fun setThemeColor(colorHex: String) {
        try {
            primaryColor = Color.parseColor(colorHex)
            postInvalidateOnAnimation()
        } catch (_: Exception) {}
    }

    fun setThemeColors(primary: Int, secondary: Int) {
        primaryColor = primary
        secondaryColor = secondary
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val count = currentWaveform.size
        val barWidth = (w / count) * 0.65f
        val gap = (w / count) * 0.35f
        val centerY = h * 0.5f

        idlePhase += 0.05f
        if (idlePhase > 1000f) idlePhase = 0f

        for (i in 0 until count) {
            val target = if (hasActiveAudio) {
                targetWaveform[i]
            } else {
                // Subtle organic idle wave
                0.09f + 0.06f * sin(idlePhase + i * 0.28f).toFloat()
            }

            // Butter-smooth exponential approach
            currentWaveform[i] += (target - currentWaveform[i]) * 0.38f

            val left = i * (barWidth + gap) + gap * 0.5f
            val right = left + barWidth
            val magnitude = (currentWaveform[i].coerceIn(0.04f, 1.0f) * h * 0.45f)
            val top = centerY - magnitude
            val bottom = centerY + magnitude

            barRect.set(left, top, right, bottom)

            barPaint.shader = LinearGradient(
                left, top, left, bottom,
                intArrayOf(Color.WHITE, primaryColor, secondaryColor),
                floatArrayOf(0.0f, 0.45f, 1.0f),
                Shader.TileMode.CLAMP
            )

            canvas.drawRoundRect(barRect, barWidth * 0.5f, barWidth * 0.5f, barPaint)
        }

        // Keep idle wave animating smoothly if visible
        if (isShown) {
            postInvalidateOnAnimation()
        }
    }
}
