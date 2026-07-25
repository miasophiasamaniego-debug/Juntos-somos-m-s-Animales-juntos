package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.pow
import kotlin.math.sin

class SoundEffects(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private val random = Random()

    init {
        try {
            toneGenerator = ToneGenerator(android.media.AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Jump sound: Springy upward pitch slide (250Hz -> 750Hz) with smooth envelope.
     */
    fun playJumpSound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            try {
                generateTone(
                    startFreq = 260f,
                    endFreq = 780f,
                    durationMs = 130,
                    waveType = WaveType.SPRING,
                    volume = 0.6f
                )
            } catch (e: Exception) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            }
        }
    }

    /**
     * Recolección de botella de plástico: Tono metálico brillante y resonante en dos notas.
     */
    fun playPickupBottleSound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            try {
                // Dual note chime (C6 -> G6)
                generateTone(
                    startFreq = 1046f,
                    endFreq = 1568f,
                    durationMs = 90,
                    waveType = WaveType.SINE_ENVELOPE,
                    volume = 0.5f
                )
            } catch (e: Exception) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
            }
        }
    }

    /**
     * Recolección de cartón: Sonido pop/impacto seco y cálido.
     */
    fun playPickupCardboardSound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            try {
                generateTone(
                    startFreq = 650f,
                    endFreq = 220f,
                    durationMs = 100,
                    waveType = WaveType.WARM_POP,
                    volume = 0.6f
                )
            } catch (e: Exception) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 70)
            }
        }
    }

    /**
     * Golpe con carro: Choque estruendoso grave con mezcla de ruido de impacto y sub-bajo.
     */
    fun playCrashSound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            try {
                generateTone(
                    startFreq = 180f,
                    endFreq = 35f,
                    durationMs = 320,
                    waveType = WaveType.CRASH_NOISE,
                    volume = 0.9f
                )
            } catch (e: Exception) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
            }
        }
    }

    /**
     * Victoria / Fanfarria al llegar a la casita o recoger super power-up.
     */
    fun playVictorySound(soundEnabled: Boolean) {
        if (!soundEnabled) return
        scope.launch {
            try {
                val notes = listOf(523.25f, 659.25f, 783.99f, 1046.50f) // C5, E5, G5, C6
                for (freq in notes) {
                    generateTone(
                        startFreq = freq,
                        endFreq = freq * 1.02f,
                        durationMs = 110,
                        waveType = WaveType.SINE_ENVELOPE,
                        volume = 0.5f
                    )
                    kotlinx.coroutines.delay(90)
                }
            } catch (e: Exception) {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            }
        }
    }

    fun triggerVibration(vibrationEnabled: Boolean, durationMs: Long = 50) {
        if (!vibrationEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private enum class WaveType {
        SPRING,
        SINE_ENVELOPE,
        WARM_POP,
        CRASH_NOISE
    }

    private fun generateTone(
        startFreq: Float,
        endFreq: Float,
        durationMs: Int,
        waveType: WaveType,
        volume: Float = 0.5f
    ) {
        val sampleRate = 22050
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
        val buffer = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val progress = i.toFloat() / numSamples
            // Decay envelope (attack fast, decay over time)
            val envelope = (1.0f - progress).pow(1.5f)

            val sampleVal = when (waveType) {
                WaveType.SPRING -> {
                    // Exponential pitch curve for boing jump
                    val currentFreq = startFreq + (endFreq - startFreq) * progress.pow(0.8f)
                    val angle = 2.0 * Math.PI * currentFreq * i / sampleRate
                    val sine = sin(angle)
                    // Add light 2nd harmonic
                    val harmonic = sin(angle * 2.0) * 0.25
                    (sine + harmonic) * envelope
                }

                WaveType.SINE_ENVELOPE -> {
                    val currentFreq = startFreq + (endFreq - startFreq) * progress
                    val angle = 2.0 * Math.PI * currentFreq * i / sampleRate
                    sin(angle) * envelope
                }

                WaveType.WARM_POP -> {
                    // Fast pitch drop with slight triangle wave
                    val currentFreq = startFreq + (endFreq - startFreq) * progress.pow(2.0f)
                    val angle = 2.0 * Math.PI * currentFreq * i / sampleRate
                    val sine = sin(angle)
                    // Triangle aspect
                    val tri = if (sine >= 0) sine * 0.8 else sine * 0.8
                    tri * envelope
                }

                WaveType.CRASH_NOISE -> {
                    // Sub bass pitch drop + white noise crunch
                    val currentFreq = startFreq + (endFreq - startFreq) * progress
                    val subAngle = 2.0 * Math.PI * currentFreq * i / sampleRate
                    val subBass = sin(subAngle) * 0.6
                    val noise = (random.nextFloat() * 2f - 1f) * 0.4
                    (subBass + noise) * envelope
                }
            }

            val finalSample = (sampleVal * Short.MAX_VALUE * volume).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            buffer[i] = finalSample.toShort()
        }

        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            scope.launch {
                kotlinx.coroutines.delay(durationMs.toLong() + 60)
                try {
                    track.stop()
                    track.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

