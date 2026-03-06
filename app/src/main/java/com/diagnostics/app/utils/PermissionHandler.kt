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
package com.diagnostics.app.utils

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: ComponentActivity) {

    // ═══════════════════════════════════════════════════════
    // 🔴 الصلاحيات الحرجة - مطلوبة لبدء الفحص
    // ═══════════════════════════════════════════════════════
    private val criticalPermissions: Array<String>
        get() = buildList {
            // Camera & Audio - دائماً مطلوبة
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.READ_PHONE_STATE)

            // Storage حسب إصدار Android
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    // Android 13+ → READ_MEDIA_IMAGES فقط
                    add(Manifest.permission.READ_MEDIA_IMAGES)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                    // Android 10-12 → لا تحتاج صلاحية للـ cache
                    // تُكتب البيانات في context.filesDir
                }
                else -> {
                    // Android 9 وأقل → WRITE مطلوب لحفظ PDF
                    add(Manifest.permission.READ_EXTERNAL_STORAGE)
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }.toTypedArray()

    // ═══════════════════════════════════════════════════════
    // 🟢 الصلاحيات الاختيارية - للميزات الإضافية
    // ═══════════════════════════════════════════════════════
    private val optionalPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

    private var onPermissionsGrantedCallback: (() -> Unit)? = null
    private var onPermissionsDeniedCallback: ((List<String>) -> Unit)? = null

    // Launcher لطلب صلاحيات متعددة
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val denied = permissions.entries.filter { !it.value }.map { it.key }

        if (denied.isEmpty()) {
            android.util.Log.d("PermissionHandler", "✅ All permissions granted")
            onPermissionsGrantedCallback?.invoke()
            onPermissionsGrantedCallback = null
        } else {
            android.util.Log.w("PermissionHandler", "❌ Denied: $denied")
            onPermissionsDeniedCallback?.invoke(denied)
            showPermissionDeniedDialog(denied)
        }
    }

    // ═══════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════

    fun setOnPermissionsGrantedCallback(callback: () -> Unit) {
        onPermissionsGrantedCallback = callback
    }

    fun setOnPermissionsDeniedCallback(callback: (List<String>) -> Unit) {
        onPermissionsDeniedCallback = callback
    }

    /**
     * التحقق وطلب الصلاحيات الحرجة فقط.
     * Returns true إذا كانت كل الصلاحيات ممنوحة بالفعل.
     */
    fun checkAndRequestPermissions(): Boolean {
        val missing = criticalPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missing.isNotEmpty()) {
            android.util.Log.d("PermissionHandler", "Requesting missing: $missing")
            permissionLauncher.launch(missing.toTypedArray())
            false
        } else {
            true
        }
    }

    /**
     * هل كل الصلاحيات الحرجة ممنوحة؟
     */
    fun hasAllPermissions(): Boolean {
        return criticalPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * هل صلاحية معينة ممنوحة؟
     */
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * طلب الصلاحيات الاختيارية (Location, Notifications, Bluetooth).
     */
    fun requestOptionalPermissions(onResult: (Boolean) -> Unit) {
        val missing = optionalPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isEmpty()) {
            onResult(true)
            return
        }

        // Register a one-time callback
        onPermissionsGrantedCallback = { onResult(true) }
        onPermissionsDeniedCallback = { onResult(false) }
        permissionLauncher.launch(missing.toTypedArray())
    }

    // ═══════════════════════════════════════════════════════
    // حوار شرح رفض الصلاحيات
    // ═══════════════════════════════════════════════════════

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val arabicNames = mapOf(
            "CAMERA"                to "الكاميرا",
            "RECORD_AUDIO"          to "تسجيل الصوت",
            "READ_PHONE_STATE"      to "معلومات الجهاز",
            "READ_EXTERNAL_STORAGE" to "قراءة الملفات",
            "WRITE_EXTERNAL_STORAGE" to "كتابة الملفات",
            "READ_MEDIA_IMAGES"     to "قراءة الصور",
            "ACCESS_FINE_LOCATION"  to "الموقع الدقيق",
            "POST_NOTIFICATIONS"    to "الإشعارات",
            "BLUETOOTH_CONNECT"     to "البلوتوث"
        )

        val deniedArabic = deniedPermissions.map { perm ->
            val key = perm.substringAfterLast("permission.")
            "- ${arabicNames[key] ?: key}"
        }.joinToString("\n")

        AlertDialog.Builder(activity)
            .setTitle("⚠️ صلاحيات مطلوبة")
            .setMessage(
                "لتشغيل الفحص الكامل، يجب منح الصلاحيات التالية:\n\n$deniedArabic" +
                "\n\nالرجاء الضغط على 'ابدأ الفحص' مرة أخرى وقبول جميع الصلاحيات."
            )
            .setPositiveButton("فهمت") { dialog, _ -> dialog.dismiss() }
            .setCancelable(true)
            .show()
    }
}
