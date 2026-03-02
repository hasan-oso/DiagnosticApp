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
package com.diagnostics.app.utils.language

interface ReportLanguage {
    val reportTitle: String
    val deviceInfo: String
    val overallScore: String
    val details: String
    val battery: String
    val performance: String
    val storage: String
    val age: String
    val features: String
    val recommendations: String
    val strengths: String
    val warnings: String
    val suggestedPrice: String
    val conclusion: String
    val grade: String

    // Battery
    val designCapacity: String
    val currentCapacity: String
    val degradation: String
    val health: String

    // Ratings
    val excellent: String
    val veryGood: String
    val good: String
    val fair: String
    val poor: String

    // Messages
    fun degradationMessage(percent: Double, age: Double): String
    fun getString(key: String): String
}

enum class Language {
    ARABIC, TURKISH, ENGLISH
}
