package com.offlineai.app

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.offlineai.app.data.Persona
import com.offlineai.app.databinding.ActivityMainBinding
import com.offlineai.app.engine.AppLanguage
import com.offlineai.app.engine.AudioRecordingEngine
import com.offlineai.app.engine.CelebrationEngine
import com.offlineai.app.engine.ChatEngine
import com.offlineai.app.engine.ExportImportEngine
import com.offlineai.app.engine.MultiLanguageTranslationEngine
import com.offlineai.app.engine.NlpToolkitEngine
import com.offlineai.app.engine.OnlineSyncEngine
import com.offlineai.app.engine.RavanaSkillEngine
import com.offlineai.app.engine.ScriptureLibraryEngine
import com.offlineai.app.engine.SemanticKnowledgeEngine
import com.offlineai.app.engine.SoundSynthesisEngine
import com.offlineai.app.engine.VoiceDuplicationEngine
import com.offlineai.app.network.NetworkMonitor
import com.offlineai.app.ui.chat.ChatFragment
import com.offlineai.app.ui.clock.AnalogClockView
import com.offlineai.app.ui.code.CodeStudioFragment
import com.offlineai.app.ui.creative.CreativeStudioFragment
import com.offlineai.app.ui.knowledge.KnowledgeFragment
import com.offlineai.app.ui.neural.NeuralLabFragment
import com.offlineai.app.ui.nlp.NlpStudioFragment
import com.offlineai.app.ui.theme.AccentPalette
import com.offlineai.app.ui.theme.AestheticStyle
import com.offlineai.app.ui.theme.LuminanceMode
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.MotionUtils
import com.offlineai.app.ui.motion.animateSlideFadeIn
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import com.offlineai.app.widget.MaphulaWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

