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
data class TimeSeriesSample(
    val timestamp_ms: Long,
    val time_delta_sec: Int,
    val phase: String,
    val voltage_mv: Int,
    val current_ma: Int,
    val temp_c: Float,
    val soc_percent: Int,
    val cpu_load_percent: Int,
    val cpu_freq_mhz: Int
)

@Serializable
data class DataQuality(
    val pmicStalled: Boolean,
    val voltageGlitches: Int,
    val currentGlitches: Int,
    val overallQuality: String,
    val confidenceScore: Float
)

@Serializable
data class StableFeatures(
    val baselineVoltageAvg: Float,
    val baselineVoltageStd: Float,
    val loadVoltageStable: Float,
    val voltageTransitionTimeSec: Int,
    val voltageDropMv: Int,
    val voltageChangeMv: Int,
    val voltageChangeDirection: String,
    val voltageRecoveryMv: Int,
    val tempRiseTotal: Float,
    val tempRiseRatePerMin: Float,
    val avgCpuFreqMhz: Float,
    val cpuFreqVariation: Float,
    val thermalThrottlingDetected: Boolean
)
