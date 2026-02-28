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
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import java.io.File

class SecurityCollector(private val context: Context) {
    
    fun collectSecurityData(): SecurityData {
        return SecurityData(
            playServicesAvailable = isPlayServicesAvailable(),
            deviceIntegrityVerdict = checkDeviceIntegrity(),
            unknownSourcesCount = countUnknownSourceApps(),
            developerOptionsEnabled = isDeveloperOptionsEnabled(),
            adbEnabled = isAdbEnabled(),
            usbDebuggingEnabled = isUsbDebuggingEnabled(),
            verifiedBootState = getVerifiedBootState()
        )
    }
    
    data class SecurityData(
        val playServicesAvailable: Boolean,
        val deviceIntegrityVerdict: String,
        val unknownSourcesCount: Int,
        val developerOptionsEnabled: Boolean,
        val adbEnabled: Boolean,
        val usbDebuggingEnabled: Boolean,
        val verifiedBootState: String
    )
    
    private fun isPlayServicesAvailable(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun checkDeviceIntegrity(): String {
        // Check for custom ROM indicators
        if (Build.TAGS.contains("test-keys")) {
            return "CUSTOM_ROM_DETECTED"
        }
        
        // Check for root
        if (isDeviceRooted()) {
            return "ROOTED"
        }
        
        // Check for unlocked bootloader (some devices)
        if (Build.TAGS.contains("dev-keys") || Build.TYPE == "userdebug") {
            return "BOOTLOADER_UNLOCKED"
        }
        
        return "GENUINE"
    }
    
    private fun isDeviceRooted(): Boolean {
        // Check for common root files
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in rootPaths) {
            if (File(path).exists()) {
                return true
            }
        }
        
        // Check for su command execution
        return canExecuteSuCommand()
    }
    
    private fun canExecuteSuCommand(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("su")
            process.destroy()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun countUnknownSourceApps(): Int {
        return try {
            val packages = context.packageManager.getInstalledApplications(0)
            packages.count { app ->
                (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    private fun isUsbDebuggingEnabled(): Boolean {
        return isAdbEnabled() // Same as ADB
    }
    
    private fun getVerifiedBootState(): String {
        return try {
            // This is device-specific and may not be available on all devices
            val bootState = Build.FINGERPRINT
            when {
                bootState.contains("\\release-keys") -> "VERIFIED"
                bootState.contains("test-keys") -> "UNVERIFIED"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
}
