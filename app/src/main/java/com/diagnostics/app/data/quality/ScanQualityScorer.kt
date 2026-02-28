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
package com.diagnostics.app.data.quality

import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.DataQuality
import kotlin.math.abs

/**
 * ScanQualityScorer — Evaluates the quality and reliability of each device scan.
 *
 * Assigns a quality score (0-100) based on data completeness, battery data integrity,
 * voltage stability, and test contamination (e.g., charging during test).
 *
 * This helps filter low-quality samples from the ML training dataset.
 */
class ScanQualityScorer {

    /**
     * Score a complete scan and return DataQuality metadata.
     */
    fun scoreScan(scanData: DeviceScanData): DataQuality {
        val issues = mutableListOf<String>()
        var score = 100.0f

        // 1. Check for missing critical fields
        val missingFields = countMissingFields(scanData)
        score -= missingFields * 3f
        if (missingFields > 3) {
            issues.add("Missing critical fields: $missingFields")
        }

        // 2. Battery capacity sanity check
        val designCap = scanData.batteryDesignCapacityMah
        if (designCap <= 0 || designCap > 10000) {
            score -= 20f
            issues.add("Invalid battery capacity: ${designCap}mAh")
        }

        // 3. Check if device was charging during test
        if (scanData.isChargingDuringScan || scanData.chargingDetectedMidTest) {
            score -= 30f
            issues.add("Charging detected during test — data contaminated")
        }

        // 4. Voltage analysis
        val voltageGlitches = countVoltageGlitches(scanData)
        score -= voltageGlitches * 5f
        if (voltageGlitches > 0) {
            issues.add("Voltage glitches: $voltageGlitches")
        }

        // 5. Check if voltage was stalled (PMIC frozen)
        val isStalled = scanData.batteryStressResult.dataQuality?.pmicStalled ?: false
        if (isStalled) {
            score -= 15f
            issues.add("PMIC voltage sensor stalled")
        }

        // 6. Device age validity
        if (scanData.deviceAgeYears < 0.1f || scanData.deviceAgeYears > 15f) {
            score -= 10f
            issues.add("Suspicious device age: ${scanData.deviceAgeYears} years")
        }

        // 7. Check stress test completion
        val cpuScore = scanData.cpuStressResult.singleCoreScore
        val storageRead = scanData.storageStressResult.sequentialReadMbps
        if (cpuScore == 0 && storageRead == 0f) {
            score -= 25f
            issues.add("Stress tests appear to have failed")
        }

        // 8. Check time-series data availability
        val timeSeriesCount = scanData.batteryStressResult.timeSeriesData.size
        if (timeSeriesCount < 10) {
            score -= 10f
            issues.add("Insufficient time-series data: $timeSeriesCount samples")
        }

        // 9. Check voltage readings availability
        val voltageReadingsCount = scanData.batteryStressResult.voltageReadings.size
        if (voltageReadingsCount < 20) {
            score -= 10f
            issues.add("Insufficient voltage readings: $voltageReadingsCount")
        }

        // 10. Battery drain sanity check
        val drainRate = scanData.batteryDrainRatePerMinute
        if (drainRate < 0f || drainRate > 5f) {
            score -= 8f
            issues.add("Unusual drain rate: $drainRate %/min")
        }

        // Clamp score
        score = score.coerceIn(0f, 100f)

        // Determine quality level
        val overallQuality = when {
            score >= 80 -> "EXCELLENT"
            score >= 60 -> "GOOD"
            score >= 40 -> "FAIR"
            score >= 20 -> "POOR"
            else -> "INVALID"
        }

        // Reuse existing DataQuality model
        val currentGlitches = scanData.batteryStressResult.dataQuality?.currentGlitches ?: 0

        return DataQuality(
            pmicStalled = isStalled,
            voltageGlitches = voltageGlitches,
            currentGlitches = currentGlitches,
            overallQuality = overallQuality,
            confidenceScore = score
        )
    }

    /**
     * Count critical fields that are empty/default/zero.
     */
    private fun countMissingFields(s: DeviceScanData): Int {
        var count = 0
        if (s.deviceManufacturer.isBlank()) count++
        if (s.deviceModel.isBlank()) count++
        if (s.cpuArchitecture.isBlank()) count++
        if (s.ramTotalMb == 0L) count++
        if (s.cpuMaxFreqMhz == 0) count++
        if (s.cpuCoreCount == 0) count++
        if (s.batteryDesignCapacityMah == 0) count++
        if (s.screenWidthPx == 0) count++
        return count
    }

    /**
     * Count voltage glitches (sudden jumps > 200mV between consecutive readings).
     */
    private fun countVoltageGlitches(s: DeviceScanData): Int {
        val readings = s.batteryStressResult.voltageReadings
        if (readings.size < 3) return 0

        var glitches = 0
        for (i in 1 until readings.size) {
            val diff = abs(readings[i].voltageMv - readings[i - 1].voltageMv)
            if (diff > 200) {
                glitches++
            }
        }
        return glitches
    }
}
