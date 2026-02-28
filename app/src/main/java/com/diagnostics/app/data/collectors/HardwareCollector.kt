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
import android.opengl.GLES20
import android.os.Build
import java.io.File
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import com.diagnostics.app.data.DeviceReleaseDatabase

class HardwareCollector(private val context: Context) {
    
    fun collectHardwareData(): HardwareData {
        return HardwareData(
            cpuArchitecture = Build.SUPPORTED_ABIS[0],
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            cpuMaxFreqMhz = getCpuMaxFrequency(),
            cpuMinFreqMhz = getCpuMinFrequency(),
            gpuRenderer = getGpuRenderer(),
            gpuVendor = getGpuVendor(),
            openglVersion = getOpenGLVersion(),
            vulkanVersion = getVulkanVersion(),
            npuAvailable = checkNpuAvailability()
        )
    }

    fun getDeviceAge(): Float {
        // Method 1: Try to get boot time of the very first OS installation (if preserved in some custom ROMs)
        // Usually btime in /proc/stat is just the last boot time, so it's not a reliable indicator of actual device age.
        // However, some systems might have files that indicate factory date. We'll stick to Build.TIME as the primary reliable on-device metric.
        
        // Method 2: Use Build.TIME (kernel build time is usually very close to manufacture date for factory OS)
        val buildTimeMs = Build.TIME
        
        // Let's also try to read /proc/stat just in case we can find a better metric, but typically btime is last boot.
        // As a heuristic for *device* age, Build.TIME is the safest non-database approach.
        val currentTimeMs = System.currentTimeMillis()
        
        if (buildTimeMs > 0 && buildTimeMs < currentTimeMs) {
            val ageMs = currentTimeMs - buildTimeMs
            val ageYears = ageMs / (1000f * 60f * 60f * 24f * 365.25f)
            android.util.Log.d("DeviceAge", "Device age from Build.TIME: $ageYears years")
            return ageYears.coerceAtLeast(0.1f) // Return at least 0.1 years to avoid div by zero downstream
        }
        
        // Fallback if everything fails
        android.util.Log.w("DeviceAge", "Could not determine device age from device APIs, defaulting to 2.0 years")
        return 2.0f
    }
    
    private fun getCpuMaxFrequency(): Int {
        return try {
            val cores = Runtime.getRuntime().availableProcessors()
            var maxFreq = 0
            
            for (i in 0 until cores) {
                val file = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (file.exists() && file.canRead()) {
                    val freq = file.readText().trim().toIntOrNull() ?: 0
                    if (freq > maxFreq) maxFreq = freq
                }
            }
            
            maxFreq / 1000 // Convert kHz to MHz
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getCpuMinFrequency(): Int {
        return try {
            val coreCount = Runtime.getRuntime().availableProcessors()
            // Check all cores and return the minimum (little cores have lowest min freq)
            val minFreqs = (0 until coreCount).mapNotNull { core ->
                val freqFile = File("/sys/devices/system/cpu/cpu$core/cpufreq/cpuinfo_min_freq")
                if (freqFile.exists() && freqFile.canRead()) {
                    val freq = freqFile.readText().trim().toIntOrNull()
                    if (freq != null && freq > 0) freq / 1000 else null // Convert kHz to MHz
                } else null
            }
            minFreqs.minOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getGpuRenderer(): String {
        // Try to get actual GL_RENDERER string from OpenGL
        val glRenderer = tryGetGlRenderer()
        if (glRenderer.isNotBlank() && glRenderer != "Unknown") {
            return glRenderer
        }
        
        // Fallback: Derive from hardware info
        return try {
            when {
                Build.HARDWARE.contains("qcom", ignoreCase = true) -> {
                    // Qualcomm Adreno variants
                    when {
                        Build.DEVICE.contains("sm8", ignoreCase = true) -> "Adreno 6xx/7xx"
                        Build.DEVICE.contains("sm7", ignoreCase = true) -> "Adreno 6xx"
                        else -> "Adreno"
                    }
                }
                Build.HARDWARE.contains("exynos", ignoreCase = true) || 
                Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> "ARM Mali"
                Build.HARDWARE.contains("mt", ignoreCase = true) -> "ARM Mali (MediaTek)"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun tryGetGlRenderer(): String {
        return try {
            // Note: This requires an OpenGL context which is typically only available
            // when rendering. If called from background, this will likely fail.
            // We suppress the error and fall back to hardware detection.
            android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER) ?: "Unknown"
        } catch (e: Exception) {
            // No GL context available, fall back to hardware detection
            "Unknown"
        }
    }
    
    private fun getGpuVendor(): String {
        return try {
            when {
                Build.HARDWARE.contains("qcom", ignoreCase = true) -> "Qualcomm"
                Build.HARDWARE.contains("exynos", ignoreCase = true) -> "Samsung Exynos"
                Build.HARDWARE.contains("mt", ignoreCase = true) -> "MediaTek"
                Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> "Samsung"
                Build.MANUFACTURER.equals("huawei", ignoreCase = true) -> "Huawei Kirin"
                Build.MANUFACTURER.equals("google", ignoreCase = true) -> "Google Tensor"
                else -> Build.HARDWARE
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getOpenGLVersion(): String {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val configInfo = am.deviceConfigurationInfo
            if (configInfo.reqGlEsVersion != 0) {
                val major = configInfo.reqGlEsVersion shr 16
                val minor = configInfo.reqGlEsVersion and 0xFFFF
                "$major.$minor"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getVulkanVersion(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val pm = context.packageManager
                if (pm.hasSystemFeature("android.hardware.vulkan.version")) {
                    val version = pm.getSystemAvailableFeatures()
                        .find { it.name == "android.hardware.vulkan.version" }
                        ?.version ?: 0
                    
                    if (version > 0) {
                        val major = (version shr 22) and 0x3FF
                        val minor = (version shr 12) and 0x3FF
                        val patch = version and 0xFFF
                        "$major.$minor.$patch"
                    } else {
                        "Not Supported"
                    }
                } else {
                    "Not Supported"
                }
            } else {
                "Not Supported (Android < 7.0)"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun checkNpuAvailability(): Boolean {
        return try {
            // Check for common NPU indicators
            val hardware = Build.HARDWARE.lowercase()
            hardware.contains("npu") || 
            hardware.contains("hexagon") || // Qualcomm AI Engine
            Build.MODEL.contains("Pixel", ignoreCase = true) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        } catch (e: Exception) {
            false
        }
    }
    
    data class HardwareData(
        val cpuArchitecture: String,
        val supportedAbis: List<String>,
        val cpuCoreCount: Int,
        val cpuMaxFreqMhz: Int,
        val cpuMinFreqMhz: Int,
        val gpuRenderer: String,
        val gpuVendor: String,
        val openglVersion: String,
        val vulkanVersion: String,
        val npuAvailable: Boolean
    )
}
