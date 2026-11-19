package com.offlineai.app.engine

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.offlineai.app.data.Persona
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.Random
import kotlin.math.PI
import kotlin.math.sin

data class VoiceProfile(
    val id: String,
    val name: String,
    val pitchHz: Float, // Fundamental frequency F0
    val pitchFactor: Float, // TTS pitch multiplier (0.5 to 2.0)
    val speedFactor: Float, // TTS speed rate multiplier (0.5 to 2.0)
    val formantResonance: Float, // Vocal tract length scale (0.8 to 1.3)
    val warmthHarmonics: Float, // Acoustic harmonic warmth
    val description: String,
    val colorHex: String
)

class VoiceDuplicationEngine(context: Context) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    var currentLanguage: AppLanguage = AppLanguage.ENGLISH
        private set

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        try {
            val result = tts?.setLanguage(language.locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default/US if specific locale is not present on device
                tts?.language = Locale.US
            }
        } catch (_: Exception) {}
    }

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentWaveformData = MutableStateFlow(FloatArray(32))
    val currentWaveformData: StateFlow<FloatArray> = _currentWaveformData.asStateFlow()

    private var visualizerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    val presetProfiles = listOf(
        VoiceProfile(
            id = "voice_krishna",
            name = "Krishna (Cosmic Wisdom)",
            pitchHz = 135f,
            pitchFactor = 1.05f,
            speedFactor = 1.0f,
            formantResonance = 1.0f,
            warmthHarmonics = 0.95f,
            description = "Enlightened, warm, peaceful resonance with gentle melodic harmonics.",
            colorHex = "#7C3AED"
        ),
        VoiceProfile(
            id = "voice_shiva",
            name = "Shiva (Cosmic Resonance)",
            pitchHz = 95f,
            pitchFactor = 0.78f,
            speedFactor = 1.02f,
            formantResonance = 0.88f,
            warmthHarmonics = 0.98f,
            description = "Deep, profound, low-frequency meditative and authoritative timbre.",
            colorHex = "#06B6D4"
        ),
        VoiceProfile(
            id = "voice_brahma",
            name = "Brahma (Vedic Creation)",
            pitchHz = 120f,
            pitchFactor = 0.95f,
            speedFactor = 0.98f,
            formantResonance = 1.15f,
            warmthHarmonics = 0.85f,
            description = "Scholarly, structured, foundational first-principles cadence.",
            colorHex = "#F59E0B"
        ),
        VoiceProfile(
            id = "voice_shani",
            name = "Shani (Measured Discipline)",
            pitchHz = 100f,
            pitchFactor = 0.82f,
            speedFactor = 0.92f,
            formantResonance = 0.9f,
            warmthHarmonics = 0.7f,
            description = "Grounded, sober, disciplined, and measured tone.",
            colorHex = "#64748B"
        ),
        VoiceProfile(
            id = "voice_ganesha",
            name = "Ganesha (Warm Intellect)",
            pitchHz = 140f,
            pitchFactor = 1.1f,
            speedFactor = 1.0f,
            formantResonance = 1.08f,
            warmthHarmonics = 0.92f,
            description = "Amiable, articulate, welcoming intellect profile.",
            colorHex = "#EA580C"
        ),
        VoiceProfile(
            id = "voice_muruga",
            name = "Muruga (Heroic Cadence)",
            pitchHz = 155f,
            pitchFactor = 1.16f,
            speedFactor = 1.06f,
            formantResonance = 1.1f,
            warmthHarmonics = 0.88f,
            description = "Dynamic, vibrant, sharp tactical cadence.",
            colorHex = "#E11D48"
        ),
        VoiceProfile(
            id = "voice_hanuman",
            name = "Hanuman (Resolute Strength)",
            pitchHz = 105f,
            pitchFactor = 0.85f,
            speedFactor = 1.1f,
            formantResonance = 0.92f,
            warmthHarmonics = 0.8f,
            description = "Powerful, protective, resolute emergency communicator.",
            colorHex = "#DC2626"
        ),
        VoiceProfile(
            id = "voice_lakshmi",
            name = "Lakshmi (Lotus Elegance)",
            pitchHz = 215f,
            pitchFactor = 1.38f,
            speedFactor = 0.95f,
            formantResonance = 1.25f,
            warmthHarmonics = 0.95f,
            description = "Melodic, gentle, poetic cadence with luminous acoustic grace.",
            colorHex = "#DB2777"
        ),
        VoiceProfile(
            id = "voice_parvathi",
            name = "Parvathi (Luminous Shakti)",
            pitchHz = 200f,
            pitchFactor = 1.3f,
            speedFactor = 0.98f,
            formantResonance = 1.2f,
            warmthHarmonics = 0.9f,
            description = "Clear, serene, nurturing voice with balanced harmonics.",
            colorHex = "#059669"
        ),
        VoiceProfile(
            id = "voice_narasimha",
            name = "Narasimha (Fierce Authority)",
            pitchHz = 90f,
            pitchFactor = 0.72f,
            speedFactor = 1.12f,
            formantResonance = 0.82f,
            warmthHarmonics = 0.75f,
            description = "Commanding, thunderous, unyielding security timbre.",
            colorHex = "#B45309"
        )
    )

    private var customClonedProfile = VoiceProfile(
        id = "voice_custom",
        name = "Custom Cloned Voice",
        pitchHz = 145f,
        pitchFactor = 1.1f,
        speedFactor = 1.0f,
        formantResonance = 1.05f,
        warmthHarmonics = 0.8f,
        description = "User-calibrated acoustic profile with customized pitch and formant shift.",
        colorHex = "#EC4899"
    )

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                tts?.language = currentLanguage.locale
            } catch (_: Exception) {
                tts?.language = Locale.US
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                    startWaveformSimulation()
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopWaveformSimulation()
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    stopWaveformSimulation()
                }
            })
            isTtsReady = true
        }
    }

    fun getCustomProfile(): VoiceProfile = customClonedProfile

    fun updateCustomProfile(
        pitchFactor: Float,
        speedFactor: Float,
        formantResonance: Float
    ) {
        customClonedProfile = customClonedProfile.copy(
            pitchFactor = pitchFactor,
            speedFactor = speedFactor,
            formantResonance = formantResonance,
            pitchHz = 120f * pitchFactor
        )
    }

    /**
     * Clones voice from acoustic recording parameters.
     */
    fun cloneUserAcoustics(detectedPitchHz: Float, detectedSpeed: Float): VoiceProfile {
        val pitchMultiplier = (detectedPitchHz / 130f).coerceIn(0.6f, 1.8f)
        customClonedProfile = VoiceProfile(
            id = "voice_custom_cloned",
            name = "My Calibrated Clone (${detectedPitchHz.toInt()} Hz)",
            pitchHz = detectedPitchHz,
            pitchFactor = pitchMultiplier,
            speedFactor = detectedSpeed.coerceIn(0.7f, 1.4f),
            formantResonance = (1.0f + (detectedPitchHz - 130f) / 500f).coerceIn(0.85f, 1.3f),
            warmthHarmonics = 0.88f,
            description = "Acoustically calibrated voice clone matching microphone sample.",
            colorHex = "#06B6D4"
        )
        return customClonedProfile
    }

    var isAutoSpeakEnabled: Boolean = false
    var globalSpeedMultiplier: Float = 1.0f

    /**
     * Maps Deity Persona to their specialized vocal profile with calibrated pitch, F0, and formant resonance.
     */
    fun getProfileForPersona(persona: Persona): VoiceProfile {
        return when (persona) {
            Persona.KRISHNA -> presetProfiles.getOrNull(0) ?: presetProfiles[0]
            Persona.SHIVA -> presetProfiles.getOrNull(1) ?: presetProfiles[0]
            Persona.BRAHMA -> presetProfiles.getOrNull(2) ?: presetProfiles[0]
            Persona.SHANI -> presetProfiles.getOrNull(3) ?: presetProfiles[0]
            Persona.GANESHA -> presetProfiles.getOrNull(4) ?: presetProfiles[0]
            Persona.MURUGA -> presetProfiles.getOrNull(5) ?: presetProfiles[0]
            Persona.HANUMAN -> presetProfiles.getOrNull(6) ?: presetProfiles[0]
            Persona.LAKSHMI -> presetProfiles.getOrNull(7) ?: presetProfiles[0]
            Persona.PARVATHI -> presetProfiles.getOrNull(8) ?: presetProfiles[0]
            Persona.NARASIMHA -> presetProfiles.getOrNull(9) ?: presetProfiles[0]
        }
    }

    /**
     * Returns a warm, friendly opening salutation for voice assistant responses.
     */
    fun getDeitySalutation(persona: Persona): String {
        return when (persona) {
            Persona.KRISHNA -> "Hello there! How can I brighten your day?"
            Persona.SHIVA -> "Hi friend! What shall we build or solve today?"
            Persona.BRAHMA -> "Greetings! Ready to explore something new?"
            Persona.GANESHA -> "Hey there! Let's solve this together."
            Persona.MURUGA -> "Hello! Ready to take on our goals?"
            Persona.HANUMAN -> "Hi! Hope you're feeling great today."
            Persona.LAKSHMI -> "Welcome! Let's create something wonderful."
            Persona.PARVATHI -> "Warm greetings! I'm here and listening."
            Persona.NARASIMHA -> "Hello! Your privacy and security are safe."
            Persona.SHANI -> "Hey! Let's get organized and focused."
        }
    }

    /**
     * Sanitizes markdown text for natural, smooth on-device vocal synthesis.
     */
    fun cleanTextForSpeech(text: String): String {
        return text
            .replace(Regex("```[\\s\\S]*?```"), "Code implementation omitted for speech.")
            .replace(Regex("`[^`]*`"), "")
            .replace(Regex("#+\\s*"), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
            .replace(Regex("^[•\\-*]\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("(?m)^\\s*[-=_]{3,}\\s*$"), "")
            .replace(Regex("[🦚🔱🪷🐘⚡🛡️🌸🏔️🦁⚖️📅📿🖤⚪🟢⚠️👉🎨✨🎬🎵📄🖼️📝]"), "")
            .replace(Regex("\\n{2,}"), ". ")
            .replace("\n", " ")
            .trim()
    }

    /**
     * Speaks text using the friendly voice profile for the specified persona.
     */
    fun speakDeity(text: String, persona: Persona) {
        val profile = getProfileForPersona(persona)
        val cleaned = cleanTextForSpeech(text)
        speak(cleaned, profile)
    }

    /**
     * Synthesizes and speaks text using the specified cloned voice profile.
     */
    fun speak(text: String, profile: VoiceProfile) {
        stop()

        val safeText = if (text.length > 3500) text.take(3500) + "..." else text

        if (isTtsReady && tts != null) {
            tts?.setPitch(profile.pitchFactor)
            tts?.setSpeechRate(profile.speedFactor * globalSpeedMultiplier)

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
            }
            tts?.speak(safeText, TextToSpeech.QUEUE_FLUSH, params, "utterance_maphula")
        } else {
            // Direct PCM Waveform audio synthesis fallback
            synthesizePcmAudioFallback(profile.pitchHz, profile.speedFactor)
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        stopWaveformSimulation()
    }

    private fun startWaveformSimulation() {
        visualizerJob?.cancel()
        visualizerJob = scope.launch {
            val random = Random()
            var phase = 0f
            while (isActive && _isSpeaking.value) {
                val wave = FloatArray(32)
                for (i in 0 until 32) {
                    val sinVal = (sin((phase + i * 0.4).toDouble()) + 1.0) / 2.0
                    val jitter = random.nextFloat() * 0.35f
                    wave[i] = ((sinVal * 0.7f + jitter) * 0.9f).toFloat().coerceIn(0.1f, 1.0f)
                }
                _currentWaveformData.value = wave
                phase += 0.35f
                delay(40)
            }
            _currentWaveformData.value = FloatArray(32)
        }
    }

    private fun stopWaveformSimulation() {
        visualizerJob?.cancel()
        _currentWaveformData.value = FloatArray(32)
    }

    @Suppress("DEPRECATION")
    private fun synthesizePcmAudioFallback(pitchHz: Float, speed: Float) {
        scope.launch(Dispatchers.IO) {
            _isSpeaking.value = true
            startWaveformSimulation()

            try {
                val sampleRate = 44100
                val durationMs = (1800 / (speed * globalSpeedMultiplier)).toInt()
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Harmonic overtone synthesis
                    val fundamental = sin(2 * PI * pitchHz * t)
                    val secondHarmonic = 0.5 * sin(2 * PI * (pitchHz * 2) * t)
                    val thirdHarmonic = 0.25 * sin(2 * PI * (pitchHz * 3) * t)
                    val sample = (fundamental + secondHarmonic + thirdHarmonic) / 1.75
                    buffer[i] = (sample * Short.MAX_VALUE * 0.6).toInt().toShort()
                }

                val track = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size * 2,
                    AudioTrack.MODE_STATIC
                )
                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs.toLong())
                track.stop()
                track.release()
            } catch (_: Exception) {}

            _isSpeaking.value = false
            stopWaveformSimulation()
        }
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
