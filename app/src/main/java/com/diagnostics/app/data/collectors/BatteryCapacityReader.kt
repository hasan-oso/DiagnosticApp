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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.diagnostics.app.domain.models.CapacityReading
import com.diagnostics.app.domain.models.CapacitySource
import com.diagnostics.app.domain.models.BatteryCapacityMetrics
import java.io.File
import kotlin.math.abs
import com.diagnostics.app.utils.DeviceAgeEstimator
import com.diagnostics.app.domain.models.DegradationMetrics

class BatteryCapacityReader(private val context: Context) {
    
    companion object {
        private const val TAG = "BatteryCapacityReader"
        private const val SYSFS_DESIGN = "/sys/class/power_supply/battery/charge_full_design"
        private const val SYSFS_FULL = "/sys/class/power_supply/battery/charge_full"
        private const val MIN_VALID_CAPACITY = 1000 // mAh
    }
    
    private data class CapacityEstimate(
        val capacityMah: Int,
        val confidence: Float,
        val source: String
    )
    
    /**
     * Get comprehensive battery capacity metrics
     */
    fun getCapacityMetrics(deviceAgeYears: Float, resistanceMohm: Int = 0, voltageSagMv: Int = 0): BatteryCapacityMetrics {
        val designReading = getDesignCapacity()
        val currentReading = getCurrentCapacity(deviceAgeYears, resistanceMohm, voltageSagMv)
        val batteryPercent = getBatteryPercent()
        
        val degradationMah = if (currentReading.capacityMah > 0 && 
                                 designReading.capacityMah > 0) {
            designReading.capacityMah - currentReading.capacityMah
        } else null
        
        val degradationPercent = if (degradationMah != null && 
                                     designReading.capacityMah > 0) {
            (degradationMah.toFloat() / designReading.capacityMah) * 100f
        } else null
        
        val degradationRate = if (degradationPercent != null && deviceAgeYears > 0) {
            degradationPercent / deviceAgeYears
        } else null
        
        // حساب metrics التدهور الكامل
        val deviceAge = getDeviceAge()
        val resistance = getLastMeasuredResistance()
        
        val degradationMetricsData = if (designReading.capacityMah > 0 && currentReading.capacityMah > 0) {
            calculateDegradationMetrics(designReading.capacityMah, currentReading.capacityMah, deviceAge, resistance)
        } else null
        
        return BatteryCapacityMetrics(
            designCapacity = designReading,
            currentCapacity = currentReading,
            degradationMah = degradationMah,
            degradationPercent = degradationPercent,
            degradationMetrics = degradationMetricsData,
            degradationRatePerYear = degradationRate,
            measurementTimestamp = System.currentTimeMillis(),
            batteryPercentAtMeasurement = batteryPercent
        )
    }
    
    /**
     * Read design capacity using cascading fallback
     */
    fun getDesignCapacity(): CapacityReading {
        val estimates = mutableListOf<CapacityEstimate>()
        
        // Method 1: sysfs (if available)
        val sysfsMah = readSysfsFile(SYSFS_DESIGN)
        if (sysfsMah != null && sysfsMah >= MIN_VALID_CAPACITY) {
            estimates.add(CapacityEstimate(
                capacityMah = sysfsMah,
                confidence = 0.95f,
                source = "SYSFS_DESIGN"
            ))
        }
        
        // Method 2: Counter calculation at high SOC
        val soc = getBatteryPercent()
        val counter = getChargeCounterUah()
        if (soc >= 50 && counter > 0) {
            val estimated = ((counter / 1000.0) / (soc / 100.0)).toInt()
            val confidence = when (soc) {
                in 90..100 -> 0.90f
                in 80..89 -> 0.85f
                in 70..79 -> 0.75f
                in 60..69 -> 0.65f
                else -> 0.55f
            }
            if (estimated in MIN_VALID_CAPACITY..10000) {
                estimates.add(CapacityEstimate(
                    capacityMah = estimated,
                    confidence = confidence,
                    source = "COUNTER_BASED"
                ))
            }
        }
        
        // Method 3: Typical flagship estimate (last resort)
        estimates.add(CapacityEstimate(
            capacityMah = 4800, // Conservative estimate for flagship
            confidence = 0.30f, // Low confidence
            source = "TYPICAL_FLAGSHIP"
        ))
        
        // Weighted average
        val totalWeight = estimates.sumOf { it.confidence.toDouble() }.toFloat()
        val weightedCapacity = estimates.sumOf {
            it.capacityMah * it.confidence.toDouble()
        }.toFloat() / totalWeight
        
        val result = CapacityReading(
            capacityMah = weightedCapacity.toInt(),
            source = CapacitySource.MULTI_SOURCE_ESTIMATE,
            confidence = estimates.map { it.confidence }.average().toFloat(),
            isRawReading = false,
            metadata = mapOf(
                "used_estimators" to estimates.size.toString(),
                "sources" to estimates.joinToString("+") { it.source }
            )
        )
        return applyDualCellCorrection(result)
    }
    
