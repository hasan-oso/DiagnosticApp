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
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import java.io.File

class MemoryCollector(private val context: Context) {
    
    fun collectMemoryData(): MemoryData {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        return MemoryData(
            ramTotalMb = memoryInfo.totalMem / (1024 * 1024),
            ramAvailableMb = memoryInfo.availMem / (1024 * 1024),
            ramLowThresholdMb = memoryInfo.threshold / (1024 * 1024),
            storageTotalGb = getInternalStorageTotal(),
            storageAvailableGb = getInternalStorageAvailable(),
            externalStorageAvailable = isExternalStorageAvailable(),
            adoptableStorageEnabled = isAdoptableStorageEnabled(),
            storageEmulated = Environment.isExternalStorageEmulated()
        )
    }
    
    private fun getInternalStorageTotal(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalBytes = stat.blockCountLong * stat.blockSizeLong
            totalBytes / (1024 * 1024 * 1024) // Convert to GB
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getInternalStorageAvailable(): Long {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            availableBytes / (1024 * 1024 * 1024) // Convert to GB
        } catch (e: Exception) {
            0
        }
    }
    
    private fun isExternalStorageAvailable(): Boolean {
        val state = Environment.getExternalStorageState()
        return state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    }
    
    private fun isAdoptableStorageEnabled(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val volumes = storageManager.storageVolumes
                volumes.any { volume ->
                    // Check if any volume is adoptable
                    try {
                        volume.state == Environment.MEDIA_MOUNTED && 
                        !volume.isPrimary &&
                        volume.isRemovable
                    } catch (e: Exception) {
                        false
                    }
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    data class MemoryData(
        val ramTotalMb: Long,
        val ramAvailableMb: Long,
        val ramLowThresholdMb: Long,
        val storageTotalGb: Long,
        val storageAvailableGb: Long,
        val externalStorageAvailable: Boolean,
        val adoptableStorageEnabled: Boolean,
        val storageEmulated: Boolean
    )
}
