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
package com.diagnostics.app.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.diagnostics.app.data.repository.FirestoreRepository
import com.diagnostics.app.data.storage.ScanStorageManager

/**
 * Background worker for uploading scans to Google Drive
 * Handles retry automatically with exponential backoff
 */
class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val scanId = inputData.getString("scan_id")
        
        if (scanId == null) {
            Log.e("UploadWorker", "No scan_id provided")
            return Result.failure()
        }
        
        Log.d("UploadWorker", "Starting upload attempt for: $scanId")
        Log.d("UploadWorker", "Attempt: $runAttemptCount")
        
        try {
            // Get scan from local storage
            val storage = ScanStorageManager(applicationContext)
            val scanData = storage.getPendingScan(scanId)
            
            if (scanData == null) {
                Log.e("UploadWorker", "Scan not found in pending: $scanId")
                return Result.failure()
            }
            
            Log.d("UploadWorker", "Found scan: ${scanData.deviceModel}")
            
            // Initialize Firestore repository (no init needed)
            val firestoreRepo = FirestoreRepository()
            
            // Attempt upload
            Log.d("UploadWorker", "Attempting upload to Firestore...")
            val success = firestoreRepo.uploadScan(scanData)
            
            return if (success) {
                Log.d("UploadWorker", "✅ Upload successful: $scanId")
                
                // Mark as uploaded (move to uploaded/)
                storage.markUploaded(scanId)
                
                Result.success()
            } else {
                Log.w("UploadWorker", "❌ Upload failed: $scanId")
                
                // Retry if we haven't exceeded max attempts
                if (runAttemptCount < 10) {
                    Log.d("UploadWorker", "Will retry later (attempt ${runAttemptCount + 1}/10)")
                    Result.retry()
                } else {
                    Log.e("UploadWorker", "Max retry attempts reached for: $scanId")
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("UploadWorker", "Error during upload: ${e.message}", e)
            
            // Retry on exception
            return if (runAttemptCount < 10) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
