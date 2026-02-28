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
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

class PreflightChecker(private val context: Context) {

    companion object {
        const val MAX_BATTERY_TEMP_CELSIUS = 50.0f // Battery safe threshold
    }

    data class PreflightResult(
        val canProceed: Boolean,
        val batteryLevel: Int,
        val freeStorageGB: Double,
        val cpuTempCelsius: Float?,
        val errors: List<String>
    )

    fun performPreflightChecks(): PreflightResult {
        val errors = mutableListOf<String>()

        // Check 1: Battery Level
        val batteryLevel = getBatteryLevel()
        if (batteryLevel < 50) {
            errors.add("Battery too low: $batteryLevel%. Please charge to 50%+ before starting.")
        }

        // Check 2: Free Storage
        val freeSpaceGB = getAvailableStorageGB()
        if (freeSpaceGB < 1.0) {
            errors.add("Low storage: ${String.format("%.2f", freeSpaceGB)}GB. Need 1GB+ free space for stress tests.")
        }

        // Check 3: Battery Temperature
        val batteryTemp = getBatteryTemperature()
        if (batteryTemp != null && batteryTemp > MAX_BATTERY_TEMP_CELSIUS) {
            errors.add("Battery too hot: ${String.format("%.1f", batteryTemp)}°C. Let it cool below 50°C for safety.")
        }

        return PreflightResult(
            canProceed = errors.isEmpty(),
            batteryLevel = batteryLevel,
            freeStorageGB = freeSpaceGB,
            cpuTempCelsius = batteryTemp,
            errors = errors
        )
    }

    private fun getBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            100 // Assume full if we can't read
        }
    }

    private fun getAvailableStorageGB(): Double {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            bytesAvailable / (1024.0 * 1024.0 * 1024.0)
        } catch (e: Exception) {
            10.0 // Assume enough if we can't read
        }
    }

    private fun getBatteryTemperature(): Float? {
        return try {
            // Method 1: Try sysfs battery temp (most reliable)
            val tempFile = File("/sys/class/power_supply/battery/temp")
            if (tempFile.exists() && tempFile.canRead()) {
                val tempRaw = tempFile.readText().trim().toIntOrNull()
                if (tempRaw != null) {
                    // sysfs reports in decidegrees Celsius (e.g. 350 = 35.0°C)
                    return tempRaw / 10.0f
                }
            }

            // Method 2: BatteryManager intent (fallback, works on all devices)
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val temp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
            if (temp > 0) {
                // EXTRA_TEMPERATURE is in tenths of a degree Celsius
                return temp / 10.0f
            }

            // Method 3: Return safe default if all methods fail
            25.0f
        } catch (e: Exception) {
            Log.e("ThermalSafety", "Failed to read battery temp: ${e.message}")
            25.0f // Safe default — don't block scan on read failure
        }
    }
}
