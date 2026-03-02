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
package com.diagnostics.app.utils.scoring

import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.DeviceScore

/**
 * ScoreCalculator — Calculates overall device health score (0-100)
 * based on battery, performance, storage, age, and features.
 *
 * Weights:
 *   Battery:     35%
 *   Performance: 20%
 *   Storage:     15%
 *   Age:         15%
 *   Features:    15%
 */
class ScoreCalculator {

    fun calculateOverallScore(scanData: DeviceScanData): DeviceScore {
        var totalScore = 100.0
        val strengths = mutableListOf<String>()
        val issues = mutableListOf<String>()

        // 1. Battery Score (35% weight)
        val batteryScore = calculateBatteryScore(scanData, strengths, issues)
        totalScore -= (100 - batteryScore) * 0.35

        // 2. Performance Score (20% weight)
        val performanceScore = calculatePerformanceScore(scanData, strengths, issues)
        totalScore -= (100 - performanceScore) * 0.20

        // 3. Storage Score (15% weight)
        val storageScore = calculateStorageScore(scanData, strengths, issues)
        totalScore -= (100 - storageScore) * 0.15

        // 4. Age Score (15% weight)
        val ageScore = calculateAgeScore(scanData, strengths, issues)
        totalScore -= (100 - ageScore) * 0.15

        // 5. Features Score (15% weight)
        val featuresScore = calculateFeaturesScore(scanData, strengths, issues)
        totalScore -= (100 - featuresScore) * 0.15

        val finalScore = totalScore.coerceIn(0.0, 100.0)

        return DeviceScore(
            overall = finalScore,
            battery = batteryScore,
            performance = performanceScore,
            storage = storageScore,
            age = ageScore,
            features = featuresScore,
            grade = getGrade(finalScore),
            strengths = strengths,
            issues = issues
        )
    }

    private fun calculateBatteryScore(
        scanData: DeviceScanData,
        strengths: MutableList<String>,
        issues: MutableList<String>
    ): Double {
        // capacityMetrics is nullable inside batteryStressResult
        val degradation = scanData.batteryStressResult.capacityMetrics
            ?.degradationPercent?.toDouble() ?: 50.0

        return when {
            degradation < 10 -> {
                strengths.add("battery_excellent")
                100.0
            }
            degradation < 20 -> {
                strengths.add("battery_good")
                80.0
            }
            degradation < 30 -> 60.0
            degradation < 40 -> {
                issues.add("battery_degradation_high")
                40.0
            }
            else -> {
                issues.add("battery_degradation_critical")
                20.0
            }
        }
    }

    private fun calculatePerformanceScore(
        scanData: DeviceScanData,
        strengths: MutableList<String>,
        issues: MutableList<String>
    ): Double {
        // performanceDegradationPercent is Float
        val perfDegradation = scanData.cpuStressResult.performanceDegradationPercent.toDouble()

        return when {
            perfDegradation < 5 -> {
                strengths.add("performance_excellent")
                100.0
            }
            perfDegradation < 15 -> 80.0
            perfDegradation < 30 -> 60.0
            else -> {
                issues.add("performance_degraded")
                40.0
            }
        }
    }

    private fun calculateStorageScore(
        scanData: DeviceScanData,
        strengths: MutableList<String>,
        issues: MutableList<String>
    ): Double {
        val storageType = scanData.storageStressResult.detectedType

        return when {
            storageType.contains("UFS_3", ignoreCase = true) -> {
                strengths.add("storage_fast")
                100.0
            }
            storageType.contains("UFS_2", ignoreCase = true) -> 80.0
            storageType.contains("UFS", ignoreCase = true) -> 70.0
            storageType.contains("eMMC", ignoreCase = true) -> 60.0
            else -> 50.0
        }
    }

    private fun calculateAgeScore(
        scanData: DeviceScanData,
        strengths: MutableList<String>,
        issues: MutableList<String>
    ): Double {
        // deviceAgeYears is Float
        val age = scanData.deviceAgeYears.toDouble()

        return when {
            age < 1.0 -> {
                strengths.add("device_new")
                100.0
            }
            age < 2.0 -> 85.0
            age < 3.0 -> 70.0
            age < 4.0 -> 55.0
            else -> {
                issues.add("device_old")
                40.0
            }
        }
    }

    private fun calculateFeaturesScore(
        scanData: DeviceScanData,
        strengths: MutableList<String>,
        issues: MutableList<String>
    ): Double {
        var score = 0.0
        var workingCount = 0

        // NFC — top-level field
        if (scanData.nfcAvailable) {
            score += 20.0
            workingCount++
        }

        // Bluetooth — top-level field
        if (scanData.bluetoothVersion.isNotEmpty()) {
            score += 20.0
            workingCount++
        }

        // WiFi — top-level field
        if (scanData.wifiAvailable) {
            score += 20.0
            workingCount++
        }

        // Cameras — top-level non-nullable Int
        if (scanData.cameraCount > 0) {
            score += 20.0
            workingCount++
        }

        // Sensors — top-level non-nullable Int
        if (scanData.sensorCountTotal > 10) {
            score += 20.0
            workingCount++
        }

        if (workingCount >= 4) {
            strengths.add("all_features_work")
        }

        return score
    }

    private fun getGrade(score: Double): String = when {
        score >= 85 -> "A"
        score >= 70 -> "B"
        score >= 55 -> "C"
        score >= 40 -> "D"
        else -> "F"
    }
}
