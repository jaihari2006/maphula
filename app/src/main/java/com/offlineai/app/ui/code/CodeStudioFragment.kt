package com.offlineai.app.ui.code

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.offlineai.app.MainActivity
import com.offlineai.app.R
import com.offlineai.app.databinding.FragmentCodeStudioBinding
import com.offlineai.app.engine.code.ClaudeCodeAssistantEngine
import com.offlineai.app.engine.code.CodeAgentMode
import com.offlineai.app.engine.code.CodeExecutionEngine
import com.offlineai.app.engine.code.CodeLanguage
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import com.offlineai.app.ui.theme.AestheticStyle
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class CodeStudioFragment : Fragment() {

    private var _binding: FragmentCodeStudioBinding? = null
    private val binding get() = _binding!!

    private val executionEngine = CodeExecutionEngine()
    private val claudeCodeAssistant = ClaudeCodeAssistantEngine(executionEngine)

    private var currentLanguage: CodeLanguage = CodeLanguage.JAVASCRIPT
    private var currentMode: CodeAgentMode = CodeAgentMode.AUTONOMOUS_CODER
    private var agentJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCodeStudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLanguageSelector()
        setupAgentModeSelector()
        setupEditor()
        setupActions()
        loadLanguageTemplate(CodeLanguage.JAVASCRIPT)
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
        binding.layoutCodeHeader.background = ThemeManager.surfaceBackground(ctx, theme)
        binding.layoutClaudeCodeBar.background = ThemeManager.surfaceBackground(ctx, theme)

        binding.etCodePrompt.background = ThemeManager.inputBackground(ctx, theme)
        binding.etCodePrompt.setTextColor(theme.textPrimary)
        binding.etCodePrompt.setHintTextColor(theme.textMuted)

        binding.btnApplyCodex.backgroundTintList = if (theme.style == AestheticStyle.IOS) {
            ColorStateList.valueOf(Color.parseColor("#0A84FF"))
        } else {
            ColorStateList.valueOf(theme.accentColor)
        }

        binding.btnQuickCodeTemplates.background = ThemeManager.chipBackground(ctx, theme)
        binding.btnResetCode.background = ThemeManager.chipBackground(ctx, theme)
    }

    private fun setupLanguageSelector() {
        binding.chipGroupLanguages.setOnCheckedChangeListener { _, checkedId ->
            currentLanguage = when (checkedId) {
                R.id.chipPython -> CodeLanguage.PYTHON
                R.id.chipHtml -> CodeLanguage.HTML_CSS
                R.id.chipKotlin -> CodeLanguage.KOTLIN
                R.id.chipSql -> CodeLanguage.SQL
                else -> CodeLanguage.JAVASCRIPT
            }
            loadLanguageTemplate(currentLanguage)
        }
    }

    private fun setupAgentModeSelector() {
        val modes = listOf(
            binding.modeCoder to CodeAgentMode.AUTONOMOUS_CODER,
            binding.modeDebugger to CodeAgentMode.DEBUGGER,
            binding.modeRefactor to CodeAgentMode.REFACTOR,
            binding.modeTests to CodeAgentMode.TEST_GENERATOR,
            binding.modeExplain to CodeAgentMode.EXPLAINER
        )

        fun updateSelectedModeUI(selected: CodeAgentMode) {
            currentMode = selected
            for ((view, mode) in modes) {
                if (mode == selected) {
                    view.alpha = 1.0f
                    view.elevation = 4f
                } else {
                    view.alpha = 0.6f
                    view.elevation = 0f
                }
            }
            val hintText = when (selected) {
                CodeAgentMode.AUTONOMOUS_CODER -> "Ask Claude Code (e.g. 'implement binary search')"
                CodeAgentMode.DEBUGGER -> "Ask Debugger (e.g. 'find memory leaks & race conditions')"
                CodeAgentMode.REFACTOR -> "Ask Refactor (e.g. 'optimize complexity to O(N)')"
                CodeAgentMode.TEST_GENERATOR -> "Ask Tests (e.g. 'generate unit assertions')"
                CodeAgentMode.EXPLAINER -> "Ask Explainer (e.g. 'walk through execution flow')"
            }
            binding.etCodePrompt.hint = hintText
        }

        for ((view, mode) in modes) {
            view.applyButteryPress()
            view.setOnClickListener {
                view.animateSpringBounce()
                (activity as? MainActivity)?.soundEngine?.playButteryClick()
                updateSelectedModeUI(mode)
            }
        }
        updateSelectedModeUI(CodeAgentMode.AUTONOMOUS_CODER)
    }

    private fun setupEditor() {
        binding.etCodeEditor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateStats()
            }
        })
    }

    private fun setupActions() {
        binding.btnRunCode.applyButteryPress()
        binding.btnResetCode.applyButteryPress()
        binding.btnQuickCodeTemplates.applyButteryPress()
        binding.btnApplyCodex.applyButteryPress()

        binding.btnRunCode.setOnClickListener {
            binding.btnRunCode.animateSpringBounce()
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            executeCurrentCode()
        }

        binding.btnResetCode.setOnClickListener {
            binding.btnResetCode.animateSpringBounce()
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            loadLanguageTemplate(currentLanguage)
            Toast.makeText(requireContext(), "Reset to ${currentLanguage.displayName} template", Toast.LENGTH_SHORT).show()
        }

        binding.btnQuickCodeTemplates.setOnClickListener {
            binding.btnQuickCodeTemplates.animateSpringBounce()
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            showQuickPresetsDialog()
        }

        binding.btnApplyCodex.setOnClickListener {
            binding.btnApplyCodex.animateSpringBounce()
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            val prompt = binding.etCodePrompt.text.toString().trim()
            if (prompt.isNotEmpty()) {
                runClaudeCodeAgent(prompt)
            } else {
                Toast.makeText(requireContext(), "Enter an instruction for Claude Code", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadLanguageTemplate(language: CodeLanguage) {
        binding.tvCurrentFileName.text = language.defaultFileName
        binding.etCodeEditor.setText(language.starterTemplate)
        binding.tvConsoleOutput.text = "› Switched to ${language.displayName} environment.\n› Claude Code engine initialized. Ready to execute."
        binding.wvLivePreview.visibility = View.GONE
        binding.scrollConsoleOutput.visibility = View.VISIBLE
        updateStats()
    }

    private fun executeCurrentCode() {
        val code = binding.etCodeEditor.text.toString()
        val result = executionEngine.execute(code, currentLanguage)

        if (currentLanguage == CodeLanguage.HTML_CSS && result.renderedHtml != null) {
            binding.scrollConsoleOutput.visibility = View.GONE
            binding.wvLivePreview.visibility = View.VISIBLE
            binding.wvLivePreview.settings.javaScriptEnabled = true
            binding.wvLivePreview.settings.domStorageEnabled = true
            binding.wvLivePreview.setBackgroundColor(Color.parseColor("#070B14"))
            binding.wvLivePreview.loadDataWithBaseURL("https://offline-ai.local/", result.renderedHtml, "text/html", "UTF-8", null)
            Toast.makeText(requireContext(), "⚡ Live Web Preview Rendered", Toast.LENGTH_SHORT).show()
        } else {
            binding.wvLivePreview.visibility = View.GONE
            binding.scrollConsoleOutput.visibility = View.VISIBLE
            val displayOutput = if (result.stderr.isNotEmpty()) {
                "${result.stdout}\n\n[STDERR]:\n${result.stderr}"
            } else {
                result.stdout
            }
            binding.tvConsoleOutput.text = "$displayOutput\n\n› Process exited with code ${result.exitCode} (${result.durationMs}ms)"
        }
        updateStats(result.issuesDetected.size)
    }

    private fun runClaudeCodeAgent(prompt: String) {
        val code = binding.etCodeEditor.text.toString()
        binding.etCodePrompt.setText("")
        binding.scrollConsoleOutput.visibility = View.VISIBLE
        binding.wvLivePreview.visibility = View.GONE

        agentJob?.cancel()
        agentJob = viewLifecycleOwner.lifecycleScope.launch {
            (activity as? MainActivity)?.updateDynamicIsland("Claude Code Agent", "⚡")
            claudeCodeAssistant.processCodeAgentQuery(prompt, code, currentLanguage, currentMode).collect { partialOutput ->
                binding.tvConsoleOutput.text = partialOutput
            }
            (activity as? MainActivity)?.updateDynamicIsland("Offline AI Core", "🧠")

            // Automatically extract generated code payload if present
            val generated = claudeCodeAssistant.generateCodeForPrompt(prompt, code, currentLanguage, currentMode)
            if (generated != code && generated.isNotBlank()) {
                binding.etCodeEditor.setText(generated)
                Toast.makeText(requireContext(), "Applied Claude Code solution to editor!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats(errorCount: Int? = null) {
        if (_binding == null) return
        val code = binding.etCodeEditor.text.toString()
        val lineCount = code.lines().size
        val issues = if (errorCount == null) executionEngine.lintCode(code, currentLanguage).size else errorCount

        binding.tvCodeStats.text = "Lines: $lineCount • Language: ${currentLanguage.displayName} • Sovereign Sandbox"
        if (issues == 0) {
            binding.tvLinterBadge.text = "✓ 0 Errors"
            binding.tvLinterBadge.setTextColor(Color.parseColor("#10B981"))
        } else {
            binding.tvLinterBadge.text = "⚠️ $issues Issues"
            binding.tvLinterBadge.setTextColor(Color.parseColor("#F59E0B"))
        }
    }

    private fun showQuickPresetsDialog() {
        val dialog = BottomSheetDialog(requireContext())
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_inspect_attachment, null)
        dialog.setContentView(view)

        val tvTitle = view.findViewById<android.widget.TextView>(R.id.tvInspectFileName)
        val tvMeta = view.findViewById<android.widget.TextView>(R.id.tvInspectFileMeta)
        val tvSpecs = view.findViewById<android.widget.TextView>(R.id.tvInspectSpecs)
        val btnAsk = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAskDeityAboutAttachment)
        val btnClose = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseInspectModal)

        tvTitle?.text = "Claude Code / Codex Architecture"
        tvMeta?.text = "100% On-Device Neural Code Assistant"
        tvSpecs?.text = """
• AST-Driven Syntax Linting: Evaluates code invariants in memory without external servers
• Multi-Dialect Support: JavaScript (Node/QuickJS), Python 3.12, HTML/CSS Canvas, Kotlin JVM, SQLite
• Autonomous Agent Modes: Coder, Deep Debugger, Refactor & Optimize, Unit Test Suite, Explainer
• Live Sandboxing: Interactive touch canvas previews and sandboxed console execution
        """.trimIndent()

        btnAsk?.text = "Load Interactive Canvas Demo"
        btnAsk?.setOnClickListener {
            dialog.dismiss()
            binding.chipHtml.isChecked = true
            binding.etCodePrompt.setText("Create cyber particle canvas animation")
            runClaudeCodeAgent("Create cyber particle canvas animation")
        }

        btnClose?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        agentJob?.cancel()
        _binding = null
    }
}