val Persona.icon: String
    get() = when (this) {
        Persona.KRISHNA -> "🦚"
        Persona.SHIVA -> "🔱"
        Persona.BRAHMA -> "🪷"
        Persona.SHANI -> "⚖️"
        Persona.GANESHA -> "🐘"
        Persona.MURUGA -> "⚡"
        Persona.HANUMAN -> "🛡️"
        Persona.LAKSHMI -> "🌸"
        Persona.PARVATHI -> "🏔️"
        Persona.NARASIMHA -> "🦁"
    }

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Network & Sync Infrastructure
    val networkMonitor by lazy { NetworkMonitor(this) }

    // Shared On-Device AI Engines
    val knowledgeEngine by lazy { SemanticKnowledgeEngine() }
    val nlpEngine by lazy { NlpToolkitEngine() }
    val onlineSyncEngine by lazy { OnlineSyncEngine(this, networkMonitor, knowledgeEngine, nlpEngine) }
    val chatEngine by lazy { ChatEngine(knowledgeEngine, nlpEngine, onlineSyncEngine) }
    val voiceEngine by lazy { VoiceDuplicationEngine(this) }
    val celebrationEngine by lazy { CelebrationEngine() }
    val soundEngine by lazy { SoundSynthesisEngine() }
    val audioRecordingEngine by lazy { AudioRecordingEngine(this) }
    val exportImportEngine by lazy { ExportImportEngine(this) }
    val scriptureEngine by lazy { ScriptureLibraryEngine() }
    val ravanaEngine by lazy { RavanaSkillEngine() }
    val translationEngine by lazy { MultiLanguageTranslationEngine() }
    var currentAppLanguage: AppLanguage = AppLanguage.ENGLISH
        private set

    fun setAppLanguage(language: AppLanguage) {
        currentAppLanguage = language
        voiceEngine.setLanguage(language)
        chatFragment.updateLanguage(language)
    }

    private var clock8BitJob: Job? = null
    private var batteryReceiver: BroadcastReceiver? = null

    // Fragment instances
    private val chatFragment by lazy { ChatFragment() }
    private val codeFragment by lazy { CodeStudioFragment() }
    private val creativeFragment by lazy { CreativeStudioFragment() }
    private val knowledgeFragment by lazy { KnowledgeFragment() }
    private val nlpFragment by lazy { NlpStudioFragment() }
    private val neuralFragment by lazy { NeuralLabFragment() }

    private var activeFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize persistent storage for synced offline knowledge & sovereign chat history
        knowledgeEngine.initPersistence(this)
        chatEngine.initPersistence(this)

        setupSkillsDrawer()
        setupCalendarTab()
        setupScripturesTab()
        setupVoiceAssistantTab()
        setup8BitClock()
        setupDynamicIsland()
        setupThemeBadge()
        setupSettings()
        setupNavigation()
        setupNetworkBadge()
        setupBatteryMonitoring()
        applyThemeAcrossApp(ThemeManager.currentTheme.value)

        val isFromWidget = isWidgetIntent(intent)
        if (!isFromWidget) {
            setupAppOpenAnimation()
        } else {
            binding.layoutAppOpenSplash.visibility = View.GONE
            binding.layoutMainContent.scaleX = 1.0f
            binding.layoutMainContent.scaleY = 1.0f
            binding.layoutMainContent.alpha = 1.0f
        }

        if (savedInstanceState == null) {
            // Start with ChatFragment
            showFragment(chatFragment, "CHAT")
        }
        handleWidgetIntent(intent)
        refreshHomeScreenWidgets()
    }

    private fun setupAppOpenAnimation() {
        val splash = binding.layoutAppOpenSplash
        val appIcon = binding.cardSplashAppIcon
        val glowRing = binding.splashGlowRing
        val title = binding.tvSplashTitle
        val badge = binding.tvSplashBadge
        val tagline = binding.tvSplashTagline
        val status = binding.layoutSplashStatus
        val content = binding.layoutMainContent
        val creatorCredit = binding.layoutCreatorCredit

        // Trigger gentle tactile audio feedback
        soundEngine.playButteryClick()

        MotionUtils.animateAppOpen(
            splashView = splash,
            appIconView = appIcon,
            glowRingView = glowRing,
            titleView = title,
            badgeView = badge,
            taglineView = tagline,
            statusView = status,
            mainContentView = content,
            creatorCreditView = creatorCredit,
            onComplete = {
                Toast.makeText(this@MainActivity, "✨ App Created by Jaihari U S", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupSkillsDrawer() {
        binding.btnSkillsTab.applyButteryPress()
        binding.btnSkillsTab.setOnClickListener {
            soundEngine.playButteryClick()
            binding.btnSkillsTab.animateSpringBounce()
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Drawer inner controls
        val drawerMenu = binding.drawerSkillsMenu
        drawerMenu.btnCloseDrawer.applyButteryPress()
        drawerMenu.btnCloseDrawer.setOnClickListener {
            soundEngine.playButteryClick()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        drawerMenu.btnOpenRavanaSkill.applyButteryPress()
        drawerMenu.btnOpenRavanaSkill.setOnClickListener {
            soundEngine.playButteryClick()
            drawerMenu.btnOpenRavanaSkill.animateSpringBounce()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showRavanaArenaModal()
        }

        drawerMenu.cardSkillRavana.applyButteryPress()
        drawerMenu.cardSkillRavana.setOnClickListener {
            soundEngine.playButteryClick()
            drawerMenu.cardSkillRavana.animateSpringBounce()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showRavanaArenaModal()
        }

        drawerMenu.btnOpenGitaSkill.applyButteryPress()
        drawerMenu.btnOpenGitaSkill.setOnClickListener {
            soundEngine.playButteryClick()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showScripturesModal()
        }

        drawerMenu.btnOpenNeuralSkill.applyButteryPress()
        drawerMenu.btnOpenNeuralSkill.setOnClickListener {
            soundEngine.playButteryClick()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.bottomNav.selectedItemId = R.id.nav_neural
            showFragment(neuralFragment, "NEURAL")
        }

        drawerMenu.btnOpenVoiceSkill.applyButteryPress()
        drawerMenu.btnOpenVoiceSkill.setOnClickListener {
            soundEngine.playButteryClick()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.bottomNav.selectedItemId = R.id.nav_creative
            showFragment(creativeFragment, "CREATIVE")
        }

        drawerMenu.btnOpenRagSkill.applyButteryPress()
        drawerMenu.btnOpenRagSkill.setOnClickListener {
            soundEngine.playButteryClick()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            binding.bottomNav.selectedItemId = R.id.nav_knowledge
            showFragment(knowledgeFragment, "KNOWLEDGE")
        }

        drawerMenu.btnOpenNlpSkill.applyButteryPress()
        drawerMenu.btnOpenNlpSkill.setOnClickListener {
            soundEngine.playButteryClick()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            showFragment(nlpFragment, "NLP")
        }

        drawerMenu.cardSkillWidget.applyButteryPress()
        drawerMenu.cardSkillWidget.setOnClickListener {
            soundEngine.playButteryClick()
            drawerMenu.cardSkillWidget.animateSpringBounce()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            pinWidgetToHomeScreen()
        }

        drawerMenu.btnOpenWidgetSkill.applyButteryPress()
        drawerMenu.btnOpenWidgetSkill.setOnClickListener {
            soundEngine.playButteryClick()
            drawerMenu.btnOpenWidgetSkill.animateSpringBounce()
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            pinWidgetToHomeScreen()
        }
    }

    private fun setupCalendarTab() {
        binding.layoutCalendarTab.applyButteryPress()
        binding.layoutCalendarTab.setOnClickListener {
            soundEngine.playButteryClick()
            binding.layoutCalendarTab.animateSpringBounce()
            showCalendarModal()
        }
    }

    private fun setupScripturesTab() {
        binding.btnScripturesTab.applyButteryPress()
        binding.btnScripturesTab.setOnClickListener {
            soundEngine.playButteryClick()
            binding.btnScripturesTab.animateSpringBounce()
            showScripturesModal()
        }
    }

    private fun setupVoiceAssistantTab() {
        binding.btnVoiceAssistantTab.applyButteryPress()
        binding.btnVoiceAssistantTab.setOnClickListener {
            soundEngine.playButteryClick()
            binding.btnVoiceAssistantTab.animateSpringBounce()
            showVoiceAssistantDialog()
        }
    }

    @Suppress("DEPRECATION")
    fun showVoiceAssistantDialog(initialPersona: Persona? = null) {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_voice_assistant, null)
        dialog.setContentView(view)

        var selectedPersona: Persona = initialPersona ?: Persona.KRISHNA

        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseAssistantModal)
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupAssistantDeities)

        val chipKrishna = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipKrishna)
        val chipShiva = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipShiva)
        val chipBrahma = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipBrahma)
        val chipGanesha = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipGanesha)
        val chipMuruga = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipMuruga)
        val chipHanuman = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipHanuman)
        val chipLakshmi = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipLakshmi)
        val chipParvathi = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipParvathi)
        val chipNarasimha = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipNarasimha)
        val chipShani = view.findViewById<com.google.android.material.chip.Chip>(R.id.vaChipShani)

        val tvAvatar = view.findViewById<TextView>(R.id.tvDeityVoiceAvatar)
        val tvTitle = view.findViewById<TextView>(R.id.tvDeityVoiceTitle)
        val tvAcoustics = view.findViewById<TextView>(R.id.tvDeityVoiceAcoustics)
        val btnSample = view.findViewById<MaterialButton>(R.id.btnTestDeityVoice)
        val visualizer = view.findViewById<com.offlineai.app.ui.creative.VoiceWaveformVisualizerView>(R.id.vaWaveformVisualizer)
        val tvStatus = view.findViewById<TextView>(R.id.tvAssistantStatus)
        val tvResponse = view.findViewById<TextView>(R.id.tvAssistantResponse)

        val btnMic = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.btnVoiceAssistantMic)
        val btnStop = view.findViewById<MaterialButton>(R.id.btnStopSpeaking)
        val btnToggleAuto = view.findViewById<MaterialButton>(R.id.btnToggleAutoSpeak)

        val promptBlessing = view.findViewById<TextView>(R.id.promptBlessing)
        val promptCode = view.findViewById<TextView>(R.id.promptCodeArchitecture)
        val promptObstacles = view.findViewById<TextView>(R.id.promptRemoveObstacles)
        val promptFirstAid = view.findViewById<TextView>(R.id.promptFirstAid)
        val promptPrinciples = view.findViewById<TextView>(R.id.promptFirstPrinciples)
        val promptPoetry = view.findViewById<TextView>(R.id.promptCreativePoetry)

        fun updateDeityUI() {
            val prof = voiceEngine.getProfileForPersona(selectedPersona)
            tvAvatar?.text = selectedPersona.icon
            tvTitle?.text = "${selectedPersona.displayName} • ${selectedPersona.title}"
            tvAcoustics?.text = "Pitch: ${prof.pitchHz.toInt()} Hz F0 • Formant: ${prof.formantResonance}x • Warmth: ${prof.warmthHarmonics}"
            visualizer?.setThemeColor(prof.colorHex)
        }

        when (selectedPersona) {
            Persona.SHIVA -> chipShiva?.isChecked = true
            Persona.BRAHMA -> chipBrahma?.isChecked = true
            Persona.GANESHA -> chipGanesha?.isChecked = true
            Persona.MURUGA -> chipMuruga?.isChecked = true
            Persona.HANUMAN -> chipHanuman?.isChecked = true
            Persona.LAKSHMI -> chipLakshmi?.isChecked = true
            Persona.PARVATHI -> chipParvathi?.isChecked = true
            Persona.NARASIMHA -> chipNarasimha?.isChecked = true
            Persona.SHANI -> chipShani?.isChecked = true
            else -> chipKrishna?.isChecked = true
        }
        updateDeityUI()

        chipGroup?.setOnCheckedChangeListener { _, checkedId ->
            selectedPersona = when (checkedId) {
                R.id.vaChipShiva -> Persona.SHIVA
                R.id.vaChipBrahma -> Persona.BRAHMA
                R.id.vaChipGanesha -> Persona.GANESHA
                R.id.vaChipMuruga -> Persona.MURUGA
                R.id.vaChipHanuman -> Persona.HANUMAN
                R.id.vaChipLakshmi -> Persona.LAKSHMI
                R.id.vaChipParvathi -> Persona.PARVATHI
                R.id.vaChipNarasimha -> Persona.NARASIMHA
                R.id.vaChipShani -> Persona.SHANI
                else -> Persona.KRISHNA
            }
            updateDeityUI()
            soundEngine.playButteryClick()
        }

        fun updateAutoSpeakBtn() {
            btnToggleAuto?.text = if (voiceEngine.isAutoSpeakEnabled) "🔊 Auto-Speak: ON" else "🔈 Auto-Speak: OFF"
        }
        updateAutoSpeakBtn()

        btnToggleAuto?.setOnClickListener {
            voiceEngine.isAutoSpeakEnabled = !voiceEngine.isAutoSpeakEnabled
            updateAutoSpeakBtn()
            soundEngine.playButteryClick()
        }

        btnSample?.setOnClickListener {
            btnSample.animateSpringBounce()
            val sampleText = "Hello! I am ${selectedPersona.displayName}, your friendly offline assistant. How can I help you today?"
            tvStatus?.text = "🔊 ${selectedPersona.displayName} speaking sample..."
            tvResponse?.text = sampleText
            voiceEngine.speakDeity(sampleText, selectedPersona)
        }

        btnStop?.setOnClickListener {
            btnStop.animateSpringBounce()
            voiceEngine.stop()
            tvStatus?.text = "Voice stopped"
        }

        fun executeVoiceQuery(query: String) {
            tvStatus?.text = "✨ ${selectedPersona.displayName} is thinking..."
            tvResponse?.text = "\"$query\"\n\nThinking of a helpful answer..."
            soundEngine.playButteryClick()

            lifecycleScope.launch {
                try {
                    val accumulated = StringBuilder()
                    chatEngine.generateStreamingResponse(query, selectedPersona).collect { chunk ->
                        accumulated.clear()
                        accumulated.append(chunk)
                        tvResponse?.text = accumulated.toString()
                    }
                    tvStatus?.text = "🔊 ${selectedPersona.displayName} is speaking..."
                    voiceEngine.speakDeity(accumulated.toString(), selectedPersona)
                } catch (e: Exception) {
                    tvStatus?.text = "Error: ${e.message}"
                }
            }
        }

        // Voice Cadence / Speed Buttons
        val btnSpeedSlow = view.findViewById<TextView>(R.id.btnSpeedSlow)
        val btnSpeedNormal = view.findViewById<TextView>(R.id.btnSpeedNormal)
        val btnSpeedFast = view.findViewById<TextView>(R.id.btnSpeedFast)
        val btnSpeedRapid = view.findViewById<TextView>(R.id.btnSpeedRapid)

        val speedButtons = listOf(btnSpeedSlow, btnSpeedNormal, btnSpeedFast, btnSpeedRapid)
        fun updateSpeedUI(selected: TextView, multiplier: Float) {
            voiceEngine.globalSpeedMultiplier = multiplier
            speedButtons.forEach { btn ->
                if (btn == selected) {
                    btn?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.secondary))
                    btn?.setTypeface(null, android.graphics.Typeface.BOLD)
                } else {
                    btn?.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                    btn?.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }
            soundEngine.playButteryClick()
            Toast.makeText(this@MainActivity, "Cadence: ${multiplier}x", Toast.LENGTH_SHORT).show()
        }

        btnSpeedSlow?.setOnClickListener { updateSpeedUI(btnSpeedSlow, 0.8f) }
        btnSpeedNormal?.setOnClickListener { updateSpeedUI(btnSpeedNormal, 1.0f) }
        btnSpeedFast?.setOnClickListener { updateSpeedUI(btnSpeedFast, 1.25f) }
        btnSpeedRapid?.setOnClickListener { updateSpeedUI(btnSpeedRapid, 1.5f) }

        btnMic?.setOnClickListener {
            btnMic.animateSpringBounce()
            if (audioRecordingEngine.isRecording.value) {
                audioRecordingEngine.stopSpeechRecognition()
                val durationMs = audioRecordingEngine.stopRecording()
                btnMic.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.primary)
                tvStatus?.text = "Processing speech..."
                val voicePrompt = audioRecordingEngine.getSuggestedPromptFromRecording(durationMs)
                executeVoiceQuery(voicePrompt)
            } else {
                if (!audioRecordingEngine.hasRecordPermission()) {
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1001)
                    return@setOnClickListener
                }
                voiceEngine.stop()
                btnMic.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.accent_rose)
                tvStatus?.text = "🎤 Listening offline... Speak now (Tap mic to finish)"

                audioRecordingEngine.startOfflineSpeechRecognition(
                    onPartialResult = { partial ->
                        runOnUiThread {
                            tvStatus?.text = "🎤 Listening: \"$partial\""
                        }
                    },
                    onFinalResult = { transcribed ->
                        runOnUiThread {
                            btnMic.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.primary)
                            executeVoiceQuery(transcribed)
                        }
                    },
                    onErrorFallback = {
                        runOnUiThread {
                            // Fallback to acoustic PCM recording & waveform
                            audioRecordingEngine.startRecording { amp ->
                                runOnUiThread {
                                    val simulatedWave = FloatArray(32) { (amp * (0.4f + (it % 5) * 0.15f)).coerceIn(0.08f, 1f) }
                                    visualizer?.setWaveform(simulatedWave)
                                }
                            }
                        }
                    }
                )
            }
        }

        val waveJob = lifecycleScope.launch {
            voiceEngine.currentWaveformData.collectLatest { wave ->
                if (voiceEngine.isSpeaking.value) {
                    visualizer?.setWaveform(wave)
                }
            }
        }

        promptBlessing?.setOnClickListener {
            chipKrishna?.isChecked = true
            executeVoiceQuery("Bestow upon me divine wisdom, clarity, and peace for today.")
        }
        promptCode?.setOnClickListener {
            chipShiva?.isChecked = true
            executeVoiceQuery("Explain optimal software architecture and how to eliminate computational complexity.")
        }
        promptObstacles?.setOnClickListener {
            chipGanesha?.isChecked = true
            executeVoiceQuery("How do I clear intricate obstacles and sharpen my intellect?")
        }
        promptFirstAid?.setOnClickListener {
            chipHanuman?.isChecked = true
            executeVoiceQuery("Give me rapid first aid and emergency survival protocol for life protection.")
        }
        promptPrinciples?.setOnClickListener {
            chipBrahma?.isChecked = true
            executeVoiceQuery("Explain the fundamental creation principles of quantum physics and wave mechanics.")
        }
        promptPoetry?.setOnClickListener {
            chipLakshmi?.isChecked = true
            executeVoiceQuery("Compose an inspiring story celebrating beauty, creative flourishing, and grace.")
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            waveJob.cancel()
            audioRecordingEngine.stopSpeechRecognition()
            if (audioRecordingEngine.isRecording.value) {
                audioRecordingEngine.stopRecording()
            }
            voiceEngine.stop()
        }

        dialog.show()
    }

    private fun showScripturesModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_scriptures, null)
        dialog.setContentView(view)

        val allVerses = scriptureEngine.verses
        var filteredVerses = allVerses
        var currentIndex = 0

        val tvIndex = view.findViewById<TextView>(R.id.tvScriptureIndex)
        val tvTitle = view.findViewById<TextView>(R.id.tvVerseScriptureTitle)
        val tvSection = view.findViewById<TextView>(R.id.tvVerseSection)
        val tvOriginal = view.findViewById<TextView>(R.id.tvVerseOriginalScript)
        val tvTranslit = view.findViewById<TextView>(R.id.tvVerseTransliteration)
        val tvEnglish = view.findViewById<TextView>(R.id.tvVerseEnglish)
        val tvMeaning = view.findViewById<TextView>(R.id.tvVerseMeaning)
        val tvDeityBadge = view.findViewById<TextView>(R.id.tvVerseDeityBadge)

        val btnPrev = view.findViewById<MaterialButton>(R.id.btnPrevVerse)
        val btnPlayTone = view.findViewById<MaterialButton>(R.id.btnPlaySacredTone)
        val btnNext = view.findViewById<MaterialButton>(R.id.btnNextVerse)
        val btnAskDeity = view.findViewById<MaterialButton>(R.id.btnAskDeityVerse)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseScriptures)

        // Filter chips
        val chipAll = view.findViewById<TextView>(R.id.chipFilterAll)
        val chipGita = view.findViewById<TextView>(R.id.chipFilterGita)
        val chipUpanishads = view.findViewById<TextView>(R.id.chipFilterUpanishads)
        val chipYoga = view.findViewById<TextView>(R.id.chipFilterYoga)
        val chipThirukkural = view.findViewById<TextView>(R.id.chipFilterThirukkural)
        val chipVedicMath = view.findViewById<TextView>(R.id.chipFilterVedicMath)
        val chipMantras = view.findViewById<TextView>(R.id.chipFilterMantras)

        val allChips = listOf(chipAll, chipGita, chipUpanishads, chipYoga, chipThirukkural, chipVedicMath, chipMantras)
        allChips.forEach { it.applyButteryPress() }
        btnPrev.applyButteryPress()
        btnPlayTone.applyButteryPress()
        btnNext.applyButteryPress()
        btnAskDeity.applyButteryPress()
        btnClose.applyButteryPress()

        fun renderVerse() {
            if (filteredVerses.isEmpty()) return
            if (currentIndex !in filteredVerses.indices) currentIndex = 0
            val verse = filteredVerses[currentIndex]

            tvIndex.text = "${currentIndex + 1}/${filteredVerses.size}"
            tvTitle.text = verse.scriptureName
            tvSection.text = verse.sectionOrChapter
            tvOriginal.text = verse.originalScript
            tvTranslit.text = verse.romanTransliteration
            tvEnglish.text = verse.englishTranslation
            tvMeaning.text = verse.deepPhilosophicalMeaning
            tvDeityBadge.text = "Presiding Deity: ${verse.associatedDeity.displayName} (${verse.associatedDeity.title})"
            btnAskDeity.text = "💬 Ask ${verse.associatedDeity.displayName} AI About This Shloka"
        }

        fun selectChip(selected: TextView, filterKeyword: String) {
            soundEngine.playButteryClick()
            allChips.forEach { chip ->
                chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            }
            selected.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            filteredVerses = scriptureEngine.getVersesForScripture(filterKeyword)
            currentIndex = 0
            renderVerse()
        }

        chipAll.setOnClickListener { selectChip(chipAll, "ALL") }
        chipGita.setOnClickListener { selectChip(chipGita, "Bhagavad Gita") }
        chipUpanishads.setOnClickListener { selectChip(chipUpanishads, "Upanishad") }
        chipYoga.setOnClickListener { selectChip(chipYoga, "Yoga") }
        chipThirukkural.setOnClickListener { selectChip(chipThirukkural, "Thirukkural") }
        chipVedicMath.setOnClickListener { selectChip(chipVedicMath, "Vedic Math") }
        chipMantras.setOnClickListener { selectChip(chipMantras, "Mantra") }

        renderVerse()

        btnPrev.setOnClickListener {
            btnPrev.animateSpringBounce()
            soundEngine.playButteryClick()
            if (filteredVerses.isNotEmpty()) {
                currentIndex = if (currentIndex > 0) currentIndex - 1 else filteredVerses.size - 1
                renderVerse()
            }
        }

        btnNext.setOnClickListener {
            btnNext.animateSpringBounce()
            soundEngine.playButteryClick()
            if (filteredVerses.isNotEmpty()) {
                currentIndex = (currentIndex + 1) % filteredVerses.size
                renderVerse()
            }
        }

        btnPlayTone.setOnClickListener {
            btnPlayTone.animateSpringBounce()
            val verse = filteredVerses.getOrNull(currentIndex) ?: return@setOnClickListener
            soundEngine.playChantCadence(verse.audioMelodyFrequencies)
            Toast.makeText(this, "Harmonic acoustic chant playing...", Toast.LENGTH_SHORT).show()
        }

        val btnRecite = view.findViewById<MaterialButton>(R.id.btnReciteShlokaVoice)
        btnRecite?.setOnClickListener {
            btnRecite.animateSpringBounce()
            val verse = filteredVerses.getOrNull(currentIndex) ?: return@setOnClickListener
            soundEngine.stopAudio()
            voiceEngine.speakDeity("${verse.englishTranslation}. ${verse.deepPhilosophicalMeaning}", verse.associatedDeity)
            Toast.makeText(this, "Speaking as ${verse.associatedDeity.displayName}...", Toast.LENGTH_SHORT).show()
        }

        btnAskDeity.setOnClickListener {
            btnAskDeity.animateSpringBounce()
            soundEngine.playButteryClick()
            voiceEngine.stop()
            dialog.dismiss()
            val verse = filteredVerses.getOrNull(currentIndex) ?: return@setOnClickListener
            binding.bottomNav.selectedItemId = R.id.nav_chat
            showFragment(chatFragment, "CHAT")
            chatFragment.selectPersonaAndPrompt(
                verse.associatedDeity,
                "Please explain the spiritual wisdom and deep meaning of this sacred verse from ${verse.scriptureName} (${verse.sectionOrChapter}): \"${verse.romanTransliteration}\". How can I apply its teaching to cultivate mental clarity, duty, and peace?"
            )
        }

        btnClose.setOnClickListener {
            soundEngine.stopAudio()
            voiceEngine.stop()
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            soundEngine.stopAudio()
            voiceEngine.stop()
        }

        dialog.show()
    }

    private fun setup8BitClock() {
        binding.layoutClock8Bit.applyButteryPress()
        binding.layoutClock8Bit.setOnClickListener {
            binding.layoutClock8Bit.animateSpringBounce()
            showAnalogClockModal()
        }

        clock8BitJob?.cancel()
        clock8BitJob = lifecycleScope.launch {
            while (isActive) {
                val cal = Calendar.getInstance()
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val min = cal.get(Calendar.MINUTE)
                val sec = cal.get(Calendar.SECOND)
                val year = cal.get(Calendar.YEAR)

                binding.tvClock8Bit.text = String.format("%02dD %02d:%02d:%02d", dayOfMonth, hour, min, sec)
                binding.tvCalendarYearTab.text = year.toString()
                binding.tvIosStatusTime.text = String.format("%d:%02d", if (hour % 12 == 0) 12 else hour % 12, min)

                delay(1000L)
            }
        }
    }

    private fun setupDynamicIsland() {
        binding.layoutDynamicIsland.applyButteryPress(pressedScale = 0.94f)
        binding.layoutDynamicIsland.setOnClickListener {
            soundEngine.playButteryClick()
            binding.layoutDynamicIsland.animateSpringBounce()
            showDynamicIslandModal()
        }
        val pulseAnim = android.animation.ObjectAnimator.ofFloat(binding.viewIslandPulse, "alpha", 0.35f, 1.0f).apply {
            duration = 1100L
            repeatCount = android.animation.ValueAnimator.INFINITE
            repeatMode = android.animation.ValueAnimator.REVERSE
        }
        pulseAnim.start()
    }

    fun updateDynamicIsland(status: String, icon: String = "🧠") {
        runOnUiThread {
            binding.tvIslandStatus.text = status
            binding.tvIslandIcon.text = icon
            binding.layoutDynamicIsland.animateSpringBounce()
        }
    }

    private fun setupBatteryMonitoring() {
        val updateBatteryFromIntent: (Intent?) -> Unit = { intent ->
            if (intent != null) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (level >= 0 && scale > 0) {
                    val pct = (level * 100) / scale
                    val icon = if (isCharging) "⚡" else when {
                        pct >= 90 -> "🔋"
                        pct >= 50 -> "🔋"
                        pct >= 20 -> "🪫"
                        else -> "🪫"
                    }
                    binding.tvIosBattery.text = "$icon $pct%"
                }
            }
        }

        // Get immediate initial battery sticky intent
        try {
            val stickyIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            updateBatteryFromIntent(stickyIntent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to query initial battery status", e)
        }

        // Register dynamic receiver for live battery changes
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateBatteryFromIntent(intent)
            }
        }
        batteryReceiver = receiver
        try {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to register battery receiver", e)
        }
    }

    private fun showDynamicIslandModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_dynamic_island, null)
        dialog.setContentView(view)

        val btnDismiss = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDismissDynamicIsland)
        btnDismiss?.applyButteryPress()
        btnDismiss?.setOnClickListener {
            soundEngine.playButteryClick()
            btnDismiss.animateSpringBounce()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAnalogClockModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_analog_clock, null)
        dialog.setContentView(view)

        val analogClock = view.findViewById<AnalogClockView>(R.id.analogClockView)
        val tvDigitalTime = view.findViewById<TextView>(R.id.tvAnalogDigitalTime)
        val tvDigitalDate = view.findViewById<TextView>(R.id.tvAnalogDigitalDate)
        val tvTzInfo = view.findViewById<TextView>(R.id.tvAnalogTzInfo)
        val btnToggleSweep = view.findViewById<MaterialButton>(R.id.btnToggleSweep)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseClock)

        val theme = ThemeManager.currentTheme.value
        analogClock.setThemeColors(theme.accentColor, theme.accentSecondary, theme.surfaceColor)

        btnToggleSweep.applyButteryPress()
        btnClose.applyButteryPress()

        btnToggleSweep.setOnClickListener {
            btnToggleSweep.animateSpringBounce()
            analogClock.isSmoothSweep = !analogClock.isSmoothSweep
            btnToggleSweep.text = if (analogClock.isSmoothSweep) "🌊 Smooth Sweep" else "⏱️ Step Tick"
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())

        val clockModalJob = lifecycleScope.launch {
            while (isActive && dialog.isShowing) {
                val now = Calendar.getInstance()
                tvDigitalTime.text = timeFormat.format(now.time)
                tvDigitalDate.text = dateFormat.format(now.time)
                tvTzInfo.text = "Day ${now.get(Calendar.DAY_OF_YEAR)} of ${now.getActualMaximum(Calendar.DAY_OF_YEAR)} • ${now.timeZone.displayName}"
                delay(1000L)
            }
        }

        dialog.setOnDismissListener {
            clockModalJob.cancel()
        }

        dialog.show()
    }

    private fun showCalendarModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_calendar, null)
        dialog.setContentView(view)

        var currentYear = Calendar.getInstance().get(Calendar.YEAR)
        var currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1..12
        var selectedDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        val monthNames = listOf(
            "JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE",
            "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"
        )

        val tvYear = view.findViewById<TextView>(R.id.tvCurrentYear)
        val tvMonth = view.findViewById<TextView>(R.id.tvCurrentMonth)
        val btnPrevYear = view.findViewById<ImageButton>(R.id.btnPrevYear)
        val btnNextYear = view.findViewById<ImageButton>(R.id.btnNextYear)
        val btnPrevMonth = view.findViewById<ImageButton>(R.id.btnPrevMonth)
        val btnNextMonth = view.findViewById<ImageButton>(R.id.btnNextMonth)
        val grid = view.findViewById<GridLayout>(R.id.gridCalendarDays)

        // Celebration card views
        val tvEmoji = view.findViewById<TextView>(R.id.tvCelebrationEmoji)
        val tvTitle = view.findViewById<TextView>(R.id.tvCelebrationTitle)
        val tvCategory = view.findViewById<TextView>(R.id.tvCelebrationCategory)
        val tvDateBadge = view.findViewById<TextView>(R.id.tvCelebrationSelectedDate)
        val tvDesc = view.findViewById<TextView>(R.id.tvCelebrationDescription)
        val tvRituals = view.findViewById<TextView>(R.id.tvCelebrationRituals)
        val btnAskDeity = view.findViewById<MaterialButton>(R.id.btnAskDeityAboutCelebration)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseCalendar)

        // Quick Year jump chips
        val chipToday = view.findViewById<TextView>(R.id.chipYearToday)
        val chip2030 = view.findViewById<TextView>(R.id.chipYear2030)
        val chip2050 = view.findViewById<TextView>(R.id.chipYear2050)
        val chip3000 = view.findViewById<TextView>(R.id.chipYear3000)
        val chipMax = view.findViewById<TextView>(R.id.chipYearMax)
        val chip1AD = view.findViewById<TextView>(R.id.chipYear1AD)

        btnPrevYear.applyButteryPress()
        btnNextYear.applyButteryPress()
        btnPrevMonth.applyButteryPress()
        btnNextMonth.applyButteryPress()
        chipToday.applyButteryPress()
        chip2030.applyButteryPress()
        chip2050.applyButteryPress()
        chip3000.applyButteryPress()
        chipMax.applyButteryPress()
        chip1AD.applyButteryPress()
        btnAskDeity.applyButteryPress()
        btnClose.applyButteryPress()

        fun updateCelebrationCard() {
            val cel = celebrationEngine.getCelebrationForDate(currentYear, currentMonth, selectedDay)
            tvEmoji.text = cel.deityEmoji
            tvTitle.text = cel.title
            tvCategory.text = "Presiding Deity: ${cel.deityEmoji} ${cel.deityName} • ${cel.category}"
            tvDateBadge.text = "${monthNames[currentMonth - 1].take(3)} $selectedDay, $currentYear"
            tvDesc.text = cel.description
            tvRituals.text = "• Observance: ${cel.ritualsAndObservance}"
            btnAskDeity.text = "${cel.deityEmoji} Ask ${cel.deityName} AI About This Celebration"

            btnAskDeity.setOnClickListener {
                dialog.dismiss()
                binding.bottomNav.selectedItemId = R.id.nav_chat
                showFragment(chatFragment, "CHAT")
                val targetPersona = when (cel.deityName) {
                    "Shiva" -> Persona.SHIVA
                    "Brahma" -> Persona.BRAHMA
                    "Shani" -> Persona.SHANI
                    "Ganesha" -> Persona.GANESHA
                    "Muruga" -> Persona.MURUGA
                    "Hanuman" -> Persona.HANUMAN
                    "Lakshmi" -> Persona.LAKSHMI
                    "Parvathi" -> Persona.PARVATHI
                    "Narasimha" -> Persona.NARASIMHA
                    else -> Persona.KRISHNA
                }
                chatFragment.selectPersonaAndPrompt(
                    targetPersona,
                    "Tell me the spiritual significance, history, and sacred rituals of ${cel.title} (celebrated on ${monthNames[currentMonth - 1]} $selectedDay, $currentYear)."
                )
            }
        }

        fun renderCalendar() {
            tvYear.text = "$currentYear"
            tvMonth.text = "${monthNames[currentMonth - 1]} $currentYear"

            grid.removeAllViews()

            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, currentYear)
                set(Calendar.MONTH, currentMonth - 1)
                set(Calendar.DAY_OF_MONTH, 1)
            }

            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            if (selectedDay > maxDays) selectedDay = maxDays

            // Leading blanks
            for (i in 1 until firstDayOfWeek) {
                val blank = View(this).apply {
                    val params = GridLayout.LayoutParams().apply {
                        width = 0
                        height = (40 * resources.displayMetrics.density).toInt()
                        columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    }
                    layoutParams = params
                }
                grid.addView(blank)
            }

            val todayCal = Calendar.getInstance()
            val isCurrentYearToday = (currentYear == todayCal.get(Calendar.YEAR))
            val isCurrentMonthToday = (currentMonth == todayCal.get(Calendar.MONTH) + 1)
            val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

            val theme = ThemeManager.currentTheme.value

            for (day in 1..maxDays) {
                val dayView = layoutInflater.inflate(R.layout.item_calendar_day, grid, false)
                val tvDayNum = dayView.findViewById<TextView>(R.id.tvDayNumber)
                val tvBadge = dayView.findViewById<TextView>(R.id.tvDayBadge)

                tvDayNum.text = "$day"

                val hasCeleb = celebrationEngine.hasMajorCelebration(currentMonth, day)
                if (hasCeleb) {
                    tvBadge.visibility = View.VISIBLE
                    tvBadge.text = "✨"
                } else {
                    tvBadge.visibility = View.GONE
                }

                // Selected day styling
                if (day == selectedDay) {
                    dayView.setBackgroundColor(theme.accentColor)
                    tvDayNum.setTextColor(Color.WHITE)
                } else if (isCurrentYearToday && isCurrentMonthToday && day == todayDay) {
                    dayView.setBackgroundColor(Color.parseColor("#334155"))
                    tvDayNum.setTextColor(Color.parseColor("#06B6D4"))
                } else {
                    dayView.background = ThemeManager.chipBackground(this, theme)
                    tvDayNum.setTextColor(theme.textPrimary)
                }

                dayView.applyButteryPress(pressedScale = 0.90f)
                dayView.setOnClickListener {
                    dayView.animateSpringBounce()
                    selectedDay = day
                    renderCalendar()
                }

                grid.addView(dayView)
            }

            updateCelebrationCard()
        }

        renderCalendar()

        btnPrevYear.setOnClickListener {
            btnPrevYear.animateSpringBounce()
            currentYear = (currentYear - 1).coerceAtLeast(1)
            renderCalendar()
        }

        btnNextYear.setOnClickListener {
            btnNextYear.animateSpringBounce()
            currentYear = (currentYear + 1).coerceAtMost(9999)
            renderCalendar()
        }

        btnPrevMonth.setOnClickListener {
            btnPrevMonth.animateSpringBounce()
            if (currentMonth == 1) {
                currentMonth = 12
                currentYear = (currentYear - 1).coerceAtLeast(1)
            } else {
                currentMonth--
            }
            renderCalendar()
        }

        btnNextMonth.setOnClickListener {
            btnNextMonth.animateSpringBounce()
            if (currentMonth == 12) {
                currentMonth = 1
                currentYear = (currentYear + 1).coerceAtMost(9999)
            } else {
                currentMonth++
            }
            renderCalendar()
        }

        chipToday.setOnClickListener {
            chipToday.animateSpringBounce()
            val now = Calendar.getInstance()
            currentYear = now.get(Calendar.YEAR)
            currentMonth = now.get(Calendar.MONTH) + 1
            selectedDay = now.get(Calendar.DAY_OF_MONTH)
            renderCalendar()
        }

        chip2030.setOnClickListener {
            chip2030.animateSpringBounce()
            currentYear = 2030
            renderCalendar()
        }

        chip2050.setOnClickListener {
            chip2050.animateSpringBounce()
            currentYear = 2050
            renderCalendar()
        }

        chip3000.setOnClickListener {
            chip3000.animateSpringBounce()
            currentYear = 3000
            renderCalendar()
        }

        chipMax.setOnClickListener {
            chipMax.animateSpringBounce()
            currentYear = 9999
            renderCalendar()
        }

        chip1AD.setOnClickListener {
            chip1AD.animateSpringBounce()
            currentYear = 1
            renderCalendar()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupThemeBadge() {
        binding.layoutThemeBadge.applyButteryPress()
        binding.layoutThemeBadge.setOnClickListener {
            showSettingsModal()
        }
    }

    private fun setupSettings() {
        binding.btnSettings.applyButteryPress()
        binding.btnSettings.setOnClickListener {
            showSettingsModal()
        }
    }

    private fun showSettingsModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_settings, null)
        dialog.setContentView(view)

        // Lighting mode buttons
        val btnDark = view.findViewById<View>(R.id.btnSettingsModeDark)
        val btnSemiDark = view.findViewById<View>(R.id.btnSettingsModeSemiDark)
        val btnLight = view.findViewById<View>(R.id.btnSettingsModeLight)

        val indDark = view.findViewById<View>(R.id.tvSettingsModeDarkInd)
        val indSemiDark = view.findViewById<View>(R.id.tvSettingsModeSemiDarkInd)
        val indLight = view.findViewById<View>(R.id.tvSettingsModeLightInd)

        // Aesthetic style cards
        val cardIos = view.findViewById<View>(R.id.cardSettingsIos)
        val cardMatte = view.findViewById<View>(R.id.cardSettingsMatte)
        val cardSolid = view.findViewById<View>(R.id.cardSettingsSolid)
        val cardGlassy = view.findViewById<View>(R.id.cardSettingsGlassy)

        val tvCheckIos = view.findViewById<View>(R.id.tvSettingsCheckIos)
        val tvCheckMatte = view.findViewById<View>(R.id.tvSettingsCheckMatte)
        val tvCheckSolid = view.findViewById<View>(R.id.tvSettingsCheckSolid)
        val tvCheckGlassy = view.findViewById<View>(R.id.tvSettingsCheckGlassy)

        // Deity Accent Palette buttons
        val btnPaletteViolet = view.findViewById<View>(R.id.btnSettingsPaletteViolet)
        val btnPaletteCyan = view.findViewById<View>(R.id.btnSettingsPaletteCyan)
        val btnPaletteAmber = view.findViewById<View>(R.id.btnSettingsPaletteAmber)
        val btnPaletteRose = view.findViewById<View>(R.id.btnSettingsPaletteRose)
        val btnPaletteEmerald = view.findViewById<View>(R.id.btnSettingsPaletteEmerald)
        val btnPaletteRuby = view.findViewById<View>(R.id.btnSettingsPaletteRuby)
        val btnPaletteGold = view.findViewById<View>(R.id.btnSettingsPaletteGold)
        val btnPaletteAzure = view.findViewById<View>(R.id.btnSettingsPaletteAzure)
        val btnPaletteTitanium = view.findViewById<View>(R.id.btnSettingsPaletteTitanium)

        val indPaletteViolet = view.findViewById<View>(R.id.tvSettingsPaletteVioletInd)
        val indPaletteCyan = view.findViewById<View>(R.id.tvSettingsPaletteCyanInd)
        val indPaletteAmber = view.findViewById<View>(R.id.tvSettingsPaletteAmberInd)
        val indPaletteRose = view.findViewById<View>(R.id.tvSettingsPaletteRoseInd)
        val indPaletteEmerald = view.findViewById<View>(R.id.tvSettingsPaletteEmeraldInd)
        val indPaletteRuby = view.findViewById<View>(R.id.tvSettingsPaletteRubyInd)
        val indPaletteGold = view.findViewById<View>(R.id.tvSettingsPaletteGoldInd)
        val indPaletteAzure = view.findViewById<View>(R.id.tvSettingsPaletteAzureInd)
        val indPaletteTitanium = view.findViewById<View>(R.id.tvSettingsPaletteTitaniumInd)

        // Bright UI Palette buttons (Optimized for Solid Colour & Matte Finish)
        val btnPaletteBrightYellow = view.findViewById<View>(R.id.btnSettingsPaletteBrightYellow)
        val btnPaletteBrightCyan = view.findViewById<View>(R.id.btnSettingsPaletteBrightCyan)
        val btnPaletteBrightMagenta = view.findViewById<View>(R.id.btnSettingsPaletteBrightMagenta)
        val btnPaletteBrightLime = view.findViewById<View>(R.id.btnSettingsPaletteBrightLime)
        val btnPaletteBrightOrange = view.findViewById<View>(R.id.btnSettingsPaletteBrightOrange)
        val btnPaletteBrightBlue = view.findViewById<View>(R.id.btnSettingsPaletteBrightBlue)
        val btnPaletteBrightViolet = view.findViewById<View>(R.id.btnSettingsPaletteBrightViolet)

        val indPaletteBrightYellow = view.findViewById<View>(R.id.tvSettingsPaletteBrightYellowInd)
        val indPaletteBrightCyan = view.findViewById<View>(R.id.tvSettingsPaletteBrightCyanInd)
        val indPaletteBrightMagenta = view.findViewById<View>(R.id.tvSettingsPaletteBrightMagentaInd)
        val indPaletteBrightLime = view.findViewById<View>(R.id.tvSettingsPaletteBrightLimeInd)
        val indPaletteBrightOrange = view.findViewById<View>(R.id.tvSettingsPaletteBrightOrangeInd)
        val indPaletteBrightBlue = view.findViewById<View>(R.id.tvSettingsPaletteBrightBlueInd)
        val indPaletteBrightViolet = view.findViewById<View>(R.id.tvSettingsPaletteBrightVioletInd)

        // Live preview elements
        val layoutPreview = view.findViewById<View>(R.id.layoutSettingsLivePreview)
        val previewUserBubble = view.findViewById<View>(R.id.previewUserBubble)
        val previewAiBubble = view.findViewById<View>(R.id.previewAiBubble)
        val tvPreviewAiText = view.findViewById<TextView>(R.id.tvPreviewAiText)

        // Storage & Action buttons
        val tvStorageInfo = view.findViewById<TextView>(R.id.tvSettingsStorageInfo)
        val btnFetchKnowledge = view.findViewById<MaterialButton>(R.id.btnSettingsFetchKnowledge)
        val btnClearCache = view.findViewById<MaterialButton>(R.id.btnSettingsClearCache)
        val btnToggleSound = view.findViewById<View>(R.id.btnToggleSoundFeedback)
        val tvSoundStatus = view.findViewById<TextView>(R.id.tvSoundFeedbackStatus)
        val btnOpenDataManager = view.findViewById<MaterialButton>(R.id.btnSettingsOpenDataManager)

        // Apply buttery tactile touch physics
        btnDark.applyButteryPress()
        btnSemiDark.applyButteryPress()
        btnLight.applyButteryPress()
        cardIos?.applyButteryPress()
        cardMatte.applyButteryPress()
        cardSolid.applyButteryPress()
        cardGlassy.applyButteryPress()
        btnFetchKnowledge.applyButteryPress()
        btnClearCache.applyButteryPress()
        btnToggleSound?.applyButteryPress()
        btnOpenDataManager?.applyButteryPress()

        tvSoundStatus?.text = if (soundEngine.isSoundFeedbackEnabled) "ON" else "OFF"
        tvSoundStatus?.setTextColor(if (soundEngine.isSoundFeedbackEnabled) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"))

        btnToggleSound?.setOnClickListener {
            btnToggleSound.animateSpringBounce()
            soundEngine.isSoundFeedbackEnabled = !soundEngine.isSoundFeedbackEnabled
            tvSoundStatus?.text = if (soundEngine.isSoundFeedbackEnabled) "ON" else "OFF"
            tvSoundStatus?.setTextColor(if (soundEngine.isSoundFeedbackEnabled) Color.parseColor("#10B981") else Color.parseColor("#94A3B8"))
            if (soundEngine.isSoundFeedbackEnabled) soundEngine.playButteryClick()
        }

        btnOpenDataManager?.setOnClickListener {
            btnOpenDataManager.animateSpringBounce()
            soundEngine.playButteryClick()
            dialog.dismiss()
            showDataManagerModal()
        }

        val syncedCount = knowledgeEngine.getSyncedCount()
        tvStorageInfo.text = "Memory: $syncedCount synced articles in local TF-IDF semantic database"

        fun updateUI() {
            val style = ThemeManager.getActiveStyle()
            val mode = ThemeManager.getActiveMode()
            val activePalette = ThemeManager.getActivePalette()
            val theme = ThemeManager.currentTheme.value

            indDark.visibility = if (mode == LuminanceMode.DARK) View.VISIBLE else View.GONE
            indSemiDark.visibility = if (mode == LuminanceMode.SEMI_DARK) View.VISIBLE else View.GONE
            indLight.visibility = if (mode == LuminanceMode.LIGHT) View.VISIBLE else View.GONE

            tvCheckIos?.visibility = if (style == AestheticStyle.IOS) View.VISIBLE else View.GONE
            tvCheckMatte.visibility = if (style == AestheticStyle.MATTE) View.VISIBLE else View.GONE
            tvCheckSolid.visibility = if (style == AestheticStyle.SOLID) View.VISIBLE else View.GONE
            tvCheckGlassy.visibility = if (style == AestheticStyle.GLASSY) View.VISIBLE else View.GONE

            indPaletteViolet?.visibility = if (activePalette == AccentPalette.COSMIC_VIOLET) View.VISIBLE else View.GONE
            indPaletteCyan?.visibility = if (activePalette == AccentPalette.KAILASH_CYAN) View.VISIBLE else View.GONE
            indPaletteAmber?.visibility = if (activePalette == AccentPalette.VEDIC_SAFFRON) View.VISIBLE else View.GONE
            indPaletteRose?.visibility = if (activePalette == AccentPalette.LOTUS_ROSE) View.VISIBLE else View.GONE
            indPaletteEmerald?.visibility = if (activePalette == AccentPalette.HARMONY_EMERALD) View.VISIBLE else View.GONE
            indPaletteRuby?.visibility = if (activePalette == AccentPalette.SACRED_RUBY) View.VISIBLE else View.GONE
            indPaletteGold?.visibility = if (activePalette == AccentPalette.SOLAR_GOLD) View.VISIBLE else View.GONE
            indPaletteAzure?.visibility = if (activePalette == AccentPalette.ROYAL_AZURE) View.VISIBLE else View.GONE
            indPaletteTitanium?.visibility = if (activePalette == AccentPalette.SHANI_TITANIUM) View.VISIBLE else View.GONE

            indPaletteBrightYellow?.visibility = if (activePalette == AccentPalette.BRIGHT_CYBER_YELLOW) View.VISIBLE else View.GONE
            indPaletteBrightCyan?.visibility = if (activePalette == AccentPalette.BRIGHT_ELECTRIC_CYAN) View.VISIBLE else View.GONE
            indPaletteBrightMagenta?.visibility = if (activePalette == AccentPalette.BRIGHT_HOT_MAGENTA) View.VISIBLE else View.GONE
            indPaletteBrightLime?.visibility = if (activePalette == AccentPalette.BRIGHT_RADIANT_LIME) View.VISIBLE else View.GONE
            indPaletteBrightOrange?.visibility = if (activePalette == AccentPalette.BRIGHT_BLAZING_ORANGE) View.VISIBLE else View.GONE
            indPaletteBrightBlue?.visibility = if (activePalette == AccentPalette.BRIGHT_ELECTRIC_BLUE) View.VISIBLE else View.GONE
            indPaletteBrightViolet?.visibility = if (activePalette == AccentPalette.BRIGHT_ULTRA_VIOLET) View.VISIBLE else View.GONE

            // Live preview update
            layoutPreview.background = ThemeManager.cardBackground(this@MainActivity, theme)
            previewUserBubble.background = ThemeManager.chatUserBackground(this@MainActivity, theme)
            previewAiBubble.background = ThemeManager.chatAiBackground(this@MainActivity, theme)
            tvPreviewAiText.setTextColor(theme.textPrimary)
        }

        updateUI()

        val paletteMap = listOf(
            Pair(btnPaletteViolet, AccentPalette.COSMIC_VIOLET),
            Pair(btnPaletteCyan, AccentPalette.KAILASH_CYAN),
            Pair(btnPaletteAmber, AccentPalette.VEDIC_SAFFRON),
            Pair(btnPaletteRose, AccentPalette.LOTUS_ROSE),
            Pair(btnPaletteEmerald, AccentPalette.HARMONY_EMERALD),
            Pair(btnPaletteRuby, AccentPalette.SACRED_RUBY),
            Pair(btnPaletteGold, AccentPalette.SOLAR_GOLD),
            Pair(btnPaletteAzure, AccentPalette.ROYAL_AZURE),
            Pair(btnPaletteTitanium, AccentPalette.SHANI_TITANIUM),
            Pair(btnPaletteBrightYellow, AccentPalette.BRIGHT_CYBER_YELLOW),
            Pair(btnPaletteBrightCyan, AccentPalette.BRIGHT_ELECTRIC_CYAN),
            Pair(btnPaletteBrightMagenta, AccentPalette.BRIGHT_HOT_MAGENTA),
            Pair(btnPaletteBrightLime, AccentPalette.BRIGHT_RADIANT_LIME),
            Pair(btnPaletteBrightOrange, AccentPalette.BRIGHT_BLAZING_ORANGE),
            Pair(btnPaletteBrightBlue, AccentPalette.BRIGHT_ELECTRIC_BLUE),
            Pair(btnPaletteBrightViolet, AccentPalette.BRIGHT_ULTRA_VIOLET)
        )
        for ((btn, pal) in paletteMap) {
            btn?.applyButteryPress()
            btn?.setOnClickListener {
                btn.animateSpringBounce()
                soundEngine.playButteryClick()
                ThemeManager.setAccentPalette(this, pal)
                updateUI()
                applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
            }
        }

        btnDark.setOnClickListener {
            btnDark.animateSpringBounce()
            ThemeManager.setMode(this, LuminanceMode.DARK)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        btnSemiDark.setOnClickListener {
            btnSemiDark.animateSpringBounce()
            ThemeManager.setMode(this, LuminanceMode.SEMI_DARK)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        btnLight.setOnClickListener {
            btnLight.animateSpringBounce()
            ThemeManager.setMode(this, LuminanceMode.LIGHT)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        cardIos?.setOnClickListener {
            cardIos.animateSpringBounce()
            ThemeManager.setStyle(this, AestheticStyle.IOS)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        cardMatte.setOnClickListener {
            cardMatte.animateSpringBounce()
            ThemeManager.setStyle(this, AestheticStyle.MATTE)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        cardSolid.setOnClickListener {
            cardSolid.animateSpringBounce()
            ThemeManager.setStyle(this, AestheticStyle.SOLID)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        cardGlassy.setOnClickListener {
            cardGlassy.animateSpringBounce()
            ThemeManager.setStyle(this, AestheticStyle.GLASSY)
            updateUI()
            applyThemeAcrossApp(ThemeManager.currentTheme.value, animate = true)
        }

        btnFetchKnowledge.setOnClickListener {
            dialog.dismiss()
            binding.bottomNav.selectedItemId = R.id.nav_knowledge
            showFragment(knowledgeFragment, "KNOWLEDGE")
        }

        btnClearCache.setOnClickListener {
            btnClearCache.animateSpringBounce()
            knowledgeEngine.clearSyncedArticles()
            tvStorageInfo.text = "Memory: 0 synced articles (re-indexed base corpus)"
            Toast.makeText(this, "Local knowledge cache reset", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    fun showDataManagerModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_data_manager, null)
        dialog.setContentView(view)

        val tvTotalArticles = view.findViewById<TextView>(R.id.tvStatTotalArticles)
        val tvSyncedArticles = view.findViewById<TextView>(R.id.tvStatSyncedArticles)
        val tvSavedPictures = view.findViewById<TextView>(R.id.tvStatSavedPictures)
        val tvDbSize = view.findViewById<TextView>(R.id.tvStatDbSize)

        val btnExportChat = view.findViewById<MaterialButton>(R.id.btnExportChatHistory)
        val btnExportKnowledge = view.findViewById<MaterialButton>(R.id.btnExportKnowledgeJson)
        val btnImportNotes = view.findViewById<MaterialButton>(R.id.btnImportSampleNotes)
        val btnClearCache = view.findViewById<MaterialButton>(R.id.btnClearCacheAction)
        val btnClose = view.findViewById<MaterialButton>(R.id.btnCloseDataManager)

        btnExportChat.applyButteryPress()
        btnExportKnowledge.applyButteryPress()
        btnImportNotes.applyButteryPress()
        btnClearCache.applyButteryPress()
        btnClose.applyButteryPress()

        fun updateStats() {
            val stats = exportImportEngine.getStorageFootprintInfo(
                articlesCount = knowledgeEngine.getAllArticles().size,
                syncedCount = knowledgeEngine.getSyncedCount()
            )
            tvTotalArticles.text = "${stats.totalArticlesCount}"
            tvSyncedArticles.text = "${stats.syncedArticlesCount}"
            tvSavedPictures.text = "${stats.savedPicturesCount}"
            val kb = stats.databaseSizeBytes / 1024
            tvDbSize.text = if (kb > 1024) "${kb / 1024} MB" else "${kb} KB"
        }

        updateStats()

        btnExportChat.setOnClickListener {
            btnExportChat.animateSpringBounce()
            soundEngine.playButteryClick()
            val file = exportImportEngine.exportChatToMarkdown(chatFragment.getMessages())
            if (file != null && file.exists()) {
                Toast.makeText(this, "Exported: ${file.name} to Documents/Maphula!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Exported chat conversation to storage!", Toast.LENGTH_SHORT).show()
            }
            updateStats()
        }

        btnExportKnowledge.setOnClickListener {
            btnExportKnowledge.animateSpringBounce()
            soundEngine.playButteryClick()
            val file = exportImportEngine.exportKnowledgeToJson(knowledgeEngine.getAllArticles())
            if (file != null && file.exists()) {
                Toast.makeText(this, "Backed up: ${file.name} to Documents/Maphula!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Knowledge database backed up!", Toast.LENGTH_SHORT).show()
            }
            updateStats()
        }

        btnImportNotes.setOnClickListener {
            btnImportNotes.animateSpringBounce()
            soundEngine.playButteryClick()
            val sampleJson = """
                [
                  {
                    "id": "custom_samkhya_philosophy",
                    "title": "Samkhya Dualism: Purusha & Prakriti",
                    "category": "Sacred Scriptures & Philosophy",
                    "tags": ["samkhya", "purusha", "prakriti", "philosophy", "consciousness"],
                    "summary": "Foundational metaphysics of Samkhya philosophy distinguishing eternal consciousness (Purusha) from manifest matter (Prakriti).",
                    "content": "Samkhya philosophy establishes that liberation (Kaivalya) is attained through discriminative knowledge separating Purusha (pure unattached consciousness) from Prakriti (cosmic matter with the three Gunas: Sattva, Rajas, Tamas).",
                    "keyTakeaways": ["Purusha is the unattached conscious witness", "Prakriti is creative manifestation", "Discriminative wisdom brings liberation"]
                  }
                ]
            """.trimIndent()
            val imported = exportImportEngine.importKnowledgeFromJson(sampleJson)
            if (imported.isNotEmpty()) {
                knowledgeEngine.addCustomArticles(imported)
                updateStats()
                Toast.makeText(this, "Imported ${imported.size} custom knowledge articles into local database!", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearCache.setOnClickListener {
            btnClearCache.animateSpringBounce()
            soundEngine.playButteryClick()
            exportImportEngine.clearCache()
            knowledgeEngine.clearSyncedArticles()
            updateStats()
            Toast.makeText(this, "Cache and temporary files cleared!", Toast.LENGTH_SHORT).show()
        }

        val btnPinWidget = view.findViewById<View>(R.id.btnSettingsPinWidget)
        btnPinWidget?.setOnClickListener {
            btnPinWidget.animateSpringBounce()
            soundEngine.playButteryClick()
            pinWidgetToHomeScreen()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    fun applyThemeAcrossApp(theme: ResolvedTheme, animate: Boolean = false) {
        if (animate) {
            val currentBg = (binding.root.background as? android.graphics.drawable.ColorDrawable)?.color ?: theme.bgColor
            MotionUtils.animateColor(currentBg, theme.bgColor, duration = 250L) { animatedColor ->
                binding.root.setBackgroundColor(animatedColor)
            }
        }
        ThemeManager.applyThemeToMain(binding, window, theme)

        supportFragmentManager.fragments.forEach { fragment ->
            when (fragment) {
                is ChatFragment -> fragment.applyTheme(theme)
                is CodeStudioFragment -> fragment.applyTheme(theme)
                is KnowledgeFragment -> fragment.applyTheme(theme)
                is NlpStudioFragment -> fragment.applyTheme(theme)
                is NeuralLabFragment -> fragment.applyTheme(theme)
                is CreativeStudioFragment -> fragment.applyTheme(theme)
            }
        }
    }

    private fun setupNetworkBadge() {
        binding.layoutNetworkBadge.applyButteryPress()
        binding.layoutNetworkBadge.setOnClickListener {
            showNetworkStatusModal()
        }

        lifecycleScope.launch {
            networkMonitor.isOnlineFlow.collectLatest { isOnline ->
                if (isOnline) {
                    binding.viewNetworkDot.backgroundTintList =
                        ContextCompat.getColorStateList(this@MainActivity, R.color.secondary)
                    binding.tvNetworkStatus.text = "ONLINE"
                    binding.tvNetworkStatus.setTextColor(
                        ContextCompat.getColor(this@MainActivity, R.color.secondary)
                    )
                } else {
                    binding.viewNetworkDot.backgroundTintList =
                        ContextCompat.getColorStateList(this@MainActivity, R.color.accent_green)
                    binding.tvNetworkStatus.text = "OFFLINE"
                    binding.tvNetworkStatus.setTextColor(
                        ContextCompat.getColor(this@MainActivity, R.color.accent_green)
                    )
                }
            }
        }
    }

    private fun showNetworkStatusModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_sync_knowledge, null)
        dialog.setContentView(view)

        val vDialogDot = view.findViewById<View>(R.id.vDialogDot)
        val tvDialogStatus = view.findViewById<TextView>(R.id.tvDialogStatus)
        val btnAutoSwitch = view.findViewById<MaterialButton>(R.id.btnToggleAutoSwitch)
        val seekNetwork = view.findViewById<SeekBar>(R.id.seekNetworkMode)
        val tvSliderCurrentMode = view.findViewById<TextView>(R.id.tvSliderCurrentMode)
        val btnSyncCurated = view.findViewById<MaterialButton>(R.id.btnSyncCurated)

        val syncedCount = knowledgeEngine.getSyncedCount()

        fun refreshNetworkModalUI() {
            val isOnline = networkMonitor.isOnline()
            val isAuto = networkMonitor.isAutoSwitchEnabled

            btnAutoSwitch?.text = if (isAuto) "🤖 Auto Switch: ON" else "✋ Auto Switch: OFF"
            seekNetwork?.isEnabled = !isAuto
            seekNetwork?.progress = if (isOnline) 1 else 0

            if (isOnline) {
                vDialogDot.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.secondary)
                tvDialogStatus.text = "🟢 Online Mode: Ready to retrieve and cache new knowledge"
                tvDialogStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.secondary))
            } else {
                vDialogDot.backgroundTintList = ContextCompat.getColorStateList(this@MainActivity, R.color.accent_green)
                tvDialogStatus.text = "⚪ Offline Mode: Operating 100% on local storage ($syncedCount synced articles)"
                tvDialogStatus.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.accent_green))
            }

            tvSliderCurrentMode?.text = when {
                isAuto && isOnline -> "Current Mode: Auto-Switch (🟢 Hardware Internet Connected)"
                isAuto && !isOnline -> "Current Mode: Auto-Switch (⚪ No Hardware Network, 100% Local)"
                !isAuto && isOnline -> "Current Mode: Manual Override (🟢 Online Retrieval Forced)"
                else -> "Current Mode: Manual Override (⚪ Strict Offline Air-Gap Forced)"
            }
        }

        refreshNetworkModalUI()

        btnAutoSwitch?.setOnClickListener {
            btnAutoSwitch.animateSpringBounce()
            networkMonitor.setAutoSwitch(!networkMonitor.isAutoSwitchEnabled)
            soundEngine.playButteryClick()
            refreshNetworkModalUI()
        }

        seekNetwork?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val forceOnline = progress == 1
                    networkMonitor.setManualMode(forceOnline)
                    soundEngine.playButteryClick()
                    refreshNetworkModalUI()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSyncCurated.setOnClickListener {
            dialog.dismiss()
            binding.bottomNav.selectedItemId = R.id.nav_knowledge
            showFragment(knowledgeFragment, "KNOWLEDGE")
        }

        dialog.show()
    }

    private fun setupNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    showFragment(chatFragment, "CHAT")
                    true
                }
                R.id.nav_code -> {
                    showFragment(codeFragment, "CODE")
                    true
                }
                R.id.nav_creative -> {
                    showFragment(creativeFragment, "CREATIVE")
                    true
                }
                R.id.nav_knowledge -> {
                    showFragment(knowledgeFragment, "KNOWLEDGE")
                    true
                }
                R.id.nav_neural -> {
                    showFragment(neuralFragment, "NEURAL")
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        val fm = supportFragmentManager
        val transaction = fm.beginTransaction()

        val outgoingView = activeFragment?.view

        if (activeFragment != null && activeFragment !== fragment) {
            transaction.hide(activeFragment!!)
        }

        val existing = fm.findFragmentByTag(tag)
        if (existing == null) {
            transaction.add(R.id.fragment_container, fragment, tag)
        } else {
            transaction.show(existing)
        }

        transaction.commit()
        activeFragment = fragment

        // Fluid buttery slide & fade transition between fragments
        MotionUtils.animateFragmentTransition(outgoingView, fragment.view, 240L)

        val theme = ThemeManager.currentTheme.value
        when (fragment) {
            is ChatFragment -> fragment.applyTheme(theme)
            is CodeStudioFragment -> fragment.applyTheme(theme)
            is KnowledgeFragment -> fragment.applyTheme(theme)
            is NlpStudioFragment -> fragment.applyTheme(theme)
            is NeuralLabFragment -> fragment.applyTheme(theme)
            is CreativeStudioFragment -> fragment.applyTheme(theme)
        }
    }

    fun askAiFromKnowledge(articleTitle: String) {
        binding.bottomNav.selectedItemId = R.id.nav_chat
        showFragment(chatFragment, "CHAT")
        chatFragment.sendPrompt("Tell me more about $articleTitle and how to apply it.")
    }

    fun openKnowledgeSearch(query: String) {
        binding.bottomNav.selectedItemId = R.id.nav_knowledge
        showFragment(knowledgeFragment, "KNOWLEDGE")
        knowledgeFragment.setSearchQuery(query)
    }

    fun showRavanaArenaModal() {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_ravana_arena, null)
        dialog.setContentView(view)

        var currentBotCount = 2 // Minimum of two AI bots

        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseRavana)
        val btnDecrease = view.findViewById<Button>(R.id.btnDecreaseBots)
        val btnIncrease = view.findViewById<Button>(R.id.btnIncreaseBots)
        val btnAddCustomBot = view.findViewById<Button>(R.id.btnAddCustomBot)
        val tvBotCount = view.findViewById<TextView>(R.id.tvBotCount)
        val tvBotRoster = view.findViewById<TextView>(R.id.tvBotRosterDescription)
        val tvAvatars = view.findViewById<TextView>(R.id.tvBotAvatarsRow)
        val etIdea = view.findViewById<EditText>(R.id.etIdeaInput)
        val etUserReinforcement = view.findViewById<EditText>(R.id.etUserReinforcement)
        val chip1 = view.findViewById<TextView>(R.id.chipIdea1)
        val chip2 = view.findViewById<TextView>(R.id.chipIdea2)
        val chip3 = view.findViewById<TextView>(R.id.chipIdea3)
        val chip4 = view.findViewById<TextView>(R.id.chipIdea4)
        val btnIgnite = view.findViewById<Button>(R.id.btnIgniteArena)
        val layoutLoading = view.findViewById<LinearLayout>(R.id.layoutBattleLoading)
        val arenaBattleVisualizer = view.findViewById<com.offlineai.app.ui.arena.RavanaArenaBattleView>(R.id.arenaBattleVisualizer)
        val arenaBattleReplayVisualizer = view.findViewById<com.offlineai.app.ui.arena.RavanaArenaBattleView>(R.id.arenaBattleReplayVisualizer)
        val layoutResults = view.findViewById<LinearLayout>(R.id.layoutBattleResults)
        val tvVerdictEmoji = view.findViewById<TextView>(R.id.tvVerdictEmoji)
        val tvVerdictTitle = view.findViewById<TextView>(R.id.tvVerdictTitle)
        val tvSurvivalScore = view.findViewById<TextView>(R.id.tvSurvivalScore)
        val pbScore = view.findViewById<ProgressBar>(R.id.pbSurvivalScore)
        val tvArmorBadge = view.findViewById<TextView>(R.id.tvArmorRatingBadge)
        val tvPenetrationBadge = view.findViewById<TextView>(R.id.tvAttackPenetrationBadge)
        val tvVerdictSummary = view.findViewById<TextView>(R.id.tvVerdictSummary)
        val layoutRounds = view.findViewById<LinearLayout>(R.id.layoutRoundsContainer)
        val tvFatalFlaws = view.findViewById<TextView>(R.id.tvFatalFlawsContent)
        val tvSurvivingPillars = view.findViewById<TextView>(R.id.tvSurvivingPillarsContent)
        val tvFortification = view.findViewById<TextView>(R.id.tvFortificationContent)
        val btnReFight = view.findViewById<Button>(R.id.btnReFight)
        val btnCopy = view.findViewById<Button>(R.id.btnCopyVerdict)
        val btnExportScorecard = view.findViewById<Button>(R.id.btnExportScorecard)

        fun updateBotCountUI() {
            val totalBotsAvailable = ravanaEngine.getAllAvailableBots().size
            tvBotCount.text = "$currentBotCount Bots"
            val bots = ravanaEngine.getBotsForCount(currentBotCount)
            val rosterNames = bots.joinToString(" vs ") { "${it.avatar} ${it.name}" }
            tvBotRoster.text = rosterNames
            tvAvatars.text = bots.joinToString("  ") { it.avatar }
            btnDecrease.isEnabled = currentBotCount > RavanaSkillEngine.MIN_BOTS
            btnIncrease.isEnabled = currentBotCount < totalBotsAvailable
            btnDecrease.alpha = if (btnDecrease.isEnabled) 1.0f else 0.4f
            btnIncrease.alpha = if (btnIncrease.isEnabled) 1.0f else 0.4f
            arenaBattleVisualizer?.setupFighters(bots)
            arenaBattleReplayVisualizer?.setupFighters(bots)
        }

        updateBotCountUI()

        btnAddCustomBot?.applyButteryPress()
        btnAddCustomBot?.setOnClickListener {
            soundEngine.playButteryClick()
            btnAddCustomBot.animateSpringBounce()
            showCustomBotBuilderModal {
                currentBotCount = ravanaEngine.getAllAvailableBots().size
                updateBotCountUI()
            }
        }

        btnDecrease.applyButteryPress()
        btnDecrease.setOnClickListener {
            if (currentBotCount > RavanaSkillEngine.MIN_BOTS) {
                soundEngine.playButteryClick()
                btnDecrease.animateSpringBounce()
                currentBotCount--
                updateBotCountUI()
            }
        }

        btnIncrease.applyButteryPress()
        btnIncrease.setOnClickListener {
            if (currentBotCount < ravanaEngine.getAllAvailableBots().size) {
                soundEngine.playButteryClick()
                btnIncrease.animateSpringBounce()
                currentBotCount++
                updateBotCountUI()
            }
        }

        chip1.applyButteryPress()
        chip1.setOnClickListener { etIdea.setText("AI writing all code autonomously without human intervention") }
        chip2.applyButteryPress()
        chip2.setOnClickListener { etIdea.setText("Quitting full-time job to launch a 100% on-device AI startup") }
        chip3.applyButteryPress()
        chip3.setOnClickListener { etIdea.setText("Replacing relational SQL and NoSQL databases with flat local append-only text files") }
        chip4.applyButteryPress()
        chip4.setOnClickListener { etIdea.setText("Building 100% sovereign on-device software to defeat cloud vendor lock-in") }

        btnClose.applyButteryPress()
        btnClose.setOnClickListener {
            soundEngine.playButteryClick()
            dialog.dismiss()
        }

        fun executeBattle() {
            val ideaText = etIdea.text.toString().trim().ifEmpty {
                "Autonomous On-Device AI Architecture without Cloud Dependencies"
            }
            val reinforcementText = etUserReinforcement?.text?.toString()?.trim()?.takeIf { it.isNotBlank() }

            soundEngine.playButteryClick()
            btnIgnite.animateSpringBounce()

            layoutLoading.visibility = View.VISIBLE
            layoutResults.visibility = View.GONE
            btnIgnite.isEnabled = false

            val currentBots = ravanaEngine.getBotsForCount(currentBotCount)
            arenaBattleVisualizer?.setupFighters(currentBots)
            arenaBattleVisualizer?.startBattleAnimation(durationMs = 2800L)

            lifecycleScope.launch {
                delay(2800)
                val battleResult = ravanaEngine.evaluateIdeaBattle(ideaText, currentBotCount, reinforcementText)

                arenaBattleVisualizer?.stopBattleAnimation()
                layoutLoading.visibility = View.GONE
                layoutResults.visibility = View.VISIBLE
                btnIgnite.isEnabled = true

                arenaBattleReplayVisualizer?.setupFighters(currentBots)
                arenaBattleReplayVisualizer?.startBattleAnimation(durationMs = 3500L)

                // Populate Verdict
                tvVerdictEmoji.text = battleResult.verdict.emoji
                tvVerdictTitle.text = battleResult.verdict.title
                tvVerdictTitle.setTextColor(Color.parseColor(battleResult.verdict.badgeColor))
                tvSurvivalScore.text = "Survival Probability: ${battleResult.survivalScore}%"
                pbScore.progress = battleResult.survivalScore
                pbScore.progressTintList = ColorStateList.valueOf(Color.parseColor(battleResult.verdict.badgeColor))
                tvArmorBadge?.text = "🛡️ Armor Rating: ${battleResult.armorRating}%"
                tvPenetrationBadge?.text = "⚡ Penetration: ${battleResult.attackPenetration}%"
                tvVerdictSummary.text = battleResult.verdictSummary

                // Render Rounds with Health & Armor Gauges
                layoutRounds.removeAllViews()
                for (round in battleResult.rounds) {
                    val roundCard = layoutInflater.inflate(R.layout.item_ravana_round, layoutRounds, false)
                    val tvRoundTitle = roundCard.findViewById<TextView>(R.id.tvRoundTitle)
                    val tvRoundDesc = roundCard.findViewById<TextView>(R.id.tvRoundDescription)
                    val tvRoundMetrics = roundCard.findViewById<TextView>(R.id.tvRoundMetrics)
                    val pbRoundIntegrity = roundCard.findViewById<ProgressBar>(R.id.pbRoundIntegrity)
                    val tvRoundTurns = roundCard.findViewById<TextView>(R.id.tvRoundTurnsText)

                    tvRoundTitle.text = round.title
                    tvRoundDesc.text = round.description
                    tvRoundMetrics?.text = "🛡️ Integrity: ${round.ideaHealthAfterRound}% • Armor: ${round.armorRating}% • Penetration: ${round.attackPenetration}%"
                    pbRoundIntegrity?.progress = round.ideaHealthAfterRound

                    val integrityColor = if (round.ideaHealthAfterRound >= 70) "#10B981" else if (round.ideaHealthAfterRound >= 50) "#F59E0B" else "#EF4444"
                    pbRoundIntegrity?.progressTintList = ColorStateList.valueOf(Color.parseColor(integrityColor))

                    val turnsSb = StringBuilder()
                    for (turn in round.turns) {
                        turnsSb.append("${turn.bot.avatar} **${turn.bot.name}** (${turn.bot.title}):\n")
                        turnsSb.append("\"${turn.statement}\"\n")
                        turnsSb.append("⚡ Impact: ${turn.impactScore}/100\n\n")
                    }
                    tvRoundTurns.text = turnsSb.toString().trim()
                    layoutRounds.addView(roundCard)
                }

                // Populate Lists
                tvFatalFlaws.text = battleResult.fatalFlaws.joinToString("\n") { "• $it" }
                tvSurvivingPillars.text = battleResult.survivingPillars.joinToString("\n") { "• $it" }
                tvFortification.text = battleResult.fortificationRoadmap.joinToString("\n")

                btnCopy.applyButteryPress()
                btnCopy.setOnClickListener {
                    btnCopy.animateSpringBounce()
                    soundEngine.playButteryClick()
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText(
                        "Ravana Arena Report",
                        """
                        ⚔️ RAVANA ADVERSARIAL ARENA REPORT
                        Idea: ${battleResult.userIdea}
                        Council: ${battleResult.botCount} AI Bots
                        Verdict: ${battleResult.verdict.emoji} ${battleResult.verdict.title} (${battleResult.survivalScore}% Survival)
                        Armor Integrity: ${battleResult.armorRating}% | Attack Penetration: ${battleResult.attackPenetration}%
                        
                        ${battleResult.verdictSummary}
                        
                        🔴 Fatal Flaws:
                        ${battleResult.fatalFlaws.joinToString("\n") { "- $it" }}
                        
                        🟢 Surviving Pillars:
                        ${battleResult.survivingPillars.joinToString("\n") { "- $it" }}
                        
                        🛡️ Fortification Directive:
                        ${battleResult.fortificationRoadmap.joinToString("\n")}
                        """.trimIndent()
                    )
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "Battle Verdict Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                }

                btnExportScorecard?.applyButteryPress()
                btnExportScorecard?.setOnClickListener {
                    btnExportScorecard.animateSpringBounce()
                    soundEngine.playButteryClick()
                    val scorecard = ravanaEngine.generateBattleScorecard(battleResult)
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Ravana Battle Scorecard", scorecard)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@MainActivity, "⚔️ Battle Scorecard Copied to Clipboard!", Toast.LENGTH_SHORT).show()
                }

                btnReFight.applyButteryPress()
                btnReFight.setOnClickListener {
                    btnReFight.animateSpringBounce()
                    executeBattle()
                }
            }
        }

        btnIgnite.applyButteryPress()
        btnIgnite.setOnClickListener {
            executeBattle()
        }

        dialog.show()
    }

    fun showCustomBotBuilderModal(onCreated: () -> Unit) {
        val dialog = BottomSheetDialog(this)
        ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_custom_bot_builder, null)
        dialog.setContentView(view)

        val etName = view.findViewById<EditText>(R.id.etCustomBotName)
        val etTitle = view.findViewById<EditText>(R.id.etCustomBotTitle)
        val etAvatar = view.findViewById<EditText>(R.id.etCustomBotAvatar)
        val etFocus = view.findViewById<EditText>(R.id.etCustomBotFocus)
        val btnDeploy = view.findViewById<Button>(R.id.btnDeployCustomBot)

        val chipAttacker = view.findViewById<TextView>(R.id.chipStanceAttacker)
        val chipDefender = view.findViewById<TextView>(R.id.chipStanceDefender)
        val chipAuditor = view.findViewById<TextView>(R.id.chipStanceAuditor)
        val chipChaos = view.findViewById<TextView>(R.id.chipStanceChaos)
        val chipSpecialist = view.findViewById<TextView>(R.id.chipStanceSpecialist)

        var selectedStance = RavanaSkillEngine.BotStance.ATTACKER
        val stanceChips = listOf(
            chipAttacker to RavanaSkillEngine.BotStance.ATTACKER,
            chipDefender to RavanaSkillEngine.BotStance.DEFENDER,
            chipAuditor to RavanaSkillEngine.BotStance.AUDITOR,
            chipChaos to RavanaSkillEngine.BotStance.CHAOS,
            chipSpecialist to RavanaSkillEngine.BotStance.SPECIALIST
        )

        fun updateStanceSelection(stance: RavanaSkillEngine.BotStance) {
            selectedStance = stance
            stanceChips.forEach { (chip, st) ->
                if (chip != null) {
                    if (st == stance) {
                        chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                    } else {
                        chip.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    }
                }
            }
        }

        stanceChips.forEach { (chip, stance) ->
            chip?.applyButteryPress()
            chip?.setOnClickListener {
                soundEngine.playButteryClick()
                updateStanceSelection(stance)
            }
        }

        btnDeploy.applyButteryPress()
        btnDeploy.setOnClickListener {
            soundEngine.playButteryClick()
            btnDeploy.animateSpringBounce()
            val name = etName.text.toString().trim().ifEmpty { "Custom Vanguard" }
            val title = etTitle.text.toString().trim().ifEmpty { "Domain Specialist" }
            val avatar = etAvatar.text.toString().trim().ifEmpty { "👾" }
            val focus = etFocus.text.toString().trim().ifEmpty { "Stress-testing targeted vulnerability surface" }

            ravanaEngine.addCustomBot(
                name = name,
                title = title,
                avatar = avatar,
                stance = selectedStance,
                tagline = "Specialist perspective attacking or defending: $focus",
                primaryFocus = focus
            )

            Toast.makeText(this, "⚔️ Bot '$name' ($avatar) joined Ravana Council!", Toast.LENGTH_SHORT).show()
            onCreated()
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun isWidgetIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val action = intent.action ?: ""
        return action.startsWith("com.offlineai.app.widget.") ||
                intent.hasExtra(MaphulaWidgetProvider.EXTRA_TARGET_TAB) ||
                intent.hasExtra(MaphulaWidgetProvider.EXTRA_OPEN_VOICE) ||
                intent.hasExtra(MaphulaWidgetProvider.EXTRA_PREFILL_PROMPT)
    }

    fun refreshHomeScreenWidgets() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, MaphulaWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(componentName)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    MaphulaWidgetProvider.updateWidget(this, appWidgetManager, id)
                }
            }
        } catch (_: Exception) {}
    }

    fun pinWidgetToHomeScreen() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val canPin = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("🏠 Mapula Home Screen Widget")
            .setMessage(
                "Access 1-Tap Offline Chat, Voice Dictation, Creative Studio & Daily Inspiration sparks directly from your home screen!\n\n" +
                "• 1-Tap Pinning: Tap 'Pin to Home' below to place it automatically (supported launchers).\n\n" +
                "• Manual Placement: On your phone's Home Screen, long-press empty space -> tap 'Widgets' -> choose 'Mapula Offline AI' -> drag to your home screen."
            )
            .setPositiveButton("Pin to Home") { d, _ ->
                d.dismiss()
                if (canPin) {
                    val myProvider = ComponentName(this, MaphulaWidgetProvider::class.java)
                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                    Toast.makeText(this, "✨ Prompting to place Mapula AI Widget on your Home Screen!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "💡 To add: Long-press Home Screen -> tap 'Widgets' -> select 'Mapula Offline AI'", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val targetTab = intent.getStringExtra(MaphulaWidgetProvider.EXTRA_TARGET_TAB)
        val openVoice = intent.getBooleanExtra(MaphulaWidgetProvider.EXTRA_OPEN_VOICE, false)
        val prefillPrompt = intent.getStringExtra(MaphulaWidgetProvider.EXTRA_PREFILL_PROMPT)

        // Dismiss splash if active
        if (binding.layoutAppOpenSplash.visibility == View.VISIBLE) {
            binding.layoutAppOpenSplash.visibility = View.GONE
            binding.layoutMainContent.scaleX = 1.0f
            binding.layoutMainContent.scaleY = 1.0f
            binding.layoutMainContent.alpha = 1.0f
        }

        when {
            action == MaphulaWidgetProvider.ACTION_OPEN_VOICE || openVoice -> {
                binding.bottomNav.selectedItemId = R.id.nav_chat
                showFragment(chatFragment, "CHAT")
                chatFragment.triggerVoiceInput()
            }
            action == MaphulaWidgetProvider.ACTION_OPEN_PREFILL || !prefillPrompt.isNullOrBlank() -> {
                binding.bottomNav.selectedItemId = R.id.nav_chat
                showFragment(chatFragment, "CHAT")
                if (!prefillPrompt.isNullOrBlank()) {
                    chatFragment.setInputPrompt(prefillPrompt)
                }
            }
            action == MaphulaWidgetProvider.ACTION_OPEN_CHAT || targetTab == "chat" -> {
                binding.bottomNav.selectedItemId = R.id.nav_chat
                showFragment(chatFragment, "CHAT")
                chatFragment.focusInput()
            }
            action == MaphulaWidgetProvider.ACTION_OPEN_CREATIVE || targetTab == "creative" -> {
                binding.bottomNav.selectedItemId = R.id.nav_creative
                showFragment(creativeFragment, "CREATIVE")
            }
            targetTab == "code" -> {
                binding.bottomNav.selectedItemId = R.id.nav_code
                showFragment(codeFragment, "CODE")
            }
            targetTab == "knowledge" -> {
                binding.bottomNav.selectedItemId = R.id.nav_knowledge
                showFragment(knowledgeFragment, "KNOWLEDGE")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        clock8BitJob?.cancel()
        voiceEngine.release()
        networkMonitor.unregister()
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }
        batteryReceiver = null
    }
}
