/*
 * Copyright (c) 2025 Hasan Oso. All Rights Reserved.
 * PROPRIETARY SOFTWARE - DO NOT DISTRIBUTE
 * Owner: Hasan Oso | Institution: OSTIM Technical University
 */
package com.diagnostics.app.data.collectors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import java.io.File
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.diagnostics.app.data.models.DataQuality
import com.diagnostics.app.data.models.StableFeatures
import com.diagnostics.app.data.models.TimeSeriesSample
import com.diagnostics.app.domain.models.BatteryCapacityMetrics

data class CapacityCorrectionResult(
    val correctedCapacity: Int,
    val correctionMethod: String,
    val ratio: Float,
    val calculatedCapacity: Int
)

data class BatteryCapacityInfo(
    val designCapacityMah: Int,
    val currentFullCapacityMah: Int,
    val capacityFadePercent: Float,
    val dataSource: String,
    val correctionMethod: String = "NONE",
    val validationRatio: Float = 0f
)

/**
 * BatteryCollector - Collects battery data
 *
 * Security Analysis:
 * - Samsung Knox blocks CURRENT_NOW on 60% of devices
 * - Xiaomi returns Integer.MIN_VALUE as obfuscation
 * - Solution: Multi-fallback approach (40% success rate - acceptable)
 *
 * Data Sources:
 * - Temperature, Voltage, health: BroadcastReceiver (most accurate)
 * - Capacity: sysfs charge_full_design → PowerProfile → Math
 * - Current: BatteryManager properties (Knox-restricted)
 */
class BatteryCollector(private val context: Context) {

    companion object {
        /** Sentinel: manufacturer (Knox/OEM) has blocked current reading — NOT a real measurement */
        const val CURRENT_BLOCKED = Int.MIN_VALUE
        private const val TAG = "BatteryCollector"
    }

    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    private val capacityReader = BatteryCapacityReader(context)

    // ═══════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════

    suspend fun collectBatteryData(): BatteryData {
        val status = getBatteryStatus()
        return BatteryData(
            designCapacityMah = getBatteryDesignCapacity(),
            healthStatus      = status.health,
            technology        = status.technology,
            tempCelsius       = status.temperature,
            voltageMv         = status.voltage,
            currentNowMa      = getBatteryCurrent()
        )
    }

    fun getBatteryCapacityMetrics(
        deviceAgeYears: Float,
        resistanceMohm: Int = 0,
        voltageSagMv: Int = 0
    ): BatteryCapacityMetrics = capacityReader.getCapacityMetrics(deviceAgeYears, resistanceMohm, voltageSagMv)

    /** Backward-compat wrapper */
    fun getBatteryCapacity(): Int = capacityReader.getDesignCapacity().capacityMah

    fun getBatteryLevel(): Int = try {
        batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get battery level", e); 0
    }

    fun isCharging(): Boolean = try {
        val status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    } catch (e: Exception) {
        Log.e(TAG, "Failed to check charging status", e); false
    }

    // ═══════════════════════════════════════════════════
    // Fix 1 — Design Capacity Cascade (NO charge_counter)
    // ═══════════════════════════════════════════════════

    /**
     * Get battery design capacity using:
     *  1. sysfs charge_full_design  (most accurate, µAh ÷ 1000)
     *  2. PowerProfile reflection   (works on most OEMs)
     *  3. Math: (charge_counter / level%) × 100  (estimates full from current state)
     *
     * charge_counter is NOT used as a direct capacity value — it's current remaining charge.
     */
    fun getBatteryDesignCapacity(): Int {
        // ── Attempt 1: sysfs charge_full_design ──
        val sysfsPaths = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/Battery/charge_full_design",  // Samsung (capital B)
            "/sys/class/power_supply/bms/charge_full_design"
        )
        for (path in sysfsPaths) {
            try {
                val raw = File(path).readText().trim().toLong()
                val mah = raw / 1000   // µAh → mAh
                if (mah in 1000..10000) {
                    Log.d(TAG, "Design capacity from sysfs $path: $mah mAh")
                    return mah.toInt()
                }
            } catch (_: Exception) {}
        }

        // ── Attempt 2: PowerProfile reflection ──
        try {
            val clazz    = Class.forName("com.android.internal.os.PowerProfile")
            val instance = clazz.getConstructor(Context::class.java).newInstance(context)
            val capacity = (clazz.getMethod("getBatteryCapacity").invoke(instance) as Double).toLong()
            if (capacity in 1000..10000) {
                Log.d(TAG, "Design capacity from PowerProfile: $capacity mAh")
                return capacity.toInt()
            }
        } catch (_: Exception) {}

