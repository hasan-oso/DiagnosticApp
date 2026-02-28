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
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager

class DisplayCollector(private val context: Context) {
    
    fun collectDisplayData(): DisplayData {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display ?: windowManager.defaultDisplay
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        }
        
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display?.getRealMetrics(metrics)
        
        return DisplayData(
            screenWidthPx = metrics.widthPixels,
            screenHeightPx = metrics.heightPixels,
            screenDensityDpi = metrics.densityDpi,
            screenRefreshRateHz = display?.refreshRate ?: 60f,
            screenRefreshRatesSupported = getSupportedRefreshRates(display),
            displayHdrCapabilities = getHdrCapabilities(display),
            wideColorGamutSupport = checkWideColorGamut(display),
            screenBrightnessMode = "AUTO" // Placeholder, requires Settings permission
        )
    }
    
    private fun getSupportedRefreshRates(display: Display?): List<Float> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && display != null) {
                display.supportedModes.map { it.refreshRate }.distinct().sorted()
            } else {
                listOf(display?.refreshRate ?: 60f)
            }
        } catch (e: Exception) {
            listOf(60f)
        }
    }
    
    private fun getHdrCapabilities(display: Display?): List<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && display != null) {
                val hdrCapabilities = display.hdrCapabilities
                val types = mutableListOf<String>()
                
                hdrCapabilities?.supportedHdrTypes?.forEach { type ->
                    types.add(when (type) {
                        Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "DOLBY_VISION"
                        Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                        Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                        Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10_PLUS"
                        else -> "UNKNOWN_$type"
                    })
                }
                
                types
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun checkWideColorGamut(display: Display?): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && display != null) {
                display.isWideColorGamut
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    data class DisplayData(
        val screenWidthPx: Int,
        val screenHeightPx: Int,
        val screenDensityDpi: Int,
        val screenRefreshRateHz: Float,
        val screenRefreshRatesSupported: List<Float>,
        val displayHdrCapabilities: List<String>,
        val wideColorGamutSupport: Boolean,
        val screenBrightnessMode: String
    )
}
