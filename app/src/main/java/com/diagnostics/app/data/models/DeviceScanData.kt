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
package com.diagnostics.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DeviceScanData(
    // ===== Identity & Authentication (12) =====
    val deviceBrand: String,                    // Build.BRAND
    val deviceModel: String,                    // Build.MODEL
    val deviceManufacturer: String,             // Build.MANUFACTURER
    val boardPlatform: String,                  // Build.BOARD
    val buildFingerprint: String,               // Build.FINGERPRINT
    val buildId: String,                        // Build.ID
    val buildDateUtc: Long,                     // Build.TIME
    val androidVersion: String,                 // Build.VERSION.RELEASE
    val androidSdk: Int,                        // Build.VERSION.SDK_INT
    val kernelVersion: String,                  // System.getProperty("os.version")
    val securityPatchDate: String,              // Build.VERSION.SECURITY_PATCH
    val buildTags: String,                      // Build.TAGS
    
    // ===== Security & Integrity (9) =====
    val playServicesAvailable: Boolean,
    val playIntegrityVerdict: String,           // من Play Integrity API
    val safetyNetBasicIntegrity: Boolean,
    val unknownSourcesCount: Int,
    val developerOptionsEnabled: Boolean,
    val adbEnabled: Boolean,
    val usbDebuggingEnabled: Boolean,
    val deviceAdminCount: Int,
    val verifiedBootState: String,
    
    // ===== CPU/GPU (10) =====
    val cpuArchitecture: String,                // Build.SUPPORTED_ABIS[0]
    val supportedAbis: List<String>,            // Build.SUPPORTED_ABIS.toList()
    val cpuCoreCount: Int,                      // Runtime.getRuntime().availableProcessors()
    val cpuMaxFreqMhz: Int,
    val cpuMinFreqMhz: Int,
    val gpuRenderer: String,                    // من GLES20
    val gpuVendor: String,
    val openglVersion: String,
    val vulkanVersion: String,
    val npuAvailable: Boolean,
    
    // ===== Memory/Storage (8) =====
    val ramTotalMb: Long,
    val ramAvailableMb: Long,
    val ramLowThresholdMb: Long,
    val storageTotalGb: Long,
    val storageAvailableGb: Long,
    val externalStorageAvailable: Boolean,
    val adoptableStorageEnabled: Boolean,
    val storageEmulated: Boolean,
    
    // ===== Display (8) =====
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val screenDensityDpi: Int,
    val screenRefreshRateHz: Float,
    val screenRefreshRatesSupported: List<Float>,
    val displayHdrCapabilities: List<String>,
    val wideColorGamutSupport: Boolean,
    val screenBrightnessMode: String,
    
    // ===== Camera/Sensors (7) =====
    val cameraCount: Int,
    val backCameraResolutionMp: Float,
    val frontCameraResolutionMp: Float,
    val cameraFlashAvailable: Boolean,
    val cameraOisAvailable: Boolean,
    val sensorCountTotal: Int,
    val sensorTypesList: List<String>,
    
    // ===== Battery (6) =====
    val batteryDesignCapacityMah: Int,
    val batteryHealthStatus: String,
    val batteryTechnology: String,
    val batteryTempCelsius: Float,
    val batteryVoltageMv: Int,
    val batteryCurrentNowMa: Int,
    val batteryMultiCellDetected: Boolean = false,
    
    // ===== Connectivity (7) =====
    val wifiAvailable: Boolean,
    val bluetoothVersion: String,
    val nfcAvailable: Boolean,
    val mobileNetworkType: String,
    val dualSimSupport: Boolean,
    val esimSupport: Boolean,
    val usbVersion: String,
    
    // ===== Performance (6) =====
    val systemUptimeHours: Float,
    val runningProcessesCount: Int,
    val runningServicesCount: Int,
    val installedPackagesCount: Int,
    val systemAppsCount: Int,
    val userAppsCount: Int,
    
    // ===== Thermal/Stress (7) =====
    val cpuTempCelsius: Float?,
    val thermalThrottlingStatus: String,
    val cpuUsagePercent: Float,
    val memoryPressureLevel: String,
    val anrCountRecent: Int,
    val crashCountRecent: Int,
    val bootloopIndicator: Boolean,
    
    // ===== Device Age (2) =====
    val deviceAgeYears: Float,                  // From release date database (or Build.TIME fallback)
    val deviceAgeConfidence: Float? = null,
    val deviceAgeCategory: String,              // "0-1yr", "1-2yr", etc.

    // ===== Adaptive Stress Info =====
    val quickBenchmarkScore: Int = 0,
    val devicePerformanceTier: String = "",
    val cpuTestIterations: Long = 0,
    val storageTestFileCount: Int = 0,

    // ===== CPU Extended Info =====
    val cpuGovernor: String = "unknown",
    val cpuBigCores: Int = 0,
    val cpuLittleCores: Int = 0,
    
    // ===== Stress Test Results =====
    val cpuStressResult: CpuStressResult,
    val storageStressResult: StorageStressResult,
    val batteryStressResult: BatteryStressResult,
    
    // ===== Audio Hardware (3) =====
    val speakerWorking: Boolean = false,
    val microphoneWorking: Boolean = false,
    val audioTestPassed: Boolean = false,

    // ===== Human Labels (سيتم ملؤها من الخبير) =====
    // ===== Human Labels (سيتم ملؤها من الخبير) =====
    val humanLabels: String? = null,
    val labelStatus: String = "UNLABELED",
    val labelTimestamp: Long? = null,

    // ===== Battery Drain Analysis =====
    val batteryLevelStart: Int = 0,              // 0-100% (rounded)
    val batteryLevelEnd: Int = 0,                // 0-100% (rounded)
    val batteryDrainPercent: Int = 0,            // Level difference (rounded)
    val batteryDrainRatePerMinute: Float = 0f,   // %/min (rounded)
    val isChargingDuringScan: Boolean = false,

    // ===== Precise Battery Drain (charge_counter) =====
    val chargeCounterStartUah: Long = -1,        // µAh at test start
    val chargeCounterEndUah: Long = -1,          // µAh at test end
    val actualDrainMah: Float = 0f,              // Precise mAh drained
    val actualDrainPercent: Float = 0f,          // Precise % drained
    val batteryTempStartCelsius: Float = -1f,    // °C at test start
    val batteryTempEndCelsius: Float = -1f,      // °C at test end
    val batteryCycleCount: Int = -1,             // Charge cycles (-1 = unavailable)
    val isChargingStart: Boolean = false,
    val isChargingEnd: Boolean = false,
    val chargingDetectedMidTest: Boolean = false,
    
    // ===== Metadata =====
    val scanTimestamp: Long,
    val scanDurationSeconds: Int,
    val appVersion: String,
    val scanId: String,                         // UUID

    // Metadata لتحليل البيانات المستقبلي
    val data_collection_metadata: com.diagnostics.app.domain.models.DataCollectionMetadata? = null
)
