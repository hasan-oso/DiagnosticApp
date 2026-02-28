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
import kotlin.math.max

/**
 * حاسب الجهد الاصطناعي - دمج حساسات متعددة عند تعطل voltage sensor
 */
object SyntheticVoltageCalculator {
    
    /**
     * حساب voltage drop اصطناعي من مصادر متعددة
     * 
     * @param currentMa التيار بالـ mA (إذا متاح)
     * @param tempRiseCelsius ارتفاع الحرارة بالدرجة
     * @param cpuThrottlingPercent نسبة خنق المعالج (0-100)
     * @param socDropPercent انخفاض نسبة الشحن (0-100)
     * @return تقدير انخفاض الجهد بالـ mV
     */
    fun calculateSyntheticVoltageDrop(
        currentMa: Int,
        tempRiseCelsius: Float,
        cpuThrottlingPercent: Float,
        socDropPercent: Float
    ): SyntheticVoltage {
        
        // 1. مساهمة التيار (Current Component)
        val currentComponent = if (currentMa > 0) {
            // تيار عالي = voltage drop أكبر
            // 1000mA ≈ 50mV drop تقريباً
            (currentMa / 20f) * 0.4f
        } else {
            0f
        }
        
        // 2. مساهمة الحرارة (Temperature Component)
        val tempComponent = if (tempRiseCelsius > 0.5f) {
            // ارتفاع حرارة = stress على البطارية
            // كل 5 درجات ≈ 20mV drop
            (tempRiseCelsius * 4f) * 0.3f
        } else {
            0f
        }
        
        // 3. مساهمة الخنق (Throttling Component)
        val throttlingComponent = if (cpuThrottlingPercent > 10f) {
            // خنق حراري = استهلاك عالي سابق
            (cpuThrottlingPercent * 0.5f) * 0.2f
        } else {
            0f
        }
        
        // 4. مساهمة انخفاض الشحن (SOC Drop Component)
        val socComponent = if (socDropPercent > 0.5f) {
            // انخفاض سريع في SOC = حمل عالي
            (socDropPercent * 10f) * 0.1f
        } else {
            0f
        }
        
        // 5. الجمع الموزون
        val totalDrop = currentComponent + tempComponent + 
                        throttlingComponent + socComponent
        
        // 6. حساب الثقة
        val confidence = calculateConfidence(
            currentMa > 0,
            tempRiseCelsius > 0.5f,
            cpuThrottlingPercent > 10f,
            socDropPercent > 0.5f
        )
        
        // 7. تحديد المصادر المستخدمة
        val sources = mutableListOf<String>()
        if (currentMa > 0) sources.add("CURRENT")
        if (tempRiseCelsius > 0.5f) sources.add("TEMPERATURE")
        if (cpuThrottlingPercent > 10f) sources.add("THROTTLING")
        if (socDropPercent > 0.5f) sources.add("SOC_DROP")
        
        return SyntheticVoltage(
            estimatedDropMv = totalDrop.toInt(),
            confidence = confidence,
            sources = sources,
            components = mapOf(
                "current" to currentComponent,
                "temperature" to tempComponent,
                "throttling" to throttlingComponent,
                "soc" to socComponent
            )
        )
    }
    
    /**
     * حساب ثقة التقدير
     */
    private fun calculateConfidence(
        hasCurrent: Boolean,
        hasTemp: Boolean,
        hasThrottling: Boolean,
        hasSocDrop: Boolean
    ): Float {
        var confidence = 0f
        
        if (hasCurrent) confidence += 0.40f      // التيار أهم مصدر
        if (hasTemp) confidence += 0.30f
        if (hasThrottling) confidence += 0.20f
        if (hasSocDrop) confidence += 0.10f
        
        return confidence
    }
}

/**
 * نتيجة حساب الجهد الاصطناعي
 */
@Serializable
data class SyntheticVoltage(
    val estimatedDropMv: Int,                    // التقدير بالـ mV
    val confidence: Float,                       // الثقة (0.0-1.0)
    val sources: List<String>,                   // المصادر المستخدمة
    val components: Map<String, Float>           // مساهمة كل مصدر
)
