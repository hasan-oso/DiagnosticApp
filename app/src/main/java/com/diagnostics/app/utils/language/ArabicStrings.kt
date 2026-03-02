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

class ArabicStrings : ReportLanguage {
    override val reportTitle = "تقرير صحة الجهاز"
    override val deviceInfo = "معلومات الجهاز"
    override val overallScore = "التقييم العام"
    override val details = "التفاصيل"
    override val battery = "البطارية"
    override val performance = "الأداء"
    override val storage = "التخزين"
    override val age = "العمر"
    override val features = "المميزات"
    override val recommendations = "التوصيات"
    override val strengths = "نقاط القوة"
    override val warnings = "نقاط الانتباه"
    override val suggestedPrice = "السعر المقترح"
    override val conclusion = "الخلاصة"
    override val grade = "الدرجة"

    override val designCapacity = "السعة المصممة"
    override val currentCapacity = "السعة الحالية"
    override val degradation = "التدهور"
    override val health = "الصحة"

    override val excellent = "ممتاز"
    override val veryGood = "جيد جداً"
    override val good = "جيد"
    override val fair = "مقبول"
    override val poor = "ضعيف"

    override fun degradationMessage(percent: Double, age: Double) = when {
        percent < 10 -> "ممتاز للعمر"
        percent < 20 -> "جيد للعمر"
        percent < 30 -> "عادي"
        else -> "مرتفع قليلاً"
    }

    override fun getString(key: String): String = when (key) {
        "battery_degradation_high" -> "تدهور البطارية مرتفع"
        "battery_degradation_critical" -> "تدهور البطارية حرج — يُنصح بالاستبدال"
        "battery_excellent" -> "البطارية بحالة ممتازة"
        "battery_good" -> "البطارية بحالة جيدة"
        "performance_excellent" -> "الأداء ممتاز"
        "performance_degraded" -> "الأداء متراجع"
        "storage_fast" -> "التخزين سريع (UFS 3.x)"
        "all_features_work" -> "كل المميزات تعمل"
        "device_new" -> "الجهاز جديد"
        "device_old" -> "الجهاز قديم نسبياً"
        else -> key
    }
}