    /**
     * Read current capacity using cascading fallback
     */
    fun getCurrentCapacity(deviceAgeYears: Float = 0f, resistanceMohm: Int = 0, voltageSagMv: Int = 0): CapacityReading {
        // Method 1: Read from sysfs charge_full (BEST)
        val sysfsMah = readSysfsFile(SYSFS_FULL)
        var result: CapacityReading
        if (sysfsMah != null && sysfsMah >= MIN_VALID_CAPACITY) {
            Log.i(TAG, "Current capacity from sysfs: $sysfsMah mAh")
            result = CapacityReading(
                capacityMah = sysfsMah,
                source = CapacitySource.SYSFS_FULL,
                confidence = 0.95f,
                isRawReading = true,
                metadata = mapOf("file_path" to SYSFS_FULL)
            )
            return applyDualCellCorrection(result)
        }
        
        // Method 2: Calculate from charge_counter (works at any SOC now via Estimator)
        val calculatedReading = calculateCurrentFromCounter(deviceAgeYears, resistanceMohm, voltageSagMv)
        if (calculatedReading != null && calculatedReading.capacityMah >= MIN_VALID_CAPACITY) {
            val batteryPercent = getBatteryPercent()
            Log.i(TAG, "Current capacity estimated: ${calculatedReading.capacityMah} mAh (at $batteryPercent%)")
            return applyDualCellCorrection(calculatedReading)
        }
        
        // Method 3: NOT AVAILABLE
        val batteryPercent = getBatteryPercent()
        Log.w(TAG, "Current capacity not available (SOC: $batteryPercent%)")
        return CapacityReading(
            capacityMah = 0,
            source = CapacitySource.NOT_AVAILABLE,
            confidence = 0f,
            isRawReading = false,
            metadata = mapOf(
                "reason" to "no_sysfs_and_soc_too_low",
                "battery_percent" to batteryPercent.toString(),
                "recommendation" to "charge_to_90_and_retest"
            )
        )
    }

    /**
     * Apply dual-cell correction if needed (based on sysfs readings)
     */
    private fun applyDualCellCorrection(reading: CapacityReading): CapacityReading {
        // Only correct sysfs readings that seem like half values
        if (reading.source != CapacitySource.SYSFS_DESIGN && 
            reading.source != CapacitySource.SYSFS_FULL) {
            return reading
        }
        
        // If capacity is suspiciously low (< 3000 mAh for modern phones)
        if (reading.capacityMah < 3000) {
            Log.i(TAG, "Dual-cell suspected: ${reading.capacityMah} mAh < 3000")
            
            return reading.copy(
                capacityMah = reading.capacityMah * 2,
                metadata = reading.metadata + mapOf(
                    "dual_cell_correction" to "applied",
                    "original_value" to reading.capacityMah.toString()
                )
            )
        }
        
        return reading
    }
    
    // ════════════════════════════════════════════════════
    // Private Helper Functions
    // ════════════════════════════════════════════════════
    
