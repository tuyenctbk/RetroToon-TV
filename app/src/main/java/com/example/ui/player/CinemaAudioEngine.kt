package com.example.ui.player

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.LoudnessEnhancer
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

enum class PlayerAudioMode(val label: String) {
    BOOST("Boost"),
    NORMAL("Sound"),
    PIANO("Piano"),
    MUTE("Muted")
}

/**
 * Zero-latency pass-through Media3 AudioProcessor that inspects decoded PCM buffers
 * so RetroPlayerScreen can detect silent-era films muxed with empty 0-amplitude audio tracks.
 */
@OptIn(UnstableApi::class)
class SilenceDetectingAudioProcessor : BaseAudioProcessor() {

    private val everDetectedAudible = AtomicBoolean(false)
    private val accumulatedSilentBytes = AtomicLong(0L)
    @Volatile
    private var bytesPerSecond: Long = 44100L * 2L * 2L

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        return if (inputAudioFormat.encoding == C.ENCODING_PCM_16BIT) {
            val rate = inputAudioFormat.sampleRate.coerceAtLeast(8000).toLong()
            val channels = inputAudioFormat.channelCount.coerceAtLeast(1).toLong()
            bytesPerSecond = rate * channels * 2L
            inputAudioFormat
        } else {
            AudioProcessor.AudioFormat.NOT_SET
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        // Sample every 8th 16-bit PCM sample to detect audible signal with negligible CPU cost
        val startPos = inputBuffer.position()
        val limit = inputBuffer.limit()
        var maxSample = 0
        var idx = startPos
        while (idx + 1 < limit) {
            val lo = inputBuffer.get(idx).toInt() and 0xFF
            val hi = inputBuffer.get(idx + 1).toInt()
            val sample = abs((hi shl 8) or lo)
            if (sample > maxSample) {
                maxSample = sample
                if (maxSample > AUDIBLE_THRESHOLD_PCM16) break
            }
            idx += 16
        }

        if (maxSample > AUDIBLE_THRESHOLD_PCM16) {
            everDetectedAudible.set(true)
            accumulatedSilentBytes.set(0L)
        } else if (!everDetectedAudible.get()) {
            accumulatedSilentBytes.addAndGet(remaining.toLong())
        }

        val out = replaceOutputBuffer(remaining)
        out.put(inputBuffer)
        out.flip()
    }

    fun resetForNewMedia() {
        everDetectedAudible.set(false)
        accumulatedSilentBytes.set(0L)
    }

    /**
     * Returns true ONLY if at least [minSilentSeconds] of decoded PCM audio have been inspected
     * from the start of the stream and zero audible samples have ever occurred (0-amplitude track).
     */
    fun isConfirmedZeroAmplitudeTrack(minSilentSeconds: Float = 3.2f): Boolean {
        if (everDetectedAudible.get()) return false
        val thresholdBytes = (bytesPerSecond * minSilentSeconds).toLong()
        return accumulatedSilentBytes.get() >= thresholdBytes
    }

    fun hasDetectedAudibleAudio(): Boolean = everDetectedAudible.get()

    companion object {
        // ~ -52 dBFS threshold: distinguishes real optical soundtrack from digital zero-fill
        private const val AUDIBLE_THRESHOLD_PCM16 = 85
    }
}

/**
 * Lightweight procedural 1920s Theater Upright Piano synthesizer for silent-era films
 * that lack an audio track or contain a 0-amplitude silent transfer on Internet Archive.
 */
