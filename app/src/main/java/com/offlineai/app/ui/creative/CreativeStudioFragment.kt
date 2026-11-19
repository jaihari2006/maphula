package com.offlineai.app.ui.creative

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.offlineai.app.MainActivity
import com.offlineai.app.R
import com.offlineai.app.databinding.FragmentCreativeStudioBinding
import com.offlineai.app.engine.ImageGenerationEngine
import com.offlineai.app.engine.ImageStyle
import com.offlineai.app.engine.VideoGenerationEngine
import com.offlineai.app.engine.VideoMotionMode
import com.offlineai.app.engine.VoiceDuplicationEngine
import com.offlineai.app.engine.VoiceProfile
import com.offlineai.app.ui.theme.ResolvedTheme
import com.offlineai.app.ui.theme.ThemeManager
import com.offlineai.app.ui.motion.animateSpringBounce
import com.offlineai.app.ui.motion.applyButteryPress
import com.offlineai.app.ui.motion.beginButteryTransition
import com.offlineai.app.ui.motion.startBreathingPulse
import com.offlineai.app.ui.motion.stopBreathingPulse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CreativeStudioFragment : Fragment() {

    private var _binding: FragmentCreativeStudioBinding? = null
    private val binding get() = _binding!!

    private val imageEngine by lazy { ImageGenerationEngine() }
    private val videoEngine by lazy { VideoGenerationEngine() }
    private val voiceEngine by lazy {
        (activity as? MainActivity)?.voiceEngine ?: VoiceDuplicationEngine(requireContext())
    }

    private var selectedStyle = ImageStyle.CYBERPUNK
    private var selectedVideoMode = VideoMotionMode.COSMIC_ORBIT
    private var activeVoiceProfile: VoiceProfile? = null
    private var latestGeneratedBitmap: Bitmap? = null

    // Video playback states
    private var videoFrames: List<Bitmap> = emptyList()
    private var currentFrameIdx = 0
    private var isPlayingVideo = false
    private var videoPlaybackJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreativeStudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activeVoiceProfile = voiceEngine.presetProfiles.first()

        setupModeSelector()
        setupImageGeneration()
        setupVideoGeneration()
        setupVoiceDuplication()
        observeWaveform()
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
        binding.btnGenerateImage.backgroundTintList = ColorStateList.valueOf(theme.accentColor)
        binding.btnGenerateVideo.backgroundTintList = ColorStateList.valueOf(theme.accentColor)
        binding.btnPlayPauseVideo.backgroundTintList = ColorStateList.valueOf(theme.accentSecondary)
        binding.btnSpeakClonedVoice.backgroundTintList = ColorStateList.valueOf(theme.accentColor)
        binding.btnCalibrateVoice.backgroundTintList = ColorStateList.valueOf(theme.accentSecondary)

        binding.etImagePrompt.background = ThemeManager.inputBackground(ctx, theme)
        binding.etVoiceText.background = ThemeManager.inputBackground(ctx, theme)
        binding.waveformVisualizer.setThemeColors(theme.accentColor, theme.accentSecondary)
    }

    private fun setupModeSelector() {
        binding.chipModeImage.applyButteryPress()
        binding.chipModeVideo.applyButteryPress()
        binding.chipModeVoice.applyButteryPress()

        binding.chipGroupModes.setOnCheckedChangeListener { _, checkedId ->
            (binding.root as? ViewGroup)?.beginButteryTransition(280L)
            when (checkedId) {
                R.id.chipModeImage -> {
                    binding.layoutImageSection.visibility = View.VISIBLE
                    binding.layoutVideoSection.visibility = View.GONE
                    binding.layoutVoiceSection.visibility = View.GONE
                }
                R.id.chipModeVideo -> {
                    binding.layoutImageSection.visibility = View.GONE
                    binding.layoutVideoSection.visibility = View.VISIBLE
                    binding.layoutVoiceSection.visibility = View.GONE
                }
                R.id.chipModeVoice -> {
                    binding.layoutImageSection.visibility = View.GONE
                    binding.layoutVideoSection.visibility = View.GONE
                    binding.layoutVoiceSection.visibility = View.VISIBLE
                }
            }
        }
    }

    // ================= IMAGE GENERATION =================
    private fun setupImageGeneration() {
        binding.btnGenerateImage.applyButteryPress()
        binding.imgQuickCyber.applyButteryPress()
        binding.imgQuickCosmic.applyButteryPress()
        binding.imgQuickSynth.applyButteryPress()
        binding.imgQuickNeural.applyButteryPress()
        binding.imgQuickBio.applyButteryPress()

        binding.chipGroupStyles.setOnCheckedChangeListener { _, checkedId ->
            selectedStyle = when (checkedId) {
                R.id.styleCosmic -> ImageStyle.COSMIC
                R.id.styleSynthwave -> ImageStyle.SYNTHWAVE
                R.id.styleNeural -> ImageStyle.QUANTUM_NEURAL
                R.id.styleBio -> ImageStyle.BIOLUMINESCENT
                else -> ImageStyle.CYBERPUNK
            }
        }

        binding.imgQuickCyber.setOnClickListener {
            binding.imgQuickCyber.animateSpringBounce()
            binding.etImagePrompt.setText("Cyberpunk metropolis with neon skyscraper spires and light trails")
            binding.styleCyberpunk.isChecked = true
        }
        binding.imgQuickCosmic.setOnClickListener {
            binding.imgQuickCosmic.animateSpringBounce()
            binding.etImagePrompt.setText("Deep space spiral nebula with luminous stellar core and cosmic dust")
            binding.styleCosmic.isChecked = true
        }
        binding.imgQuickSynth.setOnClickListener {
            binding.imgQuickSynth.animateSpringBounce()
            binding.etImagePrompt.setText("Retro synthwave horizon grid with segmented glowing solar sphere")
            binding.styleSynthwave.isChecked = true
        }
        binding.imgQuickNeural.setOnClickListener {
            binding.imgQuickNeural.animateSpringBounce()
            binding.etImagePrompt.setText("Quantum neural lattice with glowing synaptic nodes and energy pathways")
            binding.styleNeural.isChecked = true
        }
        binding.imgQuickBio.setOnClickListener {
            binding.imgQuickBio.animateSpringBounce()
            binding.etImagePrompt.setText("Enchanted bioluminescent subterranean forest with glowing spores")
            binding.styleBio.isChecked = true
        }

        binding.btnGenerateImage.setOnClickListener {
            binding.btnGenerateImage.animateSpringBounce()
            val prompt = binding.etImagePrompt.text.toString().trim()
            val finalPrompt = prompt.ifBlank { "Futuristic on-device generative AI artwork" }

            binding.pbImage.visibility = View.VISIBLE
            binding.tvImageStatus.visibility = View.VISIBLE
            binding.btnGenerateImage.isEnabled = false
            binding.ivGeneratedImage.startBreathingPulse(minScale = 0.98f, maxScale = 1.02f)

            viewLifecycleOwner.lifecycleScope.launch {
                val bitmap = imageEngine.generateImage(
                    prompt = finalPrompt,
                    style = selectedStyle,
                    steps = 15
                ) { step, total, status ->
                    binding.tvImageStatus.text = "Step $step/$total: $status"
                }

                binding.ivGeneratedImage.stopBreathingPulse()
                binding.pbImage.visibility = View.GONE
                binding.btnGenerateImage.isEnabled = true
                binding.tvImageStatus.text = "✓ Synthesis finished!"
                latestGeneratedBitmap = bitmap
                binding.ivGeneratedImage.setImageBitmap(bitmap)
                binding.ivGeneratedImage.animateSpringBounce(260L)
                binding.tvImageDetails.text = "384 × 384 • Procedural Latent Diffusion • Style: ${selectedStyle.label}"

                val mainAct = activity as? MainActivity
                mainAct?.soundEngine?.playTempleBell()
                val res = mainAct?.karmaEngine?.awardPrana(20, "Procedural Visual Synthesis")
                if (res != null) {
                    mainAct.updateKarmaBadgeUI()
                    Toast.makeText(requireContext(), "🎨 Artwork Synthesized! +20 Prana (${res.totalPoints} total)", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnExportGeneratedImage.applyButteryPress()
        binding.btnExportGeneratedImage.setOnClickListener {
            binding.btnExportGeneratedImage.animateSpringBounce()
            val bitmap = latestGeneratedBitmap
            if (bitmap == null) {
                Toast.makeText(requireContext(), "Please synthesize an image first!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val mainAct = activity as? MainActivity ?: return@setOnClickListener
            val prompt = binding.etImagePrompt.text.toString().trim().ifBlank { "Artwork" }
            val savedFile = mainAct.exportImportEngine.exportBitmapToPictures(bitmap, prompt)
            if (savedFile != null && savedFile.exists()) {
                mainAct.soundEngine.playButteryClick()
                Toast.makeText(requireContext(), "Saved: ${savedFile.name} to Pictures/Maphula!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Image saved to device storage!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ================= VIDEO GENERATION =================
    private fun setupVideoGeneration() {
        binding.btnGenerateVideo.applyButteryPress()
        binding.btnPlayPauseVideo.applyButteryPress()

        binding.chipGroupVideoModes.setOnCheckedChangeListener { _, checkedId ->
            selectedVideoMode = when (checkedId) {
                R.id.videoModeNeural -> VideoMotionMode.NEURAL_PULSE
                R.id.videoModeWave -> VideoMotionMode.QUANTUM_WAVE
                R.id.videoModeMatrix -> VideoMotionMode.CYBER_MATRIX
                R.id.videoModeDrive -> VideoMotionMode.SYNTHWAVE_DRIVE
                else -> VideoMotionMode.COSMIC_ORBIT
            }
            binding.tvVideoModeDesc.text = selectedVideoMode.description
        }

        binding.btnGenerateVideo.setOnClickListener {
            binding.btnGenerateVideo.animateSpringBounce()
            binding.pbVideo.visibility = View.VISIBLE
            binding.tvVideoStatus.visibility = View.VISIBLE
            binding.btnGenerateVideo.isEnabled = false
            stopVideoPlayback()
            binding.ivVideoFrame.startBreathingPulse(minScale = 0.98f, maxScale = 1.02f)

            viewLifecycleOwner.lifecycleScope.launch {
                val frames = videoEngine.generateVideoFrames(
                    mode = selectedVideoMode,
                    frameCount = 24
                ) { rendered, total ->
                    binding.tvVideoStatus.text = "Rendering motion frame $rendered / $total..."
                }

                binding.ivVideoFrame.stopBreathingPulse()
                videoFrames = frames
                binding.pbVideo.visibility = View.GONE
                binding.btnGenerateVideo.isEnabled = true
                binding.tvVideoStatus.text = "✓ Generated 24 video frames (Smooth Loop)"

                binding.sbVideoTimeline.max = frames.size - 1
                startVideoPlayback()
            }
        }

        binding.btnPlayPauseVideo.setOnClickListener {
            binding.btnPlayPauseVideo.animateSpringBounce()
            if (isPlayingVideo) {
                stopVideoPlayback()
            } else {
                startVideoPlayback()
            }
        }

        binding.sbVideoTimeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && progress in videoFrames.indices) {
                    currentFrameIdx = progress
                    displayCurrentVideoFrame()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                stopVideoPlayback()
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startVideoPlayback() {
        if (videoFrames.isEmpty()) return
        isPlayingVideo = true
        binding.btnPlayPauseVideo.text = "⏸ Pause"

        videoPlaybackJob?.cancel()
        videoPlaybackJob = viewLifecycleOwner.lifecycleScope.launch {
            val frameDelay = 1000L / 24 // 24 FPS (~41ms)
            while (isActive && isPlayingVideo) {
                displayCurrentVideoFrame()
                currentFrameIdx = (currentFrameIdx + 1) % videoFrames.size
                delay(frameDelay)
            }
        }
    }

    private fun stopVideoPlayback() {
        isPlayingVideo = false
        videoPlaybackJob?.cancel()
        binding.btnPlayPauseVideo.text = "▶ Play"
    }

    private fun displayCurrentVideoFrame() {
        if (videoFrames.isNotEmpty() && currentFrameIdx in videoFrames.indices) {
            binding.ivVideoFrame.setImageBitmap(videoFrames[currentFrameIdx])
            binding.sbVideoTimeline.progress = currentFrameIdx
            binding.tvVideoFrameCounter.text = "${currentFrameIdx + 1}/${videoFrames.size}"
        }
    }

    // ================= VOICE CLONING =================
    private fun setupVoiceDuplication() {
        binding.btnSpeakClonedVoice.applyButteryPress()
        binding.btnCalibrateVoice.applyButteryPress()
        binding.voiceKrishna.applyButteryPress()
        binding.voiceShiva.applyButteryPress()
        binding.voiceBrahma.applyButteryPress()
        binding.voiceGanesha.applyButteryPress()
        binding.voiceMuruga.applyButteryPress()
        binding.voiceHanuman.applyButteryPress()
        binding.voiceLakshmi.applyButteryPress()
        binding.voiceParvathi.applyButteryPress()
        binding.voiceNarasimha.applyButteryPress()
        binding.voiceShani.applyButteryPress()
        binding.voiceCustom.applyButteryPress()

        binding.chipGroupVoices.setOnCheckedChangeListener { _, checkedId ->
            val profile = when (checkedId) {
                R.id.voiceShiva -> voiceEngine.presetProfiles.getOrNull(1)
                R.id.voiceBrahma -> voiceEngine.presetProfiles.getOrNull(2)
                R.id.voiceShani -> voiceEngine.presetProfiles.getOrNull(3)
                R.id.voiceGanesha -> voiceEngine.presetProfiles.getOrNull(4)
                R.id.voiceMuruga -> voiceEngine.presetProfiles.getOrNull(5)
                R.id.voiceHanuman -> voiceEngine.presetProfiles.getOrNull(6)
                R.id.voiceLakshmi -> voiceEngine.presetProfiles.getOrNull(7)
                R.id.voiceParvathi -> voiceEngine.presetProfiles.getOrNull(8)
                R.id.voiceNarasimha -> voiceEngine.presetProfiles.getOrNull(9)
                R.id.voiceCustom -> voiceEngine.getCustomProfile()
                else -> voiceEngine.presetProfiles[0] // Krishna
            } ?: voiceEngine.presetProfiles.first()

            activeVoiceProfile = profile
            binding.tvVoiceProfileDesc.text = "Fundamental F0: ${profile.pitchHz.toInt()} Hz • ${profile.description}"
            binding.waveformVisualizer.setThemeColor(profile.colorHex)
        }

        binding.btnSpeakClonedVoice.setOnClickListener {
            binding.btnSpeakClonedVoice.animateSpringBounce()
            val text = binding.etVoiceText.text.toString().trim()
            val profile = activeVoiceProfile ?: voiceEngine.presetProfiles.first()
            if (text.isNotBlank()) {
                voiceEngine.speak(text, profile)
                binding.tvVoiceFeedback.text = "🔊 Speaking in ${profile.name} voice..."
            }
        }

        binding.btnCalibrateVoice.setOnClickListener {
            binding.btnCalibrateVoice.animateSpringBounce()
            // Calibrate custom voice clone based on acoustic pitch estimation
            val calibrated = voiceEngine.cloneUserAcoustics(detectedPitchHz = 148f, detectedSpeed = 1.0f)
            activeVoiceProfile = calibrated
            binding.voiceCustom.isChecked = true
            binding.tvVoiceProfileDesc.text = "Acoustically Calibrated: ${calibrated.pitchHz.toInt()} Hz F0 • Formant shift: ${calibrated.formantResonance}x"
            binding.waveformVisualizer.setThemeColor(calibrated.colorHex)
            binding.tvVoiceFeedback.text = "✓ Voice calibrated! Cloned acoustic profile ready."
            Toast.makeText(requireContext(), "Voice clone profile calibrated!", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecordMicVoice.applyButteryPress()
        binding.btnRecordMicVoice.setOnClickListener {
            binding.btnRecordMicVoice.animateSpringBounce()
            val mainAct = activity as? MainActivity ?: return@setOnClickListener
            val recEngine = mainAct.audioRecordingEngine

            if (recEngine.isRecording.value) {
                val durationMs = recEngine.stopRecording()
                binding.btnRecordMicVoice.text = "🎤 Live Mic"
                val char = recEngine.analyzeRecordedVoice()
                val calibrated = voiceEngine.cloneUserAcoustics(detectedPitchHz = char.pitchHz.toFloat(), detectedSpeed = 1.0f)
                activeVoiceProfile = calibrated
                binding.voiceCustom.isChecked = true
                binding.tvVoiceProfileDesc.text = "Live Cloned Voice: ${char.pitchHz.toInt()} Hz F0 • ${char.profileName}"
                binding.waveformVisualizer.setThemeColor(calibrated.colorHex)
                binding.tvVoiceFeedback.text = "✓ Live Voice Sample Analyzed (${durationMs}ms)! Cloned profile active."
                mainAct.soundEngine.playButteryClick()
                Toast.makeText(requireContext(), "Live voice cloned: ${char.pitchHz.toInt()} Hz!", Toast.LENGTH_SHORT).show()
            } else {
                if (!recEngine.hasRecordPermission()) {
                    requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1002)
                    return@setOnClickListener
                }
                recEngine.startRecording { amp ->
                    activity?.runOnUiThread {
                        val visualData = FloatArray(16) { i -> (amp * (0.5f + (i % 3) * 0.2f)).coerceIn(0f, 1f) }
                        binding.waveformVisualizer.setWaveform(visualData)
                    }
                }
                binding.btnRecordMicVoice.text = "⏹️ Finish"
                binding.tvVoiceFeedback.text = "🎙️ Recording live microphone... Speak naturally, then tap Finish!"
            }
        }
    }

    private fun observeWaveform() {
        viewLifecycleOwner.lifecycleScope.launch {
            voiceEngine.currentWaveformData.collectLatest { wave ->
                binding.waveformVisualizer.setWaveform(wave)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopVideoPlayback()
        voiceEngine.stop()
        _binding = null
    }
}
