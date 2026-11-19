package com.offlineai.app.ui.neural

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.offlineai.app.engine.NeuralNetwork
import kotlin.math.abs
import kotlin.math.min

class NeuralVisualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var network: NeuralNetwork? = null

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val nodeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#94A3B8")
    }

    private val positiveWeightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#06B6D4")
    }

    private val negativeWeightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#F43F5E")
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
    }

    fun setNetwork(net: NeuralNetwork) {
        this.network = net
        invalidate()
    }

    fun setThemeColors(positiveColor: Int, negativeColor: Int) {
        positiveWeightPaint.color = positiveColor
        negativeWeightPaint.color = negativeColor
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val net = network ?: return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val layerSizes = net.layerSizes
        val numLayers = layerSizes.size
        if (numLayers < 2) return

        val layerSpacing = (w - 120f) / (numLayers - 1)
        val maxNodes = (layerSizes.maxOrNull() ?: 1).toFloat()
        val nodeRadius = min(28f, h / (maxNodes * 4f))

        // Pre-compute node positions (layer -> list of (x, y))
        val nodeCoords = Array(numLayers) { l ->
            val numNodes = layerSizes[l]
            val x = 60f + l * layerSpacing
            val verticalSpacing = (h - 100f) / (numNodes + 1)
            Array(numNodes) { i ->
                val y = 50f + (i + 1) * verticalSpacing
                Pair(x, y)
            }
        }

        // Draw connections (synapses / weights)
        for (l in 0 until numLayers - 1) {
            val fromCoords = nodeCoords[l]
            val toCoords = nodeCoords[l + 1]
            val layerWeights = net.weights[l]

            for (toIdx in toCoords.indices) {
                val (toX, toY) = toCoords[toIdx]
                val weightsTo = layerWeights[toIdx]

                for (fromIdx in fromCoords.indices) {
                    val (fromX, fromY) = fromCoords[fromIdx]
                    val weight = weightsTo[fromIdx]

                    val paint = if (weight >= 0) positiveWeightPaint else negativeWeightPaint
                    val absWeight = abs(weight).toFloat()
                    val strokeW = min(8f, maxOf(1.5f, absWeight * 2f))
                    paint.strokeWidth = strokeW
                    paint.alpha = min(255, maxOf(40, (absWeight * 120).toInt()))

                    canvas.drawLine(fromX, fromY, toX, toY, paint)
                }
            }
        }

        // Draw nodes (neurons)
        for (l in 0 until numLayers) {
            val coords = nodeCoords[l]
            val acts = net.activations[l]

            for (i in coords.indices) {
                val (x, y) = coords[i]
                val act = acts.getOrNull(i) ?: 0.0

                // Fill color interpolates based on activation level
                val alpha = min(255, maxOf(30, (act * 255).toInt()))
                val fillColor = when {
                    l == 0 -> Color.argb(alpha, 124, 58, 237) // Violet input
                    l == numLayers - 1 -> Color.argb(alpha, 16, 185, 129) // Emerald output
                    else -> Color.argb(alpha, 6, 182, 212) // Cyan hidden
                }

                nodePaint.color = fillColor
                canvas.drawCircle(x, y, nodeRadius, nodePaint)
                canvas.drawCircle(x, y, nodeRadius, nodeStrokePaint)

                // Draw activation value text if radius allows
                if (nodeRadius >= 20f) {
                    val actStr = "%.2f".format(act)
                    canvas.drawText(actStr, x, y + 10f, textPaint)
                }
            }
        }
    }
}
