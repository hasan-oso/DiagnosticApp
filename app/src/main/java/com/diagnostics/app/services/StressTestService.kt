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
package com.diagnostics.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground Service for stress tests.
 * Prevents the system from killing the app during 15+ minute tests.
 * Shows a persistent notification with progress updates.
 */
class StressTestService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _progress = MutableStateFlow(StressTestProgress())
    val progress: StateFlow<StressTestProgress> = _progress.asStateFlow()

    private var currentJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): StressTestService = this@StressTestService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "StressTestService created")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("Initializing stress tests...", 0)
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "StressTestService started in foreground")
        return START_STICKY
    }

    /**
     * Update progress from ViewModel and refresh notification
     */
    fun updateProgress(status: String, progressPercent: Int, error: String? = null) {
        _progress.value = StressTestProgress(status, progressPercent, error)
        val notification = createNotification(status, progressPercent)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel all running tests and stop the service
     */
    fun cancelTests() {
        currentJob?.cancel()
        _progress.value = StressTestProgress(status = "Cancelled", progress = 0)
        Log.d(TAG, "Tests cancelled, stopping service")
        stopSelf()
    }

    /**
     * Stop the service gracefully when tests are complete
     */
    fun onTestsComplete() {
        Log.d(TAG, "Tests complete, stopping service")
        stopSelf()
    }

    private fun createNotification(status: String, progress: Int = 0): Notification {
        // Try to get MainActivity class for the pending intent
        val pendingIntent = try {
            val mainActivityClass = Class.forName("com.diagnostics.app.MainActivity")
            val intent = Intent(this, mainActivityClass).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Device Diagnostic")
            .setContentText(status)
            .setSmallIcon(android.R.drawable.ic_menu_manage) // System icon as fallback
            .setOngoing(true)
            .setSilent(true)

        if (progress > 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(100, 0, true) // Indeterminate
        }

        pendingIntent?.let { builder.setContentIntent(it) }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Stress Tests",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running device stress tests"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        currentJob?.cancel()
        serviceScope.cancel()
        Log.d(TAG, "StressTestService destroyed")
        super.onDestroy()
    }

    companion object {
        private const val TAG = "StressTestService"
        private const val CHANNEL_ID = "stress_test_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, StressTestService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

data class StressTestProgress(
    val status: String = "Idle",
    val progress: Int = 0,
    val error: String? = null
)
