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
 * Institution: OSTİM Technical University
 */
package com.diagnostics.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class StorageStressResult(
    val sequentialWriteMbps: Float,
    val sequentialReadMbps: Float,
    val random4kReadIops: Int,
    val random4kWriteIops: Int,
    val slcCacheCrash: Boolean,
    val detectedType: String,                   // "UFS_3_1", "eMMC_5_1", etc.
    val storageTestFileCount: Int = 0,          // New field
    val writeSpeedCurve: List<SpeedPoint>       // 10 نقاط
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "sequentialReadMbps" to sequentialReadMbps,
            "sequentialWriteMbps" to sequentialWriteMbps,
            "random4kReadIops" to random4kReadIops,
            "detectedType" to detectedType
        )
    }
}

@Serializable
data class SpeedPoint(
    val fileNumber: Int,
    val speedMbps: Float
)
