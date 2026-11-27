package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 22050

    @Volatile
    var enabled: Boolean = true

    fun playPop() {
        if (!enabled) return
        scope.launch {
            playSweep(startHz = 360.0, endHz = 880.0, durationMs = 110, volume = 0.45)
        }
    }

    fun playChime() {
        if (!enabled) return
        scope.launch {
            playTone(freqHz = 784.0, durationMs = 130, volume = 0.35)
            playTone(freqHz = 1046.5, durationMs = 200, volume = 0.40)
        }
    }

    fun playBoing() {
        if (!enabled) return
        scope.launch {
            playSweep(startHz = 310.0, endHz = 180.0, durationMs = 160, volume = 0.32)
        }
    }

    fun playFanfare() {
        if (!enabled) return
        scope.launch {
            val notes = listOf(523.25 to 110, 659.25 to 110, 783.99 to 110, 1046.50 to 260)
            for ((hz, ms) in notes) {
                playTone(freqHz = hz, durationMs = ms, volume = 0.40)
            }
        }
    }

    fun playXylophoneNote(noteIndex: Int) {
        if (!enabled) return
        val frequencies = doubleArrayOf(
            261.63, // C4
            293.66, // D4
            329.63, // E4
            349.23, // F4
            392.00, // G4
            440.00, // A4
            493.88, // B4
            523.25  // C5
        )
        val hz = frequencies[noteIndex.coerceIn(0, frequencies.lastIndex)]
        scope.launch {
            playMarimbaNote(freqHz = hz, durationMs = 340, volume = 0.50)
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int, volume: Double) {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-5.5 * i / count.toDouble())
            val sample = sin(2.0 * PI * freqHz * t) * env * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        writePcm(buffer)
    }

    private fun playMarimbaNote(freqHz: Double, durationMs: Int, volume: Double) {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val attack = (i / (sampleRate * 0.008)).coerceAtMost(1.0)
            val decay = exp(-6.0 * i / count.toDouble())
            val fundamental = sin(2.0 * PI * freqHz * t)
            val overtone = 0.25 * sin(2.0 * PI * (freqHz * 2.0) * t)
            val sample = (fundamental + overtone) * attack * decay * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        writePcm(buffer)
    }

    private fun playSweep(startHz: Double, endHz: Double, durationMs: Int, volume: Double) {
        val count = (sampleRate * durationMs) / 1000
        val buffer = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val frac = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * frac
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-4.5 * frac)
            val sample = sin(phase) * env * volume
            buffer[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        writePcm(buffer)
    }

    private fun writePcm(buffer: ShortArray) {
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            val sleepMs = (buffer.size * 1000L) / sampleRate + 30L
            Thread.sleep(sleepMs)
            track.stop()
            track.release()
        } catch (_: Exception) {
            // Gracefully ignore audio errors on headless/muted devices
        }
    }
}
