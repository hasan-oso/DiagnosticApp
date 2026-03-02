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

data class ShopAccount(
    val id: String = "",
    val email: String = "",
    val accountType: AccountType = AccountType.SHOP,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val shopInfo: ShopInfo = ShopInfo()
)

data class ShopInfo(
    val shopName: String = "",
    val shopNameAr: String = "",
    val shopNameTr: String = "",
    val shopNameEn: String = "",
    val ownerName: String = "",
    val phoneNumber: String = "",
    val whatsappNumber: String = "",
    val address: String = "",
    val city: String = "",
    val district: String = "",
    val country: String = "Turkey",
    val businessType: BusinessType = BusinessType.SHOP,
    val logoUrl: String = "",
    val primaryColor: String = "#1976D2",
    val secondaryColor: String = "#424242",
    val instagramHandle: String = "",
    val facebookPage: String = "",
    val website: String = "",
    val subscriptionTier: SubscriptionTier = SubscriptionTier.FREE,
    val totalScans: Int = 0,
    val monthlyScans: Int = 0,
    val lastScanDate: Long = 0
)

enum class AccountType {
    GUEST, SHOP, WORKSHOP, ADMIN
}

enum class BusinessType {
    SHOP, WORKSHOP, DISTRIBUTOR, OTHER
}

enum class SubscriptionTier {
    FREE, BASIC, PRO, PARTNER
}
