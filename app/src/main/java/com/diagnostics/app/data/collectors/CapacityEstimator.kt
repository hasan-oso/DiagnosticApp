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
package com.diagnostics.app.data.collectors

import android.util.Log
import com.diagnostics.app.domain.models.CapacityReading
import com.diagnostics.app.domain.models.CapacitySource
import kotlin.math.abs

class CapacityEstimator {

    companion object {
        private const val TAG = "CapacityEstimator"
        private const val MAX_PLAUSIBLE_CAPACITY = 7000 // mAh
        private const val MIN_PLAUSIBLE_CAPACITY = 2000 // mAh
    }

    /**
     * Estimates battery capacity using multiple data sources (charge counter, SOC, voltage curve,
     * internal resistance, and voltage sag). It calculates a weighted average.
     */
    fun estimateCapacity(
        chargeCounterMah: Int,
        soc: Int,
        voltageMv: Int,
        resistanceMohm: Int,
        voltageSagMv: Int,
        deviceAgeYears: Float
    ): CapacityReading {
        
        Log.d(TAG, """
            ══════════════════════════════════════
            Multi-Source Capacity Estimation
            ══════════════════════════════════════
            Input: SOC=$soc%, Counter=$chargeCounterMah mAh, V=$voltageMv mV,
            R=$resistanceMohm mΩ, Sag=$voltageSagMv mV, Age=$deviceAgeYears y
        """.trimIndent())

        val estimates = mutableListOf<Pair<Int, Float>>() // Pair<Capacity, Weight>
        val metadata = mutableMapOf<String, String>()

        // 1. Voltage-validated counter calculation
        val counterEstimate = estimateFromVoltageCurve(chargeCounterMah, soc, voltageMv)
        if (counterEstimate != null) {
            Log.i(TAG, "Counter estimate: ${counterEstimate.first} mAh")
            estimates.add(counterEstimate)
            metadata["counter_estimate"] = counterEstimate.first.toString()
            metadata["counter_weight"] = counterEstimate.second.toString()
        }

        // 2. Resistance-based estimation
        val resistanceEstimate = estimateFromResistance(resistanceMohm, deviceAgeYears)
        if (resistanceEstimate != null) {
            Log.i(TAG, "Resistance estimate: ${resistanceEstimate.first} mAh")
            estimates.add(resistanceEstimate)
            metadata["resistance_estimate"] = resistanceEstimate.first.toString()
            metadata["resistance_weight"] = resistanceEstimate.second.toString()
        } else {
            Log.i(TAG, "Resistance estimate skipped (Mohm: $resistanceMohm)")
        }

        // 3. Voltage sag-based estimation
        val sagEstimate = estimateFromVoltageSag(voltageSagMv, deviceAgeYears)
        if (sagEstimate != null) {
            Log.i(TAG, "Voltage sag estimate: ${sagEstimate.first} mAh")
            estimates.add(sagEstimate)
            metadata["sag_estimate"] = sagEstimate.first.toString()
            metadata["sag_weight"] = sagEstimate.second.toString()
        } else {
            Log.i(TAG, "Voltage sag estimate skipped (Sag: $voltageSagMv mV)")
        }
        
        Log.i(TAG, "Total estimators used: ${estimates.size}")

        // Combine using weighted average
        if (estimates.isEmpty()) {
            return CapacityReading(
                capacityMah = 0,
                source = CapacitySource.NOT_AVAILABLE,
                confidence = 0f,
                isRawReading = false,
                metadata = mapOf("reason" to "all_estimators_failed")
            )
        }

        var totalWeight = 0f
        var weightedSum = 0f
        for ((capacity, weight) in estimates) {
            weightedSum += (capacity * weight)
            totalWeight += weight
        }

        val finalCapacity = (weightedSum / totalWeight).toInt()
        
        // Calculate dynamic confidence based on SOC and available weights
        val baseConfidence = when {
            soc >= 90 -> 0.90f
            soc >= 60 -> 0.75f
            soc >= 30 -> 0.60f
            else -> 0.45f
        }
        
        // Adjust confidence slightly upwards if we have multiple corroborating signals
        val totalValidEstimators = estimates.size
        val confidenceBonus = (totalValidEstimators - 1) * 0.05f
        val finalConfidence = (baseConfidence + confidenceBonus).coerceAtMost(0.95f)

        metadata["soc_at_measurement"] = soc.toString()
        metadata["used_estimators"] = totalValidEstimators.toString()

        Log.d(TAG, """
            Final Estimate: $finalCapacity mAh (Confidence: $finalConfidence)
            ══════════════════════════════════════
        """.trimIndent())

        return CapacityReading(
            capacityMah = finalCapacity,
            source = CapacitySource.MULTI_SOURCE_ESTIMATE,
            confidence = finalConfidence,
            isRawReading = false,
            metadata = metadata
        )
    }

