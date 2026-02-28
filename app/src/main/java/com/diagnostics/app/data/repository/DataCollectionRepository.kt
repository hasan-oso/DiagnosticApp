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
package com.diagnostics.app.data.repository

import android.content.Context
import com.diagnostics.app.data.collectors.*
import com.diagnostics.app.data.models.*
import com.diagnostics.app.data.DeviceReleaseDatabase
import com.diagnostics.app.data.collectors.CPUInfoCollector
import com.diagnostics.app.data.collectors.DisplayInfoCollector
import com.diagnostics.app.benchmark.QuickBenchmark
import com.diagnostics.app.utils.Constants
import java.util.UUID
import org.json.JSONObject
import org.json.JSONArray
import com.diagnostics.app.data.repository.FirestoreRepository
import android.util.Log
import com.diagnostics.app.data.storage.ScanStorageManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.BackoffPolicy
import androidx.work.workDataOf
import com.diagnostics.app.workers.UploadWorker
import java.util.concurrent.TimeUnit

class DataCollectionRepository(private val context: Context) {
    
    private val firestoreRepository = FirestoreRepository()
    private val storageManager = ScanStorageManager(context)
    
    private val identityCollector = IdentityCollector()
    private val securityCollector = SecurityCollector(context)
    private val hardwareCollector = HardwareCollector(context)
    private val memoryCollector = MemoryCollector(context)
    private val displayCollector = DisplayCollector(context)
    private val cameraCollector = CameraCollector(context) // ✅ Now with session opening
    private val batteryCollector = BatteryCollector(context) // ✅ Now with BroadcastReceiver
    private val connectivityCollector = ConnectivityCollector(context)
    private val performanceCollector = PerformanceCollector(context)
    private val thermalCollector = ThermalCollector(context) // ✅ Now with BroadcastReceiver
    private val sensorCollector = SensorCollector(context) // ✅ NEW: Dedicated sensor collector
    private val audioCollector = AudioCollector(context) // ✅ NEW: Audio collector
    private val stressTestEngine = StressTestEngine(context)

    
    suspend fun collectAllData(): DeviceScanData {
        val startTime = System.currentTimeMillis()
        
        // Capture battery level at start
        val batteryLevelStart = batteryCollector.getBatteryLevel()
        val isChargingAtStart = batteryCollector.isCharging()
        
        // Collect from all collectors
        val identity = identityCollector.collectIdentityData()
        val security = securityCollector.collectSecurityData()
        val hardware = hardwareCollector.collectHardwareData()
        val memory = memoryCollector.collectMemoryData()
        val display = displayCollector.collectDisplayData()
        val camera = cameraCollector.collectCameraData() // ✅ Now with camera session
        val battery = batteryCollector.collectBatteryData() // ✅ Now with BroadcastReceiver
        
        // Check for multi-cell battery (likely if capacity > 4000 mAh and we are on a flagship)
        // But strictly, we check the capacity itself to see if it makes sense
        val isMultiCell = battery.designCapacityMah > 4000 // Simple heuristic or derived from collector if we updated BatteryData
        val connectivity = connectivityCollector.collectConnectivityData()
        val performance = performanceCollector.collectPerformanceData()
        val thermal = thermalCollector.collectThermalData() // ✅ Now with BroadcastReceiver
        val sensors = sensorCollector.collectSensorData() // ✅ NEW: Sensor enumeration
        val audio = audioCollector.collectAudioData() // ✅ NEW: Audio loopback test
        
        // Calculate device age
        val resistance = getLastMeasuredResistance()
        val (deviceAge, ageConfidence) = com.diagnostics.app.utils.DeviceAgeEstimator.estimateDeviceAgeYears(context)
        val ageCategory = getAgeCategory(deviceAge)

        // Benchmarking & Performance Tier
        val quickBenchmarkScore = com.diagnostics.app.benchmark.QuickBenchmark.getCPUScore()
        val deviceTier = com.diagnostics.app.benchmark.QuickBenchmark.getDeviceTier(quickBenchmarkScore)

        // CPU extended info
        val cpuGovernor = CPUInfoCollector.getCPUGovernor()
        val (cpuBigCores, cpuLittleCores) = CPUInfoCollector.getCoreSplit()

        // GPU renderer & screen refresh rate
        val gpuRendererDynamic = DisplayInfoCollector.getGPURenderer(context)
        val screenRefreshRateDynamic = DisplayInfoCollector.getRefreshRate(context)
        
        // ── STRESS TESTS MOVED TO VIEWMODEL ────────────────────────────────
        // We do NOT run stress tests here to avoid blocking the data collection phase.
        // These will be run in Step 3 by ScanViewModel.
        
        // 1. CPU Stress Test (Placeholder)
        val cpuStressResult = CpuStressResult(
            durationSeconds = 0,
            maxTempCelsius = 0f,
            throttlingStartSec = null,
            performanceDegradationPercent = 0f,
            singleCoreScore = 0,
            multiCoreScore = 0,
            quickBenchmarkScore = quickBenchmarkScore,
            devicePerformanceTier = deviceTier.toString(),
            cpuTestIterations = 0,
            tempReadings = emptyList(),
            freqReadings = emptyList()
        )
        
        // 2. Storage Stress Test (Placeholder)
        val storageStressResult = StorageStressResult(
            sequentialWriteMbps = 0f,
            sequentialReadMbps = 0f,
            random4kReadIops = 0,
            random4kWriteIops = 0,
            slcCacheCrash = false,
            detectedType = "UNKNOWN",
            storageTestFileCount = 0,
            writeSpeedCurve = emptyList()
        )
        
        // 3. Battery Stress Test (Placeholder)
        val batteryStressResult = BatteryStressResult(
            durationSeconds = 0,
            initialVoltageMv = 0,
            loadVoltageMv = 0,
            voltageSagMv = 0,
            peakCurrentMa = 0,
            internalResistanceMohm = 0f,
            estimatedHealthPercent = 0,
            voltageReadings = emptyList()
        )
        
        val endTime = System.currentTimeMillis()
        val scanDuration = ((endTime - startTime) / 1000).toInt()
        
        // Capture battery level at end
        val batteryLevelEnd = batteryCollector.getBatteryLevel()
        val isChargingDuringScan = batteryCollector.isCharging() || isChargingAtStart
        
        // Calculate drain
        val batteryDrain = if (batteryLevelStart >= batteryLevelEnd) batteryLevelStart - batteryLevelEnd else 0
        val scanDurationMinutes = scanDuration / 60f
        val drainRate = if (scanDurationMinutes > 0 && !isChargingDuringScan) {
            batteryDrain / scanDurationMinutes
        } else {
            0f
        }
        
        // Aggregate all data into DeviceScanData
        return DeviceScanData(
            // Identity (12 fields)
            deviceBrand = identity.deviceBrand,
            deviceModel = identity.deviceModel,
            deviceManufacturer = identity.deviceManufacturer,
            boardPlatform = identity.boardPlatform,
            buildFingerprint = identity.buildFingerprint,
            buildId = identity.buildId,
            buildDateUtc = identity.buildDateUtc,
            androidVersion = identity.androidVersion,
            androidSdk = identity.androidSdk,
            kernelVersion = identity.kernelVersion,
            securityPatchDate = identity.securityPatchDate,
            buildTags = identity.buildTags,
            
            // Security (9 fields)
            playServicesAvailable = security.playServicesAvailable,
            playIntegrityVerdict = security.deviceIntegrityVerdict,
            safetyNetBasicIntegrity = security.deviceIntegrityVerdict == "GENUINE",
            unknownSourcesCount = security.unknownSourcesCount,
            developerOptionsEnabled = security.developerOptionsEnabled,
            adbEnabled = security.adbEnabled,
            usbDebuggingEnabled = security.usbDebuggingEnabled,
            deviceAdminCount = 0, // Would require DevicePolicyManager
            verifiedBootState = security.verifiedBootState,
            
            // CPU/GPU (10 fields)
            cpuArchitecture = hardware.cpuArchitecture,
            supportedAbis = hardware.supportedAbis,
            cpuCoreCount = hardware.cpuCoreCount,
            cpuMaxFreqMhz = hardware.cpuMaxFreqMhz,
            cpuMinFreqMhz = hardware.cpuMinFreqMhz,
            gpuRenderer = gpuRendererDynamic.takeIf { it != "unknown" } ?: hardware.gpuRenderer,
            gpuVendor = hardware.gpuVendor,
            openglVersion = hardware.openglVersion,
            vulkanVersion = hardware.vulkanVersion,
            npuAvailable = hardware.npuAvailable,
            
            // Memory/Storage (8 fields)
            ramTotalMb = memory.ramTotalMb,
            ramAvailableMb = memory.ramAvailableMb,
            ramLowThresholdMb = memory.ramLowThresholdMb,
            storageTotalGb = memory.storageTotalGb,
            storageAvailableGb = memory.storageAvailableGb,
            externalStorageAvailable = memory.externalStorageAvailable,
            adoptableStorageEnabled = memory.adoptableStorageEnabled,
            storageEmulated = memory.storageEmulated,
            
            // Display (8 fields)
            screenWidthPx = display.screenWidthPx,
            screenHeightPx = display.screenHeightPx,
            screenDensityDpi = display.screenDensityDpi,
            screenRefreshRateHz = screenRefreshRateDynamic.takeIf { it > 0f } ?: display.screenRefreshRateHz,
            screenRefreshRatesSupported = display.screenRefreshRatesSupported,
            displayHdrCapabilities = display.displayHdrCapabilities,
            wideColorGamutSupport = display.wideColorGamutSupport,
            screenBrightnessMode = display.screenBrightnessMode,
            
            // Camera/Sensors (7 fields) - ✅ Now separated
            cameraCount = camera.cameraCount,
            backCameraResolutionMp = camera.backCameraMP,
            frontCameraResolutionMp = camera.frontCameraMP,
            cameraFlashAvailable = camera.hasFlash,
            cameraOisAvailable = camera.hasOIS,
            sensorCountTotal = sensors.sensorCountTotal,
            sensorTypesList = sensors.sensorTypesList,
            
            // Audio Hardware (3 fields)
            speakerWorking = audio.speakerWorking,
            microphoneWorking = audio.microphoneWorking,
            audioTestPassed = audio.audioTestPassed,
            
            // Battery (6 fields) - ✅ Now with multi-fallback
            batteryDesignCapacityMah = battery.designCapacityMah,
            batteryMultiCellDetected = isMultiCell,
            batteryHealthStatus = battery.healthStatus,
            batteryTechnology = battery.technology,
            batteryTempCelsius = battery.tempCelsius,
            batteryVoltageMv = battery.voltageMv,
            batteryCurrentNowMa = battery.currentNowMa,
            
            // Connectivity (7 fields)
            wifiAvailable = connectivity.wifiAvailable,
            bluetoothVersion = connectivity.bluetoothVersion,
            nfcAvailable = connectivity.nfcAvailable,
            mobileNetworkType = connectivity.mobileNetworkType,
            dualSimSupport = connectivity.dualSimSupport,
            esimSupport = connectivity.esimSupport,
            usbVersion = connectivity.usbVersion,
            
            // Performance/Thermal (15 fields)
            systemUptimeHours = performance.systemUptimeHours,
            runningProcessesCount = performance.runningProcessesCount,
            runningServicesCount = performance.runningServicesCount,
            installedPackagesCount = performance.installedPackagesCount,
            systemAppsCount = performance.systemAppsCount,
            userAppsCount = performance.userAppsCount,
            cpuTempCelsius = thermal.cpuTempCelsius, // ✅ Battery proxy
            thermalThrottlingStatus = thermal.thermalThrottlingStatus,
            cpuUsagePercent = thermal.cpuUsagePercent,
            memoryPressureLevel = thermal.memoryPressureLevel,
            anrCountRecent = thermal.anrCountRecent,
            crashCountRecent = thermal.crashCountRecent,
            bootloopIndicator = thermal.bootloopIndicator,
            
            // Device Age (2 fields)
            deviceAgeYears = deviceAge,
            deviceAgeConfidence = ageConfidence,
            deviceAgeCategory = ageCategory,
            quickBenchmarkScore = quickBenchmarkScore,
            devicePerformanceTier = deviceTier.toString(),

            // CPU Extended Info
            cpuGovernor = cpuGovernor,
            cpuBigCores = cpuBigCores,
            cpuLittleCores = cpuLittleCores,

            // Stress Test Results (3 objects)
            cpuStressResult = cpuStressResult,
            storageStressResult = storageStressResult,
            batteryStressResult = batteryStressResult,

            // Metadata (4 fields)
            scanTimestamp = startTime,
            scanDurationSeconds = scanDuration,
            appVersion = Constants.APP_VERSION,
            scanId = UUID.randomUUID().toString(),

            // Battery Drain Analysis (rounded %)
            batteryLevelStart = batteryLevelStart,
            batteryLevelEnd = batteryLevelEnd,
            batteryDrainPercent = batteryDrain,
            batteryDrainRatePerMinute = drainRate,
            isChargingDuringScan = isChargingDuringScan,

            // Precise battery drain (from BatteryStressResult)
            chargeCounterStartUah = batteryStressResult.chargeCounterStartUah,
            chargeCounterEndUah = batteryStressResult.chargeCounterEndUah,
            actualDrainMah = batteryStressResult.actualDrainMah,
            actualDrainPercent = if (battery.designCapacityMah > 0)
                (batteryStressResult.actualDrainMah / battery.designCapacityMah) * 100f
            else 0f,
            batteryTempStartCelsius = batteryStressResult.batteryTempStartCelsius,
            batteryTempEndCelsius = batteryStressResult.batteryTempEndCelsius,
            batteryCycleCount = batteryStressResult.batteryCycleCount,
            isChargingStart = batteryStressResult.isChargingStart,
            isChargingEnd = batteryStressResult.isChargingEnd,
            chargingDetectedMidTest = batteryStressResult.chargingDetectedMidTest
        )
    }
    
