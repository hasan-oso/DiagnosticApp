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

class EnglishStrings : ReportLanguage {
    override val reportTitle = "Device Health Report"
    override val deviceInfo = "Device Information"
    override val overallScore = "Overall Score"
    override val details = "Details"
    override val battery = "Battery"
    override val performance = "Performance"
    override val storage = "Storage"
    override val age = "Age"
    override val features = "Features"
    override val recommendations = "Recommendations"
    override val strengths = "Strengths"
    override val warnings = "Watch Points"
    override val suggestedPrice = "Suggested Price"
    override val conclusion = "Conclusion"
    override val grade = "Grade"

    override val designCapacity = "Design Capacity"
    override val currentCapacity = "Current Capacity"
    override val degradation = "Degradation"
    override val health = "Health"

    override val excellent = "Excellent"
    override val veryGood = "Very Good"
    override val good = "Good"
    override val fair = "Fair"
    override val poor = "Poor"

    override fun degradationMessage(percent: Double, age: Double) = when {
        percent < 10 -> "Excellent for age"
        percent < 20 -> "Good for age"
        percent < 30 -> "Normal"
        else -> "Slightly high"
    }

    override fun getString(key: String): String = when (key) {
        "report_title" -> reportTitle
        "diagnostic_results" -> "Diagnostic Results"
        "battery" -> battery
        "performance" -> performance
        "storage" -> storage
        "security" -> "Security"
        "recommendation" -> "Recommendation & Price Estimate"
        "suggested_price" -> suggestedPrice
        "strengths" -> "$strengths:"
        "warnings" -> "$warnings:"
        "battery_degradation_high" -> "Battery degradation high"
        "battery_degradation_critical" -> "Battery degradation critical — replacement recommended"
        "battery_excellent" -> "Battery in excellent condition"
        "battery_good" -> "Battery in good condition"
        "performance_excellent" -> "Performance excellent"
        "performance_degraded" -> "Performance degraded"
        "storage_fast" -> "Storage fast (UFS 3.x)"
        "all_features_work" -> "All features working"
        "device_new" -> "Device is new"
        "device_old" -> "Device relatively old"
        "security_warning" -> "Security Warning"
        "device_rooted" -> "Device is Rooted"
        "developer_options_on" -> "Developer Options Enabled"
        "adb_warning" -> "USB Debugging Enabled"
        else -> key
    }
}
