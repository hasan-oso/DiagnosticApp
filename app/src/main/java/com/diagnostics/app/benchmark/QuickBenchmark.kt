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
package com.diagnostics.app.benchmark

import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

object QuickBenchmark {
    
    /**
     * Quick 5-second CPU benchmark to classify device tier
     * Returns: estimated CPU score (similar to Geekbench single-core)
     */
    fun getCPUScore(): Int {
        var operations = 0
        val duration = measureTimeMillis {
            val endTime = System.currentTimeMillis() + 5000 // 5 seconds
            
            while (System.currentTimeMillis() < endTime) {
                // CPU-intensive operations
                var result = 0.0
                for (i in 1..10000) {
                    result += sqrt(i.toDouble())
                    result *= 1.00001
                }
                operations++
            }
        }
        
        // Normalize: operations per second, scaled to 0-70 range
        val opsPerSecond = (operations.toDouble() / duration) * 1000
        val maxReference = 50_000.0
        return (opsPerSecond / maxReference * 70.0).coerceIn(0.0, 70.0).toInt()
    }

    /**
     * Classify device into performance tier dynamically
     */
    fun calculatePerformanceTier(
        cpuMaxFreqMhz: Int,
        cpuCoreCount: Int,
        ramTotalMb: Int,
        gpuRenderer: String,
        androidSdk: Int
    ): String {
        var score = 0

        score += when {
            cpuMaxFreqMhz >= 3000 -> 40
            cpuMaxFreqMhz >= 2700 -> 35
            cpuMaxFreqMhz >= 2400 -> 28
            cpuMaxFreqMhz >= 2000 -> 20
            else -> 10
        }
        score += when {
            ramTotalMb >= 10000 -> 25
            ramTotalMb >= 8000  -> 20
            ramTotalMb >= 6000  -> 15
            ramTotalMb >= 4000  -> 10
            else -> 5
        }
        score += when {
            gpuRenderer.contains("Adreno 7", ignoreCase = true) -> 25
            gpuRenderer.contains("Adreno 6", ignoreCase = true) -> 20
            gpuRenderer.contains("Mali-G7",  ignoreCase = true)  -> 18
            gpuRenderer.contains("Mali-G6",  ignoreCase = true)  -> 15
            gpuRenderer.contains("Mali-G5",  ignoreCase = true)  -> 10
            else -> 8
        }
        score += when {
            androidSdk >= 34 -> 10
            androidSdk >= 31 -> 8
            androidSdk >= 28 -> 5
            else -> 2
        }

        return when {
            score >= 85 -> "FLAGSHIP"
            score >= 65 -> "HIGH_END"
            score >= 45 -> "MID_RANGE"
            score >= 25 -> "BUDGET"
            else        -> "ENTRY_LEVEL"
        }
    }

    /**
     * Calculate normalized score 0-100 based on iterations per second
     */
    fun calculateNormalizedScore(
        cpuTestIterations: Long,
        scanDurationSeconds: Int,
        cpuMaxFreqMhz: Int,
        ramTotalMb: Int
    ): Int {
        val iterPerSec = if (scanDurationSeconds > 0)
            cpuTestIterations / scanDurationSeconds.toFloat()
        else cpuTestIterations.toFloat()

        val cpuScore  = (iterPerSec / 50_000f * 70f).coerceIn(0f, 70f)
        val ramScore  = (ramTotalMb  / 12_000f * 15f).coerceIn(0f, 15f)
        val freqScore = (cpuMaxFreqMhz / 3_500f * 15f).coerceIn(0f, 15f)

        return (cpuScore + ramScore + freqScore).toInt().coerceIn(0, 100)
    }
}
