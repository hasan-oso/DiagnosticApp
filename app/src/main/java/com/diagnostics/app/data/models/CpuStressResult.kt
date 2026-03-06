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
 * Institution: OSTİM Technical University
 */
package com.diagnostics.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class CpuStressResult(
    val durationSeconds: Int,
    val maxTempCelsius: Float,
    val throttlingStartSec: Int?,
    val performanceDegradationPercent: Float,
    val singleCoreScore: Int,
    val multiCoreScore: Int,
    val quickBenchmarkScore: Int = 0,             // New field
    val devicePerformanceTier: String = "",       // New field
    val cpuTestIterations: Long = 0,              // New field
    val tempReadings: List<TempReading>,        // كل 5 ثواني
    val freqReadings: List<FreqReading>
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "performanceDegradationPercent" to performanceDegradationPercent,
            "peakTemperatureCelsius" to maxTempCelsius,
            "thermalThrottlingDetected" to (throttlingStartSec != null),
            "baselineScore" to (quickBenchmarkScore.takeIf { it in 0..100 } ?: 0),
            "stressScore" to ((quickBenchmarkScore.takeIf { it in 0..100 } ?: 0) * (1f - (performanceDegradationPercent / 100f))).toInt()
        )
    }
}

@Serializable
data class TempReading(
    val timestampSec: Int,
    val tempCelsius: Float
)

@Serializable
data class FreqReading(
    val timestampSec: Int,
    val freqMhz: Int
)
