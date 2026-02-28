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
 * Institution: OST?M Technical University
 */
package com.diagnostics.app.data.collectors

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.sqrt

class AudioCollector(private val context: Context) {
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    suspend fun collectAudioData(): AudioData = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w("AudioCollector", "RECORD_AUDIO permission not granted")
            return@withContext AudioData(
                speakerWorking = false,
                microphoneWorking = false,
                audioTestPassed = false,
                speakerVolume = 0,
                micSensitivity = 0f,
                errorMessage = "Permission denied"
            )
        }
        
        try {
            performAudioLoopbackTest()
        } catch (e: Exception) {
            Log.e("AudioCollector", "Audio test failed", e)
            AudioData(
                speakerWorking = false,
                microphoneWorking = false,
                audioTestPassed = false,
                speakerVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),
                micSensitivity = 0f,
                errorMessage = e.message ?: "Unknown error"
            )
        }
    }
    
    private suspend fun performAudioLoopbackTest(): AudioData {
        val sampleRate = 44100
        // We will rely on RMS (volume) detection since we are using a complex audio file (beep)
        // rather than a pure sine wave, making simple frequency detection less reliable without FFT.
        
        // Save original volume
        val originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        
        var mediaPlayer: android.media.MediaPlayer? = null
        
        try {
            // Set volume to 80% for test to ensure microphone picks it up
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.8).toInt(), 0)
            
            // Initialize AudioRecord (Microphone)
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
            
            // Prepare MediaPlayer with the resource file
            // Note: R.raw.test_audio must exist. 
            // We use the context passed to the class.
            // Dynamically get resource ID to avoid import issues if R class isn't regenerated yet in IDE context
            val resId = context.resources.getIdentifier("test_audio", "raw", context.packageName)
            mediaPlayer = android.media.MediaPlayer.create(context, resId)
            
            // Start recording
            audioRecord.startRecording()
            delay(100) // Small delay to stabilize
            
            // Play beep
            mediaPlayer?.start()
            
            // Record audio while playing
            // The beep is short, we record for ~2 seconds or until it stops
            val numSamples = sampleRate * 2 // 2 seconds buffer
            val recordedData = ShortArray(numSamples)
            var totalRead = 0
            val startTime = System.currentTimeMillis()
            
            while (totalRead < numSamples && (mediaPlayer?.isPlaying == true || System.currentTimeMillis() - startTime < 2000)) {
                val read = audioRecord.read(recordedData, totalRead, minOf(bufferSize, numSamples - totalRead))
                if (read > 0) {
                    totalRead += read
                } else {
                    break
                }
                delay(10) // Small yield
            }
            
            // Stop and release
            try {
                if (mediaPlayer?.isPlaying == true) mediaPlayer?.stop()
            } catch (e: Exception) { /* Ignore if already stopped */ }
            mediaPlayer?.release()
            mediaPlayer = null
            
            audioRecord.stop()
            audioRecord.release()
            
            // Restore volume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            
            // Analyze recorded audio
            val rms = calculateRMS(recordedData, totalRead)
            
            // Threshold for "hearing itself". 
            // In a quiet room, RMS is usually < 50. 
            // A loud beep loops back significantly higher, usually > 200-500 depending on gain.
            // We set a conservative threshold.
            val micWorking = rms > 100 
            
            // If mic 'heard' the sound (RMS > threshold), we assume purely that the speaker 
            // produced it and the mic captured it. 
            // Validation of exact signal content is complex without FFT/Correlation.
            val speakerWorking = micWorking
            
            Log.d("AudioCollector", "Audio test - RMS: $rms")
            
            return AudioData(
                speakerWorking = speakerWorking,
                microphoneWorking = micWorking,
                audioTestPassed = speakerWorking && micWorking,
                speakerVolume = originalVolume,
                micSensitivity = rms,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            // Restore volume on error
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            mediaPlayer?.release()
            throw e
        }
    }
    
    private fun calculateRMS(data: ShortArray, length: Int): Float {
        var sum = 0.0
        for (i in 0 until length) {
            sum += (data[i] * data[i]).toDouble()
        }
        return sqrt(sum / length).toFloat()
    }
    
    private fun detectFrequency(data: ShortArray, length: Int, sampleRate: Int): Int {
        // Simple zero-crossing method for frequency detection
        var zeroCrossings = 0
        for (i in 1 until length) {
            if ((data[i-1] < 0 && data[i] >= 0) || (data[i-1] >= 0 && data[i] < 0)) {
                zeroCrossings++
            }
        }
        val durationSeconds = length.toFloat() / sampleRate
        return (zeroCrossings / (2 * durationSeconds)).toInt()
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
