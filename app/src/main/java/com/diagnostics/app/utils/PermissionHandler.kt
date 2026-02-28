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

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class PermissionHandler(private val activity: ComponentActivity) {
    
    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            // Android 12 and below
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

    private var onPermissionsGrantedCallback: (() -> Unit)? = null
    
    // Define the launcher to handle the permission request result
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if all permissions are granted
        val allGranted = permissions.entries.all { it.value }
        
        if (allGranted) {
            android.util.Log.d("PermissionHandler", "✅ All permissions granted - triggering callback")
            onPermissionsGrantedCallback?.invoke()
            onPermissionsGrantedCallback = null // Clear after use
        } else {
            // Find which permissions were denied
            val deniedPermissions = permissions.entries
                .filter { !it.value }
                .map { it.key }
            
            android.util.Log.w("PermissionHandler", "❌ Permissions denied: $deniedPermissions")
            
            // Optional: Notify user which permissions are missing
            showPermissionDeniedDialog(deniedPermissions)
        }
    }
    
    fun setOnPermissionsGrantedCallback(callback: () -> Unit) {
        onPermissionsGrantedCallback = callback
    }
    
    fun checkAndRequestPermissions(): Boolean {
        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }
        
        return if (missingPermissions.isNotEmpty()) {
            permissionLauncher.launch(missingPermissions.toTypedArray())
            false
        } else {
            true
        }
    }
    
    fun hasAllPermissions(): Boolean {
        val result = requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
        
        return result
    }

    private fun showPermissionDeniedDialog(deniedPermissions: List<String>) {
        val arabicNames = mapOf(
            "CAMERA" to "الكاميرا",
            "RECORD_AUDIO" to "تسجيل الصوت",
            "READ_EXTERNAL_STORAGE" to "قراءة الملفات",
            "WRITE_EXTERNAL_STORAGE" to "كتابة الملفات",
            "READ_MEDIA_IMAGES" to "قراءة الصور",
            "READ_MEDIA_VIDEO" to "قراءة الفيديو"
        )
        
        val deniedArabic = deniedPermissions.map { perm ->
            val key = perm.substringAfterLast("permission.")
            arabicNames[key] ?: key
        }.joinToString("\n- ", prefix = "\n- ")

        AlertDialog.Builder(activity)
            .setTitle("⚠️ صلاحيات مطلوبة")
            .setMessage(
                "لتشغيل الفحص الكامل، يجب منح جميع الصلاحيات:\n\n" +
                deniedArabic +
                "\n\nالرجاء الضغط على 'ابدأ الفحص' مرة أخرى وقبول جميع الصلاحيات."
            )
            .setPositiveButton("فهمت") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
}
