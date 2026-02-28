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

import android.util.Log
import java.io.File

/**
 * Collects additional CPU information not available via standard Android APIs.
 * All reads are read-only sysfs files — no root required.
 */
object CPUInfoCollector {

    private const val TAG = "CPUInfoCollector"

    /**
     * Get the current CPU frequency scaling governor (e.g. "schedutil", "performance").
     */
    fun getCPUGovernor(): String {
        return try {
            val file = File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")
            if (file.exists() && file.canRead()) {
                file.readText().trim().also {
                    Log.d(TAG, "CPU governor: $it")
                }
            } else {
                Log.w(TAG, "Governor file not accessible")
                "unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get governor: ${e.message}")
            "unknown"
        }
    }

    /**
     * Get big/little core split by comparing max frequencies.
     * Returns Pair(bigCores, littleCores).
     */
    fun getCoreSplit(): Pair<Int, Int> {
        return try {
            val coreCount = Runtime.getRuntime().availableProcessors()
            val frequencies = mutableListOf<Int>()

            for (i in 0 until coreCount) {
                val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (file.exists() && file.canRead()) {
                    val freq = file.readText().trim().toIntOrNull()
                    if (freq != null && freq > 0) frequencies.add(freq)
                }
            }

            if (frequencies.isEmpty()) {
                Log.w(TAG, "No frequency data — returning unknown split")
                return Pair(0, coreCount)
            }

            // Find the median frequency as the threshold between big/little clusters
            val sorted = frequencies.sorted()
            val median = sorted[sorted.size / 2]

            val bigCores = frequencies.count { it >= median }
            val littleCores = frequencies.count { it < median }

            Log.d(TAG, "Core split: $bigCores big + $littleCores little (median=${median}kHz)")
            Pair(bigCores, littleCores)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get core split: ${e.message}")
            Pair(0, Runtime.getRuntime().availableProcessors())
        }
    }
}
