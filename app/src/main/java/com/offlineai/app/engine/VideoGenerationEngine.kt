package com.offlineai.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class VideoMotionMode(val label: String, val description: String) {
    COSMIC_ORBIT("Cosmic Warp & Orbit", "Orbiting stellar cluster with rotating deep-space perspective"),
    NEURAL_PULSE("Neural Synapse Pulses", "Action potentials and luminous energy packets traveling across nodes"),
    QUANTUM_WAVE("Quantum Wave Superposition", "Harmonic undulating wave interference with chromatic bloom"),
    CYBER_MATRIX("Cyber Data Streams", "Cascading phosphor code trails streaming down multi-column matrix"),
    SYNTHWAVE_DRIVE("Synthwave Grid Drive", "Infinite forward velocity down perspective neon grid toward sunset")
}

class VideoGenerationEngine {

    /**
     * Generates a smooth, loopable sequence of video frames based on generative motion parameters.
     */
    suspend fun generateVideoFrames(
        mode: VideoMotionMode,
        frameCount: Int = 24,
        width: Int = 320,
        height: Int = 320,
        seed: Long = System.currentTimeMillis(),
        onProgress: (renderedFrame: Int, totalFrames: Int) -> Unit = { _, _ -> }
    ): List<Bitmap> = withContext(Dispatchers.Default) {
        val frames = mutableListOf<Bitmap>()
        val random = Random(seed)

        for (frameIdx in 0 until frameCount) {
            val t = frameIdx.toFloat() / frameCount // normalized cycle [0.0 .. 1.0)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            when (mode) {
                VideoMotionMode.COSMIC_ORBIT -> renderCosmicFrame(canvas, width, height, t, random)
                VideoMotionMode.NEURAL_PULSE -> renderNeuralFrame(canvas, width, height, t, random)
                VideoMotionMode.QUANTUM_WAVE -> renderWaveFrame(canvas, width, height, t)
                VideoMotionMode.CYBER_MATRIX -> renderMatrixFrame(canvas, width, height, t, random)
                VideoMotionMode.SYNTHWAVE_DRIVE -> renderDriveFrame(canvas, width, height, t)
            }

            frames.add(bitmap)
            onProgress(frameIdx + 1, frameCount)
        }

