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
package com.diagnostics.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ThermalData(
    val cpuTempCelsius: Float,
    val thermalThrottlingStatus: String,
    val cpuUsagePercent: Float,
    val memoryPressureLevel: String,
    val anrCountRecent: Int,
    val crashCountRecent: Int,
    val bootloopIndicator: Boolean
)
