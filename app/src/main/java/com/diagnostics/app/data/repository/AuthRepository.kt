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
package com.diagnostics.app.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.diagnostics.app.data.models.auth.ShopAccount
import com.diagnostics.app.data.models.auth.ShopInfo
import com.diagnostics.app.data.models.auth.RegistrationData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * AuthRepository — Handles Firebase Authentication, shop profile CRUD,
 * and logo upload to Firebase Storage.
 */
class AuthRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "AuthRepo"
        private const val COLLECTION_SHOPS = "shops"
        private const val STORAGE_LOGOS = "shop_logos"
    }

    // ══════════════════════════════════════════════
    // Authentication
    // ══════════════════════════════════════════════

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = currentUser != null

    /**
     * Register a new shop account with email/password and shop info.
     */
    suspend fun register(data: RegistrationData): Result<ShopAccount> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Create Firebase Auth user
                val authResult = auth.createUserWithEmailAndPassword(
                    data.email, data.password
                ).await()

                val userId = authResult.user?.uid
                    ?: return@withContext Result.failure(Exception("Registration failed"))

                Log.d(TAG, "Auth user created: $userId")

                // 2. Create shop profile in Firestore
                val shopAccount = ShopAccount(
                    id = userId,
                    email = data.email,
                    shopInfo = ShopInfo(
                        shopName = data.shopNameEn.ifEmpty { data.shopNameTr.ifEmpty { data.shopNameAr } },
                        shopNameAr = data.shopNameAr,
                        shopNameTr = data.shopNameTr,
                        shopNameEn = data.shopNameEn,
                        ownerName = data.ownerName,
                        phoneNumber = data.phoneNumber,
                        whatsappNumber = data.whatsapp,
                        address = data.address,
                        city = data.city,
                        district = data.district,
                        businessType = data.businessType,
                        instagramHandle = data.instagram,
                        logoUrl = data.logoUrl,
                        primaryColor = data.primaryColor,
                        secondaryColor = data.secondaryColor
                    )
                )

                firestore.collection(COLLECTION_SHOPS)
                    .document(userId)
                    .set(shopAccount)
                    .await()

                Log.d(TAG, "✅ Shop profile created for: ${shopAccount.shopInfo.shopName}")
                Result.success(shopAccount)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Registration failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Login with email and password.
     */
    suspend fun login(email: String, password: String): Result<ShopAccount> {
        return withContext(Dispatchers.IO) {
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val userId = authResult.user?.uid
                    ?: return@withContext Result.failure(Exception("Login failed"))

                Log.d(TAG, "Auth login successful: $userId")

                // Fetch shop profile
                val shopAccount = getShopProfile(userId)
                if (shopAccount != null) {
                    Result.success(shopAccount)
                } else {
                    Result.failure(Exception("Shop profile not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Login failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Logout current user.
     */
    fun logout() {
        auth.signOut()
        Log.d(TAG, "User logged out")
    }

    // ══════════════════════════════════════════════
    // Shop Profile
    // ══════════════════════════════════════════════

    /**
     * Get shop profile from Firestore.
     */
    suspend fun getShopProfile(userId: String? = null): ShopAccount? {
        return withContext(Dispatchers.IO) {
            try {
                val uid = userId ?: currentUser?.uid ?: return@withContext null

                val doc = firestore.collection(COLLECTION_SHOPS)
                    .document(uid)
                    .get()
                    .await()

                doc.toObject(ShopAccount::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching shop profile: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Update shop profile info.
     */
    suspend fun updateShopProfile(shopInfo: ShopInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val uid = currentUser?.uid ?: return@withContext false

                firestore.collection(COLLECTION_SHOPS)
                    .document(uid)
                    .update("shopInfo", shopInfo)
                    .await()

                Log.d(TAG, "✅ Shop profile updated")
                true
            } catch (e: Exception) {
                Log.e(TAG, "❌ Profile update failed: ${e.message}", e)
                false
            }
        }
    }

    // ══════════════════════════════════════════════
    // Logo Upload
    // ══════════════════════════════════════════════

    /**
     * Upload shop logo to Firebase Storage.
     * @return The download URL of the uploaded logo.
     */
    suspend fun uploadLogo(imageUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val uid = currentUser?.uid ?: return@withContext null
                val filename = "${uid}_${UUID.randomUUID()}.jpg"

                val ref = storage.reference
                    .child(STORAGE_LOGOS)
                    .child(filename)

                ref.putFile(imageUri).await()
                val downloadUrl = ref.downloadUrl.await().toString()

                Log.d(TAG, "✅ Logo uploaded: $downloadUrl")
                downloadUrl
            } catch (e: Exception) {
                Log.e(TAG, "❌ Logo upload failed: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Increment the scan counter for this shop.
     */
    suspend fun incrementScanCount() {
        withContext(Dispatchers.IO) {
            try {
                val uid = currentUser?.uid ?: return@withContext

                firestore.collection(COLLECTION_SHOPS)
                    .document(uid)
                    .update(
                        mapOf(
                            "shopInfo.totalScans" to com.google.firebase.firestore.FieldValue.increment(1),
                            "shopInfo.monthlyScans" to com.google.firebase.firestore.FieldValue.increment(1),
                            "shopInfo.lastScanDate" to System.currentTimeMillis()
                        )
                    )
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Error incrementing scan count: ${e.message}", e)
            }
        }
    }
}