        frames
    }

    private fun renderCosmicFrame(canvas: Canvas, w: Int, h: Int, t: Float, random: Random) {
        canvas.drawColor(Color.parseColor("#030712"))

        val cx = w * 0.5f
        val cy = h * 0.5f
        val angle = t * 2 * PI.toFloat()

        // Rotating nebula center
        val nebPaint = Paint().apply {
            shader = RadialGradient(
                cx + cos(angle) * 30f, cy + sin(angle) * 30f, w * 0.45f,
                intArrayOf(Color.parseColor("#7C3AED"), Color.parseColor("#06B6D4"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
            alpha = 140
        }
        canvas.drawCircle(cx, cy, w * 0.45f, nebPaint)

        // Orbiting star field
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val numStars = 60
        for (i in 0 until numStars) {
            val starDist = (30f + (i * 4f)) % (w * 0.48f)
            val starAngle = (i * 0.35f) + angle * (1.2f - (starDist / w))
            val sx = cx + cos(starAngle) * starDist
            val sy = cy + sin(starAngle) * starDist

            paint.color = if (i % 3 == 0) Color.parseColor("#93C5FD") else Color.WHITE
            val size = 1.2f + (i % 4) * 0.7f
            canvas.drawCircle(sx, sy, size, paint)
        }

        // Central Supernova Core
        val corePaint = Paint().apply {
            shader = RadialGradient(
                cx, cy, 25f + sin(angle * 2) * 5f,
                intArrayOf(Color.WHITE, Color.parseColor("#FDE047"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(cx, cy, 30f, corePaint)
    }

    private fun renderNeuralFrame(canvas: Canvas, w: Int, h: Int, t: Float, random: Random) {
        canvas.drawColor(Color.parseColor("#0B0F19"))

        // Fixed network node positions
        val nodes = listOf(
            Pair(w * 0.2f, h * 0.3f),
            Pair(w * 0.2f, h * 0.7f),
            Pair(w * 0.5f, h * 0.2f),
            Pair(w * 0.5f, h * 0.5f),
            Pair(w * 0.5f, h * 0.8f),
            Pair(w * 0.8f, h * 0.35f),
            Pair(w * 0.8f, h * 0.65f)
        )

        val edges = listOf(
            Pair(0, 2), Pair(0, 3), Pair(1, 3), Pair(1, 4),
            Pair(2, 5), Pair(3, 5), Pair(3, 6), Pair(4, 6)
        )

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#334155")
            strokeWidth = 2f
        }

        // Draw static connections
        for ((u, v) in edges) {
            val (x1, y1) = nodes[u]
            val (x2, y2) = nodes[v]
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        // Draw traveling action potentials / pulses
        val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06B6D4")
        }

        for (i in edges.indices) {
            val (u, v) = edges[i]
            val (x1, y1) = nodes[u]
            val (x2, y2) = nodes[v]

            val pulseT = (t + (i * 0.15f)) % 1.0f
            val px = x1 + (x2 - x1) * pulseT
            val py = y1 + (y2 - y1) * pulseT

            val glowPaint = Paint().apply {
                shader = RadialGradient(
                    px, py, 14f,
                    intArrayOf(Color.parseColor("#06B6D4"), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(px, py, 14f, glowPaint)
            pulsePaint.color = Color.WHITE
            canvas.drawCircle(px, py, 4f, pulsePaint)
        }

        // Draw glowing nodes
        val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in nodes.indices) {
            val (nx, ny) = nodes[i]
            val isFiring = sin(t * 2 * PI.toFloat() + i) > 0.3f
            nodePaint.color = if (isFiring) Color.parseColor("#A855F7") else Color.parseColor("#475569")

            canvas.drawCircle(nx, ny, 8f, nodePaint)
            if (isFiring) {
                val glow = Paint().apply {
                    shader = RadialGradient(
                        nx, ny, 20f,
                        intArrayOf(Color.parseColor("#C084FC"), Color.TRANSPARENT),
                        null, Shader.TileMode.CLAMP
                    )
                }
                canvas.drawCircle(nx, ny, 20f, glow)
            }
        }
    }

    private fun renderWaveFrame(canvas: Canvas, w: Int, h: Int, t: Float) {
        canvas.drawColor(Color.parseColor("#050510"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }

        val numLines = 8
        for (i in 0 until numLines) {
            val phaseOffset = i * (PI.toFloat() / numLines)
            val freq = 2.5f + (i * 0.3f)
            val amp = (h * 0.12f) + (i * 4f)
            val baseCenterY = h * 0.5f

            val path = Path()
            val color = if (i % 2 == 0) Color.parseColor("#06B6D4") else Color.parseColor("#EC4899")
            paint.color = color
            paint.alpha = 160 + (i * 10)

            for (x in 0..w step 4) {
                val xNorm = x.toFloat() / w
                val y = baseCenterY + sin(xNorm * freq * 2 * PI.toFloat() + (t * 2 * PI.toFloat()) + phaseOffset) * amp
                if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun renderMatrixFrame(canvas: Canvas, w: Int, h: Int, t: Float, random: Random) {
        canvas.drawColor(Color.parseColor("#020B05"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            color = Color.parseColor("#10B981")
        }

        val cols = 16
        val colWidth = w / cols.toFloat()

        for (c in 0 until cols) {
            val speed = 1.0f + (c % 4) * 0.5f
            val startY = ((t * speed * h) + (c * 35f)) % (h + 80f) - 80f

            for (row in 0 until 10) {
                val charY = startY + row * 16f
                if (charY in 0f..h.toFloat()) {
                    val isHead = row == 9
                    paint.color = if (isHead) Color.WHITE else Color.parseColor("#10B981")
                    paint.alpha = if (isHead) 255 else (row * 24)
                    val glyph = ((c * 7 + row) % 9 + 48).toChar().toString()
                    canvas.drawText(glyph, c * colWidth + 4f, charY, paint)
                }
            }
        }
    }

    private fun renderDriveFrame(canvas: Canvas, w: Int, h: Int, t: Float) {
        val skyPaint = Paint().apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.45f, h * 0.6f,
                intArrayOf(Color.parseColor("#F97316"), Color.parseColor("#D946EF"), Color.parseColor("#1E1B4B")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)

        // Pulsing sun
        val sunCy = h * 0.45f
        val sunCx = w * 0.5f
        val sunR = w * 0.22f + sin(t * 2 * PI.toFloat()) * 4f
        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                sunCx, sunCy, sunR,
                intArrayOf(Color.parseColor("#FDE047"), Color.parseColor("#F43F5E")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(sunCx, sunCy, sunR, sunPaint)

        // Moving ground grid
        val horizonY = h * 0.56f
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06B6D4")
            strokeWidth = 2f
        }

        // Animate horizontal grid lines rushing forward
        val offset = (t * 24f) % 18f
        var curY = horizonY + offset
        var step = 8f
        while (curY < h) {
            canvas.drawLine(0f, curY, w.toFloat(), curY, gridPaint)
            curY += step
            step *= 1.3f
        }

        // Perspective longitudinal lines
        for (i in -10..10) {
            canvas.drawLine(sunCx, horizonY, sunCx + (i * w * 0.14f), h.toFloat(), gridPaint)
        }
    }
}
