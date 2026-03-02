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
 * Institution: OSTIM Technical University
 */
package com.diagnostics.app.utils.pricing

import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.PriceRange

/**
 * PriceCalculator — Estimates a suggested price range for the device
 * based on model, age, battery health, and detected issues.
 *
 * Prices are in Turkish Lira (TRY).
 */
class PriceCalculator {

    fun calculateSuggestedPrice(scanData: DeviceScanData): PriceRange {
        // 1. Get base price for model
        val basePrice = getBasePriceForModel(scanData.deviceModel)

        // 2. Age multiplier (deviceAgeYears is Float)
        val ageMultiplier = when {
            scanData.deviceAgeYears < 1.0f -> 0.85
            scanData.deviceAgeYears < 2.0f -> 0.70
            scanData.deviceAgeYears < 3.0f -> 0.55
            scanData.deviceAgeYears < 4.0f -> 0.40
            else -> 0.30
        }

        // 3. Battery multiplier (capacityMetrics is nullable)
        val degradation = scanData.batteryStressResult.capacityMetrics
            ?.degradationPercent?.toDouble() ?: 20.0
        val batteryMultiplier = when {
            degradation < 10 -> 1.00
            degradation < 20 -> 0.95
            degradation < 30 -> 0.85
            else -> 0.75
        }

        // 4. Issues multiplier
        val issuesCount = countIssues(scanData)
        val issuesMultiplier = when (issuesCount) {
            0 -> 1.00
            1 -> 0.95
            2 -> 0.90
            else -> 0.80
        }

        // 5. Calculate final price
        val adjustedPrice = basePrice * ageMultiplier * batteryMultiplier * issuesMultiplier

        // 6. Create range (±10%)
        val min = (adjustedPrice * 0.90).toInt()
        val max = (adjustedPrice * 1.10).toInt()

        val condition = getConditionText(degradation)
        val confidence = calculateConfidence(scanData)

        return PriceRange(
            min = min,
            max = max,
            condition = condition,
            confidence = confidence
        )
    }

    private fun getBasePriceForModel(model: String): Double {
        return when {
            // Samsung Flagships
            model.contains("SM-S928") -> 45000.0  // S24 Ultra
            model.contains("SM-S926") -> 38000.0  // S24+
            model.contains("SM-S921") -> 32000.0  // S24
            model.contains("SM-S918") -> 35000.0  // S23 Ultra
            model.contains("SM-S916") -> 28000.0  // S23+
            model.contains("SM-S911") -> 24000.0  // S23
            model.contains("SM-G998") -> 25000.0  // S21 Ultra
            model.contains("SM-G991") -> 18000.0  // S21
            model.contains("SM-G977") -> 12000.0  // S10 5G

            // Samsung Mid-range
            model.contains("SM-A546") -> 12000.0  // A54
            model.contains("SM-A536") -> 10000.0  // A53
            model.contains("SM-A346") -> 8000.0   // A34
            model.contains("SM-A256") -> 7000.0   // A25
            model.contains("SM-A156") -> 6000.0   // A15

            // Xiaomi Flagships
            model.contains("23090RA98G") -> 25000.0  // 14 Pro
            model.contains("22101316G") -> 18000.0   // 12 Pro

            // Xiaomi Mid-range
            model.contains("Redmi Note 13") -> 8000.0
            model.contains("Redmi Note 12") -> 7000.0
            model.contains("Redmi Note 11") -> 6000.0
            model.contains("Redmi Note 10") -> 5000.0
            model.contains("Redmi Note 8") -> 3500.0

            // iPhone (if model string detected)
            model.contains("iPhone15") -> 45000.0
            model.contains("iPhone14") -> 35000.0
            model.contains("iPhone13") -> 28000.0
            model.contains("iPhone12") -> 22000.0
            model.contains("iPhone11") -> 16000.0

            // Default
            else -> 5000.0
        }
    }

    private fun countIssues(scanData: DeviceScanData): Int {
        var count = 0

        // Battery degradation
        val degradation = scanData.batteryStressResult.capacityMetrics
            ?.degradationPercent?.toDouble() ?: 0.0
        if (degradation > 30) count++

        // Performance degradation
        if (scanData.cpuStressResult.performanceDegradationPercent > 30f) count++

        // Age
        if (scanData.deviceAgeYears > 4.0f) count++

        return count
    }

    private fun getConditionText(degradation: Double): String {
        return when {
            degradation < 10 -> "حالة ممتازة / Mükemmel durum / Excellent condition"
            degradation < 20 -> "حالة جيدة جداً / Çok iyi durum / Very good condition"
            degradation < 30 -> "حالة جيدة / İyi durum / Good condition"
            else -> "حالة مقبولة / Orta durum / Fair condition"
        }
    }

    private fun calculateConfidence(scanData: DeviceScanData): Double {
        var confidence = 0.7

        // Increase if we have quality data
        if (scanData.batteryStressResult.dataQuality != null) {
            confidence += 0.15
        }

        // Increase if device age is known with confidence
        if (scanData.deviceAgeYears > 0.1f) {
            confidence += 0.10
        }

        // Increase if we have capacity metrics
        if (scanData.batteryStressResult.capacityMetrics != null) {
            confidence += 0.05
        }

        return confidence.coerceIn(0.0, 1.0)
    }
}
