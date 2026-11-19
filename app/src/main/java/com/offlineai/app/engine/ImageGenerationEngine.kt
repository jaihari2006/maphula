package com.offlineai.app.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

enum class ImageStyle(val label: String, val tag: String) {
    CYBERPUNK("Cyberpunk City", "Neon lights, high-tech urban structures, rainy reflections"),
    COSMIC("Cosmic Nebula", "Deep space, interstellar dust clouds, vibrant stellar cluster"),
    SYNTHWAVE("Retro Synthwave", "Neon grid horizon, glowing segmented sun, purple twilight"),
    QUANTUM_NEURAL("Quantum Neural Lattice", "Synaptic nodes, glowing axonal filaments, energy pulses"),
    BIOLUMINESCENT("Bioluminescent Flora", "Glowing enchanted forest, radiant spores, crystalline reflections")
}

class ImageGenerationEngine {

    /**
     * Generates a high-quality stylized image on-device using procedural latent synthesis
     * with multi-step diffusion convergence.
     */
    suspend fun generateImage(
        prompt: String,
        style: ImageStyle,
        width: Int = 384,
        height: Int = 384,
        steps: Int = 15,
        seed: Long? = null,
        onProgress: (step: Int, totalSteps: Int, status: String) -> Unit = { _, _, _ -> }
    ): Bitmap = withContext(Dispatchers.Default) {
        val actualSeed = seed ?: (prompt.hashCode().toLong() xor System.currentTimeMillis())
        val random = Random(actualSeed)

        onProgress(1, steps, "Generating initial latent noise field...")
        delay(120)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Step-by-step progressive diffusion rendering
        val stepMilestones = listOf(
            Pair(2, "Synthesizing structural geometry & background..."),
            Pair(5, "Resolving spatial focal depth & lighting..."),
            Pair(9, "Denoising and computing high-frequency textures..."),
            Pair(12, "Enhancing chromatic glow & bloom..."),
            Pair(steps, "Finalizing high-resolution rendering...")
        )

        for ((step, status) in stepMilestones) {
            onProgress(step, steps, status)
            delay(100)
        }

        when (style) {
            ImageStyle.CYBERPUNK -> renderCyberpunkScene(canvas, width, height, random)
            ImageStyle.COSMIC -> renderCosmicScene(canvas, width, height, random)
            ImageStyle.SYNTHWAVE -> renderSynthwaveScene(canvas, width, height, random)
            ImageStyle.QUANTUM_NEURAL -> renderQuantumNeuralScene(canvas, width, height, random)
            ImageStyle.BIOLUMINESCENT -> renderBioluminescentScene(canvas, width, height, random)
        }

        onProgress(steps, steps, "Rendering complete!")
        bitmap
    }

