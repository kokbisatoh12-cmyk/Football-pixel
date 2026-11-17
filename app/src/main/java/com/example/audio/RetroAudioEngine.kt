package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class RetroAudioEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var volume: Float = 0.7f

    fun playMenuClick() = playAsync {
        generateSquareSweep(durationMs = 55, startFreq = 720f, endFreq = 980f, gain = 0.28f)
    }

    fun playPass() = playAsync {
        generateKickThud(durationMs = 65, startFreq = 260f, endFreq = 95f, noiseMix = 0.25f, gain = 0.4f)
    }

    fun playShoot() = playAsync {
        generateKickThud(durationMs = 110, startFreq = 340f, endFreq = 65f, noiseMix = 0.45f, gain = 0.55f)
    }

    fun playTackle() = playAsync {
        generateNoiseBurst(durationMs = 120, gain = 0.35f)
    }

    fun playPostClang() = playAsync {
        generateSquareSweep(durationMs = 180, startFreq = 1120f, endFreq = 1040f, gain = 0.4f)
    }

    fun playWhistle(longWhistle: Boolean = false) = playAsync {
        val ms = if (longWhistle) 420 else 190
        val count = (sampleRate * ms) / 1000
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val env = if (t < 0.08f) t / 0.08f else if (t > 0.85f) (1f - t) / 0.15f else 1f
            val trill = sin(2.0 * PI * 38.0 * (i.toDouble() / sampleRate))
            val freq = 2650.0 + trill * 140.0
            phase += 2.0 * PI * freq / sampleRate
            val sample = (if (sin(phase) >= 0.0) 0.5 else -0.5) * env * 0.45 * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buffer
    }

    fun playGoalRoar() = playAsync {
        val notes = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
        val noteSamples = (sampleRate * 95) / 1000
        val tailSamples = (sampleRate * 380) / 1000
        val total = notes.size * noteSamples + tailSamples
        val buffer = ShortArray(total)
        val rng = Random(42)
        var phase = 0.0
        for (i in 0 until total) {
            val noteIdx = (i / noteSamples).coerceAtMost(notes.lastIndex)
            val freq = notes[noteIdx].toDouble()
            phase += 2.0 * PI * freq / sampleRate
            val sq = if (sin(phase) >= 0.0) 1.0 else -1.0
            val fade = if (i > notes.size * noteSamples) {
                1.0 - (i - notes.size * noteSamples).toDouble() / tailSamples
            } else 1.0
            val crowd = (rng.nextDouble() * 2.0 - 1.0) * 0.25 * fade
            val mixed = (sq * 0.28 * fade + crowd) * volume
            buffer[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        buffer
    }

    private fun playAsync(generator: () -> ShortArray) {
        if (!enabled || volume <= 0.01f) return
        scope.launch {
            runCatching {
                val samples = generator()
                val byteLen = samples.size * 2
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
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(byteLen)
                    .build()
                track.write(samples, 0, samples.size)
                track.play()
                val sleepMs = (samples.size * 1000L / sampleRate) + 40L
                kotlinx.coroutines.delay(sleepMs)
                track.stop()
                track.release()
            }
        }
    }

    private fun generateSquareSweep(durationMs: Int, startFreq: Float, endFreq: Float, gain: Float): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = 1f - t * 0.85f
            val sq = if (sin(phase) >= 0.0) 1.0 else -1.0
            val v = sq * env * gain * volume
            buffer[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateKickThud(durationMs: Int, startFreq: Float, endFreq: Float, noiseMix: Float, gain: Float): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        var phase = 0.0
        val rng = Random(System.nanoTime())
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - t) * (1f - t)
            val tone = sin(phase) * (1f - noiseMix)
            val noise = (rng.nextDouble() * 2.0 - 1.0) * noiseMix
            val v = (tone + noise) * env * gain * volume
            buffer[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateNoiseBurst(durationMs: Int, gain: Float): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        val rng = Random(System.nanoTime())
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val env = 1f - t
            val v = (rng.nextDouble() * 2.0 - 1.0) * env * gain * volume
            buffer[i] = (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }
}
