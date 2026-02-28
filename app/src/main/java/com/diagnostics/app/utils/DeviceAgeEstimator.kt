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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

/**
 * أداة متقدمة لتحديد العمر الحقيقي للجهاز
 * تستخدم تقنيات forensic متعددة لتجاوز قيود Android
 * 
 * Based on: "Mobile Hardware Forensics: Determining True Device Age in Android Environments"
 */
object DeviceAgeEstimator {

    private const val MILLIS_IN_YEAR = 1000L * 60 * 60 * 24 * 365.25f
    private const val TAG = "DeviceAgeEstimator"

    /**
     * تقدير عمر الجهاز بالسنوات
     * @return Pair<Float, Float> حيث:
     *   first = العمر بالسنوات
     *   second = درجة الثقة (0.0 إلى 1.0)
     */
    fun estimateDeviceAgeYears(context: Context): Pair<Float, Float> {
        val ageEstimates = mutableListOf<Pair<Float, Float>>() // (العمر, الوزن)
        val currentTime = System.currentTimeMillis()

        // Vector 1: Sysfs Battery Nodes (أعلى ثقة، محظور بواسطة SELinux في Android 10+)
        val sysfsAge = getAgeFromSysfs(currentTime)
        if (sysfsAge != null) {
            ageEstimates.add(Pair(sysfsAge, 0.9f))
        }

        // Vector 2: System Properties عبر Reflection
        val sysPropAge = getAgeFromSystemProperties(currentTime)
        if (sysPropAge != null) {
            ageEstimates.add(Pair(sysPropAge, 0.8f))
        }

        // Vector 3: Serial Number Decoding (Samsung/Xiaomi)
        // ملاحظة: يتطلب READ_PHONE_STATE permission
        val serialAge = decodeOEMSerialNumber(currentTime)
        if (serialAge != null) {
            ageEstimates.add(Pair(serialAge, 1.0f))
        }

        // Vector 4: Vendor Partition Timestamps (مقاوم لـ Factory Reset)
        val vendorAge = getAgeFromVendorPartition(currentTime)
        if (vendorAge != null) {
            ageEstimates.add(Pair(vendorAge, 0.6f))
        }

        // Vector 5: Build.TIME (أقل ثقة، يتغير مع OTA updates)
        val buildAge = getAgeFromBuildTime(currentTime)
        if (buildAge != null) {
            ageEstimates.add(Pair(buildAge, 0.3f))
        }

        return calculateWeightedAge(ageEstimates)
    }

