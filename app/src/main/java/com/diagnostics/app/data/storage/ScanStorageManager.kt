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
package com.diagnostics.app.data.storage

import android.content.Context
import android.util.Log
import com.diagnostics.app.data.models.DeviceScanData
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

/**
 * Manages local storage of scan data
 * - pending/: Scans waiting to be uploaded
 * - uploaded/: Successfully uploaded scans
 */
class ScanStorageManager(private val context: Context) {
    
    private val pendingDir = File(context.filesDir, "pending")
    private val uploadedDir = File(context.filesDir, "uploaded")
    
    private val jsonHandler = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = true
        encodeDefaults = true
    }
    
    init {
        // Create directories if they don't exist
        pendingDir.mkdirs()
        uploadedDir.mkdirs()
        Log.d("ScanStorage", "Storage initialized")
        Log.d("ScanStorage", "Pending dir: ${pendingDir.absolutePath}")
        Log.d("ScanStorage", "Uploaded dir: ${uploadedDir.absolutePath}")
    }
    
    /**
     * Save scan to pending directory
     * This ALWAYS succeeds (local storage)
     */
    fun savePending(scanData: DeviceScanData): Boolean {
        return try {
            val file = File(pendingDir, "${scanData.scanId}.json")
            val json = jsonHandler.encodeToString(scanData)
            file.writeText(json)
            
            Log.d("ScanStorage", "✅ Saved to pending: ${scanData.scanId}")
            Log.d("ScanStorage", "File size: ${file.length()} bytes")
            true
        } catch (e: Exception) {
            Log.e("ScanStorage", "❌ Failed to save pending: ${e.message}", e)
            false
        }
    }
    
    /**
     * Move scan from pending to uploaded
     * Called after successful upload
     */
    fun markUploaded(scanId: String): Boolean {
        return try {
            val pendingFile = File(pendingDir, "$scanId.json")
            val uploadedFile = File(uploadedDir, "$scanId.json")
            
            if (pendingFile.exists()) {
                pendingFile.renameTo(uploadedFile)
                Log.d("ScanStorage", "✅ Moved to uploaded: $scanId")
                true
            } else {
                Log.w("ScanStorage", "Pending file not found: $scanId")
                false
            }
        } catch (e: Exception) {
            Log.e("ScanStorage", "❌ Failed to mark uploaded: ${e.message}", e)
            false
        }
    }
    
    /**
     * Get all pending scans
     */
    fun getPendingScans(): List<DeviceScanData> {
        return try {
            pendingDir.listFiles()?.mapNotNull { file ->
                try {
                    val json = file.readText()
                    jsonHandler.decodeFromString<DeviceScanData>(json)
                } catch (e: Exception) {
                    Log.e("ScanStorage", "Failed to parse: ${file.name}", e)
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e("ScanStorage", "Failed to get pending scans: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get count of pending scans
     */
    fun getPendingCount(): Int {
        return try {
            pendingDir.listFiles()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get count of uploaded scans
     */
    fun getUploadedCount(): Int {
        return try {
            uploadedDir.listFiles()?.size ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get scan by ID from pending
     */
    fun getPendingScan(scanId: String): DeviceScanData? {
        return try {
            val file = File(pendingDir, "$scanId.json")
            if (file.exists()) {
                val json = file.readText()
                jsonHandler.decodeFromString<DeviceScanData>(json)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ScanStorage", "Failed to get scan: $scanId", e)
            null
        }
    }
    
    /**
     * Delete pending scan (use with caution)
     */
    fun deletePending(scanId: String): Boolean {
        return try {
            val file = File(pendingDir, "$scanId.json")
            val deleted = file.delete()
            if (deleted) {
                Log.d("ScanStorage", "Deleted pending: $scanId")
            }
            deleted
        } catch (e: Exception) {
            Log.e("ScanStorage", "Failed to delete: $scanId", e)
            false
        }
    }
}
