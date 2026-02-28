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

/**
 * Metadata لجمع البيانات - للتحليل المستقبلي في Python
 */
@Serializable
data class DataCollectionMetadata(
    // معلومات التجميع
    val collection_timestamp: Long,                  // وقت الجمع
    val app_version: String,                         // نسخة التطبيق
    val test_duration_seconds: Int,                  // مدة الاختبار
    
    // معلومات الجهاز
    val device_manufacturer: String,
    val device_model: String,
    val android_version: String,
    val android_sdk: Int,
    
    // Flags للتحليل
    val flags: List<String> = emptyList(),           // أعلام خاصة
    
    // Raw voltage readings (للتحليل العميق)
    val raw_voltage_readings: List<Int> = emptyList(),
    
    // Age estimation details
    val age_estimation_source: String = "UNKNOWN",   // VENDOR, BUILD, SERIAL, etc.
    val age_estimation_confidence: Float = 0f,
    
    // Voltage analysis details
    val voltage_primary_source: String = "UNKNOWN",  // DIRECT_VOLTAGE, SYNTHETIC, BLENDED
    val voltage_is_stalled: Boolean = false,
    val voltage_variance_score: Float = 0f,
    
    // Data completeness indicators
    val has_current_readings: Boolean = false,
    val has_temperature_readings: Boolean = false,
    val has_resistance_readings: Boolean = false,
    
    // User notes (optional)
    val notes: String = ""
)
