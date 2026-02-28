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
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * محلل الجهد المتقدم - يكتشف التغيرات رغم Android Smoothing
 */
object VoltageAnalyzer {
    
    /**
     * تحليل قراءات الجهد وحساب metrics
     */
    fun analyzeVoltageReadings(voltageReadings: List<Int>): VoltageAnalysis {
        if (voltageReadings.size < 3) {
            return VoltageAnalysis(
                isStalled = true,
                varianceScore = 0f,
                derivative = 0f,
                trend = "INSUFFICIENT_DATA"
            )
        }
        
        // 1. حساب الانحراف المعياري
        val mean = voltageReadings.average()
        val variance = voltageReadings.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance).toFloat()
        
        // 2. حساب Derivative (معدل التغيير)
        val derivatives = mutableListOf<Float>()
        for (i in 1 until voltageReadings.size) {
            val dV = voltageReadings[i] - voltageReadings[i - 1]
            derivatives.add(dV.toFloat())
        }
        val avgDerivative = if (derivatives.isNotEmpty()) {
            derivatives.average().toFloat()
        } else {
            0f
        }
        
        // 3. تحديد الاتجاه (Trend)
        val trend = when {
            avgDerivative < -2f -> "DROPPING"      // انخفاض واضح
            avgDerivative < -0.5f -> "DECLINING"   // انخفاض طفيف
            avgDerivative > 2f -> "RISING"         // ارتفاع واضح
            avgDerivative > 0.5f -> "INCREASING"   // ارتفاع طفيف
            else -> "STABLE"                       // ثابت
        }
        
        // 4. تحديد إذا كان Stalled
        val isStalled = stdDev < 3f && abs(avgDerivative) < 0.3f
        
        // 5. حساب Variance Score (0.0 - 1.0)
        val varianceScore = when {
            stdDev >= 15f -> 1.0f      // تباين ممتاز
            stdDev >= 10f -> 0.8f
            stdDev >= 5f -> 0.6f
            stdDev >= 3f -> 0.4f
            else -> 0.2f               // ثابت تقريباً
        }
        
        return VoltageAnalysis(
            isStalled = isStalled,
            varianceScore = varianceScore,
            derivative = avgDerivative,
            trend = trend,
            standardDeviation = stdDev
        )
    }
}

/**
 * نتيجة تحليل الجهد
 */
@Serializable
data class VoltageAnalysis(
    val isStalled: Boolean,          // هل الحساس متجمد؟
    val varianceScore: Float,        // درجة التباين (0.0-1.0)
    val derivative: Float,           // معدل التغيير (mV/reading)
    val trend: String,               // DROPPING, STABLE, RISING
    val standardDeviation: Float = 0f // الانحراف المعياري
)
