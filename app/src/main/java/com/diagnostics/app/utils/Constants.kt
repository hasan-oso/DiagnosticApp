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
package com.diagnostics.app.utils

object Constants {
    const val APP_VERSION = "1.3.0"
    
    // Stress test durations (seconds)
    const val CPU_STRESS_DURATION = 480      // 8 minutes
    const val STORAGE_STRESS_DURATION = 180  // 3 minutes
    const val BATTERY_STRESS_DURATION = 240  // 4 minutes
    
    // Storage test parameters
    const val STORAGE_TEST_FILES = 10
    const val STORAGE_TEST_FILE_SIZE_MB = 50
    const val STORAGE_MIN_FREE_SPACE_MB = 1000
    
    // Temperature sampling
    const val TEMP_SAMPLE_INTERVAL_SEC = 5
    
    // Battery requirements
    const val MIN_BATTERY_PERCENT = 50
    
    // Supabase (will be replaced with actual credentials)
    const val SUPABASE_URL = "https://hfxbsjvwaiutdnulozte.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_tPdFRD4MEAfKSLjOdwhmeQ_2AzPDcv9"
}