    private fun readSysfsFile(path: String): Int? {
        return try {
            val file = File(path)
            if (!file.exists()) {
                Log.d(TAG, "File not found: $path")
                return null
            }
            
            val content = file.readText().trim()
            val microAh = content.toLongOrNull()
            
            if (microAh == null || microAh <= 0) {
                Log.w(TAG, "Invalid value in $path: $content")
                return null
            }
            
            // Convert μAh to mAh
            val mAh = (microAh / 1000).toInt()
            
            Log.d(TAG, "Read from $path: $mAh mAh (raw: $microAh μAh)")
            mAh
            
        } catch (e: Exception) {
            Log.e(TAG, "Error reading $path: ${e.message}")
            null
        }
    }
    

    
    private fun calculateCurrentFromCounter(deviceAgeYears: Float, resistanceMohm: Int, voltageSagMv: Int): CapacityReading? {
        val chargeCounterUah = getChargeCounterUah()
        val soc = getBatteryPercent()
        
        if (chargeCounterUah <= 0 || soc < 10) {
            Log.d(TAG, "Cannot estimate: SOC=$soc%, counter=$chargeCounterUah")
            return null
        }
        
        // Get voltage directly 
        val voltageMv = getBatteryVoltage()
        
        Log.d(TAG, """
            Estimator inputs:
            - Counter: $chargeCounterUah μAh
            - SOC: $soc%
            - Voltage: $voltageMv mV
            - Resistance: $resistanceMohm mΩ
            - Voltage sag: $voltageSagMv mV
            - Age: $deviceAgeYears years
        """.trimIndent())

        val estimator = CapacityEstimator()
        return estimator.estimateCapacity(
            chargeCounterMah = (chargeCounterUah / 1000).toInt(),
            soc = soc,
            voltageMv = voltageMv,
            resistanceMohm = resistanceMohm,
            voltageSagMv = voltageSagMv,
            deviceAgeYears = deviceAgeYears
        )
    }
    
    private fun getChargeCounterUah(): Long {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting charge counter: ${e.message}")
            0L
        }
    }
    
    private fun getBatteryPercent(): Int {
        return try {
            val intent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery percent: ${e.message}")
            -1
        }
    }
    
    private fun getBatteryVoltage(): Int {
        return try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery voltage: ${e.message}")
            0
        }
    }

    private fun getDeviceAge(): Float {
        return try {
            val resistance = getLastMeasuredResistance()
            val (ageYears, confidence) = com.diagnostics.app.utils.DeviceAgeEstimator.estimateDeviceAgeYears(context)
            Log.d(TAG, "Device age: $ageYears years (confidence: $confidence)")
            ageYears
        } catch (e: Exception) {
            Log.e(TAG, "Error estimating device age: ${e.message}")
            0f
        }
    }

    /**
     * حساب التدهور مع معالجة القيم السالبة
     */
    private fun calculateDegradationMetrics(
        designCapacity: Int,
        currentCapacity: Int,
        deviceAgeYears: Float,
        resistance: Int
    ): DegradationMetrics {
        
        val rawDegradationMah = designCapacity - currentCapacity
        val rawDegradationPercent = if (designCapacity > 0) {
            (rawDegradationMah.toFloat() / designCapacity) * 100f
        } else {
            0f
        }
        
        val isLikelyNewDevice = deviceAgeYears < 0.5f && resistance < 60
        
        // للعرض: حد أدنى 0%
        val displayDegradationPercent = maxOf(0f, rawDegradationPercent)
        
        // الموثوقية
        val reliability = when {
            isLikelyNewDevice && rawDegradationPercent < 0 -> 0.3f
            deviceAgeYears > 2 -> 0.9f
            deviceAgeYears > 1 -> 0.8f
            else -> 0.6f
        }
        
        return DegradationMetrics(
            rawDegradationMah = rawDegradationMah,
            rawDegradationPercent = rawDegradationPercent,
            displayDegradationPercent = displayDegradationPercent,
            isLikelyNewDevice = isLikelyNewDevice,
            reliability = reliability
        )
    }

    /**
     * الحصول على آخر قياس للمقاومة من اختبار البطارية
     */
    private fun getLastMeasuredResistance(): Int {
        return try {
            val prefs = context.getSharedPreferences(
                "battery_test_cache", 
                Context.MODE_PRIVATE
            )
            prefs.getInt("last_resistance_mohm", 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading resistance: ${e.message}")
            0
        }
    }
}
