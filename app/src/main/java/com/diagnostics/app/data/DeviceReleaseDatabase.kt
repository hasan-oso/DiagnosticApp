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
package com.diagnostics.app.data

import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Database of device release dates for accurate age calculation.
 * Format: Year + (Month / 12.0)
 * Example: Jan 2021 = 2021.083f, Apr 2019 = 2019.25f
 */
object DeviceReleaseDatabase {

    /**
     * Map of device model codes to release dates (year + month/12).
     */
    private val RELEASE_DATES = mapOf(
        // ── Samsung Galaxy S Series ──────────────────────────────────────
        "SM-G998B" to 2021.083f,  // S21 Ultra 5G - Jan 2021
        "SM-G998U" to 2021.083f,  // S21 Ultra 5G (US)
        "SM-G998N" to 2021.083f,  // S21 Ultra 5G (Korea)
        "SM-G991B" to 2021.083f,  // S21 5G - Jan 2021
        "SM-G991U" to 2021.083f,  // S21 5G (US)
        "SM-G996B" to 2021.083f,  // S21+ 5G - Jan 2021
        "SM-G996U" to 2021.083f,  // S21+ 5G (US)

        "SM-G988B" to 2020.167f,  // S20 Ultra 5G - Feb 2020
        "SM-G988U" to 2020.167f,  // S20 Ultra 5G (US)
        "SM-G981B" to 2020.167f,  // S20 5G - Feb 2020
        "SM-G981U" to 2020.167f,  // S20 5G (US)
        "SM-G986B" to 2020.167f,  // S20+ 5G - Feb 2020
        "SM-G986U" to 2020.167f,  // S20+ 5G (US)

        "SM-G977N" to 2019.25f,   // S10 5G - Apr 2019
        "SM-G977B" to 2019.25f,   // S10 5G
        "SM-G977U" to 2019.25f,   // S10 5G (US)
        "SM-G973F" to 2019.167f,  // S10 - Feb 2019
        "SM-G973U" to 2019.167f,  // S10 (US)
        "SM-G975F" to 2019.167f,  // S10+ - Feb 2019
        "SM-G975U" to 2019.167f,  // S10+ (US)
        "SM-G970F" to 2019.167f,  // S10e - Feb 2019
        "SM-G970U" to 2019.167f,  // S10e (US)

        "SM-G965F" to 2018.167f,  // S9+ - Feb 2018
        "SM-G965U" to 2018.167f,  // S9+ (US)
        "SM-G960F" to 2018.167f,  // S9 - Feb 2018
        "SM-G960U" to 2018.167f,  // S9 (US)

        "SM-G955F" to 2017.25f,   // S8+ - Apr 2017
        "SM-G955U" to 2017.25f,   // S8+ (US)
        "SM-G950F" to 2017.25f,   // S8 - Apr 2017
        "SM-G950U" to 2017.25f,   // S8 (US)

        // ── Samsung Galaxy S22/S23/S24 Series ───────────────────────────
        "SM-S908B" to 2022.167f,  // S22 Ultra - Feb 2022
        "SM-S908U" to 2022.167f,  // S22 Ultra (US)
        "SM-S901B" to 2022.167f,  // S22 - Feb 2022
        "SM-S906B" to 2022.167f,  // S22+ - Feb 2022

        "SM-S918B" to 2023.167f,  // S23 Ultra - Feb 2023
        "SM-S918U" to 2023.167f,  // S23 Ultra (US)
        "SM-S911B" to 2023.167f,  // S23 - Feb 2023
        "SM-S916B" to 2023.167f,  // S23+ - Feb 2023

        "SM-S928B" to 2024.083f,  // S24 Ultra - Jan 2024
        "SM-S928U" to 2024.083f,  // S24 Ultra (US)
        "SM-S921B" to 2024.083f,  // S24 - Jan 2024
        "SM-S926B" to 2024.083f,  // S24+ - Jan 2024

        // ── Samsung Galaxy Note Series ───────────────────────────────────
        "SM-N986B" to 2020.667f,  // Note20 Ultra 5G - Aug 2020
        "SM-N986U" to 2020.667f,  // Note20 Ultra 5G (US)
        "SM-N981B" to 2020.667f,  // Note20 5G - Aug 2020
        "SM-N975F" to 2019.667f,  // Note10+ - Aug 2019
        "SM-N975U" to 2019.667f,  // Note10+ (US)
        "SM-N971N" to 2019.667f,  // Note10 5G - Aug 2019
        "SM-N970F" to 2019.667f,  // Note10 - Aug 2019
        "SM-N970U" to 2019.667f,  // Note10 (US)
        "SM-N960F" to 2018.667f,  // Note9 - Aug 2018
        "SM-N950F" to 2017.667f,  // Note8 - Aug 2017

        // ── Samsung Galaxy A Series ──────────────────────────────────────
        "SM-A546B" to 2023.25f,   // A54 5G - Mar 2023
        "SM-A536B" to 2022.25f,   // A53 5G - Mar 2022
        "SM-A525F" to 2021.25f,   // A52 - Mar 2021
        "SM-A526B" to 2021.25f,   // A52 5G - Mar 2021
        "SM-A515F" to 2020.0f,    // A51 - Jan 2020
        "SM-A516B" to 2020.5f,    // A51 5G - Jun 2020
        "SM-A505F" to 2019.25f,   // A50 - Mar 2019
        "SM-A405F" to 2019.25f,   // A40 - Mar 2019
        "SM-A305F" to 2019.25f,   // A30 - Mar 2019
        "SM-A205F" to 2019.25f,   // A20 - Mar 2019
        "SM-A715F" to 2020.0f,    // A71 - Jan 2020
        "SM-A725F" to 2021.0f,    // A72 - Mar 2021
        "SM-A736B" to 2022.5f,    // A73 5G - Jun 2022

        // ── Samsung Galaxy Z Series ──────────────────────────────────────
        "SM-F946B" to 2023.667f,  // Z Fold5 - Aug 2023
        "SM-F936B" to 2022.667f,  // Z Fold4 - Aug 2022
        "SM-F926B" to 2021.667f,  // Z Fold3 - Aug 2021
        "SM-F916B" to 2020.667f,  // Z Fold2 - Sep 2020
        "SM-F711B" to 2021.667f,  // Z Flip3 - Aug 2021
        "SM-F721B" to 2022.667f,  // Z Flip4 - Aug 2022
        "SM-F731B" to 2023.667f,  // Z Flip5 - Aug 2023

        // ── Google Pixel Series ──────────────────────────────────────────
        "Pixel 9 Pro" to 2024.667f,   // Pixel 9 Pro - Aug 2024
        "Pixel 9"     to 2024.667f,   // Pixel 9 - Aug 2024
        "Pixel 8 Pro" to 2023.833f,   // Pixel 8 Pro - Oct 2023
        "Pixel 8"     to 2023.833f,   // Pixel 8 - Oct 2023
        "Pixel 7 Pro" to 2022.833f,   // Pixel 7 Pro - Oct 2022
        "Pixel 7"     to 2022.833f,   // Pixel 7 - Oct 2022
        "Pixel 6 Pro" to 2021.833f,   // Pixel 6 Pro - Oct 2021
        "Pixel 6"     to 2021.833f,   // Pixel 6 - Oct 2021
        "Pixel 5"     to 2020.833f,   // Pixel 5 - Oct 2020
        "Pixel 4 XL"  to 2019.833f,   // Pixel 4 XL - Oct 2019
        "Pixel 4"     to 2019.833f,   // Pixel 4 - Oct 2019
        "Pixel 3 XL"  to 2018.833f,   // Pixel 3 XL - Oct 2018
        "Pixel 3"     to 2018.833f,   // Pixel 3 - Oct 2018
    )

