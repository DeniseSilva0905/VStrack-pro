package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.PI
import kotlin.math.sin

enum class PadTimbre(val displayName: String, val description: String) {
    WARM("Warm", "Analógico rico com sub-harmônicos e corte suave de agudos"),
    CELESTIAL("Celestial", "Etéreo com quinta justa e harmônicos arejados"),
    SHIMMER("Shimmer", "Oitavas cintilantes com movimento harmônico contínuo")
}

class VSTracksPadEngine {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var synthJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Frequencies for octave 3/4
    private val keyFrequencies = mapOf(
        "C" to 130.81f,
        "C#" to 138.59f,
        "Db" to 138.59f,
        "D" to 146.83f,
        "D#" to 155.56f,
        "Eb" to 155.56f,
        "E" to 164.81f,
        "F" to 174.61f,
        "F#" to 185.00f,
        "Gb" to 185.00f,
        "G" to 196.00f,
        "G#" to 207.65f,
        "Ab" to 207.65f,
        "A" to 220.00f,
        "A#" to 233.08f,
        "Bb" to 233.08f,
        "B" to 246.94f
    )

    var currentKey: String = "C"
        private set

    var currentTimbre: PadTimbre = PadTimbre.WARM
        private set

    var volume: Float = 0.8f

    // Target crossfade frequency
    @Volatile
    private var targetFreq = 130.81f
    @Volatile
    private var activeFreq = 130.81f
    @Volatile
    private var masterGain = 0f

    fun start(key: String = "C", timbre: PadTimbre = PadTimbre.WARM) {
        currentKey = key
        currentTimbre = timbre
        targetFreq = keyFrequencies[key] ?: 130.81f
        activeFreq = targetFreq

        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
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
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        synthJob = scope.launch {
            val numFrames = 1024
            val buffer = ShortArray(numFrames * 2) // Stereo
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var lfoPhase = 0.0

            while (isActive && isPlaying) {
                // Smooth gain ramping
                val targetMasterGain = volume
                if (masterGain < targetMasterGain) {
                    masterGain = (masterGain + 0.02f).coerceAtMost(targetMasterGain)
                } else if (masterGain > targetMasterGain) {
                    masterGain = (masterGain - 0.02f).coerceAtLeast(0f)
                }

                // Smooth frequency slew for seamless key transitions
                activeFreq += (targetFreq - activeFreq) * 0.005f

                val f0 = activeFreq
                val fFifth = f0 * 1.498307f // Just fifth
                val fOctave = f0 * 2.0f

                val step1 = 2.0 * PI * f0 / sampleRate
                val step2 = 2.0 * PI * fFifth / sampleRate
                val step3 = 2.0 * PI * fOctave / sampleRate
                val lfoStep = 2.0 * PI * 0.2 / sampleRate // 0.2Hz slow shimmer pulse

                for (i in 0 until numFrames) {
                    val lfo = 0.85 + 0.15 * sin(lfoPhase)
                    lfoPhase += lfoStep

                    val s1 = sin(phase1)
                    val s2 = sin(phase2)
                    val s3 = sin(phase3)

                    phase1 += step1
                    phase2 += step2
                    phase3 += step3

                    val mixed: Double = when (currentTimbre) {
                        PadTimbre.WARM -> {
                            // Sub + Fundamental + gentle harmonic
                            (s1 * 0.55 + sin(phase1 * 0.5) * 0.35 + s3 * 0.10)
                        }
                        PadTimbre.CELESTIAL -> {
                            // Airy fifths + fundamental
                            (s1 * 0.40 + s2 * 0.35 + s3 * 0.25) * lfo
                        }
                        PadTimbre.SHIMMER -> {
                            // Sparkling octave chorus + subtle detuning
                            val sDetune = sin(phase3 * 1.003)
                            (s1 * 0.30 + s2 * 0.25 + s3 * 0.25 + sDetune * 0.20) * lfo
                        }
                    }

                    val sampleValue = (mixed * masterGain * 16000.0).coerceIn(-32767.0, 32767.0).toInt().toShort()

                    // Stereo output with slight stereo width
                    buffer[i * 2] = sampleValue
                    buffer[i * 2 + 1] = sampleValue
                }

                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    fun changeKey(newKey: String) {
        currentKey = newKey
        targetFreq = keyFrequencies[newKey] ?: 130.81f
        if (!isPlaying) {
            start(newKey, currentTimbre)
        }
    }

    fun changeTimbre(newTimbre: PadTimbre) {
        currentTimbre = newTimbre
        if (!isPlaying) {
            start(currentKey, newTimbre)
        }
    }

    fun stop() {
        isPlaying = false
        synthJob?.cancel()
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
        masterGain = 0f
    }

    fun isPadPlaying(): Boolean = isPlaying
}
