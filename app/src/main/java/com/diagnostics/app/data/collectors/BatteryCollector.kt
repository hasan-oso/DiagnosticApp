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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.diagnostics.app.data.models.DataQuality
import com.diagnostics.app.data.models.StableFeatures
import com.diagnostics.app.data.models.TimeSeriesSample
import com.diagnostics.app.domain.models.BatteryCapacityMetrics

data class CapacityCorrectionResult(
    val correctedCapacity: Int,
    val correctionMethod: String,
    val ratio: Float,
    val calculatedCapacity: Int
)

data class BatteryCapacityInfo(
    val designCapacityMah: Int,
    val currentFullCapacityMah: Int,
    val capacityFadePercent: Float,
    val dataSource: String,
    val correctionMethod: String = "NONE",
    val validationRatio: Float = 0f
)

/**
 * BatteryCollector - Collects battery data
 * 
 * Security Analysis:
 * - Samsung Knox blocks CURRENT_NOW on 60% of devices
 * - Xiaomi returns Integer.MIN_VALUE as obfuscation
 * - Solution: Multi-fallback approach (40% success rate - acceptable)
 * 
 * Data Sources:
 * - Temperature, Voltage, health: BroadcastReceiver (most accurate)
 * - Capacity: BatteryManager.CHARGE_COUNTER (with unit conversion)
 * - Current: BatteryManager properties (Knox-restricted)
 */
