package com.offlineai.app.ui.neural

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.offlineai.app.R
import com.offlineai.app.databinding.FragmentNeuralLabBinding
import com.offlineai.app.engine.NeuralNetwork
import com.offlineai.app.engine.TrainingSample
import com.offlineai.app.ui.theme.AestheticStyle
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import com.offlineai.app.ui.motion.beginButteryTransition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NeuralLabFragment : Fragment() {

    private var _binding: FragmentNeuralLabBinding? = null
    private val binding get() = _binding!!

    private var currentModelType: Int = MODEL_XOR
    private lateinit var currentNetwork: NeuralNetwork
    private lateinit var currentSamples: List<TrainingSample>
    private var totalEpochsTrained = 0

    // 3x3 grid state for Pattern Classifier
    private val gridPixels = DoubleArray(9) { 0.0 }
    private lateinit var gridButtons: Array<Button>

    companion object {
        private const val MODEL_XOR = 1
        private const val MODEL_PATTERN = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNeuralLabBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initGridButtons()
        loadModel(MODEL_XOR)
        setupModelSelection()
        setupTrainingControls()
        setupXorInference()
        setupPatternInference()
        applyTheme(ThemeManager.currentTheme.value)
    }

    override fun onResume() {
        super.onResume()
        applyTheme(ThemeManager.currentTheme.value)
    }

    fun applyTheme(theme: ResolvedTheme) {
        if (_binding == null) return

        binding.root.setBackgroundColor(theme.bgColor)
        binding.btnTrain.backgroundTintList = ColorStateList.valueOf(theme.accentColor)
        binding.btnReset.backgroundTintList = ColorStateList.valueOf(theme.surfaceVariant)

        val posColor = when (theme.style) {
            AestheticStyle.IOS -> theme.accentColor
            AestheticStyle.MATTE -> theme.accentSecondary
            AestheticStyle.SOLID -> theme.accentColor
            AestheticStyle.GLASSY -> theme.accentColor
        }
        val negColor = when (theme.style) {
            AestheticStyle.IOS -> Color.parseColor("#FF3B30") // Apple System Red
            AestheticStyle.MATTE -> Color.parseColor("#F43F5E")
            AestheticStyle.SOLID -> Color.parseColor("#EF4444")
            AestheticStyle.GLASSY -> theme.accentSecondary
        }
        binding.neuralView.setThemeColors(posColor, negColor)
    }

    private fun initGridButtons() {
        gridButtons = arrayOf(
            binding.cell0, binding.cell1, binding.cell2,
            binding.cell3, binding.cell4, binding.cell5,
            binding.cell6, binding.cell7, binding.cell8
        )
        for (btn in gridButtons) {
            btn.applyButteryPress(pressedScale = 0.88f)
        }
    }

    private fun loadModel(type: Int) {
        currentModelType = type
        totalEpochsTrained = 0

        if (type == MODEL_XOR) {
            val (net, samples) = NeuralNetwork.createXorNetwork()
            currentNetwork = net
            currentSamples = samples
            binding.layoutXorControls.visibility = View.VISIBLE
            binding.layoutPatternControls.visibility = View.GONE
        } else {
            val (net, samples) = NeuralNetwork.createPatternClassifier()
            currentNetwork = net
            currentSamples = samples
            binding.layoutXorControls.visibility = View.GONE
            binding.layoutPatternControls.visibility = View.VISIBLE

            // Pre-fill with a '+' shape
            val defaultPlus = doubleArrayOf(0.0, 1.0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0, 0.0)
            for (i in 0 until 9) {
                gridPixels[i] = defaultPlus[i]
                updateGridButtonVisual(i)
            }
        }

        binding.neuralView.setNetwork(currentNetwork)
        updateStatusLabels(0.5)
        runLiveInference()
    }

    private fun setupModelSelection() {
        binding.chipXor.applyButteryPress()
        binding.chipPattern.applyButteryPress()

        binding.chipGroupModels.setOnCheckedChangeListener { _, checkedId ->
            (binding.root as? ViewGroup)?.beginButteryTransition(240L)
            if (checkedId == R.id.chipPattern) {
                loadModel(MODEL_PATTERN)
            } else {
                loadModel(MODEL_XOR)
            }
        }
    }

    private fun setupTrainingControls() {
        binding.btnTrain.applyButteryPress()
        binding.btnReset.applyButteryPress()

        binding.btnTrain.setOnClickListener {
            binding.btnTrain.animateSpringBounce()
            binding.btnTrain.isEnabled = false
            binding.btnTrain.text = "Training..."

            viewLifecycleOwner.lifecycleScope.launch {
                val finalLoss = withContext(Dispatchers.Default) {
                    var loss = 0.0
                    for (epoch in 1..50) {
                        loss = currentNetwork.trainEpoch(
                            samples = currentSamples,
                            learningRate = if (currentModelType == MODEL_XOR) 0.5 else 0.3,
                            momentum = 0.8
                        )
                    }
                    loss
                }

                totalEpochsTrained += 50
                updateStatusLabels(finalLoss)
                binding.neuralView.invalidate()
                runLiveInference()

                val mainAct = activity as? com.offlineai.app.MainActivity
                if (finalLoss < 0.1) {
                    mainAct?.soundEngine?.playLevelUp()
                } else {
                    mainAct?.soundEngine?.playButteryClick()
                }
                val res = mainAct?.karmaEngine?.awardPrana(25, "Trained Neural Network")
                if (res != null) {
                    mainAct.updateKarmaBadgeUI()
                    android.widget.Toast.makeText(requireContext(), "🧠 Neural Training Complete! +25 Prana (${res.totalPoints} total)", android.widget.Toast.LENGTH_SHORT).show()
                }

                binding.btnTrain.isEnabled = true
                binding.btnTrain.text = "▶ Train 50 Epochs"
            }
        }

        binding.btnReset.setOnClickListener {
            binding.btnReset.animateSpringBounce()
            loadModel(currentModelType)
        }
    }

    private fun updateStatusLabels(loss: Double) {
        binding.tvEpochs.text = "$totalEpochsTrained"
        binding.tvLoss.text = "%.4f".format(loss)
    }

    private fun setupXorInference() {
        val listener = { _: Any ->
            runLiveInference()
        }
        binding.switchInputA.setOnCheckedChangeListener { _, isChecked ->
            binding.switchInputA.text = if (isChecked) "Input A: 1" else "Input A: 0"
            listener(Unit)
        }
        binding.switchInputB.setOnCheckedChangeListener { _, isChecked ->
            binding.switchInputB.text = if (isChecked) "Input B: 1" else "Input B: 0"
            listener(Unit)
        }
    }

    private fun setupPatternInference() {
        for (i in 0 until 9) {
            gridButtons[i].setOnClickListener {
                gridButtons[i].animateSpringBounce(duration = 240L)
                gridPixels[i] = if (gridPixels[i] == 0.0) 1.0 else 0.0
                updateGridButtonVisual(i)
                runLiveInference()
            }
        }
    }

    private fun updateGridButtonVisual(index: Int) {
        val btn = gridButtons[index]
        val isPressed = gridPixels[index] == 1.0
        btn.text = if (isPressed) "1" else "0"
        btn.setBackgroundColor(if (isPressed) Color.parseColor("#06B6D4") else Color.parseColor("#1E293B"))
        btn.setTextColor(if (isPressed) Color.BLACK else Color.WHITE)
    }

    private fun runLiveInference() {
        if (currentModelType == MODEL_XOR) {
            val a = if (binding.switchInputA.isChecked) 1.0 else 0.0
            val b = if (binding.switchInputB.isChecked) 1.0 else 0.0
            val out = currentNetwork.forward(doubleArrayOf(a, b))
            val prediction = out[0]
            val predictedClass = if (prediction >= 0.5) 1 else 0
            val expectedClass = ((a.toInt()) xor (b.toInt()))

            val matchSymbol = if (predictedClass == expectedClass) "✓ (Accurate)" else "⏳ (Needs more training)"
            binding.tvPredictionXor.text = "Predicted: %.2f → Class %d  %s".format(prediction, predictedClass, matchSymbol)
        } else {
            val out = currentNetwork.forward(gridPixels.clone())
            val plusProb = out.getOrElse(0) { 0.0 }
            val crossProb = out.getOrElse(1) { 0.0 }

            val winner = if (plusProb >= crossProb) "Plus (+)" else "Cross (X)"
            val maxProb = kotlin.math.max(plusProb, crossProb)
            val pct = (maxProb * 100).toInt()

            binding.tvPredictionPattern.text = "Prediction: %s (%d%%)\n[Plus: %.2f, Cross: %.2f]".format(winner, pct, plusProb, crossProb)
        }

        binding.neuralView.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
