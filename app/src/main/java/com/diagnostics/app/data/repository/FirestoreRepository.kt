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

import android.util.Log
import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.BatteryStressResult
import com.diagnostics.app.data.models.CpuStressResult
import com.diagnostics.app.data.models.StorageStressResult
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FirestoreRepository {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    companion object {
        private const val COLLECTION_SCANS = "device_scans"
        private const val TAG = "FirestoreRepo"
    }
    
    /**
     * Upload COMPLETE scan to Firestore — ALL fields
     */
    suspend fun uploadScan(scanData: DeviceScanData): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Uploading COMPLETE scan: ${scanData.scanId}")
                
                val scanMap = createCompleteScanMap(scanData)
                
                firestore.collection(COLLECTION_SCANS)
                    .document(scanData.scanId)
                    .set(scanMap)
                    .await()
                
                Log.d(TAG, "✅ Upload successful! (${scanMap.size} top-level fields)")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Upload failed: ${e.message}", e)
                false
            }
        }
    }
    
    /**
     * Creates a COMPLETE map of ALL DeviceScanData fields.
     * Structured as nested maps for Firestore subcollections.
     */
    private fun createCompleteScanMap(s: DeviceScanData): Map<String, Any?> {
        return mapOf(
            // ══════════ METADATA ══════════
            "scan_id" to s.scanId,
            "scan_timestamp" to s.scanTimestamp,
            "scan_duration_seconds" to s.scanDurationSeconds,
            "app_version" to s.appVersion,
            "created_at" to com.google.firebase.Timestamp.now(),
            
            // ══════════ IDENTITY (12) ══════════
            "identity" to mapOf(
                "device_brand" to s.deviceBrand,
                "device_model" to s.deviceModel,
                "device_manufacturer" to s.deviceManufacturer,
                "board_platform" to s.boardPlatform,
                "build_fingerprint" to s.buildFingerprint,
                "build_id" to s.buildId,
                "build_date_utc" to s.buildDateUtc,
                "android_version" to s.androidVersion,
                "android_sdk" to s.androidSdk,
                "kernel_version" to s.kernelVersion,
                "security_patch_date" to s.securityPatchDate,
                "build_tags" to s.buildTags
            ),
            
            // ══════════ SECURITY (9) ══════════
            "security" to mapOf(
                "play_services_available" to s.playServicesAvailable,
                "play_integrity_verdict" to s.playIntegrityVerdict,
                "safety_net_basic_integrity" to s.safetyNetBasicIntegrity,
                "unknown_sources_count" to s.unknownSourcesCount,
                "developer_options_enabled" to s.developerOptionsEnabled,
                "adb_enabled" to s.adbEnabled,
                "usb_debugging_enabled" to s.usbDebuggingEnabled,
                "device_admin_count" to s.deviceAdminCount,
                "verified_boot_state" to s.verifiedBootState
            ),
            
            // ══════════ CPU/GPU (10+) ══════════
            "cpu_gpu" to mapOf(
                "cpu_architecture" to s.cpuArchitecture,
                "supported_abis" to s.supportedAbis,
                "cpu_core_count" to s.cpuCoreCount,
                "cpu_max_freq_mhz" to s.cpuMaxFreqMhz,
                "cpu_min_freq_mhz" to s.cpuMinFreqMhz,
                "gpu_renderer" to s.gpuRenderer,
                "gpu_vendor" to s.gpuVendor,
                "opengl_version" to s.openglVersion,
                "vulkan_version" to s.vulkanVersion,
                "npu_available" to s.npuAvailable,
                "cpu_governor" to s.cpuGovernor,
                "cpu_big_cores" to s.cpuBigCores,
                "cpu_little_cores" to s.cpuLittleCores
            ),
            
            // ══════════ MEMORY/STORAGE (8) ══════════
            "memory" to mapOf(
                "ram_total_mb" to s.ramTotalMb,
                "ram_available_mb" to s.ramAvailableMb,
                "ram_low_threshold_mb" to s.ramLowThresholdMb,
                "storage_total_gb" to s.storageTotalGb,
                "storage_available_gb" to s.storageAvailableGb,
                "external_storage_available" to s.externalStorageAvailable,
                "adoptable_storage_enabled" to s.adoptableStorageEnabled,
                "storage_emulated" to s.storageEmulated
            ),
            
            // ══════════ DISPLAY (8) ══════════
            "display" to mapOf(
                "screen_width_px" to s.screenWidthPx,
                "screen_height_px" to s.screenHeightPx,
                "screen_density_dpi" to s.screenDensityDpi,
                "screen_refresh_rate_hz" to s.screenRefreshRateHz,
                "screen_refresh_rates_supported" to s.screenRefreshRatesSupported,
                "display_hdr_capabilities" to s.displayHdrCapabilities,
                "wide_color_gamut_support" to s.wideColorGamutSupport,
                "screen_brightness_mode" to s.screenBrightnessMode
            ),
            
            // ══════════ CAMERA/SENSORS (7) ══════════
            "camera_sensors" to mapOf(
                "camera_count" to s.cameraCount,
                "back_camera_resolution_mp" to s.backCameraResolutionMp,
                "front_camera_resolution_mp" to s.frontCameraResolutionMp,
                "camera_flash_available" to s.cameraFlashAvailable,
                "camera_ois_available" to s.cameraOisAvailable,
                "sensor_count_total" to s.sensorCountTotal,
                "sensor_types_list" to s.sensorTypesList
            ),
            
            // ══════════ AUDIO (3) ══════════
            "audio" to mapOf(
                "speaker_working" to s.speakerWorking,
                "microphone_working" to s.microphoneWorking,
                "audio_test_passed" to s.audioTestPassed
            ),
            
            // ══════════ BATTERY STATIC (7) ══════════
            "battery" to mapOf(
                "design_capacity_mah" to s.batteryDesignCapacityMah,
                "multi_cell_detected" to s.batteryMultiCellDetected,
                "health_status" to s.batteryHealthStatus,
                "technology" to s.batteryTechnology,
                "temp_celsius" to s.batteryTempCelsius,
                "voltage_mv" to s.batteryVoltageMv,
                "current_now_ma" to s.batteryCurrentNowMa
            ),
            
            // ══════════ CONNECTIVITY (7) ══════════
            "connectivity" to mapOf(
                "wifi_available" to s.wifiAvailable,
                "bluetooth_version" to s.bluetoothVersion,
                "nfc_available" to s.nfcAvailable,
                "mobile_network_type" to s.mobileNetworkType,
                "dual_sim_support" to s.dualSimSupport,
                "esim_support" to s.esimSupport,
                "usb_version" to s.usbVersion
            ),
            
            // ══════════ PERFORMANCE (6) ══════════
            "performance" to mapOf(
                "system_uptime_hours" to s.systemUptimeHours,
                "running_processes_count" to s.runningProcessesCount,
                "running_services_count" to s.runningServicesCount,
                "installed_packages_count" to s.installedPackagesCount,
                "system_apps_count" to s.systemAppsCount,
                "user_apps_count" to s.userAppsCount
            ),
            
            // ══════════ THERMAL (7) ══════════
            "thermal" to mapOf(
                "cpu_temp_celsius" to s.cpuTempCelsius,
                "thermal_throttling_status" to s.thermalThrottlingStatus,
                "cpu_usage_percent" to s.cpuUsagePercent,
                "memory_pressure_level" to s.memoryPressureLevel,
                "anr_count_recent" to s.anrCountRecent,
                "crash_count_recent" to s.crashCountRecent,
                "bootloop_indicator" to s.bootloopIndicator
            ),
            
            // ══════════ DEVICE AGE ══════════
            "device_age" to mapOf(
                "age_years" to s.deviceAgeYears,
                "age_confidence" to s.deviceAgeConfidence,
                "age_category" to s.deviceAgeCategory
            ),
            
            // ══════════ ADAPTIVE BENCHMARK ══════════
            "benchmark" to mapOf(
                "quick_benchmark_score" to s.quickBenchmarkScore,
                "device_performance_tier" to s.devicePerformanceTier,
                "cpu_test_iterations" to s.cpuTestIterations,
                "storage_test_file_count" to s.storageTestFileCount
            ),
            
            // ══════════ BATTERY DRAIN ANALYSIS ══════════
            "battery_drain" to mapOf(
                "level_start" to s.batteryLevelStart,
                "level_end" to s.batteryLevelEnd,
                "drain_percent" to s.batteryDrainPercent,
                "drain_rate_per_minute" to s.batteryDrainRatePerMinute,
                "is_charging_during_scan" to s.isChargingDuringScan,
                "charge_counter_start_uah" to s.chargeCounterStartUah,
                "charge_counter_end_uah" to s.chargeCounterEndUah,
                "actual_drain_mah" to s.actualDrainMah,
                "actual_drain_percent" to s.actualDrainPercent,
                "temp_start_celsius" to s.batteryTempStartCelsius,
                "temp_end_celsius" to s.batteryTempEndCelsius,
                "cycle_count" to s.batteryCycleCount,
                "is_charging_start" to s.isChargingStart,
                "is_charging_end" to s.isChargingEnd,
                "charging_detected_mid_test" to s.chargingDetectedMidTest
            ),
            
            // ══════════ STRESS TEST RESULTS ══════════
            "cpu_stress_result" to createCpuStressMap(s.cpuStressResult),
            "storage_stress_result" to createStorageStressMap(s.storageStressResult),
            "battery_stress_result" to createBatteryStressMap(s.batteryStressResult),
            
            // ══════════ HUMAN LABELS ══════════
            "human_labels" to s.humanLabels,
            "label_status" to s.labelStatus,
            "label_timestamp" to s.labelTimestamp,
            
            // ══════════ DATA COLLECTION METADATA ══════════
            "data_collection_metadata" to s.data_collection_metadata?.let { m ->
                mapOf(
                    "collection_timestamp" to m.collection_timestamp,
                    "app_version" to m.app_version,
                    "test_duration_seconds" to m.test_duration_seconds,
                    "device_manufacturer" to m.device_manufacturer,
                    "device_model" to m.device_model,
                    "android_version" to m.android_version,
                    "android_sdk" to m.android_sdk,
                    "flags" to m.flags,
                    "raw_voltage_readings_count" to m.raw_voltage_readings.size,
                    "raw_voltage_readings_sample" to m.raw_voltage_readings.take(30),
                    "age_estimation_source" to m.age_estimation_source,
                    "age_estimation_confidence" to m.age_estimation_confidence,
                    "voltage_primary_source" to m.voltage_primary_source,
                    "voltage_is_stalled" to m.voltage_is_stalled,
                    "voltage_variance_score" to m.voltage_variance_score,
                    "has_current_readings" to m.has_current_readings,
                    "has_temperature_readings" to m.has_temperature_readings,
                    "has_resistance_readings" to m.has_resistance_readings,
                    "notes" to m.notes
                )
            }
        )
    }
    
    // ═══════════════════════════════════════════════════════
    // STRESS TEST MAP BUILDERS
    // ═══════════════════════════════════════════════════════
    
    private fun createCpuStressMap(cpu: CpuStressResult): Map<String, Any?> {
        return mapOf(
            "duration_seconds" to cpu.durationSeconds,
            "max_temp_celsius" to cpu.maxTempCelsius,
            "throttling_start_sec" to cpu.throttlingStartSec,
            "performance_degradation_percent" to cpu.performanceDegradationPercent,
            "single_core_score" to cpu.singleCoreScore,
            "multi_core_score" to cpu.multiCoreScore,
            "quick_benchmark_score" to cpu.quickBenchmarkScore,
            "device_performance_tier" to cpu.devicePerformanceTier,
            "cpu_test_iterations" to cpu.cpuTestIterations,
            // Sample temp/freq readings (keep first 30 to avoid Firestore 1MB limit)
            "temp_readings" to cpu.tempReadings.take(30).map { r ->
                mapOf(
                    "timestamp_sec" to r.timestampSec,
                    "temp_celsius" to r.tempCelsius
                )
            },
            "freq_readings" to cpu.freqReadings.take(30).map { r ->
                mapOf(
                    "timestamp_sec" to r.timestampSec,
                    "freq_mhz" to r.freqMhz
                )
            },
            "temp_readings_count" to cpu.tempReadings.size,
            "freq_readings_count" to cpu.freqReadings.size
        )
    }
    
    private fun createStorageStressMap(storage: StorageStressResult): Map<String, Any?> {
        return mapOf(
            "sequential_write_mbps" to storage.sequentialWriteMbps,
            "sequential_read_mbps" to storage.sequentialReadMbps,
            "random_4k_read_iops" to storage.random4kReadIops,
            "random_4k_write_iops" to storage.random4kWriteIops,
            "slc_cache_crash" to storage.slcCacheCrash,
            "detected_type" to storage.detectedType,
            "storage_test_file_count" to storage.storageTestFileCount,
            "write_speed_curve" to storage.writeSpeedCurve.map { p ->
                mapOf(
                    "file_number" to p.fileNumber,
                    "speed_mbps" to p.speedMbps
                )
            }
        )
    }
    
    private fun createBatteryStressMap(battery: BatteryStressResult): Map<String, Any?> {
        return mapOf(
            "duration_seconds" to battery.durationSeconds,
            "initial_voltage_mv" to battery.initialVoltageMv,
            "load_voltage_mv" to battery.loadVoltageMv,
            "voltage_sag_mv" to battery.voltageSagMv,
            "peak_current_ma" to battery.peakCurrentMa,
            "internal_resistance_mohm" to battery.internalResistanceMohm,
            "calculation_method" to battery.calculationMethod,
            "estimated_health_percent" to battery.estimatedHealthPercent,
            "drop_confidence" to battery.dropConfidence,
            "class_confidence" to battery.classConfidence,
            
            // Precise drain
            "charge_counter_start_uah" to battery.chargeCounterStartUah,
            "charge_counter_end_uah" to battery.chargeCounterEndUah,
            "actual_drain_mah" to battery.actualDrainMah,
            "actual_drain_percent" to battery.actualDrainPercent,
            
            // Level tracking
            "battery_level_start" to battery.batteryLevelStart,
            "battery_level_end" to battery.batteryLevelEnd,
            "battery_drain_percent" to battery.batteryDrainPercent,
            "battery_drain_rate_per_minute" to battery.batteryDrainRatePerMinute,
            
            // Temperature
            "battery_temp_start_celsius" to battery.batteryTempStartCelsius,
            "battery_temp_end_celsius" to battery.batteryTempEndCelsius,
            
            // Metadata
            "battery_cycle_count" to battery.batteryCycleCount,
            "battery_health_status" to battery.batteryHealthStatus,
            "is_charging_start" to battery.isChargingStart,
            "is_charging_end" to battery.isChargingEnd,
            "charging_detected_mid_test" to battery.chargingDetectedMidTest,
            
            // Voltage analysis
            "voltage_analysis" to battery.voltageAnalysis?.let { v ->
                mapOf(
                    "is_stalled" to v.isStalled,
                    "variance_score" to v.varianceScore,
                    "derivative" to v.derivative,
                    "trend" to v.trend,
                    "standard_deviation" to v.standardDeviation
                )
            },
            
            // Synthetic voltage
            "synthetic_voltage" to battery.syntheticVoltage?.let { sv ->
                mapOf(
                    "estimated_drop_mv" to sv.estimatedDropMv,
                    "confidence" to sv.confidence,
                    "sources" to sv.sources,
                    "components" to sv.components
                )
            },
            
            // Hybrid voltage
            "hybrid_voltage_result" to battery.hybridVoltageResult?.let { h ->
                mapOf(
                    "voltage_drop_mv" to h.voltageDropMv,
                    "primary_source" to h.primarySource,
                    "confidence" to h.confidence,
                    "is_stalled" to h.isStalled
                )
            },
            
            // Capacity metrics
            "capacity_metrics" to battery.capacityMetrics?.let { cm ->
                mapOf(
                    "design_capacity" to mapOf(
                        "capacity_mah" to cm.designCapacity.capacityMah,
                        "source" to cm.designCapacity.source.name,
                        "confidence" to cm.designCapacity.confidence,
                        "is_raw_reading" to cm.designCapacity.isRawReading,
                        "metadata" to cm.designCapacity.metadata
                    ),
                    "current_capacity" to mapOf(
                        "capacity_mah" to cm.currentCapacity.capacityMah,
                        "source" to cm.currentCapacity.source.name,
                        "confidence" to cm.currentCapacity.confidence,
                        "is_raw_reading" to cm.currentCapacity.isRawReading,
                        "metadata" to cm.currentCapacity.metadata
                    ),
                    "degradation_mah" to cm.degradationMah,
                    "degradation_percent" to cm.degradationPercent,
                    "degradation_rate_per_year" to cm.degradationRatePerYear,
                    "measurement_timestamp" to cm.measurementTimestamp,
                    "battery_percent_at_measurement" to cm.batteryPercentAtMeasurement,
                    "degradation_metrics" to cm.degradationMetrics?.let { dm ->
                        mapOf(
                            "raw_degradation_mah" to dm.rawDegradationMah,
                            "raw_degradation_percent" to dm.rawDegradationPercent,
                            "display_degradation_percent" to dm.displayDegradationPercent,
                            "is_likely_new_device" to dm.isLikelyNewDevice,
                            "reliability" to dm.reliability
                        )
                    }
                )
            },
            
            // Data quality
            "data_quality" to battery.dataQuality?.let { dq ->
                mapOf(
                    "pmic_stalled" to dq.pmicStalled,
                    "voltage_glitches" to dq.voltageGlitches,
                    "current_glitches" to dq.currentGlitches,
                    "overall_quality" to dq.overallQuality,
                    "confidence_score" to dq.confidenceScore
                )
            },
            
            // Stable features
            "stable_features" to battery.stableFeatures?.let { sf ->
                mapOf(
                    "baseline_voltage_avg" to sf.baselineVoltageAvg,
                    "baseline_voltage_std" to sf.baselineVoltageStd,
                    "load_voltage_stable" to sf.loadVoltageStable,
                    "voltage_transition_time_sec" to sf.voltageTransitionTimeSec,
                    "voltage_drop_mv" to sf.voltageDropMv,
                    "voltage_change_mv" to sf.voltageChangeMv,
                    "voltage_change_direction" to sf.voltageChangeDirection,
                    "voltage_recovery_mv" to sf.voltageRecoveryMv,
                    "temp_rise_total" to sf.tempRiseTotal,
                    "temp_rise_rate_per_min" to sf.tempRiseRatePerMin,
                    "avg_cpu_freq_mhz" to sf.avgCpuFreqMhz,
                    "cpu_freq_variation" to sf.cpuFreqVariation,
                    "thermal_throttling_detected" to sf.thermalThrottlingDetected
                )
            },
            
            // Voltage readings (sample — first 30 to stay under 1MB)
            "voltage_readings_sample" to battery.voltageReadings.take(30).map { r ->
                mapOf(
                    "timestamp_sec" to r.timestampSec,
                    "voltage_mv" to r.voltageMv,
                    "current_ma" to r.currentMa
                )
            },
            "voltage_readings_count" to battery.voltageReadings.size,
            
            // Time-series (sample — first 30)
            "time_series_sample" to battery.timeSeriesData.take(30).map { ts ->
                mapOf(
                    "timestamp_ms" to ts.timestamp_ms,
                    "time_delta_sec" to ts.time_delta_sec,
                    "phase" to ts.phase,
                    "voltage_mv" to ts.voltage_mv,
                    "current_ma" to ts.current_ma,
                    "temp_c" to ts.temp_c,
                    "soc_percent" to ts.soc_percent,
                    "cpu_load_percent" to ts.cpu_load_percent,
                    "cpu_freq_mhz" to ts.cpu_freq_mhz
                )
            },
            "time_series_count" to battery.timeSeriesData.size
        )
    }
    
    /**
     * Update scan with human labels
     */
    suspend fun updateScanWithLabels(scanId: String, humanLabels: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating labels for: $scanId")
                
                val updates = mapOf(
                    "human_labels" to humanLabels,
                    "label_status" to "USER_LABELED",
                    "label_timestamp" to System.currentTimeMillis()
                )
                
                firestore.collection(COLLECTION_SCANS)
                    .document(scanId)
                    .update(updates)
                    .await()
                
                Log.d(TAG, "✅ Labels updated!")
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Update failed: ${e.message}", e)
                false
            }
        }
    }
}