    // Device age is now calculated via DeviceReleaseDatabase.getDeviceAge()
    // (kept for reference; no longer called)
    
    private fun getAgeCategory(ageYears: Float): String {
        return when {
            ageYears < 1 -> "0-1yr"
            ageYears < 2 -> "1-2yr"
            ageYears < 3 -> "2-3yr"
            ageYears < 4 -> "3-4yr"
            ageYears < 5 -> "4-5yr"
            else -> "5+yr"
        }
    }

    private fun getLastMeasuredResistance(): Int {
        return try {
            val prefs = context.getSharedPreferences("battery_test_cache", Context.MODE_PRIVATE)
            prefs.getInt("last_resistance_mohm", 0)
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Upload scan with offline support
     * Phase 1: Save locally (always succeeds)
     * Phase 2: Queue background upload (with retry)
     */
    suspend fun uploadScanData(scanData: DeviceScanData): Boolean {
        return try {
            Log.d("DataCollectionRepo", "═══════════════════════════════════")
            Log.d("DataCollectionRepo", "Starting upload process")
            Log.d("DataCollectionRepo", "Scan ID: ${scanData.scanId}")
            Log.d("DataCollectionRepo", "Device: ${scanData.deviceModel}")
            
            // STEP 1: Save locally FIRST (always succeeds, even offline)
            Log.d("DataCollectionRepo", "Step 1: Saving locally...")
            val savedLocally = storageManager.savePending(scanData)
            
            if (!savedLocally) {
                Log.e("DataCollectionRepo", "❌ Failed to save locally!")
                return false
            }
            
            Log.d("DataCollectionRepo", "✅ Saved locally to pending/")
            Log.d("DataCollectionRepo", "Pending count: ${storageManager.getPendingCount()}")
            
            // STEP 2: Queue background upload job
            Log.d("DataCollectionRepo", "Step 2: Queueing upload job...")
            queueUploadJob(scanData.scanId)
            
            Log.d("DataCollectionRepo", "✅ Upload job queued")
            Log.d("DataCollectionRepo", "WorkManager will retry automatically if needed")
            Log.d("DataCollectionRepo", "═══════════════════════════════════")
            
            // Return true - data is safe locally
            // Upload will happen in background (with retry)
            true
            
        } catch (e: Exception) {
            Log.e("DataCollectionRepo", "❌ Error: ${e.message}", e)
            false
        }
    }

    /**
     * Queue background upload job with WorkManager
     */
    private fun queueUploadJob(scanId: String) {
        try {
            // Network constraint - only upload when connected
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            // Create upload work request
            val uploadRequest = OneTimeWorkRequestBuilder<UploadWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf("scan_id" to scanId))
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES  // Start with 15 min, doubles each retry
                )
                .build()
            
            // Enqueue with unique work (prevents duplicates)
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "upload_$scanId",
                    ExistingWorkPolicy.KEEP,  // Keep existing work, don't replace
                    uploadRequest
                )
            
            Log.d("DataCollectionRepo", "Upload job queued: upload_$scanId")
            
        } catch (e: Exception) {
            Log.e("DataCollectionRepo", "Failed to queue upload job: ${e.message}", e)
        }
    }

    /**
     * Manual sync - retry all pending uploads
     */
    suspend fun syncPendingScans(): Int {
        return try {
            Log.d("DataCollectionRepo", "Starting manual sync...")
            
            val pending = storageManager.getPendingScans()
            Log.d("DataCollectionRepo", "Found ${pending.size} pending scans")
            
            pending.forEach { scan ->
                queueUploadJob(scan.scanId)
            }
            
            Log.d("DataCollectionRepo", "✅ Queued ${pending.size} upload jobs")
            pending.size
            
        } catch (e: Exception) {
            Log.e("DataCollectionRepo", "Sync failed: ${e.message}", e)
            0
        }
    }

    /**
     * Get pending scans count
     */
    fun getPendingCount(): Int {
        return storageManager.getPendingCount()
    }

    /**
     * Update scan with human labels (Phase 2)
     */
    suspend fun updateScanWithLabels(scanId: String, humanLabels: String): Boolean {
        return try {
            Log.d("DataCollectionRepo", "Updating scan with labels: $scanId")
            
            val success = firestoreRepository.updateScanWithLabels(scanId, humanLabels)
            
            if (success) {
                Log.d("DataCollectionRepo", "✅ Labels updated!")
            } else {
                Log.e("DataCollectionRepo", "❌ Labels update failed!")
            }
            
            success
        } catch (e: Exception) {
            Log.e("DataCollectionRepo", "Update error: ${e.message}", e)
            false
        }
    }
}