class RagtimePianoSynthesizer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var synthJob: Job? = null
    @Volatile
    private var isActivePlaying = false

    // Public-domain 1920s Ragtime Cinema progression (C Major -> F Major -> C Major -> G7 -> A minor -> D7 -> G7 -> C)
    // Pairs of (Left-hand stride bass/chord notes Hz, Right-hand syncopated melody note Hz)
    private val ragtimeSteps: Array<Pair<FloatArray, Float>> = arrayOf(
        floatArrayOf(130.81f, 261.63f) to 523.25f, // C3 + C4, C5
        floatArrayOf(196.00f, 329.63f, 392.00f) to 659.25f, // G3+E4+G4, E5
        floatArrayOf(98.00f, 261.63f) to 587.33f,  // G2 + C4, D5
        floatArrayOf(196.00f, 329.63f, 392.00f) to 523.25f, // C5
        floatArrayOf(174.61f, 261.63f) to 698.46f, // F3 + C4, F5
        floatArrayOf(220.00f, 349.23f, 440.00f) to 880.00f, // Fmaj, A5
        floatArrayOf(130.81f, 261.63f) to 783.99f, // C3, G5
        floatArrayOf(220.00f, 349.23f) to 698.46f, // F5
        floatArrayOf(130.81f, 261.63f) to 659.25f, // C3, E5
        floatArrayOf(196.00f, 329.63f, 392.00f) to 523.25f, // C5
        floatArrayOf(146.83f, 293.66f) to 587.33f, // D3, D5
        floatArrayOf(196.00f, 293.66f, 369.99f) to 659.25f, // D7, E5
        floatArrayOf(196.00f, 246.94f) to 587.33f, // G3+B3, D5
        floatArrayOf(196.00f, 293.66f, 349.23f) to 493.88f, // G7, B4
        floatArrayOf(130.81f, 261.63f, 329.63f) to 523.25f, // Cmaj, C5
        floatArrayOf(196.00f, 329.63f, 392.00f) to 392.00f  // G4 turnaround
    )

    fun setPlaying(play: Boolean) {
        if (isActivePlaying == play) return
        isActivePlaying = play
        if (play) {
            startLoop()
        } else {
            synthJob?.cancel()
            synthJob = null
        }
    }

    private fun startLoop() {
        synthJob?.cancel()
        synthJob = scope.launch {
            val sampleRate = 22050
            val stepSamples = (sampleRate * 0.24f).toInt() // ~125 BPM ragtime eighth-note stride
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(stepSamples * 2)

            val track = try {
                AudioTrack.Builder()
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
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                return@launch
            }

            val pcm = ShortArray(stepSamples)
            var stepIndex = 0
            try {
                track.play()
                while (isActive && isActivePlaying) {
                    val (chordFreqs, melodyFreq) = ragtimeSteps[stepIndex % ragtimeSteps.size]
                    for (i in 0 until stepSamples) {
                        val t = i.toFloat() / sampleRate.toFloat()
                        // Soft upright felt-hammer attack + natural wooden soundboard decay
                        val attack = (i.toFloat() / (sampleRate * 0.008f)).coerceAtMost(1f)
                        val melodyEnv = attack * exp(-6.2f * t)
                        val chordEnv = attack * exp(-8.5f * t)

                        var sampleVal = pianoVoice(melodyFreq, t) * melodyEnv * 0.52f
                        for (cf in chordFreqs) {
                            sampleVal += pianoVoice(cf, t) * chordEnv * 0.20f
                        }

                        val clamped = (sampleVal * 9500f).toInt().coerceIn(-32000, 32000)
                        pcm[i] = clamped.toShort()
                    }
                    track.write(pcm, 0, stepSamples)
                    stepIndex++
                }
            } catch (_: Exception) {
            } finally {
                try {
                    track.pause()
                    track.flush()
                    track.release()
                } catch (_: Exception) {}
            }
        }
    }

    private fun pianoVoice(freq: Float, t: Float): Float {
        val w = (2.0 * PI * freq * t).toFloat()
        // Fundamental + warm 2nd/3rd upright piano harmonics with slight honky-tonk detune
        val wDetune = (2.0 * PI * (freq * 1.0025f) * t).toFloat()
        return 0.62f * sin(w) +
            0.22f * sin(wDetune) +
            0.12f * sin(2f * w) +
            0.04f * sin(3f * w)
    }

    fun release() {
        isActivePlaying = false
        synthJob?.cancel()
        scope.cancel()
    }
}

/**
 * Safely manages Android LoudnessEnhancer for boosting quiet 1930s mono optical soundtracks.
 */
class VintageAudioBooster {
    private var enhancer: LoudnessEnhancer? = null
    private var boundSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    fun update(audioSessionId: Int, enableBoost: Boolean) {
        if (audioSessionId == C.AUDIO_SESSION_ID_UNSET || audioSessionId <= 0) return
        try {
            if (enhancer == null || boundSessionId != audioSessionId) {
                enhancer?.release()
                enhancer = LoudnessEnhancer(audioSessionId)
                boundSessionId = audioSessionId
            }
            enhancer?.apply {
                setTargetGain(if (enableBoost) 550 else 0) // +5.5 dB clear optical remaster gain
                enabled = enableBoost
            }
        } catch (_: Exception) {
            // Gracefully ignore devices without LoudnessEnhancer DSP effect
        }
    }

    fun release() {
        try {
            enhancer?.release()
        } catch (_: Exception) {}
        enhancer = null
    }
}
