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
package com.diagnostics.app.utils

import kotlinx.serialization.Serializable

/**
 * مراقب الجهد الهجين - يدمج Voltage Derivative + Multi-Sensor Fusion
 */
object HybridVoltageMonitor {
    
    /**
     * تحليل شامل للجهد مع fallback ذكي
     */
    fun analyzeVoltage(
        voltageReadings: List<Int>,
        currentMa: Int = 0,
        tempRiseCelsius: Float = 0f,
        cpuThrottlingPercent: Float = 0f,
        socDropPercent: Float = 0f
    ): HybridVoltageResult {
        
        // Level 1: تحليل الجهد المباشر
        val voltageAnalysis = VoltageAnalyzer.analyzeVoltageReadings(voltageReadings)
        
        // Level 2: قرار استخدام Synthetic أم لا
        val useSynthetic = voltageAnalysis.isStalled || 
                          voltageAnalysis.varianceScore < 0.4f
        
        val syntheticVoltage = if (useSynthetic) {
            SyntheticVoltageCalculator.calculateSyntheticVoltageDrop(
                currentMa = currentMa,
                tempRiseCelsius = tempRiseCelsius,
                cpuThrottlingPercent = cpuThrottlingPercent,
                socDropPercent = socDropPercent
            )
        } else {
            null
        }
        
        // Level 3: اختيار المصدر الأفضل
        val (finalVoltageDrop, primarySource, overallConfidence) = when {
            // إذا voltage جيد، استخدمه
            !voltageAnalysis.isStalled && voltageAnalysis.varianceScore >= 0.6f -> {
                Triple(
                    calculateActualDrop(voltageReadings),
                    "DIRECT_VOLTAGE",
                    voltageAnalysis.varianceScore
                )
            }
            
            // إذا voltage متوسط، ادمجه مع synthetic
            !voltageAnalysis.isStalled && syntheticVoltage != null -> {
                val actualDrop = calculateActualDrop(voltageReadings)
                val blendedDrop = (actualDrop * 0.6f + syntheticVoltage.estimatedDropMv * 0.4f).toInt()
                Triple(
                    blendedDrop,
                    "BLENDED",
                    (voltageAnalysis.varianceScore + syntheticVoltage.confidence) / 2f
                )
            }
            
            // إذا voltage stalled تماماً، استخدم synthetic فقط
            syntheticVoltage != null -> {
                Triple(
                    syntheticVoltage.estimatedDropMv,
                    "SYNTHETIC",
                    syntheticVoltage.confidence
                )
            }
            
            // worst case: لا بيانات كافية
            else -> {
                Triple(0, "UNAVAILABLE", 0f)
            }
        }
        
        return HybridVoltageResult(
            voltageDropMv = finalVoltageDrop,
            primarySource = primarySource,
            confidence = overallConfidence,
            voltageAnalysis = voltageAnalysis,
            syntheticVoltage = syntheticVoltage,
            isStalled = voltageAnalysis.isStalled
        )
    }
    
    /**
     * حساب الانخفاض الفعلي من القراءات
     */
    private fun calculateActualDrop(voltages: List<Int>): Int {
        if (voltages.size < 2) return 0
        return voltages.first() - voltages.last()
    }
}

/**
 * النتيجة النهائية للتحليل الهجين
 */
@Serializable
data class HybridVoltageResult(
    val voltageDropMv: Int,                      // الانخفاض النهائي
    val primarySource: String,                   // DIRECT_VOLTAGE, BLENDED, SYNTHETIC
    val confidence: Float,                       // الثقة الإجمالية
    val voltageAnalysis: VoltageAnalysis,        // تحليل الجهد المباشر
    val syntheticVoltage: SyntheticVoltage?,     // الجهد الاصطناعي (إذا استُخدم)
    val isStalled: Boolean                       // علامة للـ ML
)
