package com.diagnostics.app.data.models.auth

data class InventoryDevice(
    val id: String = "",
    val shopId: String = "",
    val scanId: String = "",
    val deviceBrand: String = "",
    val deviceModel: String = "",
    val overallScore: Int = 0,
    val scanDate: Long = 0L,
    val purchasePrice: Int = 0,
    val sellingPrice: Int = 0,
    val actualSoldPrice: Int = 0,
    val status: DeviceStatus = DeviceStatus.IN_STOCK,
    val soldDate: Long? = null,
    val notes: String = "",
    val buyerNote: String = "",
    val profit: Int = 0,
    val batteryHealth: Int = 0,
    val storageTotalGb: Long = 0,
    val ramTotalMb: Long = 0,
    val androidVersion: String = "",
    val pdfUrl: String? = null,
    val pdfLocalPath: String? = null
)

enum class DeviceStatus { IN_STOCK, SOLD, RESERVED, RETURNED }

data class ShopStats(
    val totalDevices: Int = 0,
    val inStock: Int = 0,
    val sold: Int = 0,
    val totalPurchaseCost: Int = 0,
    val totalSoldRevenue: Int = 0,
    val netProfit: Int = 0,
    val avgScore: Float = 0f
)
