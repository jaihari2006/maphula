package com.offlineai.app.ui.clock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Precision-rendered Analog Clock Canvas for Maphula.
 * Synchronized with the top right 8-bit digital clock.
 * Supports smooth continuous sweeping hand and custom theme adaptation.
 */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dialColor: Int = Color.parseColor("#0F172A")
    private var bezelColor: Int = Color.parseColor("#06B6D4")
    private var numeralColor: Int = Color.parseColor("#F8FAFC")
    private var tickColor: Int = Color.parseColor("#64748B")
    private var hourHandColor: Int = Color.parseColor("#E2E8F0")
    private var minuteHandColor: Int = Color.parseColor("#38BDF8")
    private var secondHandColor: Int = Color.parseColor("#F43F5E")

    var isSmoothSweep: Boolean = true

    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val numeralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
    }

    private val minuteHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
    }

    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f
    }

    private val centerPivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textBounds = Rect()

    fun setThemeColors(accent: Int, secondary: Int, background: Int) {
        bezelColor = accent
        minuteHandColor = accent
        secondHandColor = secondary
        dialColor = background
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val cy = h / 2f
        val radius = min(cx, cy) - 20f
        if (radius <= 0) return

        // 1. Draw Dial Background & Outer Bezel
        dialPaint.color = dialColor
        canvas.drawCircle(cx, cy, radius, dialPaint)

        bezelPaint.color = bezelColor
        canvas.drawCircle(cx, cy, radius, bezelPaint)

        // Subtle inner ring
        bezelPaint.color = Color.argb(45, 255, 255, 255)
        bezelPaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, radius * 0.92f, bezelPaint)
        bezelPaint.strokeWidth = 6f

        // 2. Draw Ticks & Roman/Standard Numerals
        numeralPaint.textSize = radius * 0.13f
        numeralPaint.color = numeralColor

        for (i in 1..60) {
            val angleDeg = (i * 6f) - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val isHour = i % 5 == 0
            val innerR = if (isHour) radius * 0.82f else radius * 0.88f
            val outerR = radius * 0.91f

            val startX = (cx + innerR * cos(angleRad)).toFloat()
            val startY = (cy + innerR * sin(angleRad)).toFloat()
            val stopX = (cx + outerR * cos(angleRad)).toFloat()
            val stopY = (cy + outerR * sin(angleRad)).toFloat()

            if (isHour) {
                tickPaint.color = bezelColor
                tickPaint.strokeWidth = 4f
                canvas.drawLine(startX, startY, stopX, stopY, tickPaint)

                // Hour Number
                val hourNum = (i / 5).toString()
                val numR = radius * 0.70f
                val numX = (cx + numR * cos(angleRad)).toFloat()
                val numY = (cy + numR * sin(angleRad)).toFloat()

                numeralPaint.getTextBounds(hourNum, 0, hourNum.length, textBounds)
                val textYAdjusted = numY + (textBounds.height() / 2f)
                canvas.drawText(hourNum, numX, textYAdjusted, numeralPaint)
            } else {
                tickPaint.color = tickColor
                tickPaint.strokeWidth = 2f
                canvas.drawLine(startX, startY, stopX, stopY, tickPaint)
            }
        }

        // 3. Current Time Angles
        val cal = Calendar.getInstance()
        val hours = cal.get(Calendar.HOUR)
        val minutes = cal.get(Calendar.MINUTE)
        val seconds = cal.get(Calendar.SECOND)
        val millis = cal.get(Calendar.MILLISECOND)

        val secFraction = if (isSmoothSweep) (seconds + millis / 1000f) else seconds.toFloat()
        val secAngle = Math.toRadians(((secFraction * 6.0) - 90.0))

        val minFraction = minutes + seconds / 60f
        val minAngle = Math.toRadians(((minFraction * 6.0) - 90.0))

        val hourFraction = (hours % 12) + minutes / 60f + seconds / 3600f
        val hourAngle = Math.toRadians(((hourFraction * 30.0) - 90.0))

        // 4. Draw Hour Hand
        hourHandPaint.color = hourHandColor
        val hourLen = radius * 0.48f
        val hourX = (cx + hourLen * cos(hourAngle)).toFloat()
        val hourY = (cy + hourLen * sin(hourAngle)).toFloat()
        canvas.drawLine(cx, cy, hourX, hourY, hourHandPaint)

        // 5. Draw Minute Hand
        minuteHandPaint.color = minuteHandColor
        val minLen = radius * 0.70f
        val minX = (cx + minLen * cos(minAngle)).toFloat()
        val minY = (cy + minLen * sin(minAngle)).toFloat()
        canvas.drawLine(cx, cy, minX, minY, minuteHandPaint)

        // 6. Draw Second Hand with Tail Counterweight
        secondHandPaint.color = secondHandColor
        val secLen = radius * 0.85f
        val secTailLen = radius * 0.20f

        val secX = (cx + secLen * cos(secAngle)).toFloat()
        val secY = (cy + secLen * sin(secAngle)).toFloat()
        val secTailX = (cx - secTailLen * cos(secAngle)).toFloat()
        val secTailY = (cy - secTailLen * sin(secAngle)).toFloat()

        canvas.drawLine(secTailX, secTailY, secX, secY, secondHandPaint)

        // 7. Center Pivot
        centerPivotPaint.color = secondHandColor
        canvas.drawCircle(cx, cy, 10f, centerPivotPaint)
        centerPivotPaint.color = numeralColor
        canvas.drawCircle(cx, cy, 4f, centerPivotPaint)

        // Schedule next smooth frame
        if (isAttachedToWindow) {
            postInvalidateOnAnimation()
        }
    }
}
