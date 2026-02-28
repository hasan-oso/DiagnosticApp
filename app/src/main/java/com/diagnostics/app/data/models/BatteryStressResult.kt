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

import com.diagnostics.app.domain.models.BatteryCapacityMetrics
import com.diagnostics.app.utils.VoltageAnalysis
import com.diagnostics.app.utils.SyntheticVoltage
import com.diagnostics.app.utils.HybridVoltageResult

@Serializable
data class BatteryStressResult(
    val durationSeconds: Int,
    val initialVoltageMv: Int,
    val loadVoltageMv: Int,
    val voltageSagMv: Int,
    val peakCurrentMa: Int,
    val internalResistanceMohm: Float,
    val estimatedHealthPercent: Int,
    val voltageReadings: List<VoltageReading>,
    val calculationMethod: String? = null,

    // NEW AI TRAINING FIELDS
    val capacityMetrics: BatteryCapacityMetrics? = null,
    val timeSeriesData: List<TimeSeriesSample> = emptyList(),
    val dataQuality: DataQuality? = null,
    val stableFeatures: StableFeatures? = null,
    val dropConfidence: String = "LOW",
    val classConfidence: Float = 0f,

    // Precise battery drain (charge_counter)
    val chargeCounterStartUah: Long = -1,
    val chargeCounterEndUah: Long = -1,
    val actualDrainMah: Float = 0f,
    val actualDrainPercent: Float = 0f,

    // Level tracking (rounded %)
    val batteryLevelStart: Int = 0,
    val batteryLevelEnd: Int = 0,
    val batteryDrainPercent: Int = 0,
    val batteryDrainRatePerMinute: Float = 0f,

    // Temperature
    val batteryTempStartCelsius: Float = -1f,
    val batteryTempEndCelsius: Float = -1f,

    // Metadata
    val batteryCycleCount: Int = -1,
    val batteryHealthStatus: String = "UNKNOWN",
    val isChargingStart: Boolean = false,
    val isChargingEnd: Boolean = false,
    val chargingDetectedMidTest: Boolean = false,

    val voltageAnalysis: VoltageAnalysis? = null,
    val syntheticVoltage: SyntheticVoltage? = null,
    val hybridVoltageResult: HybridVoltageResult? = null,
    val data_collection_metadata: com.diagnostics.app.domain.models.DataCollectionMetadata? = null
)

@Serializable
data class VoltageReading(
    val timestampSec: Int,
    val voltageMv: Int,
    val currentMa: Int
)
