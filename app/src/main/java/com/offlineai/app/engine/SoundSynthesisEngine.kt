package com.offlineai.app.engine

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * On-device procedural sound synthesis engine for Maphula.
 * Generates buttery acoustic click feedback, harmonic resonance,
 * and sacred Vedic drone/frequencies using standard Android AudioTrack.
 */
class SoundSynthesisEngine {

    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    private var activePlaybackJob: Job? = null

    var isSoundFeedbackEnabled: Boolean = true

    /**
     * Subtle, buttery haptic audio click on touch interactions.
     */
    fun playButteryClick() {
        if (!isSoundFeedbackEnabled) return
        scope.launch {
            try {
                val durationMs = 25
                val numSamples = (sampleRate * durationMs / 1000)
                val buffer = ShortArray(numSamples)
                val freq = 880.0 // Hz

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Exponential decay envelope
                    val envelope = exp(-t * 120.0)
                    val sample = sin(2.0 * PI * freq * t) * envelope
                    buffer[i] = (sample * Short.MAX_VALUE * 0.35).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    /**
     * Cosmic Om / Sacred Drone resonance at base frequency (136.1 Hz Earth frequency or 432 Hz).
     */
    fun playSacredDrone(baseFreq: Double = 136.1, durationMs: Long = 3000) {
        activePlaybackJob?.cancel()
        activePlaybackJob = scope.launch {
            try {
                val numSamples = (sampleRate * durationMs / 1000).toInt()
                val buffer = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = i.toDouble() / numSamples

                    // Smooth fade-in (first 15%) and fade-out (last 25%)
                    val fadeIn = if (progress < 0.15) progress / 0.15 else 1.0
                    val fadeOut = if (progress > 0.75) (1.0 - progress) / 0.25 else 1.0
                    val envelope = fadeIn * fadeOut

                    // Fundamental + harmonics (2nd, 3rd, 5th harmonic with subtle detuning)
                    val s1 = sin(2.0 * PI * baseFreq * t)
                    val s2 = 0.5 * sin(2.0 * PI * (baseFreq * 2.0 + 0.5) * t) // octave + subtle beat
                    val s3 = 0.25 * sin(2.0 * PI * (baseFreq * 3.0) * t)     // fifth
                    val s5 = 0.15 * sin(2.0 * PI * (baseFreq * 5.0) * t)     // third

                    val mixed = (s1 + s2 + s3 + s5) / 1.9 * envelope
                    buffer[i] = (mixed * Short.MAX_VALUE * 0.55).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                playBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    /**
     * Plays a sequence of sacred notes (e.g. Vedic chant cadence).
     */
    fun playChantCadence(frequencies: List<Double> = listOf(216.0, 243.0, 288.0, 324.0, 432.0), noteDurationMs: Long = 400) {
        activePlaybackJob?.cancel()
        activePlaybackJob = scope.launch {
            try {
                val totalDurationMs = frequencies.size * noteDurationMs
                val numSamples = (sampleRate * totalDurationMs / 1000).toInt()
                val buffer = ShortArray(numSamples)
                val samplesPerNote = (sampleRate * noteDurationMs / 1000).toInt()

                for ((idx, freq) in frequencies.withIndex()) {
                    val startOffset = idx * samplesPerNote
                    for (i in 0 until samplesPerNote) {
                        val bufferIdx = startOffset + i
                        if (bufferIdx >= numSamples) break
                        val t = i.toDouble() / sampleRate
                        val noteProgress = i.toDouble() / samplesPerNote
                        val envelope = sin(PI * noteProgress) // smooth bell envelope

                        val sample = (sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * (freq * 2.0) * t)) * envelope
                        buffer[bufferIdx] = (sample * Short.MAX_VALUE * 0.45).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }

                playBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    /**
     * Resonant Temple Bell / Ghanta chime with metallic harmonics and shimmering decay.
     */
    fun playTempleBell(baseFreq: Double = 1175.0, durationMs: Long = 1800) {
        if (!isSoundFeedbackEnabled) return
        scope.launch {
            try {
                val numSamples = (sampleRate * durationMs / 1000).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val env = exp(-t * 3.8)
                    val s1 = sin(2.0 * PI * baseFreq * t)
                    val s2 = 0.6 * sin(2.0 * PI * (baseFreq * 2.76) * t) * exp(-t * 5.0)
                    val s3 = 0.35 * sin(2.0 * PI * (baseFreq * 5.4) * t) * exp(-t * 8.0)
                    val s0 = 0.5 * sin(2.0 * PI * (baseFreq * 0.5) * t) * exp(-t * 2.5) // strike tone
                    val mixed = (s1 + s2 + s3 + s0) / 2.45 * env
                    buffer[i] = (mixed * Short.MAX_VALUE * 0.5).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                playBuffer(buffer)
            } catch (_: Exception) {}
        }
    }

    /**
     * Ascending 4-note victory / level-up arpeggio (C5 -> E5 -> G5 -> C6).
     */
    fun playLevelUp() {
        if (!isSoundFeedbackEnabled) return
        playChantCadence(listOf(523.25, 659.25, 783.99, 1046.50), noteDurationMs = 150)
    }

    fun stopAudio() {
        activePlaybackJob?.cancel()
    }

    private fun playBuffer(buffer: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufferSize = maxOf(minBufferSize, buffer.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(trackBufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()

        // Release track when done
        scope.launch {
            val durationMs = (buffer.size.toDouble() / sampleRate * 1000).toLong() + 200
            kotlinx.coroutines.delay(durationMs)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }
}
