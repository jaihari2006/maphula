package com.offlineai.app.ui.nlp

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.offlineai.app.MainActivity
import com.offlineai.app.databinding.FragmentNlpStudioBinding
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.animateSlideFadeIn
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import com.offlineai.app.ui.motion.beginButteryTransition

class NlpStudioFragment : Fragment() {

    private var _binding: FragmentNlpStudioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNlpStudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPresets()
        setupActions()

        // Load default tech sample
        loadPresetTech()
        applyTheme(ThemeManager.currentTheme.value)
    }

    override fun onResume() {
        super.onResume()
        applyTheme(ThemeManager.currentTheme.value)
    }

    fun applyTheme(theme: ResolvedTheme) {
        val ctx = context ?: return
        if (_binding == null) return

        binding.root.setBackgroundColor(theme.bgColor)
        val accentList = ColorStateList.valueOf(theme.accentColor)
        binding.btnSummarize.backgroundTintList = accentList
        binding.btnSentiment.backgroundTintList = accentList
        binding.btnKeywords.backgroundTintList = accentList
        binding.btnReadability.backgroundTintList = accentList

        binding.presetReview.background = ThemeManager.chipBackground(ctx, theme)
        binding.presetTech.background = ThemeManager.chipBackground(ctx, theme)
        binding.presetMedical.background = ThemeManager.chipBackground(ctx, theme)
        binding.presetStory.background = ThemeManager.chipBackground(ctx, theme)
    }

    private fun setupPresets() {
        binding.presetReview.applyButteryPress()
        binding.presetTech.applyButteryPress()
        binding.presetMedical.applyButteryPress()
        binding.presetStory.applyButteryPress()

        binding.presetReview.setOnClickListener {
            binding.presetReview.animateSpringBounce()
            binding.etNlpInput.setText(
                "This offline AI application is an absolute breakthrough for privacy-conscious engineers. " +
                "The response speed is incredible, and the local inference engine operates without sending any telemetry to external servers. " +
                "However, the neural network visualizer can experience occasional stutter during intensive background backpropagation on low-end hardware. " +
                "Overall, the architectural design is brilliant, robust, and exceptionally reliable for field use."
            )
        }

        binding.presetTech.setOnClickListener {
            binding.presetTech.animateSpringBounce()
            loadPresetTech()
        }

        binding.presetMedical.setOnClickListener {
            binding.presetMedical.animateSpringBounce()
            binding.etNlpInput.setText(
                "EMERGENCY ADVISORY: Patient presenting with severe second-degree thermal burns across the left forearm. " +
                "Initial vitals indicate heart rate of 110 bpm and blood pressure of 135/85. " +
                "Immediate protocol dictates cooling the affected area under clean running water for 15 minutes. " +
                "Do not apply ice, ointment, or butter under any circumstances as this causes additional tissue necrosis. " +
                "Cover loosely with sterile non-adherent dressing and monitor for hypothermia."
            )
        }

        binding.presetStory.setOnClickListener {
            binding.presetStory.animateSpringBounce()
            binding.etNlpInput.setText(
                "The derelict orbital station drifted silently through the cold shadow of Jupiter's rings. " +
                "Deep within the darkened observation deck, a single terminal screen flickered to life, its amber phosphor casting long shadows across the dust-coated console. " +
                "Dr. Alistair Vance leaned forward, his breath misting in the freezing air, staring in disbelief at the cascading telemetry data. " +
                "The signal had not originated from Earth, nor from any known automated beacon across the solar system."
            )
        }
    }

    private fun loadPresetTech() {
        binding.etNlpInput.setText(
            "Artificial neural networks are computational models inspired by biological neural architectures in the brain. " +
            "Modern deep learning systems rely on the backpropagation algorithm combined with stochastic gradient descent to iteratively minimize loss functions. " +
            "By computing partial derivatives across network layers via the chain rule of calculus, weights and biases are updated to minimize prediction errors. " +
            "On-device inference allows machine learning models to execute fully offline, eliminating latency, preserving user privacy, and functioning in remote environments."
        )
    }

    private fun setupActions() {
        val nlpEngine = (activity as? MainActivity)?.nlpEngine ?: return

        binding.btnSummarize.applyButteryPress()
        binding.btnSentiment.applyButteryPress()
        binding.btnKeywords.applyButteryPress()
        binding.btnReadability.applyButteryPress()

        binding.btnSummarize.setOnClickListener {
            binding.btnSummarize.animateSpringBounce()
            val text = binding.etNlpInput.text.toString().trim()
            if (text.length < 30) {
                Toast.makeText(requireContext(), "Please enter at least 2 sentences to summarize.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            (binding.root as? ViewGroup)?.beginButteryTransition(260L)
            val summary = nlpEngine.summarize(text, targetSentenceCount = 2)
            binding.cardResult.visibility = View.VISIBLE
            binding.cardResult.animateSlideFadeIn(220L)
            binding.tvResultHeader.text = "📝 TextRank Extractive Summary"
            binding.tvResultSubheader.text = "Compressed by ${summary.compressionRatioPercent}% (${summary.originalWordCount} words → ${summary.summaryWordCount} words)"

            val sb = StringBuilder()
            for (s in summary.keySentences) {
                sb.append("• ").append(s).append("\n\n")
            }
            binding.tvResultBody.text = sb.toString().trim()
        }

        binding.btnSentiment.setOnClickListener {
            binding.btnSentiment.animateSpringBounce()
            val text = binding.etNlpInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter text to analyze.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            (binding.root as? ViewGroup)?.beginButteryTransition(260L)
            val sentiment = nlpEngine.analyzeSentiment(text)
            binding.cardResult.visibility = View.VISIBLE
            binding.cardResult.animateSlideFadeIn(220L)
            binding.tvResultHeader.text = "📊 Sentiment & Emotional Polarity"
            binding.tvResultSubheader.text = "Verdict: ${sentiment.label.uppercase()} (Confidence: ${sentiment.confidence}%)"

            val sb = StringBuilder()
            sb.append("• Polarity Score: ").append(sentiment.score).append("  (-1.0 max negative to +1.0 max positive)\n")
            sb.append("• Positive terms detected: ").append(sentiment.positiveCount).append("\n")
            sb.append("• Negative terms detected: ").append(sentiment.negativeCount).append("\n\n")
            sb.append("Emotional Tone Spectrum:\n")
            for ((tone, pct) in sentiment.tones) {
                sb.append("  - ").append(tone).append(": ").append(pct).append("%\n")
            }
            binding.tvResultBody.text = sb.toString().trim()
        }

        binding.btnKeywords.setOnClickListener {
            binding.btnKeywords.animateSpringBounce()
            val text = binding.etNlpInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter text to extract keywords.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            (binding.root as? ViewGroup)?.beginButteryTransition(260L)
            val result = nlpEngine.extractKeywordsAndEntities(text)
            binding.cardResult.visibility = View.VISIBLE
            binding.cardResult.animateSlideFadeIn(220L)
            binding.tvResultHeader.text = "🔍 Keywords & Named Entities"
            binding.tvResultSubheader.text = "Extracted ${result.topKeywords.size} key terms & ${result.entities.size} entities"

            val sb = StringBuilder()
            sb.append("Top Salient Keywords (TF-IDF Weight):\n")
            for ((kw, score) in result.topKeywords) {
                sb.append("  • ").append(kw).append(" (score: ").append("%.2f".format(score)).append(")\n")
            }

            if (result.entities.isNotEmpty()) {
                sb.append("\nRecognized Entities & Concepts:\n")
                for (ent in result.entities) {
                    sb.append("  • [").append(ent.type).append("] ").append(ent.text).append("\n")
                }
            }

            binding.tvResultBody.text = sb.toString().trim()
        }

        binding.btnReadability.setOnClickListener {
            binding.btnReadability.animateSpringBounce()
            val text = binding.etNlpInput.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter text to analyze readability.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val r = nlpEngine.analyzeReadability(text)
            binding.cardResult.visibility = View.VISIBLE
            binding.tvResultHeader.text = "📖 Readability & Complexity Metrics"
            binding.tvResultSubheader.text = "${r.readingGrade} — ${r.readingEaseDesc}"

            val sb = StringBuilder()
            sb.append("• Flesch Reading Ease: ").append(r.fleschScore).append(" / 100\n")
            sb.append("• Word Count: ").append(r.wordCount).append(" words\n")
            sb.append("• Sentence Count: ").append(r.sentenceCount).append(" sentences\n")
            sb.append("• Avg Sentence Length: ").append(r.avgWordsPerSentence).append(" words/sentence\n")
            sb.append("• Estimated Reading Time: ").append(r.estimatedReadingTimeMinutes).append(" minutes (@ 200 WPM)\n")

            binding.tvResultBody.text = sb.toString().trim()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
