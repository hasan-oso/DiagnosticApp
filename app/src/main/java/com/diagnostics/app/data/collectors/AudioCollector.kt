/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * 
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * 
 * This file is part of DiagnosticApp.
 * Unauthorized use is strictly prohibited.
 * 
 * Owner: Hasan Oso
 * Student ID: 230212925
 * Institution: OSTİM Technical University
 */
package com.diagnostics.app.data.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.diagnostics.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class AudioCollector(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    suspend fun collectAudioData(): AudioData = withContext(Dispatchers.IO) {
        val hasRecordPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        return@withContext if (hasRecordPermission) {
            // Full loopback test: play beep + record simultaneously
            runLoopbackTest()
        } else {
            // No mic permission — only test speaker (play beep, check no exception)
            val speakerOk = testSpeakerOnly()
            Log.w("AudioCollector", "RECORD_AUDIO not granted — mic test skipped")
            AudioData(
                speakerWorking = speakerOk,
                microphoneWorking = false,
                audioTestPassed = speakerOk,
                speakerVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                micSensitivity = 0f,
                errorMessage = "RECORD_AUDIO permission not granted"
            )
        }
    }

    /**
     * Plays beep_01a.mp3 through speaker and simultaneously records with
     * the microphone. If the mic captures audio above the RMS threshold,
     * both speaker and microphone are declared working.
     */
    private suspend fun runLoopbackTest(): AudioData {
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        // Raise volume to 75% so the mic can clearly pick up the beep
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (maxVolume * 0.75).toInt().coerceAtLeast(1),
            0
        )

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).takeIf { it > 0 } ?: return AudioData(
            speakerWorking = false,
            microphoneWorking = false,
            audioTestPassed = false,
            speakerVolume = originalVolume,
            micSensitivity = 0f,
            errorMessage = "AudioRecord buffer error"
        )

        var mediaPlayer: MediaPlayer? = null
        var audioRecord: AudioRecord? = null

        return try {
            // Prepare MediaPlayer with beep_01a resource
            mediaPlayer = MediaPlayer.create(context, R.raw.beep_01a)
            if (mediaPlayer == null) {
                Log.e("AudioCollector", "MediaPlayer.create returned null for beep_01a")
                return AudioData(
                    speakerWorking = false,
                    microphoneWorking = false,
                    audioTestPassed = false,
                    speakerVolume = originalVolume,
                    micSensitivity = 0f,
                    errorMessage = "Could not load beep_01a.mp3"
                )
            }

            // Initialize microphone recorder
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 4
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioCollector", "AudioRecord failed to initialize")
                return AudioData(
                    speakerWorking = false,
                    microphoneWorking = false,
                    audioTestPassed = false,
                    speakerVolume = originalVolume,
                    micSensitivity = 0f,
                    errorMessage = "AudioRecord init failed"
                )
            }

            // Start microphone recording first
            audioRecord.startRecording()
            delay(100) // small stabilization delay

            // Play beep through speaker
            var speakerWorking = false
            mediaPlayer.setOnCompletionListener { speakerWorking = true }
            mediaPlayer.start()
            speakerWorking = true // start() didn't throw → speaker is working

            Log.d("AudioCollector", "Playing beep_01a.mp3, recording mic...")

            // Record for 2 seconds (covers the beep duration)
            val recordDurationMs = 2000L
            val totalSamples = (sampleRate * (recordDurationMs / 1000f)).toInt()
            val recorded = ShortArray(totalSamples)
            var totalRead = 0
            val endTime = System.currentTimeMillis() + recordDurationMs

            while (totalRead < totalSamples && System.currentTimeMillis() < endTime) {
                val remaining = totalSamples - totalRead
                val read = audioRecord.read(recorded, totalRead, minOf(bufferSize, remaining))
                if (read > 0) totalRead += read
                delay(5)
            }

            // Stop recording and playback
            audioRecord.stop()
            if (mediaPlayer.isPlaying) mediaPlayer.stop()

            // Calculate RMS of captured audio
            val rms = calculateRMS(recorded, totalRead)
            Log.d("AudioCollector", "Loopback RMS: $rms (threshold: 80)")

            // RMS > 80 means the mic heard the beep from the speaker
            val micWorking = rms > 80f

            AudioData(
                speakerWorking = speakerWorking,
                microphoneWorking = micWorking,
                audioTestPassed = speakerWorking && micWorking,
                speakerVolume = originalVolume,
                micSensitivity = rms,
                errorMessage = null
            )

        } catch (e: Exception) {
            Log.e("AudioCollector", "Loopback test failed: ${e.message}")
            AudioData(
                speakerWorking = false,
                microphoneWorking = false,
                audioTestPassed = false,
                speakerVolume = originalVolume,
                micSensitivity = 0f,
                errorMessage = e.message
            )
        } finally {
            audioRecord?.release()
            mediaPlayer?.release()
            // Restore original volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
        }
    }

    /**
     * Speaker-only test (no mic permission): plays beep_01a.mp3.
     * Returns true if MediaPlayer starts without exception.
     */
    private suspend fun testSpeakerOnly(): Boolean = withContext(Dispatchers.IO) {
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        var player: MediaPlayer? = null
        return@withContext try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume / 2, 0)
            player = MediaPlayer.create(context, R.raw.beep_01a)
            player?.start()
            delay(1500)
            true
        } catch (e: Exception) {
            Log.e("AudioCollector", "Speaker-only test failed: ${e.message}")
            false
        } finally {
            player?.release()
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
        }
    }

    private fun calculateRMS(data: ShortArray, length: Int): Float {
        if (length <= 0) return 0f
        var sum = 0.0
        for (i in 0 until length) {
            sum += data[i].toDouble() * data[i].toDouble()
        }
        return sqrt(sum / length).toFloat()
    }
}

data class AudioData(
    val speakerWorking: Boolean,
    val microphoneWorking: Boolean,
    val audioTestPassed: Boolean,
    val speakerVolume: Int,
    val micSensitivity: Float,
    val errorMessage: String?
)
