package com.offlineai.app.engine

import java.util.Random
import kotlin.math.exp
import kotlin.math.max

enum class ActivationType {
    SIGMOID,
    TANH,
    RELU
}

data class TrainingSample(
    val inputs: DoubleArray,
    val targets: DoubleArray,
    val label: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TrainingSample
        return inputs.contentEquals(other.inputs) && targets.contentEquals(other.targets)
    }

    override fun hashCode(): Int {
        var result = inputs.contentHashCode()
        result = 31 * result + targets.contentHashCode()
        return result
    }
}

class NeuralNetwork(
    val layerSizes: IntArray,
    private val activation: ActivationType = ActivationType.SIGMOID
) {
    private val random = Random(42)

    // weights[layerIndex][toNeuron][fromNeuron]
    val weights: Array<Array<DoubleArray>> = Array(layerSizes.size - 1) { l ->
        val fromSize = layerSizes[l]
        val toSize = layerSizes[l + 1]
        Array(toSize) {
            DoubleArray(fromSize) {
                // Xavier/Glorot initialization
                (random.nextDouble() * 2.0 - 1.0) / kotlin.math.sqrt(fromSize.toDouble())
            }
        }
    }

    // biases[layerIndex][neuronIndex]
    val biases: Array<DoubleArray> = Array(layerSizes.size - 1) { l ->
        DoubleArray(layerSizes[l + 1]) { (random.nextDouble() * 0.2 - 0.1) }
    }

    // Cached activations during forward pass for backprop and visualization
    // activations[layerIndex][neuronIndex]
    val activations: Array<DoubleArray> = Array(layerSizes.size) { l ->
        DoubleArray(layerSizes[l])
    }

    // Cached pre-activations (z = W*x + b)
    private val zs: Array<DoubleArray> = Array(layerSizes.size - 1) { l ->
        DoubleArray(layerSizes[l + 1])
    }

    // Momentum velocity buffers
    private val weightVelocities: Array<Array<DoubleArray>> = Array(layerSizes.size - 1) { l ->
        Array(layerSizes[l + 1]) { DoubleArray(layerSizes[l]) }
    }
    private val biasVelocities: Array<DoubleArray> = Array(layerSizes.size - 1) { l ->
        DoubleArray(layerSizes[l + 1])
    }

    fun forward(inputs: DoubleArray): DoubleArray {
        require(inputs.size == layerSizes[0]) { "Input size ${inputs.size} does not match layer size ${layerSizes[0]}" }
        System.arraycopy(inputs, 0, activations[0], 0, inputs.size)

        for (l in 0 until layerSizes.size - 1) {
            val fromActs = activations[l]
            val toActs = activations[l + 1]
            val zLayer = zs[l]
            val wLayer = weights[l]
            val bLayer = biases[l]

            for (to in toActs.indices) {
                var sum = bLayer[to]
                val wTo = wLayer[to]
                for (from in fromActs.indices) {
                    sum += wTo[from] * fromActs[from]
                }
                zLayer[to] = sum
                toActs[to] = activate(sum, activation)
            }
        }

        return activations.last().clone()
    }

    fun trainEpoch(samples: List<TrainingSample>, learningRate: Double = 0.2, momentum: Double = 0.8): Double {
        var totalLoss = 0.0

        for (sample in samples) {
            val outputs = forward(sample.inputs)

            // Calculate Mean Squared Error
            var sampleLoss = 0.0
            val outputErrors = DoubleArray(outputs.size)
            for (i in outputs.indices) {
                val err = sample.targets[i] - outputs[i]
                outputErrors[i] = err
                sampleLoss += 0.5 * err * err
            }
            totalLoss += sampleLoss

            // Backpropagation
            // deltas[layerIndex][neuronIndex]
            val deltas = Array(layerSizes.size - 1) { l -> DoubleArray(layerSizes[l + 1]) }

            // Output layer deltas
            val outL = layerSizes.size - 2
            for (i in outputs.indices) {
                val z = zs[outL][i]
                deltas[outL][i] = outputErrors[i] * activateDerivative(z, activation)
            }

            // Hidden layers deltas
            for (l in outL - 1 downTo 0) {
                val currentLSize = layerSizes[l + 1]
                val nextLSize = layerSizes[l + 2]
                for (j in 0 until currentLSize) {
                    var sum = 0.0
                    for (k in 0 until nextLSize) {
                        sum += deltas[l + 1][k] * weights[l + 1][k][j]
                    }
                    deltas[l][j] = sum * activateDerivative(zs[l][j], activation)
                }
            }

            // Update weights & biases with momentum
            for (l in 0 until layerSizes.size - 1) {
                val fromActs = activations[l]
                val wLayer = weights[l]
                val bLayer = biases[l]
                val vWLayer = weightVelocities[l]
                val vBLayer = biasVelocities[l]
                val deltaL = deltas[l]

                for (to in deltaL.indices) {
                    val delta = deltaL[to]

                    // Bias update
                    val vb = momentum * vBLayer[to] + learningRate * delta
                    vBLayer[to] = vb
                    bLayer[to] += vb

                    // Weight update
                    val wTo = wLayer[to]
                    val vWTo = vWLayer[to]
                    for (from in fromActs.indices) {
                        val vw = momentum * vWTo[from] + learningRate * delta * fromActs[from]
                        vWTo[from] = vw
                        wTo[from] += vw
                    }
                }
            }
        }

        return totalLoss / max(1, samples.size)
    }

    private fun activate(z: Double, type: ActivationType): Double {
        return when (type) {
            ActivationType.SIGMOID -> 1.0 / (1.0 + exp(-z.coerceIn(-60.0, 60.0)))
            ActivationType.TANH -> {
                val ez = exp(z.coerceIn(-30.0, 30.0))
                val enz = exp(-z.coerceIn(-30.0, 30.0))
                (ez - enz) / (ez + enz)
            }
            ActivationType.RELU -> max(0.0, z)
        }
    }

    private fun activateDerivative(z: Double, type: ActivationType): Double {
        return when (type) {
            ActivationType.SIGMOID -> {
                val sig = activate(z, ActivationType.SIGMOID)
                sig * (1.0 - sig)
            }
            ActivationType.TANH -> {
                val th = activate(z, ActivationType.TANH)
                1.0 - th * th
            }
            ActivationType.RELU -> if (z > 0) 1.0 else 0.0
        }
    }

    companion object {
        fun createXorNetwork(): Pair<NeuralNetwork, List<TrainingSample>> {
            val net = NeuralNetwork(intArrayOf(2, 4, 1), ActivationType.SIGMOID)
            val samples = listOf(
                TrainingSample(doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0), "0 XOR 0 = 0"),
                TrainingSample(doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0), "0 XOR 1 = 1"),
                TrainingSample(doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0), "1 XOR 0 = 1"),
                TrainingSample(doubleArrayOf(1.0, 1.0), doubleArrayOf(0.0), "1 XOR 1 = 0")
            )
            return Pair(net, samples)
        }

        fun createPatternClassifier(): Pair<NeuralNetwork, List<TrainingSample>> {
            // 9-input (3x3 grid) to 2 outputs (e.g. Cross vs Box vs Plus)
            val net = NeuralNetwork(intArrayOf(9, 6, 2), ActivationType.SIGMOID)
            val samples = listOf(
                // Plus '+' pattern -> [1.0, 0.0]
                TrainingSample(
                    doubleArrayOf(
                        0.0, 1.0, 0.0,
                        1.0, 1.0, 1.0,
                        0.0, 1.0, 0.0
                    ),
                    doubleArrayOf(1.0, 0.0),
                    "Plus (+)"
                ),
                // Cross 'X' pattern -> [0.0, 1.0]
                TrainingSample(
                    doubleArrayOf(
                        1.0, 0.0, 1.0,
                        0.0, 1.0, 0.0,
                        1.0, 0.0, 1.0
                    ),
                    doubleArrayOf(0.0, 1.0),
                    "Cross (X)"
                ),
                // Plus with noise
                TrainingSample(
                    doubleArrayOf(
                        0.1, 0.9, 0.0,
                        0.9, 1.0, 0.8,
                        0.0, 0.9, 0.1
                    ),
                    doubleArrayOf(1.0, 0.0),
                    "Noisy Plus"
                ),
                // Cross with noise
                TrainingSample(
                    doubleArrayOf(
                        0.9, 0.1, 0.9,
                        0.0, 0.8, 0.1,
                        0.9, 0.0, 1.0
                    ),
                    doubleArrayOf(0.0, 1.0),
                    "Noisy Cross"
                )
            )
            return Pair(net, samples)
        }
    }
}
