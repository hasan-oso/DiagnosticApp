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
package com.diagnostics.app.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.diagnostics.app.data.collectors.PreflightChecker
import com.diagnostics.app.data.collectors.StressTestEngine
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.repository.DataCollectionRepository
import com.diagnostics.app.services.StressTestService

import com.diagnostics.app.data.quality.ScanQualityScorer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

sealed class ScanState {
    object Idle : ScanState()
    data class Collecting(val progress: Float) : ScanState()
    data class PreflightCheck(val message: String) : ScanState()
    data class StressTesting(
        val cpuProgress: Float,
        val storageProgress: Float,
        val batteryProgress: Float
    ) : ScanState()
    data class Uploading(val progress: Float) : ScanState()
    data class Complete(val data: DeviceScanData) : ScanState()
    data class Error(val message: String) : ScanState()
}

/**
 * ScanViewModel — orchestrates the full device diagnostic scan.
 *
 * Uses AndroidViewModel to safely access applicationContext (fixes memory leak).
 * Binds to StressTestService to keep the process alive during long tests.
 * Wraps each stress test in withTimeout() to prevent infinite hangs.
 */
class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context
        get() = getApplication<Application>().applicationContext

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val preflightChecker by lazy { PreflightChecker(context) }
    private val dataCollectionRepo by lazy { DataCollectionRepository(context) }
    private val stressTestEngine by lazy { StressTestEngine(context) }

    private var stressTestService: StressTestService? = null
    private var serviceConnection: ServiceConnection? = null
    private var isServiceBound = false

    private val json = kotlinx.serialization.json.Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private const val TAG = "DIAGNOSTICS"
        private const val CPU_TIMEOUT_MS = 10L * 60 * 1000      // 10 minutes
        private const val STORAGE_TIMEOUT_MS = 5L * 60 * 1000   // 5 minutes
        private const val BATTERY_TIMEOUT_MS = 10L * 60 * 1000  // 10 minutes
    }

    fun startScan() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== SCAN STARTED ===")

                // Step 1: Start Foreground Service (prevent kill)
                startStressTestService()

                // Step 2: Preflight Safety Checks
                _scanState.value = ScanState.PreflightCheck("Checking device safety...")
                Log.d(TAG, "Step 1: Running preflight checks...")
                val preflightResult = preflightChecker.performPreflightChecks()

                if (!preflightResult.canProceed) {
                    Log.d(TAG, "Preflight FAILED: ${preflightResult.errors}")
                    _scanState.value = ScanState.Error(
                        "Cannot proceed:\n${preflightResult.errors.joinToString("\n")}"
                    )
                    stopStressTestService()
                    return@launch
                }

                Log.d(TAG, "Preflight PASSED")
                stressTestService?.updateProgress("Collecting device data...", 5)

                // Step 3: Data Collection
                Log.d(TAG, "Step 2: Starting data collection...")
                _scanState.value = ScanState.Collecting(0.3f)
                var scanData = dataCollectionRepo.collectAllData()
                Log.d(TAG, "Data collection COMPLETE")
                _scanState.value = ScanState.Collecting(1.0f)
                kotlinx.coroutines.delay(500)

                // Step 4: Stress Tests (with timeouts)
                Log.d(TAG, "Step 3: Starting stress tests...")
                _scanState.value = ScanState.StressTesting(0.0f, 0.0f, 0.0f)
                kotlinx.coroutines.delay(500)

                // Battery Stress Test
                Log.d(TAG, "Starting Battery stress test...")
                stressTestService?.updateProgress("Running battery stress test...", 15)
                val batteryResult = try {
                    withTimeout(BATTERY_TIMEOUT_MS) {
                        stressTestEngine.runBatteryStressTest(
                            onProgress = { progress ->
                                _scanState.value = ScanState.StressTesting(0.0f, 0.0f, progress)
                                stressTestService?.updateProgress(
                                    "Battery test: ${(progress * 100).toInt()}%",
                                    15 + (progress * 25).toInt()
                                )
                            },
                            context = context,
                            deviceAgeYears = scanData.deviceAgeYears,
                            designCapacityMah = scanData.batteryDesignCapacityMah
                        )
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Battery stress test TIMED OUT after ${BATTERY_TIMEOUT_MS/1000}s")
                    null
                }

                // CPU Stress Test
                Log.d(TAG, "Starting CPU stress test...")
                stressTestService?.updateProgress("Running CPU stress test...", 40)
                val cpuResult = try {
                    withTimeout(CPU_TIMEOUT_MS) {
                        stressTestEngine.runCpuStressTest(
                            onProgress = { progress ->
                                _scanState.value = ScanState.StressTesting(progress, 0.0f, 1.0f)
                                stressTestService?.updateProgress(
                                    "CPU test: ${(progress * 100).toInt()}%",
                                    40 + (progress * 30).toInt()
                                )
                            },
                            quickBenchmarkScore = scanData.quickBenchmarkScore,
                            deviceAgeYears = scanData.deviceAgeYears
                        )
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "CPU stress test TIMED OUT after ${CPU_TIMEOUT_MS/1000}s")
                    null
                }

                // Storage Stress Test
                Log.d(TAG, "Starting Storage stress test...")
                stressTestService?.updateProgress("Running storage stress test...", 70)
                val storageResult = try {
                    withTimeout(STORAGE_TIMEOUT_MS) {
                        stressTestEngine.runStorageStressTest { progress ->
                            _scanState.value = ScanState.StressTesting(1.0f, progress, 1.0f)
                            stressTestService?.updateProgress(
                                "Storage test: ${(progress * 100).toInt()}%",
                                70 + (progress * 20).toInt()
                            )
                        }
                    }
                } catch (e: TimeoutCancellationException) {
                    Log.w(TAG, "Storage stress test TIMED OUT after ${STORAGE_TIMEOUT_MS/1000}s")
                    null
                }

                // Step 5: Merge stress results into scan data
                stressTestService?.updateProgress("Finalizing results...", 95)
                val scanEndTime = System.currentTimeMillis()
                val totalDurationSeconds = ((scanEndTime - scanData.scanTimestamp) / 1000).toInt()

                scanData = scanData.copy(
                    cpuStressResult = cpuResult ?: scanData.cpuStressResult,
                    storageStressResult = storageResult ?: scanData.storageStressResult,
                    batteryStressResult = batteryResult ?: scanData.batteryStressResult,
                    scanDurationSeconds = totalDurationSeconds,

                    // Adaptive Stress Info
                    quickBenchmarkScore = cpuResult?.quickBenchmarkScore ?: scanData.quickBenchmarkScore,
                    devicePerformanceTier = cpuResult?.devicePerformanceTier ?: scanData.devicePerformanceTier,
                    cpuTestIterations = cpuResult?.cpuTestIterations ?: scanData.cpuTestIterations,
                    storageTestFileCount = storageResult?.storageTestFileCount ?: scanData.storageTestFileCount,

                    // Precise battery drain (from BatteryStressResult)
                    actualDrainMah = batteryResult?.actualDrainMah ?: scanData.actualDrainMah,
                    actualDrainPercent = batteryResult?.actualDrainPercent ?: scanData.actualDrainPercent,
                    chargeCounterStartUah = batteryResult?.chargeCounterStartUah ?: scanData.chargeCounterStartUah,
                    chargeCounterEndUah = batteryResult?.chargeCounterEndUah ?: scanData.chargeCounterEndUah,
                    batteryTempStartCelsius = batteryResult?.batteryTempStartCelsius ?: scanData.batteryTempStartCelsius,
                    batteryTempEndCelsius = batteryResult?.batteryTempEndCelsius ?: scanData.batteryTempEndCelsius,
                    batteryCycleCount = batteryResult?.batteryCycleCount ?: scanData.batteryCycleCount,
                    batteryHealthStatus = batteryResult?.batteryHealthStatus ?: scanData.batteryHealthStatus,
                    batteryDrainPercent = batteryResult?.batteryDrainPercent ?: scanData.batteryDrainPercent,
                    batteryDrainRatePerMinute = batteryResult?.batteryDrainRatePerMinute ?: scanData.batteryDrainRatePerMinute,
                    batteryLevelStart = batteryResult?.batteryLevelStart ?: scanData.batteryLevelStart,
                    batteryLevelEnd = batteryResult?.batteryLevelEnd ?: scanData.batteryLevelEnd,
                    isChargingStart = batteryResult?.isChargingStart ?: scanData.isChargingStart,
                    isChargingEnd = batteryResult?.isChargingEnd ?: scanData.isChargingEnd,
                    chargingDetectedMidTest = batteryResult?.chargingDetectedMidTest ?: scanData.chargingDetectedMidTest
                )

                // Step 6: Score data quality
                val qualityScorer = ScanQualityScorer()
                val quality = qualityScorer.scoreScan(scanData)
                Log.d(TAG, "Scan quality: ${quality.overallQuality} (${quality.confidenceScore}/100)")

                // Update battery stress result with quality info
                scanData = scanData.copy(
                    batteryStressResult = scanData.batteryStressResult.copy(
                        dataQuality = quality
                    )
                )

                // Always save locally as backup
                saveScanDataLocally(scanData)

                stressTestService?.updateProgress("Scan complete!", 100)
                _scanState.value = ScanState.Complete(scanData)

                // Stop foreground service
                kotlinx.coroutines.delay(1000)
                stopStressTestService()

            } catch (e: Exception) {
                Log.e(TAG, "Scan failed: ${e.message}", e)
                _scanState.value = ScanState.Error(e.message ?: "Unknown error")
                stopStressTestService()
            }
        }
    }

    fun cancelScan() {
        stressTestService?.cancelTests()
        stopStressTestService()
        _scanState.value = ScanState.Idle
    }

    // ═══════════════════════════════════════════════
    // FOREGROUND SERVICE MANAGEMENT
    // ═══════════════════════════════════════════════

    private fun startStressTestService() {
        try {
            val intent = Intent(context, StressTestService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val binder = service as StressTestService.LocalBinder
                    stressTestService = binder.getService()
                    isServiceBound = true
                    Log.d(TAG, "StressTestService bound successfully")
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    stressTestService = null
                    isServiceBound = false
                    Log.d(TAG, "StressTestService disconnected")
                }
            }

            serviceConnection = connection
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start foreground service, continuing without it: ${e.message}")
        }
    }

    private fun stopStressTestService() {
        try {
            if (isServiceBound) {
                serviceConnection?.let { context.unbindService(it) }
                isServiceBound = false
            }
            stressTestService?.onTestsComplete()
            stressTestService = null
            serviceConnection = null
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping service: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    // LOCAL SAVE
    // ═══════════════════════════════════════════════

    private fun saveScanDataLocally(data: DeviceScanData) {
        try {
            val jsonString = json.encodeToString(DeviceScanData.serializer(), data)
            val fileName = "diagnostic_scan_${System.currentTimeMillis()}.json"
            val file = java.io.File(context.getExternalFilesDir(null), fileName)
            file.writeText(jsonString)
            Log.d(TAG, "SAVED LOCALLY: ${file.absolutePath}")

            // Show toast on main thread
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "Saved to: ${file.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save locally", e)
        }
    }

    override fun onCleared() {
        cancelScan()
        super.onCleared()
    }
}
