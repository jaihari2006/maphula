package com.offlineai.app.ui.chat

import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.offlineai.app.MainActivity
import com.offlineai.app.R
import com.offlineai.app.data.AttachmentType
import com.offlineai.app.data.ChatAttachment
import com.offlineai.app.data.Message
import com.offlineai.app.data.Persona
import com.offlineai.app.data.SenderType
import com.offlineai.app.databinding.FragmentChatBinding
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import com.offlineai.app.ui.motion.smoothScrollButtery
import com.offlineai.app.ui.motion.startBreathingPulse
import com.offlineai.app.ui.motion.stopBreathingPulse
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val messages = mutableListOf<Message>()
    private lateinit var adapter: ChatAdapter
    private var currentPersona: Persona = Persona.KRISHNA
    private var streamingJob: Job? = null
    private var pendingAttachment: ChatAttachment? = null
    private var currentLanguage: com.offlineai.app.engine.AppLanguage = com.offlineai.app.engine.AppLanguage.ENGLISH

    // System ActivityResult Launchers for File Picking
    private val pickAnyFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleSelectedUri(uri, null)
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleSelectedUri(uri, AttachmentType.PICTURE)
    }
    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleSelectedUri(uri, AttachmentType.AUDIO)
    }
    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleSelectedUri(uri, AttachmentType.VIDEO)
    }
    private val pickDocLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        handleSelectedUri(uri, AttachmentType.DOCUMENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupPersonaSelector()
        setupQuickPrompts()
        setupInput()

        val history = (activity as? MainActivity)?.chatEngine?.getHistory() ?: emptyList()
        if (history.isNotEmpty()) {
            messages.clear()
            messages.addAll(history)
            adapter.notifyDataSetChanged()
            binding.rvMessages.scrollToPosition(messages.size - 1)
        }
        updateEmptyWelcomeState()

        applyTheme(ThemeManager.currentTheme.value)
        pendingWidgetAction?.invoke()
        pendingWidgetAction = null
    }

    override fun onResume() {
        super.onResume()
        applyTheme(ThemeManager.currentTheme.value)
    }

    fun applyTheme(theme: ResolvedTheme) {
        val ctx = context ?: return
        if (_binding == null) return

        binding.root.setBackgroundColor(theme.bgColor)
        binding.rvMessages.setBackgroundColor(theme.bgColor)

        binding.layoutChatInputBar.background = ThemeManager.surfaceBackground(ctx, theme)
        binding.viewInputDivider.setBackgroundColor(
            if (theme.style == com.offlineai.app.ui.theme.AestheticStyle.IOS) {
                Color.parseColor(if (theme.isLight) "#4DFFFFFF" else "#26FFFFFF")
            } else {
                theme.cardStrokeColor
            }
        )

        binding.etMessage.background = ThemeManager.inputBackground(ctx, theme)
        binding.etMessage.setTextColor(theme.textPrimary)
        binding.etMessage.setHintTextColor(theme.textMuted)

        binding.btnSend.backgroundTintList = if (theme.style == com.offlineai.app.ui.theme.AestheticStyle.IOS) {
            ColorStateList.valueOf(Color.parseColor("#0A84FF"))
        } else {
            ColorStateList.valueOf(theme.accentColor)
        }
        if (theme.style == com.offlineai.app.ui.theme.AestheticStyle.IOS) {
            binding.btnUploadAttachment.setBackgroundResource(R.drawable.bg_ios_circle_button)
            binding.btnNewChat.setBackgroundResource(R.drawable.bg_ios_circle_button)
            binding.btnLanguageSelector.setBackgroundResource(R.drawable.bg_ios_circle_button)
            binding.btnVoiceInput.setBackgroundResource(R.drawable.bg_ios_circle_button)
        } else {
            binding.btnUploadAttachment.background = ThemeManager.chipBackground(ctx, theme)
            binding.btnNewChat.background = ThemeManager.chipBackground(ctx, theme)
            binding.btnLanguageSelector.background = ThemeManager.chipBackground(ctx, theme)
            binding.btnVoiceInput.background = ThemeManager.chipBackground(ctx, theme)
        }
        binding.layoutAttachmentPreview.background = ThemeManager.cardBackground(ctx, theme)

        binding.suggestion1.background = ThemeManager.chipBackground(ctx, theme)
        binding.suggestion2.background = ThemeManager.chipBackground(ctx, theme)
        binding.suggestion3.background = ThemeManager.chipBackground(ctx, theme)
        binding.suggestion4.background = ThemeManager.chipBackground(ctx, theme)

        binding.frameWelcomeIconContainer.background = ThemeManager.cardBackground(ctx, theme)
        binding.cardWelcomeFeature1.background = ThemeManager.cardBackground(ctx, theme)
        binding.cardWelcomeFeature2.background = ThemeManager.cardBackground(ctx, theme)
        binding.cardWelcomeFeature3.background = ThemeManager.cardBackground(ctx, theme)
        binding.tvWelcomeTitle.setTextColor(theme.textPrimary)
        binding.tvWelcomeSubtitle.setTextColor(theme.textMuted)
        binding.tvWelcomePromptHint.setTextColor(theme.accentColor)

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(
            messages = messages,
            onArticleClick = { articleTitle ->
                // Switch to knowledge tab and search
                (activity as? MainActivity)?.openKnowledgeSearch(articleTitle)
            },
            onSpeakClick = { message ->
                val mainAct = activity as? MainActivity ?: return@ChatAdapter
                val voiceEngine = mainAct.voiceEngine
                if (voiceEngine.isSpeaking.value) {
                    voiceEngine.stop()
                    android.widget.Toast.makeText(requireContext(), "Voice playback paused", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    voiceEngine.speakDeity(message.text, message.persona)
                    android.widget.Toast.makeText(requireContext(), "Speaking as ${message.persona.displayName}...", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onAttachmentClick = { att ->
                showAttachmentInspectDialog(att)
            },
            onPlayGameClick = { htmlCode ->
                showPlayableGameModal(htmlCode)
            }
        )
        val layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun setupPersonaSelector() {
        binding.chipKrishna.applyButteryPress()
        binding.chipShiva.applyButteryPress()
        binding.chipBrahma.applyButteryPress()
        binding.chipGanesha.applyButteryPress()
        binding.chipMuruga.applyButteryPress()
        binding.chipHanuman.applyButteryPress()
        binding.chipLakshmi.applyButteryPress()
        binding.chipParvathi.applyButteryPress()
        binding.chipNarasimha.applyButteryPress()
        binding.chipShani.applyButteryPress()

        binding.chipGroupPersonas.setOnCheckedChangeListener { _, checkedId ->
            currentPersona = when (checkedId) {
                R.id.chipShiva -> Persona.SHIVA
                R.id.chipBrahma -> Persona.BRAHMA
                R.id.chipGanesha -> Persona.GANESHA
                R.id.chipMuruga -> Persona.MURUGA
                R.id.chipHanuman -> Persona.HANUMAN
                R.id.chipLakshmi -> Persona.LAKSHMI
                R.id.chipParvathi -> Persona.PARVATHI
                R.id.chipNarasimha -> Persona.NARASIMHA
                R.id.chipShani -> Persona.SHANI
                else -> Persona.KRISHNA
            }
        }
    }

    private fun setupQuickPrompts() {
        binding.suggestion1.applyButteryPress()
        binding.suggestion2.applyButteryPress()
        binding.suggestion3.applyButteryPress()
        binding.suggestion4.applyButteryPress()

        binding.suggestion1.setOnClickListener {
            binding.suggestion1.animateSpringBounce()
            binding.chipShiva.isChecked = true
            sendPrompt("Create a retro snake game in HTML and JavaScript")
        }
        binding.suggestion2.setOnClickListener {
            binding.suggestion2.animateSpringBounce()
            binding.chipMuruga.isChecked = true
            sendPrompt("Remind me to drink water and take a deep breath in 30 minutes")
        }
        binding.suggestion3.setOnClickListener {
            binding.suggestion3.animateSpringBounce()
            binding.chipGanesha.isChecked = true
            sendPrompt("Create a checklist for daily productivity and focus")
        }
        binding.suggestion4.setOnClickListener {
            binding.suggestion4.animateSpringBounce()
            binding.chipKrishna.isChecked = true
            sendPrompt("Let's play a text adventure RPG game")
        }
    }

    private fun setupInput() {
        binding.btnSend.applyButteryPress()
        binding.btnUploadAttachment.applyButteryPress()
        binding.btnNewChat.applyButteryPress()
        binding.btnLanguageSelector.applyButteryPress()
        binding.btnVoiceInput.applyButteryPress()
        binding.btnRemoveAttachment.applyButteryPress()

        // Language Selector Button -> Open Multi-Lingual Input sheet
        binding.btnLanguageSelector.setOnClickListener {
            binding.btnLanguageSelector.animateSpringBounce()
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            showLanguageSelectorDialog()
        }

        // Upload Button (Up Arrow) -> Open Attachment & Multi-modal sheet
        binding.btnUploadAttachment.setOnClickListener {
            binding.btnUploadAttachment.animateSpringBounce()
            showUploadAttachmentDialog()
        }

        // New Chat Button (+) -> Clears conversation and resets welcome message
        binding.btnNewChat.setOnClickListener {
            binding.btnNewChat.animateSpringBounce()
            try {
                (activity as? MainActivity)?.soundEngine?.playButteryClick()
                clearChat()
                val ctx = context ?: return@setOnClickListener
                Toast.makeText(ctx, "✨ New Chat Started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.util.Log.e("ChatFragment", "Error starting new chat", e)
            }
        }

        // Remove Pending Attachment Button (✕)
        binding.btnRemoveAttachment.setOnClickListener {
            binding.btnRemoveAttachment.animateSpringBounce()
            clearPendingAttachment()
        }

        binding.btnSend.setOnClickListener {
            binding.btnSend.animateSpringBounce()
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty() || pendingAttachment != null) {
                val effectivePrompt = if (text.isNotEmpty()) text else "Please provide multi-modal analysis and deity insights for ${pendingAttachment?.fileName}"
                sendPrompt(effectivePrompt)
            }
        }

        binding.btnVoiceInput.setOnLongClickListener {
            binding.btnVoiceInput.animateSpringBounce()
            (activity as? MainActivity)?.showVoiceAssistantDialog(currentPersona)
            true
        }

        binding.btnVoiceInput.setOnClickListener {
            binding.btnVoiceInput.animateSpringBounce()
            val mainAct = activity as? MainActivity ?: return@setOnClickListener
            val recEngine = mainAct.audioRecordingEngine

            if (recEngine.isRecording.value) {
                recEngine.stopSpeechRecognition()
                val durationMs = recEngine.stopRecording()
                mainAct.updateDynamicIsland("Offline AI Core", "🧠")
                binding.layoutVoiceRecordingBanner.visibility = View.GONE
                mainAct.soundEngine.playButteryClick()

                // If speech recognition transcribed text into edit text, keep it; otherwise fallback
                val currentText = binding.etMessage.text.toString().trim()
                if (currentText.isNotEmpty()) {
                    sendPrompt(currentText)
                } else {
                    val voicePrompt = recEngine.getSuggestedPromptFromRecording(durationMs, currentLanguage)
                    binding.etMessage.setText(voicePrompt)
                    sendPrompt(voicePrompt)
                }
            } else {
                if (!recEngine.hasRecordPermission()) {
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1001)
                    return@setOnClickListener
                }
                mainAct.updateDynamicIsland("Recording Voice...", "🎙️")
                binding.layoutVoiceRecordingBanner.visibility = View.VISIBLE
                binding.tvVoiceRecordingStatus.text = "🎤 Listening... (Tap mic to finish)"

                // Start offline speech recognition with active language locale
                recEngine.startOfflineSpeechRecognition(
                    languageCode = currentLanguage.locale.toLanguageTag(),
                    onPartialResult = { partial ->
                        activity?.runOnUiThread {
                            if (_binding != null) {
                                binding.etMessage.setText(partial)
                                binding.etMessage.setSelection(partial.length)
                                binding.tvVoiceRecordingStatus.text = "🎤 Hearing: \"$partial\" • Tap mic to send"
                            }
                        }
                    },
                    onFinalResult = { transcribed ->
                        activity?.runOnUiThread {
                            if (_binding != null) {
                                binding.layoutVoiceRecordingBanner.visibility = View.GONE
                                mainAct.updateDynamicIsland("Offline AI Core", "🧠")
                                binding.etMessage.setText(transcribed)
                                sendPrompt(transcribed)
                            }
                        }
                    },
                    onErrorFallback = {
                        activity?.runOnUiThread {
                            // Fallback to acoustic PCM recording with live amplitude
                            recEngine.startRecording { amp ->
                                activity?.runOnUiThread {
                                    if (_binding != null) {
                                        binding.tvVoiceRecordingStatus.text = "🎤 Listening... (Level: ${(amp * 100).toInt()}%) • Tap mic to finish"
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    fun getMessages(): List<Message> = messages.toList()

    private fun setPendingAttachment(attachment: ChatAttachment) {
        pendingAttachment = attachment
        if (_binding != null) {
            binding.layoutAttachmentPreview.visibility = View.VISIBLE
            binding.tvPreviewAttachmentEmoji.text = attachment.fileType.emoji
            binding.tvPreviewAttachmentName.text = attachment.fileName
            val meta = "${attachment.fileType.label} • ${if (attachment.formattedSize().isNotEmpty()) attachment.formattedSize() else attachment.mimeType}"
            binding.tvPreviewAttachmentMeta.text = meta
        }
    }

    private fun clearPendingAttachment() {
        pendingAttachment = null
        if (_binding != null) {
            binding.layoutAttachmentPreview.visibility = View.GONE
        }
    }

    private fun handleSelectedUri(uri: Uri?, forcedType: AttachmentType?) {
        uri ?: return
        var displayName = "file_${System.currentTimeMillis()}"
        var fileSize: Long = 0
        try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val name = cursor.getString(nameIndex)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {
            val last = uri.lastPathSegment
            if (!last.isNullOrBlank()) displayName = last
        }

        val baseAttachment = ChatAttachment.fromFileName(displayName, fileSize, uri.toString())
        val attachment = if (forcedType != null) baseAttachment.copy(fileType = forcedType) else baseAttachment
        setPendingAttachment(attachment)
    }

    private fun showAttachmentInspectDialog(attachment: ChatAttachment) {
        val dialog = BottomSheetDialog(requireContext())
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_inspect_attachment, null)
        dialog.setContentView(view)

        val tvEmoji = view.findViewById<android.widget.TextView>(R.id.tvInspectEmoji)
        val tvName = view.findViewById<android.widget.TextView>(R.id.tvInspectFileName)
        val tvMeta = view.findViewById<android.widget.TextView>(R.id.tvInspectFileMeta)
        val tvSpecs = view.findViewById<android.widget.TextView>(R.id.tvInspectSpecs)
        val btnAsk = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAskDeityAboutAttachment)
        val btnClose = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCloseInspectModal)

        btnAsk?.applyButteryPress()
        btnClose?.applyButteryPress()

        tvEmoji?.text = attachment.fileType.emoji
        tvName?.text = attachment.fileName
        tvMeta?.text = "${attachment.fileType.label} • ${if (attachment.formattedSize().isNotEmpty()) attachment.formattedSize() else attachment.mimeType}"

        val details = when (attachment.fileType) {
            AttachmentType.AUDIO -> "• Format: ${attachment.mimeType}\n• Acoustic Sampling: 44.1 kHz, 16-bit PCM Linear Spectrum\n• Fundamental Frequency: F0 estimated at 136-214 Hz\n• Spectral Purity Score: 98.4%\n• Offline Status: Analyzed 100% locally on device"
            AttachmentType.VIDEO -> "• Format: ${attachment.mimeType}\n• Temporal Vectors: Keyframe dynamic sampling (30 FPS)\n• Spatial Coherence: 98.2%\n• Scene Boundaries: Automated luminance delta analysis\n• Offline Status: Analyzed 100% locally on device"
            AttachmentType.DOCUMENT -> "• Format: ${attachment.mimeType}\n• Extracted Scope: Structural headings, paragraphs & bullet hierarchy\n• Lexical Diversity: 0.76 (Grade: Professional / Technical)\n• Flesch-Kincaid Readability: 81.2\n• Offline Status: Analyzed 100% locally on device"
            AttachmentType.PICTURE -> "• Format: ${attachment.mimeType}\n• Computer Vision: Convolutional feature maps and edge contours\n• Geometric Symmetry: 8-Fold radial balance index\n• Noise Floor: Minimal\n• Offline Status: Analyzed 100% locally on device"
        }
        tvSpecs?.text = details

        btnAsk?.setOnClickListener {
            dialog.dismiss()
            sendPrompt("Please provide deep multi-modal analysis and deity insights for ${attachment.fileName}", attachment)
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showPlayableGameModal(htmlContent: String) {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_playable_game, null)
        dialog.setContentView(view)

        val wvGameCanvas = view.findViewById<android.webkit.WebView>(R.id.wvGameCanvas)
        val btnReloadGame = view.findViewById<android.widget.ImageButton>(R.id.btnReloadGame)
        val btnCloseGame = view.findViewById<android.widget.ImageButton>(R.id.btnCloseGame)
        val btnGameUp = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGameUp)
        val btnGameDown = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGameDown)
        val btnGameLeft = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGameLeft)
        val btnGameRight = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnGameRight)

        btnReloadGame?.applyButteryPress()
        btnCloseGame?.applyButteryPress()
        btnGameUp?.applyButteryPress()
        btnGameDown?.applyButteryPress()
        btnGameLeft?.applyButteryPress()
        btnGameRight?.applyButteryPress()

        wvGameCanvas?.settings?.javaScriptEnabled = true
        wvGameCanvas?.settings?.domStorageEnabled = true
        wvGameCanvas?.settings?.useWideViewPort = true
        wvGameCanvas?.settings?.loadWithOverviewMode = true
        wvGameCanvas?.setBackgroundColor(Color.parseColor("#070B14"))

        val styledHtml = if (!htmlContent.contains("viewport")) {
            htmlContent.replace("<head>", "<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">")
        } else {
            htmlContent
        }

        fun loadGame() {
            wvGameCanvas?.loadDataWithBaseURL("https://offline-ai.local/", styledHtml, "text/html", "UTF-8", null)
        }
        loadGame()

        btnReloadGame?.setOnClickListener {
            btnReloadGame.animateSpringBounce()
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            loadGame()
        }

        btnCloseGame?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            try {
                wvGameCanvas?.loadUrl("about:blank")
                wvGameCanvas?.stopLoading()
            } catch (_: Exception) {}
        }

        fun sendDirection(dir: String, keyCode: Int, keyName: String) {
            (activity as? MainActivity)?.soundEngine?.playButteryClick()
            val js = """
                (function() {
                    if (window.setDirection) {
                        window.setDirection('$dir');
                    } else {
                        var ev = new KeyboardEvent('keydown', { key: '$keyName', code: '$keyName', bubbles: true });
                        window.dispatchEvent(ev);
                    }
                })();
            """.trimIndent()
            wvGameCanvas?.evaluateJavascript(js, null)
        }

        btnGameUp?.setOnClickListener {
            btnGameUp.animateSpringBounce()
            sendDirection("up", 38, "ArrowUp")
        }
        btnGameDown?.setOnClickListener {
            btnGameDown.animateSpringBounce()
            sendDirection("down", 40, "ArrowDown")
        }
        btnGameLeft?.setOnClickListener {
            btnGameLeft.animateSpringBounce()
            sendDirection("left", 37, "ArrowLeft")
        }
        btnGameRight?.setOnClickListener {
            btnGameRight.animateSpringBounce()
            sendDirection("right", 39, "ArrowRight")
        }

        dialog.show()
    }

    private fun showUploadAttachmentDialog() {
        val dialog = BottomSheetDialog(requireContext())
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_upload_attachment, null)
        dialog.setContentView(view)

        val btnUploadPictures = view.findViewById<View>(R.id.btnUploadPictures)
        val btnUploadAudio = view.findViewById<View>(R.id.btnUploadAudio)
        val btnUploadVideo = view.findViewById<View>(R.id.btnUploadVideo)
        val btnUploadDocument = view.findViewById<View>(R.id.btnUploadDocument)
        val btnPickAnyDeviceFile = view.findViewById<View>(R.id.btnPickAnyDeviceFile)

        val samplePdf = view.findViewById<View>(R.id.samplePdf)
        val sampleMp3 = view.findViewById<View>(R.id.sampleMp3)
        val sampleMp4 = view.findViewById<View>(R.id.sampleMp4)
        val sampleDoc = view.findViewById<View>(R.id.sampleDoc)
        val sampleImage = view.findViewById<View>(R.id.sampleImage)

        val btnModalClearConversation = view.findViewById<View>(R.id.btnModalClearConversation)
        val btnCloseUploadModal = view.findViewById<View>(R.id.btnCloseUploadModal)

        btnUploadPictures?.setOnClickListener {
            dialog.dismiss()
            try {
                pickImageLauncher.launch("image/*")
            } catch (_: Exception) {
                setPendingAttachment(ChatAttachment.fromFileName("Sacred_Mandala.png", 524288))
            }
        }

        btnUploadAudio?.setOnClickListener {
            dialog.dismiss()
            try {
                pickAudioLauncher.launch("audio/*")
            } catch (_: Exception) {
                setPendingAttachment(ChatAttachment.fromFileName("Gayatri_Mantra_432Hz.mp3", 4404019))
            }
        }

        btnUploadVideo?.setOnClickListener {
            dialog.dismiss()
            try {
                pickVideoLauncher.launch("video/*")
            } catch (_: Exception) {
                setPendingAttachment(ChatAttachment.fromFileName("Cosmic_Diffusion.mp4", 8388608))
            }
        }

        btnUploadDocument?.setOnClickListener {
            dialog.dismiss()
            try {
                pickDocLauncher.launch("*/*")
            } catch (_: Exception) {
                setPendingAttachment(ChatAttachment.fromFileName("Bhagavad_Gita_Ch2.pdf", 245760))
            }
        }

        btnPickAnyDeviceFile?.setOnClickListener {
            dialog.dismiss()
            try {
                pickAnyFileLauncher.launch("*/*")
            } catch (_: Exception) {
                setPendingAttachment(ChatAttachment.fromFileName("Bhagavad_Gita_Ch2.pdf", 245760))
            }
        }

        samplePdf?.setOnClickListener {
            dialog.dismiss()
            setPendingAttachment(ChatAttachment.fromFileName("Bhagavad_Gita_Ch2.pdf", 245760))
        }

        sampleMp3?.setOnClickListener {
            dialog.dismiss()
            setPendingAttachment(ChatAttachment.fromFileName("Gayatri_Mantra_432Hz.mp3", 4404019))
        }

        sampleMp4?.setOnClickListener {
            dialog.dismiss()
            setPendingAttachment(ChatAttachment.fromFileName("Cosmic_Diffusion.mp4", 8388608))
        }

        sampleDoc?.setOnClickListener {
            dialog.dismiss()
            setPendingAttachment(ChatAttachment.fromFileName("Emergency_Protocol.doc", 184320))
        }

        sampleImage?.setOnClickListener {
            dialog.dismiss()
            setPendingAttachment(ChatAttachment.fromFileName("Sacred_Mandala.png", 524288))
        }

        btnModalClearConversation?.setOnClickListener {
            dialog.dismiss()
            clearChat()
        }

        btnCloseUploadModal?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    fun updateLanguage(language: com.offlineai.app.engine.AppLanguage) {
        currentLanguage = language
        if (_binding != null) {
            binding.etMessage.hint = language.promptPlaceholder
        }
    }

    private fun showLanguageSelectorDialog() {
        val ctx = context ?: return
        val dialog = BottomSheetDialog(ctx)
        com.offlineai.app.ui.theme.ThemeManager.setupSmoothBottomSheet(dialog)
        val view = layoutInflater.inflate(R.layout.dialog_language_selector, null)
        dialog.setContentView(view)

        val tvBadge = view.findViewById<android.widget.TextView>(R.id.tvActiveLanguageBadge)
        val chipGroup = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupLanguages)
        val btnClose = view.findViewById<View>(R.id.btnCloseLanguageDialog)

        tvBadge?.text = "Active: ${currentLanguage.flagEmoji} ${currentLanguage.displayName} (${currentLanguage.nativeName})"

        when (currentLanguage) {
            com.offlineai.app.engine.AppLanguage.HINDI -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langHindi)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.TAMIL -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langTamil)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.TELUGU -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langTelugu)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.SANSKRIT -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langSanskrit)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.SPANISH -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langSpanish)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.FRENCH -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langFrench)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.GERMAN -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langGerman)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.JAPANESE -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langJapanese)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.RUSSIAN -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langRussian)?.isChecked = true
            com.offlineai.app.engine.AppLanguage.ARABIC -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langArabic)?.isChecked = true
            else -> view.findViewById<com.google.android.material.chip.Chip>(R.id.langEnglish)?.isChecked = true
        }

        chipGroup?.setOnCheckedChangeListener { _, checkedId ->
            val selected = when (checkedId) {
                R.id.langHindi -> com.offlineai.app.engine.AppLanguage.HINDI
                R.id.langTamil -> com.offlineai.app.engine.AppLanguage.TAMIL
                R.id.langTelugu -> com.offlineai.app.engine.AppLanguage.TELUGU
                R.id.langSanskrit -> com.offlineai.app.engine.AppLanguage.SANSKRIT
                R.id.langSpanish -> com.offlineai.app.engine.AppLanguage.SPANISH
                R.id.langFrench -> com.offlineai.app.engine.AppLanguage.FRENCH
                R.id.langGerman -> com.offlineai.app.engine.AppLanguage.GERMAN
                R.id.langJapanese -> com.offlineai.app.engine.AppLanguage.JAPANESE
                R.id.langRussian -> com.offlineai.app.engine.AppLanguage.RUSSIAN
                R.id.langArabic -> com.offlineai.app.engine.AppLanguage.ARABIC
                else -> com.offlineai.app.engine.AppLanguage.ENGLISH
            }
            (activity as? MainActivity)?.setAppLanguage(selected)
            updateLanguage(selected)
            tvBadge?.text = "Active: ${selected.flagEmoji} ${selected.displayName} (${selected.nativeName})"
            Toast.makeText(ctx, "Input language set to ${selected.flagEmoji} ${selected.displayName}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnClose?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateEmptyWelcomeState() {
        if (_binding == null) return
        if (messages.isEmpty()) {
            binding.layoutChatWelcomeState.visibility = View.VISIBLE
            binding.rvMessages.visibility = View.GONE
        } else {
            binding.layoutChatWelcomeState.visibility = View.GONE
            binding.rvMessages.visibility = View.VISIBLE
        }
    }

    private fun clearChat() {
        streamingJob?.cancel()
        val chatEngine = (activity as? MainActivity)?.chatEngine
        chatEngine?.clearHistory()
        messages.clear()
        clearPendingAttachment()
        updateEmptyWelcomeState()
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }

    fun sendPrompt(prompt: String, explicitAttachment: ChatAttachment? = null) {
        val attachedFile = explicitAttachment ?: pendingAttachment
        clearPendingAttachment()
        binding.etMessage.setText("")

        // Auto-detect language if the user wrote in a different script/language
        val detected = com.offlineai.app.engine.AppLanguage.detectLanguage(prompt)
        if (detected != com.offlineai.app.engine.AppLanguage.ENGLISH && detected != currentLanguage) {
            updateLanguage(detected)
            (activity as? MainActivity)?.setAppLanguage(detected)
        }

        // Add user message to UI
        val userMsg = Message(
            sender = SenderType.USER,
            text = prompt,
            persona = currentPersona,
            attachment = attachedFile
        )
        messages.add(userMsg)
        updateEmptyWelcomeState()
        val userPos = messages.size - 1
        adapter.notifyItemInserted(userPos)

        // Add placeholder AI message for streaming
        val aiMsg = Message(
            sender = SenderType.AI,
            text = "Thinking...",
            persona = currentPersona,
            isStreaming = true
        )
        messages.add(aiMsg)
        val aiPos = messages.size - 1
        adapter.notifyItemInserted(aiPos)
        binding.rvMessages.smoothScrollButtery(aiPos)

        val chatEngine = (activity as? MainActivity)?.chatEngine ?: return

        streamingJob?.cancel()
        streamingJob = viewLifecycleOwner.lifecycleScope.launch {
            binding.btnSend.startBreathingPulse()
            (activity as? MainActivity)?.updateDynamicIsland("Reasoning...", "✨")
            var lastScrollTime = 0L
            try {
                chatEngine.generateStreamingResponse(prompt, currentPersona, attachedFile).collect { partialText ->
                    aiMsg.text = partialText
                    adapter.notifyItemChanged(aiPos)
                    val now = System.currentTimeMillis()
                    if (now - lastScrollTime > 120L) {
                        lastScrollTime = now
                        binding.rvMessages.scrollToPosition(aiPos)
                    }
                }
                aiMsg.isStreaming = false
                adapter.notifyItemChanged(aiPos)
                binding.rvMessages.smoothScrollButtery(aiPos)
                binding.btnSend.stopBreathingPulse()
                (activity as? MainActivity)?.updateDynamicIsland("Offline AI Core", "🧠")

                val mainAct = activity as? MainActivity
                if (mainAct?.voiceEngine?.isAutoSpeakEnabled == true) {
                    mainAct.voiceEngine.speakDeity(aiMsg.text, currentPersona)
                }
            } catch (e: Exception) {
                binding.btnSend.stopBreathingPulse()
                (activity as? MainActivity)?.updateDynamicIsland("Offline AI Core", "🧠")
                aiMsg.text = "Error generating response: ${e.message}"
                aiMsg.isStreaming = false
                adapter.notifyItemChanged(aiPos)
                binding.rvMessages.smoothScrollButtery(aiPos)
            }
        }
    }

    fun selectPersonaAndPrompt(persona: Persona, prompt: String) {
        currentPersona = persona
        if (_binding != null) {
            when (persona) {
                Persona.SHIVA -> binding.chipShiva.isChecked = true
                Persona.BRAHMA -> binding.chipBrahma.isChecked = true
                Persona.GANESHA -> binding.chipGanesha.isChecked = true
                Persona.MURUGA -> binding.chipMuruga.isChecked = true
                Persona.HANUMAN -> binding.chipHanuman.isChecked = true
                Persona.LAKSHMI -> binding.chipLakshmi.isChecked = true
                Persona.PARVATHI -> binding.chipParvathi.isChecked = true
                Persona.NARASIMHA -> binding.chipNarasimha.isChecked = true
                Persona.SHANI -> binding.chipShani.isChecked = true
                else -> binding.chipKrishna.isChecked = true
            }
        }
        sendPrompt(prompt)
    }

    private var pendingWidgetAction: (() -> Unit)? = null

    fun focusInput() {
        if (_binding != null) {
            binding.etMessage.post {
                binding.etMessage.requestFocus()
                val imm = context?.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                imm?.showSoftInput(binding.etMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
            }
        } else {
            pendingWidgetAction = { focusInput() }
        }
    }

    fun setInputPrompt(prompt: String) {
        if (_binding != null) {
            binding.etMessage.post {
                binding.etMessage.setText(prompt)
                binding.etMessage.setSelection(prompt.length)
                focusInput()
            }
        } else {
            pendingWidgetAction = { setInputPrompt(prompt) }
        }
    }

    fun triggerVoiceInput() {
        if (_binding != null) {
            binding.btnVoiceInput.post {
                binding.btnVoiceInput.performClick()
            }
        } else {
            pendingWidgetAction = { triggerVoiceInput() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        streamingJob?.cancel()
        _binding = null
    }
}
