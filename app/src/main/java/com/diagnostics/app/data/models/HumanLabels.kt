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
data class HumanLabels(
    val overallHealthScore: Int,                // 0-100
    val authenticity: String,                   // "GENUINE", "SUSPICIOUS", "FAKE"
    val refurbishmentStatus: String,            // "ORIGINAL", "MINOR_REPAIR", "REFURBISHED", "HEAVY_MODIFIED"
    val replacedComponents: List<String>,       // ["BATTERY", "SCREEN", etc.]
    val knownIssues: List<String>,              // ["WATER_DAMAGE", etc.]
    val ageCategory: String,                    // "1-2yr" (للتأكيد)
    val usageIntensity: String,                 // "LIGHT", "MODERATE", "HEAVY"
    val technicianNotes: String,
    val labeledBy: String,                      // معرف التقني
    val labeledAt: Long                         // timestamp
)
