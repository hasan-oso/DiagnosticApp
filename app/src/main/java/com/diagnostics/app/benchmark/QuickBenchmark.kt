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
        
        // Normalize: operations per second, scaled to ~Geekbench range
        val opsPerSecond = (operations.toDouble() / duration) * 1000
        return (opsPerSecond * 50).toInt() // Scaling factor
    }
    
    /**
     * Classify device into performance tier
     */
    fun getDeviceTier(cpuScore: Int): DeviceTier {
        return when {
            cpuScore >= 2000 -> DeviceTier.FLAGSHIP      // 2024 flagships
            cpuScore >= 1200 -> DeviceTier.HIGH_END      // 2022-2023 flagships
            cpuScore >= 700  -> DeviceTier.MID_RANGE     // Mid-range devices
            cpuScore >= 400  -> DeviceTier.BUDGET        // Budget devices
            else             -> DeviceTier.ENTRY_LEVEL   // Very old/weak
        }
    }
    
    enum class DeviceTier {
        FLAGSHIP,      // S24 Ultra, Pixel 9 Pro
        HIGH_END,      // S22, Pixel 7
        MID_RANGE,     // A54, Pixel 6a
        BUDGET,        // A30s, older devices
        ENTRY_LEVEL    // Very weak devices
    }
}
