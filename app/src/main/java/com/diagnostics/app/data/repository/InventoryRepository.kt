package com.diagnostics.app.data.repository

import com.diagnostics.app.data.models.DeviceScanData
import com.diagnostics.app.data.models.auth.DeviceStatus
import com.diagnostics.app.data.models.auth.InventoryDevice
import com.diagnostics.app.data.models.auth.ShopStats
import com.diagnostics.app.utils.QrManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

class InventoryRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val shopsCollection = firestore.collection("shops")

    suspend fun saveDevice(shopId: String, device: InventoryDevice): Result<String> = withContext(Dispatchers.IO) {
        try {
            val deviceId = if (device.id.isEmpty()) UUID.randomUUID().toString() else device.id
            val deviceToSave = device.copy(id = deviceId)
            
            shopsCollection.document(shopId).collection("inventory").document(deviceId).set(deviceToSave).await()
            Result.success(deviceId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDevices(shopId: String): Flow<List<InventoryDevice>> = callbackFlow {
        val listener = shopsCollection.document(shopId).collection("inventory")
            .orderBy("scanDate", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val devices = snapshot.documents.mapNotNull { it.toObject(InventoryDevice::class.java) }
                    trySend(devices)
                }
            }
            
        awaitClose { listener.remove() }
    }

    suspend fun updateStatus(deviceId: String, shopId: String, status: DeviceStatus, soldPrice: Int = 0): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = mutableMapOf<String, Any>("status" to status.name)
            if (status == DeviceStatus.SOLD) {
                updates["actualSoldPrice"] = soldPrice
                updates["soldDate"] = System.currentTimeMillis()
            }
            shopsCollection.document(shopId).collection("inventory").document(deviceId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePdfUrl(shopId: String, scanId: String, pdfUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            shopsCollection.document(shopId).collection("inventory").document(scanId)
                .update("pdfUrl", pdfUrl).await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PDF", "Failed to save pdfUrl: ${e.message}")
            Result.failure(e)
        }
    }

    /** تعليم الجهاز كمباع مع حساب الربح وحفظ ملاحظة المشتري */
    suspend fun markAsSold(
        device: InventoryDevice,
        shopId: String,
        actualSoldPrice: Int,
        buyerNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val profit = actualSoldPrice - device.purchasePrice
            shopsCollection.document(shopId)
                .collection("inventory").document(device.id)
                .update(mapOf(
                    "status"          to DeviceStatus.SOLD.name,
                    "actualSoldPrice" to actualSoldPrice,
                    "soldDate"        to System.currentTimeMillis(),
                    "buyerNote"       to buyerNote,
                    "profit"          to profit
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** إعادة الجهاز للمخزون */
    suspend fun markAsInStock(device: InventoryDevice, shopId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            shopsCollection.document(shopId)
                .collection("inventory").document(device.id)
                .update(mapOf(
                    "status"          to DeviceStatus.IN_STOCK.name,
                    "actualSoldPrice" to 0,
                    "soldDate"        to null,
                    "buyerNote"       to "",
                    "profit"          to 0
                )).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePrices(deviceId: String, shopId: String, purchase: Int, selling: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            shopsCollection.document(shopId).collection("inventory").document(deviceId)
                .update(
                    mapOf(
                        "purchasePrice" to purchase,
                        "sellingPrice" to selling
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDevice(deviceId: String, shopId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            shopsCollection.document(shopId).collection("inventory").document(deviceId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStats(shopId: String): ShopStats = withContext(Dispatchers.IO) {
        try {
            val snapshot = shopsCollection.document(shopId).collection("inventory").get().await()
            val devices = snapshot.documents.mapNotNull { it.toObject(InventoryDevice::class.java) }
            
            if (devices.isEmpty()) return@withContext ShopStats()
            
            val inStock = devices.count { it.status == DeviceStatus.IN_STOCK }
            val sold = devices.count { it.status == DeviceStatus.SOLD }
            val totalPurchaseCost = devices.sumOf { it.purchasePrice }
            val totalSoldRevenue = devices.sumOf { it.actualSoldPrice }
            val netProfit = totalSoldRevenue - devices.filter { it.status == DeviceStatus.SOLD }.sumOf { it.purchasePrice }
            val avgScore = if (devices.isNotEmpty()) devices.map { it.overallScore }.average().toFloat() else 0f
            
            ShopStats(
                totalDevices = devices.size,
                inStock = inStock,
                sold = sold,
                totalPurchaseCost = totalPurchaseCost,
                totalSoldRevenue = totalSoldRevenue,
                netProfit = netProfit,
                avgScore = avgScore
            )
        } catch (e: Exception) {
            ShopStats()
        }
    }

    // ══════════════════════════════════════════════
    // QR Token Functions (استبدل PIN)
    // ══════════════════════════════════════════════

    /** توليد QR token وحفظه في Firestore — يُلغي القديم تلقائياً */
    suspend fun generateQrToken(shopId: String): Pair<String, Long> = withContext(Dispatchers.IO) {
        val token = QrManager.generateToken()
        val expiresAt = QrManager.getExpiryTime()

        // احذف كل tokens القديمة غير المستخدمة (batch atomically)
        val oldTokensSnap = firestore.collection("shops").document(shopId)
            .collection("qr_tokens")
            .whereEqualTo("used", false)
            .get().await()

        val batch = firestore.batch()
        oldTokensSnap.forEach { doc ->
            batch.update(doc.reference, "used", true)
        }

        // أضف الـ token الجديد
        val newRef = firestore.collection("shops").document(shopId)
            .collection("qr_tokens").document(token)
        batch.set(newRef, mapOf(
            "token"     to token,
            "shopId"    to shopId,
            "createdAt" to System.currentTimeMillis(),
            "expiresAt" to expiresAt,
            "used"      to false
        ))
        batch.commit().await()

        android.util.Log.d("QR_DEBUG", "New token generated=$token  invalidated ${oldTokensSnap.size()} old tokens")
        Pair(token, expiresAt)
    }

    /** التحقق من QR token بعد المسح — مسار مباشر بدون collectionGroup */
    suspend fun validateQrToken(token: String, shopId: String): String? = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("QR_DEBUG", "Validating token=$token  shopId=$shopId")

            val doc = firestore.collection("shops")
                .document(shopId)
                .collection("qr_tokens")
                .document(token)
                .get().await()

            if (!doc.exists()) {
                android.util.Log.d("QR_DEBUG", "Token doc NOT FOUND in Firestore")
                return@withContext null
            }

            val expiresAt = doc.getLong("expiresAt") ?: run {
                android.util.Log.d("QR_DEBUG", "expiresAt is null")
                return@withContext null
            }
            val used = doc.getBoolean("used") ?: true
            android.util.Log.d("QR_DEBUG", "used=$used  expiresAt=$expiresAt  now=${System.currentTimeMillis()}")

            if (used) {
                android.util.Log.d("QR_DEBUG", "Token already USED")
                return@withContext null
            }
            if (!QrManager.isTokenValid(expiresAt)) {
                android.util.Log.d("QR_DEBUG", "Token EXPIRED")
                return@withContext null
            }

            android.util.Log.d("QR_DEBUG", "Token VALID ✓ returning shopId=$shopId")
            shopId
        } catch (e: Exception) {
            android.util.Log.e("QR_DEBUG", "Validation error: ${e.message}")
            null
        }
    }

    /** إبطال QR token بعد الاستخدام */
    suspend fun invalidateQrToken(token: String, shopId: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("shops").document(shopId)
                .collection("qr_tokens").document(token)
                .update("used", true).await()
            android.util.Log.d("QR_DEBUG", "Token invalidated: $token")
        } catch (e: Exception) {
            android.util.Log.e("QR_DEBUG", "Failed to invalidate token: ${e.message}")
        }
    }

    suspend fun getScanData(scanId: String): Result<DeviceScanData> {
        return try {
            val doc = firestore.collection("device_scans")
                .document(scanId)
                .get()
                .await()
            val data = doc.toObject(DeviceScanData::class.java)
            if (data != null) Result.success(data)
            else Result.failure(Exception("Scan not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
