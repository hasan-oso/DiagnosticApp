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
package com.diagnostics.app.domain.models

import kotlinx.serialization.Serializable
import com.diagnostics.app.domain.models.DegradationMetrics

@Serializable
data class CapacityReading(
    val capacityMah: Int,
    val source: CapacitySource,
    val confidence: Float,
    val isRawReading: Boolean,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class CapacitySource {
    SYSFS_DESIGN,           // /sys/.../charge_full_design
    SYSFS_FULL,             // /sys/.../charge_full  
    COUNTER_CALCULATED,     // Calculated from charge_counter
    VOLTAGE_TEST_ESTIMATE,  // Estimated from voltage sag test
    MULTI_SOURCE_ESTIMATE,  // Weighted average from multiple factors
    NOT_AVAILABLE,          // Could not determine
    KNOWN_MODEL_FALLBACK    // Last resort fallback (low confidence)
}

@Serializable
data class BatteryCapacityMetrics(
    val designCapacity: CapacityReading,
    val currentCapacity: CapacityReading,
    val degradationMah: Int?,
    val degradationPercent: Float?,
    val degradationMetrics: DegradationMetrics?,
    val degradationRatePerYear: Float?,
    val measurementTimestamp: Long,
    val batteryPercentAtMeasurement: Int
)

/**
 * معلومات التدهور للعرض والـ ML
 */
@Serializable
data class DegradationMetrics(
    val rawDegradationMah: Int?,
    val rawDegradationPercent: Float?,
    val displayDegradationPercent: Float,
    val isLikelyNewDevice: Boolean,
    val reliability: Float
)