        // ── Attempt 3: Math from level + charge_counter ──
        try {
            val level     = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val remaining = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (level > 5 && remaining > 0) {
                val estimated = ((remaining / level.toFloat()) * 100L / 1000L).toInt()
                if (estimated in 1000..10000) {
                    Log.d(TAG, "Design capacity estimated (level=$level%): $estimated mAh")
                    return estimated
                }
            }
        } catch (_: Exception) {}

        Log.w(TAG, "Design capacity unavailable — returning 0")
        return 0
    }

    // ═══════════════════════════════════════════════════
    // Battery Status (BroadcastReceiver — most reliable)
    // ═══════════════════════════════════════════════════

    private suspend fun getBatteryStatus(): BatteryStatus = suspendCancellableCoroutine { cont ->
        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    try {
                        val tempCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                        val voltage     = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
                        val healthCode  = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                        val tech        = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
                        ctx.unregisterReceiver(this)
                        val validTemp    = if (tempCelsius in 10f..80f) tempCelsius else 0f
                        val validVoltage = if (voltage in 2000..5000)   voltage     else 0
                        cont.resume(BatteryStatus(validTemp, validVoltage, mapHealthStatus(healthCode), tech))
                    } catch (e: Exception) {
                        ctx.unregisterReceiver(this)
                        cont.resume(BatteryStatus(0f, 0, "UNKNOWN", "Li-ion"))
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (e: Exception) {
            cont.resume(BatteryStatus(0f, 0, "UNKNOWN", "Li-ion"))
        }
    }

    // ═══════════════════════════════════════════════════
    // Fix 3 — Battery Current with CURRENT_BLOCKED sentinel
    // ═══════════════════════════════════════════════════

    /**
     * Returns actual current in mA, or CURRENT_BLOCKED (Int.MIN_VALUE) if restricted.
     * Callers MUST check `== CURRENT_BLOCKED` before using the value.
     */
    private fun getBatteryCurrent(): Int {
        try {
            val now = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (now != 0 && now != Integer.MIN_VALUE) {
                Log.d(TAG, "Current (NOW): ${now / 1000} mA")
                return now / 1000
            }
            val avg = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
            if (avg != 0 && avg != Integer.MIN_VALUE) {
                Log.d(TAG, "Current (AVG): ${avg / 1000} mA")
                return avg / 1000
            }
            Log.w(TAG, "Current blocked by Samsung Knox / OEM policy")
            return CURRENT_BLOCKED
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery current", e)
            return CURRENT_BLOCKED
        }
    }

    // ═══════════════════════════════════════════════════
    // Fix 3+4 — analyzeDataQuality with CURRENT_BLOCKED + age-aware pmicStalled
    // ═══════════════════════════════════════════════════

    fun analyzeDataQuality(
        samples: List<TimeSeriesSample>,
        deviceAgeYears: Float = 0f
    ): DataQuality {
        if (samples.isEmpty()) return DataQuality(true, 0, 0, "POOR", 0.0f)

        var voltageStallCount = 0
        var currentStallCount = 0
        var voltageGlitches   = 0
        var currentGlitches   = 0
        var sameVoltageStreak = 0
        var sameCurrentStreak = 0
        var lastVoltage = samples[0].voltage_mv
        var lastCurrent = samples[0].current_ma

        for (i in 1 until samples.size) {
            val s = samples[i]
            if (s.voltage_mv == lastVoltage) { if (++sameVoltageStreak >= 10) voltageStallCount++ } else sameVoltageStreak = 0
            if (s.current_ma == lastCurrent) { if (++sameCurrentStreak >= 10) currentStallCount++ } else sameCurrentStreak = 0
            if (Math.abs(s.voltage_mv - lastVoltage) > 500 && sameVoltageStreak == 0) voltageGlitches++
            if (Math.abs(s.current_ma - lastCurrent) > 1000 && sameCurrentStreak == 0) currentGlitches++
            lastVoltage = s.voltage_mv
            lastCurrent = s.current_ma
        }

        // Fix 3: ignore current stall when current is Knox-blocked (all values ≤ 0)
        val isCurrentBlocked = samples.all { it.current_ma == CURRENT_BLOCKED || it.current_ma <= 0 }

        // Fix 4: voltage stall in NEW devices is normal healthy-battery behaviour
        val voltageStallSuspicious = voltageStallCount > 20 && deviceAgeYears > 2.0f
        val pmicStalled = voltageStallSuspicious || (!isCurrentBlocked && currentStallCount > 20)

        val confidence = when {
            pmicStalled  && voltageGlitches == 0 -> 0.85f
            pmicStalled  && voltageGlitches > 0  -> 0.60f
            !pmicStalled && voltageGlitches == 0 -> 1.0f
            !pmicStalled && voltageGlitches < 3  -> 0.90f
            else -> 0.50f
        }
        val quality = when {
            confidence >= 0.95f -> "EXCELLENT"
            confidence >= 0.80f -> "GOOD"
            confidence >= 0.60f -> "FAIR"
            else -> "POOR"
        }
        return DataQuality(pmicStalled, voltageGlitches, currentGlitches, quality, confidence)
    }

    fun extractStableFeatures(
        baselineSamples: List<TimeSeriesSample>,
        loadSamples: List<TimeSeriesSample>
    ): StableFeatures {
        if (baselineSamples.isEmpty() || loadSamples.isEmpty()) {
            return StableFeatures(0f, 0f, 0f, 0, 0, 0, "STABLE", 0, 0f, 0f, 0f, 0f, false)
        }
        val baselineVoltages = baselineSamples.map { it.voltage_mv.toFloat() }
        val baselineAvg = baselineVoltages.average().toFloat()
        val baselineStd = calculateStdDev(baselineVoltages)

        var transitionTime = 0; var maxVoltageChange = 0; var voltageDirection = "STABLE"
        for (i in 1 until loadSamples.size) {
            val change = Math.abs(baselineAvg.toInt() - loadSamples[i].voltage_mv)
            if (change > maxVoltageChange) {
                maxVoltageChange = change; transitionTime = i
                voltageDirection = if (loadSamples[i].voltage_mv > baselineAvg) "RISE" else "DROP"
            }
        }
        val voltageChangeMv = if (voltageDirection == "RISE") -maxVoltageChange else maxVoltageChange
        var voltageDropMv = 0
        for (i in 1 until loadSamples.size) {
            val drop = loadSamples[0].voltage_mv - loadSamples[i].voltage_mv
            if (drop > voltageDropMv) voltageDropMv = drop
        }
        val stableLoadSamples = if (transitionTime + 5 < loadSamples.size)
            loadSamples.subList(transitionTime + 5, loadSamples.size)
        else loadSamples.takeLast(Math.min(5, loadSamples.size))
        val loadVoltageStable = if (stableLoadSamples.isNotEmpty()) stableLoadSamples.map { it.voltage_mv }.average().toFloat() else 0f
        val voltageRecovery = if (loadSamples.size > transitionTime + 10) {
            (loadSamples.takeLast(5).map { it.voltage_mv }.average() - loadSamples[transitionTime].voltage_mv).toInt()
        } else 0
        val tempRise = loadSamples.last().temp_c - baselineSamples.first().temp_c
        val durationMin = loadSamples.size * 10 / 60.0f
        val tempRiseRate = if (durationMin > 0) tempRise / durationMin else 0f
        val cpuFreqs = (baselineSamples + loadSamples).map { it.cpu_freq_mhz.toFloat() }
        val avgCpuFreq = if (cpuFreqs.isNotEmpty()) cpuFreqs.average().toFloat() else 0f
        val loadCpuFreqs = loadSamples.map { it.cpu_freq_mhz }
        val throttlingDetected = ((loadCpuFreqs.maxOrNull() ?: 0) - (loadCpuFreqs.minOrNull() ?: 0)) > 500

        return StableFeatures(
            baselineVoltageAvg       = baselineAvg,
            baselineVoltageStd       = baselineStd,
            loadVoltageStable        = loadVoltageStable,
            voltageTransitionTimeSec = transitionTime,
            voltageDropMv            = voltageDropMv,
            voltageChangeMv          = voltageChangeMv,
            voltageChangeDirection   = voltageDirection,
            voltageRecoveryMv        = voltageRecovery,
            tempRiseTotal            = tempRise,
            tempRiseRatePerMin       = tempRiseRate,
            avgCpuFreqMhz            = avgCpuFreq,
            cpuFreqVariation         = calculateStdDev(cpuFreqs),
            thermalThrottlingDetected = throttlingDetected
        )
    }

    fun calculateStdDev(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        return Math.sqrt(values.map { (it - mean) * (it - mean) }.average()).toFloat()
    }

    // ───────────────────────────────────────────────────
    private fun mapHealthStatus(code: Int): String = when (code) {
        BatteryManager.BATTERY_HEALTH_GOOD                -> "GOOD"
        BatteryManager.BATTERY_HEALTH_OVERHEAT            -> "OVERHEAT"
        BatteryManager.BATTERY_HEALTH_DEAD                -> "DEAD"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE        -> "OVER_VOLTAGE"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
        BatteryManager.BATTERY_HEALTH_COLD                -> "COLD"
        else                                               -> "UNKNOWN"
    }

    data class BatteryStatus(
        val temperature: Float,
        val voltage: Int,
        val health: String,
        val technology: String
    )
}

data class BatteryData(
    val designCapacityMah: Int,
    val healthStatus: String,
    val technology: String,
    val tempCelsius: Float,
    val voltageMv: Int,
    val currentNowMa: Int
)