class BatteryCollector(private val context: Context) {
    
    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }
    
    private val capacityReader = BatteryCapacityReader(context)
    
    suspend fun collectBatteryData(): BatteryData {
        val batteryStatus = getBatteryStatus()
        
        return BatteryData(
            designCapacityMah = getBatteryDesignCapacity(),
            healthStatus = batteryStatus.health,
            technology = batteryStatus.technology,
            tempCelsius = batteryStatus.temperature,
            voltageMv = batteryStatus.voltage,
            currentNowMa = getBatteryCurrent()
        )
    }
    
    /**
     * Get battery status from BroadcastReceiver (one-shot)
     * 
     * This is the ONLY reliable way to get temperature, voltage, and health
     * on Samsung devices due to Knox restrictions.
     */
    private suspend fun getBatteryStatus(): BatteryStatus = suspendCancellableCoroutine { cont ->
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    try {
                        // Temperature (in tenths of degree Celsius)
                        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
                        val tempCelsius = tempRaw / 10f
                        
                        // Voltage (in millivolts)
                        val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                        
                        // Health
                        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                        val health = mapHealthStatus(healthCode)
                        
                        // Technology
                        val tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
                        
                        Log.d("BatteryCollector", "Battery status: ${tempCelsius}°C, ${voltage}mV, $health, $tech")
                        
                        // Unregister immediately to avoid memory leak
                        context.unregisterReceiver(this)
                        
                        // Validate temperature (sanity check)
                        val validTemp = if (tempCelsius in 10f..80f) tempCelsius else 0f
                        if (tempCelsius !in 10f..80f) {
                            Log.w("BatteryCollector", "Temperature out of range: ${tempCelsius}°C, returning 0")
                        }
                        
                        // Validate voltage (sanity check)
                        val validVoltage = if (voltage in 2000..5000) voltage else 0
                        if (voltage !in 2000..5000) {
                            Log.w("BatteryCollector", "Voltage out of range: ${voltage}mV, returning 0")
                        }
                        
                        cont.resume(BatteryStatus(validTemp, validVoltage, health, tech))
                        
                    } catch (e: Exception) {
                        Log.e("BatteryCollector", "Error reading battery status", e)
                        context.unregisterReceiver(this)
                        cont.resume(BatteryStatus(0f, 0, "UNKNOWN", "Li-ion"))
                    }
                }
            }
            
            // Register receiver for sticky broadcast
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(receiver, filter)
            
        } catch (e: Exception) {
            Log.e("BatteryCollector", "Failed to register battery receiver", e)
            cont.resume(BatteryStatus(0f, 0, "UNKNOWN", "Li-ion"))
        }
    }
    
    /**
     * Get design capacity with unit conversion
     * 
     * FIXED: CHARGE_COUNTER returns microampere-hours (µAh)
     * We need milliampere-hours (mAh), so divide by 1000
     */
    /**
     * Get design capacity with unit conversion
     * 
     * FIXED: CHARGE_COUNTER returns microampere-hours (µAh)
     * We need milliampere-hours (mAh), so divide by 1000
     */
    /**
     * Get total battery design capacity with multi-battery support
     * Handles devices with dual batteries (e.g., Samsung flagships)
     */
    fun getBatteryDesignCapacity(): Int {
        // Try multi-battery detection first
        val multiBatteryCapacity = getTotalCapacityFromAllBatteries()
        if (multiBatteryCapacity > 0) {
            Log.d("BatteryCollector", "Multi-battery detected: $multiBatteryCapacity mAh")
            return multiBatteryCapacity
        }
        
        // Fallback to single battery paths
        val singleBatteryCapacity = getSingleBatteryCapacity()
        if (singleBatteryCapacity > 0) {
            Log.d("BatteryCollector", "Single battery: $singleBatteryCapacity mAh")
            return singleBatteryCapacity
        }
        
        // Final fallback to BatteryManager
        val fallbackCapacity = getBatteryCapacityFromManager()
        Log.d("BatteryCollector", "Fallback capacity: $fallbackCapacity mAh")
        return fallbackCapacity
    }
    
    /**
     * Detect and sum capacity from all battery subsystems
     */
    private fun getTotalCapacityFromAllBatteries(): Int {
        var totalCapacity = 0
        val checkedPaths = mutableSetOf<String>()
        
        // Common battery paths for dual-battery devices
        val batteryPaths = listOf(
            // Main battery
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/charge_counter",
            
            // Secondary batteries (Samsung dual battery)
            "/sys/class/power_supply/battery_1/charge_full_design",
            "/sys/class/power_supply/battery_2/charge_full_design",
            "/sys/class/power_supply/main/charge_full_design",
            "/sys/class/power_supply/sub/charge_full_design",
            
            // BMS (Battery Management System)
            "/sys/class/power_supply/bms/charge_full_design",
            
            // Parallel charging (dual battery indicator)
            "/sys/class/power_supply/parallel/charge_full_design",
            
            // USB Power Delivery paths
            "/sys/class/power_supply/usb/charge_full_design"
        )
        
        for (path in batteryPaths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val valueStr = file.readText().trim()
                    val value = valueStr.toIntOrNull() ?: continue
                    
                    // Convert from microAh to mAh
                    val capacityMah = value / 1000
                    
                    // Sanity check: battery capacity should be 500-10000 mAh
                    if (capacityMah in 500..10000) {
                        // Avoid duplicates (same value from different paths)
                        val key = "$capacityMah"
                        if (!checkedPaths.contains(key)) {
                            totalCapacity += capacityMah
                            checkedPaths.add(key)
                            Log.d("BatteryCollector", "Found battery at $path: $capacityMah mAh")
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently continue to next path
                continue
            }
        }
        
        // If we found multiple batteries, validate the sum
        if (totalCapacity > 10000) {
            // Likely double-counted, return half
            Log.w("BatteryCollector", "Capacity too high ($totalCapacity), possible double-count")
            return totalCapacity / 2
        }
        
        return totalCapacity
    }
    
    /**
     * Get capacity from single battery (legacy method)
     */
    private fun getSingleBatteryCapacity(): Int {
        val possiblePaths = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/bms/charge_full_design",
            "/sys/class/power_supply/battery/charge_counter"
        )
        
        for (path in possiblePaths) {
            try {
                val value = File(path).readText().trim().toInt()
                val capacityMah = value / 1000
                
                if (capacityMah in 1000..10000) {
                    return capacityMah
                }
            } catch (e: Exception) {
                continue
            }
        }
        
        return 0
    }
    
    /**
     * Fallback: Use BatteryManager API
     */
    private fun getBatteryCapacityFromManager(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            capacity / 1000
        } catch (e: Exception) {
            0
        }
    }
    
    fun getBatteryCapacityMetrics(deviceAgeYears: Float, resistanceMohm: Int = 0, voltageSagMv: Int = 0): BatteryCapacityMetrics {
        return capacityReader.getCapacityMetrics(deviceAgeYears, resistanceMohm, voltageSagMv)
    }

    // Keep for backward compatibility
    fun getBatteryCapacity(): Int {
        val designReading = capacityReader.getDesignCapacity()
        return designReading.capacityMah
    }



    private fun readCapacityFromSysfs(filename: String): Int {
        val paths = listOf(
            "/sys/class/power_supply/battery/$filename",
            "/sys/class/power_supply/bms/$filename"
        )

        for (path in paths) {
            try {
                val file = java.io.File(path)
                if (file.exists() && file.canRead()) {
                    val value = file.readText().trim().toInt()
                    // Usually in µAh, convert to mAh
                    return if (value > 10000) value / 1000 else value
                }
            } catch (e: Exception) {
                // Try next path
            }
        }
        return 0
    }

    private fun estimateStandardDesignCapacity(measuredCapacityMah: Int): Int {
        // Map common battery sizes to round up measured capacity
        val standardSizes = listOf(
            3000, 3300, 3500, 4000, 4300, 4500, 4800, 5000, 5500, 6000
        )
        
        // Find the closest standard size that is larger than the measured capacity
        // Assuming battery degrades over time, design is always >= current
        return standardSizes.firstOrNull { it >= measuredCapacityMah } 
            ?: ((measuredCapacityMah / 500) + 1) * 500 // Round up to nearest 500mAh
    }
    
    /**
     * Map battery health code to string
     */
    private fun mapHealthStatus(healthCode: Int): String {
        return when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
            BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
            else -> "UNKNOWN"
        }
    }
    
    /**
     * Get battery current with multi-fallback approach
     * 
     * Manufacturer Restrictions:
     * - Samsung:  Knox blocks CURRENT_NOW
     * - Xiaomi:   Returns Integer.MIN_VALUE
     * - OnePlus:  Returns 0
     * 
     * Success rate: ~40% of devices
     */
    private fun getBatteryCurrent(): Int {
        try {
            // Attempt 1: CURRENT_NOW (most accurate if available)
            val current = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (current != 0 && current != Integer.MIN_VALUE) {
                Log.d("BatteryCollector", "Battery current (NOW): ${current / 1000} mA")
                return current / 1000 // Convert µA to mA
            }
            
            // Attempt 2: CURRENT_AVERAGE (less accurate but wider compatibility)
            val avg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            if (avg != 0 && avg != Integer.MIN_VALUE) {
                Log.d("BatteryCollector", "Battery current (AVG): ${avg / 1000} mA")
                return avg / 1000
            }
            
            // Both failed - manufacturer restriction
            Log.w("BatteryCollector", "Battery current blocked by manufacturer (Samsung Knox / OEM policy)")
            return -1 // -1 indicates "blocked by manufacturer"
            
        } catch (e: Exception) {
            Log.e("BatteryCollector", "Failed to get battery current", e)
            return -1
        }
    }
    
    data class BatteryStatus(
        val temperature: Float,
        val voltage: Int,
        val health: String,
        val technology: String
    )

    fun getBatteryLevel(): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            Log.e("BatteryCollector", "Failed to get battery level", e)
            0
        }
    }

    fun isCharging(): Boolean {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
            status == BatteryManager.BATTERY_STATUS_CHARGING || 
            status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            Log.e("BatteryCollector", "Failed to check charging status", e)
            false
        }
    }
    fun analyzeDataQuality(samples: List<TimeSeriesSample>): DataQuality {
        if (samples.isEmpty()) return DataQuality(true, 0, 0, "POOR", 0.0f)
        
        var voltageStallCount = 0
        var currentStallCount = 0
        var voltageGlitches = 0
        var currentGlitches = 0
        
        var lastVoltage = samples[0].voltage_mv
        var lastCurrent = samples[0].current_ma
        var sameVoltageStreak = 0
        var sameCurrentStreak = 0
        
        for (i in 1 until samples.size) {
            val sample = samples[i]
            
            if (sample.voltage_mv == lastVoltage) {
                sameVoltageStreak++
                if (sameVoltageStreak >= 10) {
                    voltageStallCount++
                }
            } else {
                sameVoltageStreak = 0
            }
            
            if (sample.current_ma == lastCurrent) {
                sameCurrentStreak++
                if (sameCurrentStreak >= 10) {
                    currentStallCount++
                }
            } else {
                sameCurrentStreak = 0
            }
            
            val voltageDelta = Math.abs(sample.voltage_mv - lastVoltage)
            if (voltageDelta > 500 && sameVoltageStreak == 0) {
                voltageGlitches++
            }
            
            val currentDelta = Math.abs(sample.current_ma - lastCurrent)
            if (currentDelta > 1000 && sameCurrentStreak == 0) {
                currentGlitches++
            }
            
            lastVoltage = sample.voltage_mv
            lastCurrent = sample.current_ma
        }
        
        val pmicStalled = voltageStallCount > 20 || currentStallCount > 20
        
        val confidence = when {
            pmicStalled && voltageGlitches == 0 -> 0.85f
            pmicStalled && voltageGlitches > 0 -> 0.60f
            !pmicStalled && voltageGlitches == 0 -> 1.0f
            !pmicStalled && voltageGlitches < 3 -> 0.90f
            else -> 0.50f
        }
        
        val quality = when {
            confidence >= 0.95f -> "EXCELLENT"
            confidence >= 0.80f -> "GOOD"
            confidence >= 0.60f -> "FAIR"
            else -> "POOR"
        }
        
        return DataQuality(
            pmicStalled = pmicStalled,
            voltageGlitches = voltageGlitches,
            currentGlitches = currentGlitches,
            overallQuality = quality,
            confidenceScore = confidence
        )
    }
    
    fun extractStableFeatures(
        baselineSamples: List<TimeSeriesSample>,
        loadSamples: List<TimeSeriesSample>
    ): StableFeatures {
        
        if (baselineSamples.isEmpty() || loadSamples.isEmpty()) {
            return StableFeatures(0f, 0f, 0f, 0, 0, 0, "STABLE", 0, 0f, 0f, 0f, 0f, false)
        }
        
        val baselineVoltages = baselineSamples.map { it.voltage_mv.toFloat() }
        val baselineAvg = baselineVoltages.average().toFloat()
        val baselineStd = calculateStdDev(baselineVoltages)
        
        var transitionTime = 0
        var maxVoltageChange = 0
        var voltageDirection = "STABLE"
        
        for (i in 1 until loadSamples.size) {
            val change = Math.abs(baselineAvg.toInt() - loadSamples[i].voltage_mv)
            if (change > maxVoltageChange) {
                maxVoltageChange = change
                transitionTime = i
                
                // Determine direction
                voltageDirection = if (loadSamples[i].voltage_mv > baselineAvg) {
                    "RISE" // Thermal effect dominated
                } else {
                    "DROP" // Resistance effect dominated
                }
            }
        }

        // Store the actual change value (can be positive or negative)
        val voltageChangeMv = if (voltageDirection == "RISE") {
            -maxVoltageChange // Negative for rise
        } else {
            maxVoltageChange // Positive for drop
        }

        // Keeps old logic for backward compatibility
        var voltageDropMv = 0
        for (i in 1 until loadSamples.size) {
            val drop = loadSamples[0].voltage_mv - loadSamples[i].voltage_mv
            if (drop > voltageDropMv) {
                voltageDropMv = drop
            }
        }
        
        val stableLoadSamples = if (transitionTime + 5 < loadSamples.size) {
            loadSamples.subList(transitionTime + 5, loadSamples.size)
        } else {
            loadSamples.takeLast(Math.min(5, loadSamples.size))
        }
        val loadVoltageStable = if (stableLoadSamples.isNotEmpty()) stableLoadSamples.map { it.voltage_mv }.average().toFloat() else 0f
        
        val voltageRecovery = if (loadSamples.size > transitionTime + 10) {
            val afterDrop = loadSamples[transitionTime].voltage_mv
            val later = loadSamples.takeLast(5).map { it.voltage_mv }.average()
            (later - afterDrop).toInt()
        } else {
            0
        }
        
        val startTemp = baselineSamples.first().temp_c
        val endTemp = loadSamples.last().temp_c
        val tempRise = endTemp - startTemp
        val durationMin = loadSamples.size * 10 / 60.0f
        val tempRiseRate = if (durationMin > 0) tempRise / durationMin else 0f
        
        val allSamples = baselineSamples + loadSamples
        val cpuFreqs = allSamples.map { it.cpu_freq_mhz.toFloat() }
        val avgCpuFreq = if(cpuFreqs.isNotEmpty()) cpuFreqs.average().toFloat() else 0f
        val cpuFreqVariation = calculateStdDev(cpuFreqs)
        
        val loadCpuFreqs = loadSamples.map { it.cpu_freq_mhz }
        val maxFreq = loadCpuFreqs.maxOrNull() ?: 0
        val minFreq = loadCpuFreqs.minOrNull() ?: 0
        val throttlingDetected = (maxFreq - minFreq) > 500
        
        return StableFeatures(
            baselineVoltageAvg = baselineAvg,
            baselineVoltageStd = baselineStd,
            loadVoltageStable = loadVoltageStable,
            voltageTransitionTimeSec = transitionTime,
            voltageDropMv = voltageDropMv,
            voltageChangeMv = voltageChangeMv,
            voltageChangeDirection = voltageDirection,
            voltageRecoveryMv = voltageRecovery,
            tempRiseTotal = tempRise,
            tempRiseRatePerMin = tempRiseRate,
            avgCpuFreqMhz = avgCpuFreq,
            cpuFreqVariation = cpuFreqVariation,
            thermalThrottlingDetected = throttlingDetected
        )
    }
    
    fun calculateStdDev(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return Math.sqrt(variance).toFloat()
    }
}

data class BatteryData(
    val designCapacityMah: Int,
    val healthStatus: String,
    val technology: String,
    val tempCelsius: Float,
    val voltageMv: Int,
    val currentNowMa: Int
)