    /**
     * Get device age in years.
     * Uses release date lookup table with Build.TIME fallback.
     *
     * @return Device age in years (decimal, e.g. 5.1f)
     */
    fun getDeviceAge(): Float {
        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-indexed
        val currentDecimal = currentYear + (currentMonth / 12.0f)

        val model = Build.MODEL
        val releaseDate = RELEASE_DATES[model]

        return if (releaseDate != null) {
            // Use lookup table (accurate)
            val age = currentDecimal - releaseDate
            Log.d("DeviceAge", """
                ═══════════════════════════════════════
                Device Age (Lookup Table):
                ═══════════════════════════════════════
                Model:          $model
                Release date:   $releaseDate
                Current date:   $currentDecimal
                Age:            ${age}y
                Source:         Release date database ✅
                ═══════════════════════════════════════
            """.trimIndent())
            age.coerceAtLeast(0f)
        } else {
            // Fallback to Build.TIME (less accurate – affected by factory reset)
            val buildCal = Calendar.getInstance().apply { timeInMillis = Build.TIME }
            val buildYear = buildCal.get(Calendar.YEAR)
            val buildMonth = buildCal.get(Calendar.MONTH) + 1
            val buildDecimal = buildYear + (buildMonth / 12.0f)

            val age = (currentDecimal - buildDecimal).coerceAtLeast(0f)

            Log.w("DeviceAge", """
                ═══════════════════════════════════════
                Device Age (Fallback):
                ═══════════════════════════════════════
                Model:          $model
                Build date:     $buildDecimal
                Current date:   $currentDecimal
                Age:            ${age}y
                Source:         Build.TIME (fallback) ⚠️
                Warning:        May be inaccurate (factory reset resets Build.TIME)
                ═══════════════════════════════════════
            """.trimIndent())
            age
        }
    }

    /**
     * Check if device model is in the database.
     */
    fun isModelKnown(): Boolean = RELEASE_DATES.containsKey(Build.MODEL)
}
