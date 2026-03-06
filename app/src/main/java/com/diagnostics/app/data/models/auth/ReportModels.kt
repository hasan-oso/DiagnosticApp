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
package com.diagnostics.app.data.models.auth

data class DeviceScore(
    val overall: Double = 0.0,
    val battery: Double = 0.0,
    val performance: Double = 0.0,
    val storage: Double = 0.0,
    val age: Double = 0.0,
    val features: Double = 0.0,
    val grade: String = "N/A",
    val strengths: List<String> = emptyList(),
    val issues: List<String> = emptyList()
)


data class RegistrationData(
    val email: String = "",
    val password: String = "",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val shopNameAr: String = "",
    val shopNameTr: String = "",
    val shopNameEn: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val businessType: BusinessType = BusinessType.SHOP,
    val instagram: String = "",
    val whatsapp: String = "",
    val logoUrl: String = "",
    val primaryColor: String = "#1976D2",
    val secondaryColor: String = "#424242"
)
