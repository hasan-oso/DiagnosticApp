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

class TurkishStrings : ReportLanguage {
    override val reportTitle = "Cihaz Sağlık Raporu"
    override val deviceInfo = "Cihaz Bilgileri"
    override val overallScore = "Genel Puan"
    override val details = "Detaylar"
    override val battery = "Pil"
    override val performance = "Performans"
    override val storage = "Depolama"
    override val age = "Yaş"
    override val features = "Özellikler"
    override val recommendations = "Öneriler"
    override val strengths = "Güçlü Yönler"
    override val warnings = "Dikkat Noktaları"
    override val suggestedPrice = "Önerilen Fiyat"
    override val conclusion = "Sonuç"
    override val grade = "Derece"

    override val designCapacity = "Tasarım Kapasitesi"
    override val currentCapacity = "Mevcut Kapasite"
    override val degradation = "Aşınma"
    override val health = "Sağlık"

    override val excellent = "Mükemmel"
    override val veryGood = "Çok İyi"
    override val good = "İyi"
    override val fair = "Orta"
    override val poor = "Zayıf"

    override fun degradationMessage(percent: Double, age: Double) = when {
        percent < 10 -> "Yaş için mükemmel"
        percent < 20 -> "Yaş için iyi"
        percent < 30 -> "Normal"
        else -> "Biraz yüksek"
    }

    override fun getString(key: String): String = when (key) {
        "report_title" -> reportTitle
        "diagnostic_results" -> "Teşhis Detayları"
        "battery" -> battery
        "performance" -> performance
        "storage" -> storage
        "security" -> "Güvenlik"
        "recommendation" -> "Öneri ve Fiyat Tahmini"
        "suggested_price" -> suggestedPrice
        "strengths" -> "$strengths:"
        "warnings" -> "$warnings:"
        "battery_degradation_high" -> "Pil aşınması yüksek"
        "battery_degradation_critical" -> "Pil aşınması kritik — değiştirilmesi önerilir"
        "battery_excellent" -> "Pil mükemmel durumda"
        "battery_good" -> "Pil iyi durumda"
        "performance_excellent" -> "Performans mükemmel"
        "performance_degraded" -> "Performans düşmüş"
        "storage_fast" -> "Depolama hızlı (UFS 3.x)"
        "all_features_work" -> "Tüm özellikler çalışıyor"
        "device_new" -> "Cihaz yeni"
        "device_old" -> "Cihaz nispeten eski"
        "security_warning" -> "Güvenlik Uyardı"
        "device_rooted" -> "Cihaz rootlanmış"
        "developer_options_on" -> "Geliştirici seçenekleri açık"
        "adb_warning" -> "USB hata ayıklama açık"
        else -> key
    }
}