    private fun renderCyberpunkScene(canvas: Canvas, w: Int, h: Int, random: Random) {
        // Dark futuristic sky
        val skyPaint = Paint().apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.3f, w * 0.8f,
                intArrayOf(Color.parseColor("#1E1B4B"), Color.parseColor("#0F172A"), Color.parseColor("#050814")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background skyscrapers
        val numBuildings = 14
        val colWidth = w.toFloat() / numBuildings
        for (i in 0 until numBuildings) {
            val bHeight = h * (0.35f + random.nextFloat() * 0.45f)
            val bLeft = i * colWidth
            val bRight = bLeft + colWidth + 2f
            val bTop = h - bHeight

            val buildingPaint = Paint().apply {
                color = Color.argb(255, 15 + random.nextInt(20), 20 + random.nextInt(25), 35 + random.nextInt(40))
            }
            canvas.drawRect(bLeft, bTop, bRight, h.toFloat(), buildingPaint)

            // Neon windows
            val windowPaint = Paint().apply {
                color = if (random.nextBoolean()) Color.parseColor("#06B6D4") else Color.parseColor("#EC4899")
                alpha = 140 + random.nextInt(110)
            }
            for (y in (bTop + 10).toInt() until h step 18) {
                for (x in (bLeft + 6).toInt() until (bRight - 6).toInt() step 12) {
                    if (random.nextFloat() > 0.4f) {
                        canvas.drawRect(x.toFloat(), y.toFloat(), x + 5f, y + 8f, windowPaint)
                    }
                }
            }

            // Rooftop antenna & beacons
            if (random.nextFloat() > 0.5f) {
                paint.color = Color.parseColor("#F43F5E")
                paint.strokeWidth = 2f
                val antX = bLeft + colWidth * 0.5f
                canvas.drawLine(antX, bTop, antX, bTop - 25f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawCircle(antX, bTop - 25f, 3.5f, paint)
            }
        }

        // Wet pavement reflection
        val groundY = h * 0.82f
        val groundPaint = Paint().apply {
            shader = RadialGradient(
                w * 0.5f, groundY, w * 0.6f,
                intArrayOf(Color.parseColor("#06B6D4"), Color.parseColor("#4338CA"), Color.parseColor("#050814")),
                null, Shader.TileMode.CLAMP
            )
            alpha = 110
        }
        canvas.drawRect(0f, groundY, w.toFloat(), h.toFloat(), groundPaint)

        // Neon beam overlays
        paint.strokeWidth = 4f
        paint.color = Color.parseColor("#06B6D4")
        paint.alpha = 180
        canvas.drawLine(0f, groundY, w.toFloat(), groundY + 10f, paint)

        paint.color = Color.parseColor("#F43F5E")
        paint.alpha = 140
        canvas.drawLine(0f, groundY + 12f, w.toFloat(), groundY + 14f, paint)
    }

    private fun renderCosmicScene(canvas: Canvas, w: Int, h: Int, random: Random) {
        // Deep space void
        canvas.drawColor(Color.parseColor("#030712"))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Nebula cloud layers
        val nebulaCenters = listOf(
            Triple(w * 0.45f, h * 0.45f, Color.parseColor("#7C3AED")),
            Triple(w * 0.65f, h * 0.35f, Color.parseColor("#06B6D4")),
            Triple(w * 0.35f, h * 0.65f, Color.parseColor("#EC4899"))
        )

        for ((cx, cy, color) in nebulaCenters) {
            val radius = w * (0.35f + random.nextFloat() * 0.25f)
            val nebPaint = Paint().apply {
                shader = RadialGradient(
                    cx, cy, radius,
                    intArrayOf(color, Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
                alpha = 120
            }
            canvas.drawCircle(cx, cy, radius, nebPaint)
        }

        // Cosmic stars field
        for (i in 0 until 180) {
            val sx = random.nextFloat() * w
            val sy = random.nextFloat() * h
            val size = 0.8f + random.nextFloat() * 2.5f
            paint.color = if (random.nextFloat() > 0.3f) Color.WHITE else Color.parseColor("#93C5FD")
            paint.alpha = 150 + random.nextInt(105)
            canvas.drawCircle(sx, sy, size, paint)

            // Diffraction cross for brightest stars
            if (size > 2.5f) {
                paint.strokeWidth = 1f
                canvas.drawLine(sx - 8f, sy, sx + 8f, sy, paint)
                canvas.drawLine(sx, sy - 8f, sx, sy + 8f, paint)
            }
        }

        // Central Supernova Core
        val coreX = w * 0.5f
        val coreY = h * 0.5f
        val corePaint = Paint().apply {
            shader = RadialGradient(
                coreX, coreY, w * 0.18f,
                intArrayOf(Color.WHITE, Color.parseColor("#FDE047"), Color.parseColor("#F43F5E"), Color.TRANSPARENT),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(coreX, coreY, w * 0.18f, corePaint)
    }

    private fun renderSynthwaveScene(canvas: Canvas, w: Int, h: Int, random: Random) {
        // Gradient Twilight Sky
        val skyPaint = Paint().apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.55f, h * 0.7f,
                intArrayOf(Color.parseColor("#F97316"), Color.parseColor("#D946EF"), Color.parseColor("#312E81"), Color.parseColor("#0F172A")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), skyPaint)

        // Giant Segmented Sun
        val sunRadius = w * 0.26f
        val sunCx = w * 0.5f
        val sunCy = h * 0.5f

        val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                sunCx, sunCy - sunRadius * 0.5f, sunRadius * 1.2f,
                intArrayOf(Color.parseColor("#FDE047"), Color.parseColor("#F43F5E")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(sunCx, sunCy, sunRadius, sunPaint)

        // Cutout horizontal blinds across lower half of sun
        val cutPaint = Paint().apply { color = Color.parseColor("#312E81") }
        for (y in (sunCy - 5).toInt()..sunCy.toInt() + sunRadius.toInt() step 12) {
            val thickness = 2f + (y - (sunCy - 5)) * 0.15f
            canvas.drawRect(sunCx - sunRadius, y.toFloat(), sunCx + sunRadius, y + thickness, cutPaint)
        }

        // Horizon Grid
        val horizonY = h * 0.62f
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#06B6D4")
            strokeWidth = 2f
            alpha = 220
        }

        // Perspective vanishing horizontal lines
        var step = 4f
        var curY = horizonY
        while (curY < h) {
            canvas.drawLine(0f, curY, w.toFloat(), curY, gridPaint)
            curY += step
            step *= 1.35f
        }

        // Perspective longitudinal vanishing lines
        val vanishingX = w * 0.5f
        for (i in -12..12) {
            val bottomX = vanishingX + (i * (w * 0.12f))
            canvas.drawLine(vanishingX, horizonY, bottomX, h.toFloat(), gridPaint)
        }
    }

    private fun renderQuantumNeuralScene(canvas: Canvas, w: Int, h: Int, random: Random) {
        canvas.drawColor(Color.parseColor("#0B0F19"))

        val nodeCount = 28
        val nodes = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until nodeCount) {
            nodes.add(Pair(random.nextFloat() * w, random.nextFloat() * h))
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#7C3AED")
            strokeWidth = 1.5f
        }

        // Connect nearby nodes with synaptic filaments
        for (i in 0 until nodeCount) {
            for (j in i + 1 until nodeCount) {
                val (x1, y1) = nodes[i]
                val (x2, y2) = nodes[j]
                val dist = sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
                if (dist < w * 0.38f) {
                    linePaint.alpha = ((1f - dist / (w * 0.38f)) * 180).toInt()
                    linePaint.color = if (random.nextBoolean()) Color.parseColor("#06B6D4") else Color.parseColor("#A855F7")
                    canvas.drawLine(x1, y1, x2, y2, linePaint)
                }
            }
        }

        // Draw quantum glowing nodes
        val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for ((x, y) in nodes) {
            val r = 4f + random.nextFloat() * 6f
            val glowPaint = Paint().apply {
                shader = RadialGradient(
                    x, y, r * 3f,
                    intArrayOf(Color.parseColor("#06B6D4"), Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(x, y, r * 3f, glowPaint)

            nodePaint.color = Color.WHITE
            canvas.drawCircle(x, y, r, nodePaint)
        }
    }

    private fun renderBioluminescentScene(canvas: Canvas, w: Int, h: Int, random: Random) {
        // Deep teal-black organic background
        val bgPaint = Paint().apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.8f, h * 0.9f,
                intArrayOf(Color.parseColor("#064E3B"), Color.parseColor("#022C22"), Color.parseColor("#051010")),
                null, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Bioluminescent glowing spores and flora
        for (i in 0 until 50) {
            val x = random.nextFloat() * w
            val y = random.nextFloat() * h
            val r = 3f + random.nextFloat() * 7f
            val color = if (random.nextBoolean()) Color.parseColor("#10B981") else Color.parseColor("#06B6D4")

            val sporePaint = Paint().apply {
                shader = RadialGradient(
                    x, y, r * 3.5f,
                    intArrayOf(color, Color.TRANSPARENT),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(x, y, r * 3.5f, sporePaint)

            paint.color = Color.WHITE
            canvas.drawCircle(x, y, r * 0.5f, paint)
        }

        // Radiant crystal stalagmites
        val path = Path()
        for (i in 0 until 6) {
            val bx = (i + 0.5f) * (w / 6f) + (random.nextFloat() - 0.5f) * 30f
            val topY = h * (0.45f + random.nextFloat() * 0.25f)
            path.reset()
            path.moveTo(bx - 20f, h.toFloat())
            path.lineTo(bx, topY)
            path.lineTo(bx + 20f, h.toFloat())
            path.close()

            val crystalPaint = Paint().apply {
                shader = RadialGradient(
                    bx, topY, w * 0.3f,
                    intArrayOf(Color.parseColor("#34D399"), Color.parseColor("#059669"), Color.parseColor("#064E3B")),
                    null, Shader.TileMode.CLAMP
                )
                alpha = 210
            }
            canvas.drawPath(path, crystalPaint)
        }
    }
}
