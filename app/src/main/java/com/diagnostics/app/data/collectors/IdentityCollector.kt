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
package com.diagnostics.app.data.collectors

import android.os.Build

class IdentityCollector {
    
    fun collectIdentityData(): IdentityData {
        return IdentityData(
            deviceBrand = Build.BRAND,
            deviceModel = Build.MODEL,
            deviceManufacturer = Build.MANUFACTURER,
            boardPlatform = Build.BOARD,
            buildFingerprint = Build.FINGERPRINT,
            buildId = Build.ID,
            buildDateUtc = Build.TIME,
            androidVersion = Build.VERSION.RELEASE,
            androidSdk = Build.VERSION.SDK_INT,
            kernelVersion = System.getProperty("os.version") ?: "unknown",
            securityPatchDate = Build.VERSION.SECURITY_PATCH,
            buildTags = Build.TAGS
        )
    }
    
    data class IdentityData(
        val deviceBrand: String,
        val deviceModel: String,
        val deviceManufacturer: String,
        val boardPlatform: String,
        val buildFingerprint: String,
        val buildId: String,
        val buildDateUtc: Long,
        val androidVersion: String,
        val androidSdk: Int,
        val kernelVersion: String,
        val securityPatchDate: String,
        val buildTags: String
    )
}
