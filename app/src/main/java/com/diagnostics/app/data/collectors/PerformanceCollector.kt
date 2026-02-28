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

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock

class PerformanceCollector(private val context: Context) {
    
    fun collectPerformanceData(): PerformanceData {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        
        return PerformanceData(
            systemUptimeHours = getSystemUptime(),
            runningProcessesCount = getRunningProcessesCount(activityManager),
            runningServicesCount = getRunningServicesCount(activityManager),
            installedPackagesCount = getInstalledPackagesCount(),
            systemAppsCount = getSystemAppsCount(),
            userAppsCount = getUserAppsCount()
        )
    }
    
    private fun getSystemUptime(): Float {
        val uptimeMillis = SystemClock.elapsedRealtime()
        return uptimeMillis / (1000f * 60f * 60f) // Convert to hours
    }
    
    private fun getRunningProcessesCount(activityManager: ActivityManager): Int {
        return try {
            @Suppress("DEPRECATION")
            activityManager.runningAppProcesses?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getRunningServicesCount(activityManager: ActivityManager): Int {
        return try {
            @Suppress("DEPRECATION")
            activityManager.getRunningServices(Int.MAX_VALUE)?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getInstalledPackagesCount(): Int {
        return try {
            context.packageManager.getInstalledApplications(0).size
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getSystemAppsCount(): Int {
        return try {
            context.packageManager.getInstalledApplications(0).count { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getUserAppsCount(): Int {
        return try {
            context.packageManager.getInstalledApplications(0).count { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    data class PerformanceData(
        val systemUptimeHours: Float,
        val runningProcessesCount: Int,
        val runningServicesCount: Int,
        val installedPackagesCount: Int,
        val systemAppsCount: Int,
        val userAppsCount: Int
    )
}
