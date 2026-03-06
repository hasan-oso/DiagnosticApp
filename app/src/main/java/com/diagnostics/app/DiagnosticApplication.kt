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
package com.diagnostics.app

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class DiagnosticApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initFirebaseAppCheck()
    }

    private fun initFirebaseAppCheck() {
        try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.i("DiagnosticApp", "Firebase App Check initialized with Play Integrity")
        } catch (e: Exception) {
            Log.e("DiagnosticApp", "Failed to initialize Firebase App Check: ${e.message}")
        }
    }
}
