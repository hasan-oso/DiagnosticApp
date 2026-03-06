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

data class StressTestConfig(
    val cpuIterationsPerCore: Long,
    val cpuTestDuration: Int,        // seconds
    val storageFileCount: Int,
    val storageFileSizeMB: Int,
    val batteryTestDuration: Int     // seconds
)

object AdaptiveStressConfig {
    
    fun getConfig(deviceTier: String): StressTestConfig {
        return when (deviceTier) {
            "FLAGSHIP" -> StressTestConfig(
                cpuIterationsPerCore = 5_000_000L,
                cpuTestDuration = 480,      // 8 minutes
                storageFileCount = 15,
                storageFileSizeMB = 100,
                batteryTestDuration = 165   // 165s load + 15s baseline = 3 minutes
            )
            
            "HIGH_END" -> StressTestConfig(
                cpuIterationsPerCore = 3_000_000L,
                cpuTestDuration = 480,
                storageFileCount = 12,
                storageFileSizeMB = 100,
                batteryTestDuration = 165   // 165s load + 15s baseline = 3 minutes
            )
            
            "MID_RANGE" -> StressTestConfig(
                cpuIterationsPerCore = 2_000_000L,
                cpuTestDuration = 420,      // 7 minutes
                storageFileCount = 10,
                storageFileSizeMB = 100,
                batteryTestDuration = 165   // 165s load + 15s baseline = 3 minutes
            )
            
            "BUDGET" -> StressTestConfig(
                cpuIterationsPerCore = 1_000_000L,
                cpuTestDuration = 360,      // 6 minutes
                storageFileCount = 8,
                storageFileSizeMB = 80,
                batteryTestDuration = 165   // 165s load + 15s baseline = 3 minutes
            )
            
            else -> StressTestConfig(
                cpuIterationsPerCore = 500_000L,
                cpuTestDuration = 300,      // 5 minutes
                storageFileCount = 5,
                storageFileSizeMB = 50,
                batteryTestDuration = 165   // 165s load + 15s baseline = 3 minutes
            )
        }
    }
}