    /**
     * Validates the charge counter extrapolation by cross-referencing expected 
     * Li-ion discharge voltages. Gives higher weight if voltage precisely matches SOC.
     */
    private fun estimateFromVoltageCurve(chargeCounterMah: Int, soc: Int, voltageMv: Int): Pair<Int, Float>? {
        if (chargeCounterMah <= 0 || soc < 10) return null

        val basicExtrapolation = (chargeCounterMah * 100) / soc
        
        // Sanity Check
        if (basicExtrapolation !in MIN_PLAUSIBLE_CAPACITY..MAX_PLAUSIBLE_CAPACITY) {
            return null
        }

        val expectedVoltage = getExpectedVoltage(soc)
        val voltageDiff = abs(voltageMv - expectedVoltage)
        
        // If voltage closely matches the expected curve for this SOC, increase weight
        val weight = when {
            voltageDiff <= 50 -> 1.0f  // Perfect match
            voltageDiff <= 100 -> 0.8f // Good match
            voltageDiff <= 200 -> 0.5f // Questionable
            else -> 0.2f               // Likely inaccurate
        } * (soc / 100f) // Always scale weight by SOC% (higher SOC = more reliable counter)

        return Pair(basicExtrapolation, weight)
    }

    /**
     * Li-ion general discharge curve lookup (Resting Voltage)
     */
    private fun getExpectedVoltage(soc: Int): Int {
        return when {
            soc >= 100 -> 4350
            soc >= 90 -> 4250
            soc >= 80 -> 4150
            soc >= 70 -> 4050
            soc >= 60 -> 3950
            soc >= 50 -> 3850
            soc >= 40 -> 3780
            soc >= 30 -> 3720
            soc >= 20 -> 3650
            soc >= 10 -> 3550
            else -> 3400
        }
    }

    /**
     * Internal resistance correlates inversely with capacity/health.
     * Higher resistance = degraded capacity.
     */
    private fun estimateFromResistance(resistanceMohm: Int, deviceAgeYears: Float): Pair<Int, Float>? {
        if (resistanceMohm <= 0 || resistanceMohm > 1000) return null
        
        // Base standard capacity assumption (Average modern phone ~4500)
        val baseCapacity = 4500
        
        // A healthy new battery is ~30-50 mΩ.
        // Deeply degraded is > 200 mΩ.
        val degradationFactor = ((resistanceMohm - 40).coerceAtLeast(0) / 200f).coerceAtMost(0.5f)
        val estimatedCapacity = (baseCapacity * (1f - degradationFactor)).toInt()
        
        // Weight is low because baseCapacity is a guess, but it helps anchor wild counter swings
        val weight = 0.15f 
        
        return Pair(estimatedCapacity, weight)
    }

    /**
     * Voltage sag correlates inversely with health.
     */
    private fun estimateFromVoltageSag(voltageSagMv: Int, deviceAgeYears: Float): Pair<Int, Float>? {
        if (abs(voltageSagMv) < 5) return null
        
        val baseCapacity = 4500
        
        // استخدام العتبة العلمية: ≥35 mV هي voltage sag حقيقي
        // القيم الأصغر قد تكون ضوضاء ADC أو ripple
        val healthPercent = when {
            voltageSagMv < -150 -> 95f  // Rise كبير = صحة ممتازة
            voltageSagMv < -100 -> 90f
            voltageSagMv < -50 -> 85f
            voltageSagMv < -35 -> 80f   // Rise صغير لكن فوق عتبة الضوضاء
            voltageSagMv < 35 -> 75f    // ضمن عتبة الضوضاء (±35 mV)
            voltageSagMv < 50 -> 70f    // Drop صغير لكن ملحوظ
            voltageSagMv < 100 -> 65f
            voltageSagMv < 150 -> 60f
            voltageSagMv < 200 -> 55f
            else -> 50f
        }
        
        val estimatedCapacity = (baseCapacity * (healthPercent / 100f)).toInt()
        
        val weight = 0.15f
        
        return Pair(estimatedCapacity, weight)
    }
}
