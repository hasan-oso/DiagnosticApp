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
 * Institution: OSTIM Technical University
 */
package com.diagnostics.app.data.collectors

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.WindowManager

class DisplayCollector(private val context: Context) {

    fun collectDisplayData(): DisplayData {
        return try {
            val metrics = context.resources.displayMetrics
            DisplayData(
                screenWidthPx = metrics.widthPixels,
                screenHeightPx = metrics.heightPixels,
                screenDensityDpi = metrics.densityDpi,
                screenRefreshRateHz = getRefreshRateSafe(),
                screenRefreshRatesSupported = getSupportedRefreshRatesSafe(),
                displayHdrCapabilities = getHdrCapabilitiesSafe(),
                wideColorGamutSupport = checkWideColorGamutSafe(),
                screenBrightnessMode = "AUTO"
            )
        } catch (e: Exception) {
            Log.e("DisplayCollector", "Error collecting display info", e)
            getDefaultDisplayData()
        }
    }

    private fun getRefreshRateSafe(): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.refreshRate ?: 60f
            } else {
                60f
            }
        } catch (e: Exception) {
            60f
        }
    }

    private fun getSupportedRefreshRatesSafe(): List<Float> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.supportedModes
                    ?.map { it.refreshRate }?.distinct()?.sorted()
                    ?: listOf(60f)
            } else {
                listOf(60f)
            }
        } catch (e: Exception) {
            listOf(60f)
        }
    }

    private fun getHdrCapabilitiesSafe(): List<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                val display = windowManager?.defaultDisplay
                val types = mutableListOf<String>()
                display?.hdrCapabilities?.supportedHdrTypes?.forEach { type ->
                    types.add(
                        when (type) {
                            Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "DOLBY_VISION"
                            Display.HdrCapabilities.HDR_TYPE_HDR10        -> "HDR10"
                            Display.HdrCapabilities.HDR_TYPE_HLG          -> "HLG"
                            Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS   -> "HDR10_PLUS"
                            else                                           -> "UNKNOWN_$type"
                        }
                    )
                }
                types
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun checkWideColorGamutSafe(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                @Suppress("DEPRECATION")
                windowManager?.defaultDisplay?.isWideColorGamut ?: false
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getDefaultDisplayData(): DisplayData {
        return DisplayData(
            screenWidthPx = 1080,
            screenHeightPx = 2400,
            screenDensityDpi = 420,
            screenRefreshRateHz = 60f,
            screenRefreshRatesSupported = listOf(60f),
            displayHdrCapabilities = emptyList(),
            wideColorGamutSupport = false,
            screenBrightnessMode = "AUTO"
        )
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