    /**
     * محاولة قراءة sysfs nodes للبطارية
     * يتعامل مع EACCES بشكل آمن دون crash
     */
    private fun getAgeFromSysfs(currentTime: Long): Float? {
        val paths = listOf(
            "/sys/class/power_supply/battery/mfg_date",
            "/sys/class/power_supply/battery/first_usage_date",
            "/sys/class/power_supply/bms/mfg_date",
            "/sys/class/power_supply/bms/first_usage_date"
        )

        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val content = file.readText().trim()
                    // قد تكون القيم hex أو decimal
                    val timestamp = content.toLongOrNull(if (content.startsWith("0x")) 16 else 10)
                    if (timestamp != null && isValidTimestamp(timestamp)) {
                        return calculateYears(timestamp, currentTime)
                    }
                }
            } catch (e: SecurityException) {
                // SELinux منع الوصول (متوقع في Android 10+)
                continue
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * استخدام Java Reflection للوصول إلى @hide API android.os.SystemProperties
     * استخراج التواريخ من ro.* properties
     */
    @SuppressLint("PrivateApi")
    private fun getAgeFromSystemProperties(currentTime: Long): Float? {
        val keys = listOf(
            "ro.ril.mfg.date",       // Samsung تاريخ RIL
            "ro.manufacturedate",    // تاريخ التصنيع العام
            "ro.boot.hwversion",     // نسخة الهاردوير
            "ro.build.date.utc"      // خط أساس compile الفيرموير
        )

        for (key in keys) {
            val value = readSystemProperty(key)
            if (value.isNotEmpty()) {
                // معالجة صيغ Unix Epoch
                val epoch = value.toLongOrNull()
                if (epoch != null) {
                    val timestampMs = if (epoch < 1000000000000L) epoch * 1000 else epoch
                    if (isValidTimestamp(timestampMs)) {
                        return calculateYears(timestampMs, currentTime)
                    }
                }

                // معالجة صيغ التاريخ النصية (مثل "2021.03.29")
                val dateFormats = listOf("yyyy.MM.dd", "yyyy-MM-dd", "yyyyMMdd")
                for (format in dateFormats) {
                    try {
                        val sdf = SimpleDateFormat(format, Locale.US)
                        val date = sdf.parse(value)
                        if (date != null && isValidTimestamp(date.time)) {
                            return calculateYears(date.time, currentTime)
                        }
                    } catch (e: Exception) {
                        // فشل التحليل، تجربة الصيغة التالية
                    }
                }
            }
        }
        return null
    }

    /**
     * قراءة system property بشكل آمن عبر Reflection
     * مع fallback إلى shell execution (getprop)
     */
    private fun readSystemProperty(key: String): String {
        // محاولة Reflection أولاً
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
            val result = getMethod.invoke(null, key) as? String
            if (!result.isNullOrEmpty()) return result
        } catch (e: Exception) {
            // قيود Hidden API في Android 9+
        }

        // Fallback: تنفيذ Shell (يتجاوز قيود Dalvik لكن يستغرق ~13ms)
        try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()
            reader.close()
            process.destroy()
            if (!result.isNullOrEmpty()) return result
        } catch (e: Exception) {
            // فشل تنفيذ العملية
        }
        return ""
    }

    /**
     * فك تشفير Serial Numbers خاص بـ OEM
     * ملاحظة: يتطلب READ_PHONE_STATE permission
     */
    @SuppressLint("MissingPermission", "HardwareIds")
    private fun decodeOEMSerialNumber(currentTime: Long): Float? {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val serial = Build.getSerial()
                
                // Samsung: الحرف الثامن = السنة، التاسع = الشهر
                // M=2019, N=2020, R=2021, T=2022, W=2023, X=2024
                if (Build.MANUFACTURER.equals("samsung", ignoreCase = true) && serial.length >= 11) {
                    val yearChar = if (serial.length == 15) serial[7] else serial[3]
                    val monthChar = if (serial.length == 15) serial[8] else serial[4]
                    
                    val year = when (yearChar.uppercaseChar()) {
                        'M' -> 2019
                        'N' -> 2020
                        'R' -> 2021
                        'T' -> 2022
                        'W' -> 2023
                        'X' -> 2024
                        'Y' -> 2025
                        'Z' -> 2026
                        else -> return null
                    }
                    
                    val month = when (monthChar) {
                        in '1'..'9' -> monthChar.digitToInt()
                        'A' -> 10
                        'B' -> 11
                        'C' -> 12
                        else -> return null
                    }
                    
                    // إنشاء timestamp من السنة والشهر
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(year, month - 1, 1, 0, 0, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    
                    return calculateYears(calendar.timeInMillis, currentTime)
                }
                
                // Xiaomi: الحرف الثالث = نصف السنة، الرابع = السنة
                if (Build.MANUFACTURER.equals("xiaomi", ignoreCase = true) && serial.length >= 5) {
                    // منطق فك تشفير Xiaomi (مبسط)
                    // يحتاج تطوير كامل بناءً على patterns محددة
                }
            }
        } catch (e: SecurityException) {
            // Permission مرفوض
        }
        return null
    }

    /**
     * فحص /vendor partition (يقاوم Factory Reset)
     * استخراج mtime لمحاكاة تاريخ flash القسم
     */
    private fun getAgeFromVendorPartition(currentTime: Long): Float? {
        val paths = listOf(
            "/vendor/build.prop",
            "/vendor/etc/vintf/manifest.xml",
            "/odm/etc/build.prop"
        )
        
        for (path in paths) {
            try {
                val file = File(path)
                if (file.exists()) {
                    val modifiedTime = file.lastModified()
                    if (isValidTimestamp(modifiedTime)) {
                        return calculateYears(modifiedTime, currentTime)
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * استرجاع Build.TIME كآخر fallback
     */
    private fun getAgeFromBuildTime(currentTime: Long): Float? {
        val buildTime = Build.TIME
        if (isValidTimestamp(buildTime)) {
            return calculateYears(buildTime, currentTime)
        }
        return null
    }

    /**
     * حساب المتوسط الموزون للتقديرات المجمعة
     */
    private fun calculateWeightedAge(estimates: List<Pair<Float, Float>>): Pair<Float, Float> {
        if (estimates.isEmpty()) return Pair(0.0f, 0.0f)

        var totalWeightedAge = 0.0f
        var totalWeight = 0.0f
        var maxConfidence = 0.0f

        for ((age, weight) in estimates) {
            totalWeightedAge += (age * weight)
            totalWeight += weight
            maxConfidence = max(maxConfidence, weight)
        }

        val finalAge = totalWeightedAge / totalWeight
        
        // تعزيز الثقة إذا توافقت مصادر متعددة
        val confidenceBoost = if (estimates.size > 2) 0.15f else 0.0f
        val finalConfidence = (maxConfidence + confidenceBoost).coerceAtMost(1.0f)

        return Pair(finalAge, finalConfidence)
    }

    /**
     * حساب العمر بالسنوات من epoch milliseconds
     */
    private fun calculateYears(timestampMs: Long, currentTimeMs: Long): Float {
        val diff = currentTimeMs - timestampMs
        return if (diff > 0) diff.toFloat() / MILLIS_IN_YEAR else 0.0f
    }

    /**
     * التحقق من صحة timestamps (تصفية الشذوذات)
     * رفض التواريخ قبل 2010 لمنع أخطاء رياضية
     */
    private fun isValidTimestamp(timestampMs: Long): Boolean {
        val year2010 = 1262304000000L // 2010-01-01
        return timestampMs > year2010 && timestampMs < System.currentTimeMillis()
    }
}
