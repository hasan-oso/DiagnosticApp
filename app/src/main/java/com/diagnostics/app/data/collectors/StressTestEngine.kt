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

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.diagnostics.app.data.models.BatteryStressResult
import com.diagnostics.app.data.models.CpuStressResult
import com.diagnostics.app.data.models.FreqReading
import com.diagnostics.app.data.models.SpeedPoint
import com.diagnostics.app.data.models.StorageStressResult
import com.diagnostics.app.data.models.TempReading
import com.diagnostics.app.data.models.VoltageReading
import com.diagnostics.app.benchmark.QuickBenchmark
import com.diagnostics.app.benchmark.AdaptiveStressConfig
import com.diagnostics.app.benchmark.StressTestConfig
import com.diagnostics.app.utils.Constants
import com.diagnostics.app.utils.HybridVoltageMonitor
import com.diagnostics.app.domain.models.DataCollectionMetadata
import android.os.Build
import kotlinx.coroutines.*
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.*
import kotlin.random.Random
import android.util.Log
import com.diagnostics.app.data.models.TimeSeriesSample
import com.diagnostics.app.data.models.DataQuality
import com.diagnostics.app.data.models.StableFeatures

data class InternalResistanceResult(
    val baselineVoltage: Int,      // mV
    val loadVoltage: Int,           // mV
    val voltageDrop: Int,           // mV
    val avgCurrent: Int,            // mA
    val internalResistance: Int,    // mΩ
    val calculationMethod: String   // "OHMS_LAW" or "AGE_BASED"
)

class StressTestEngine(private val context: Context) {

    private val batteryCollector = BatteryCollector(context)

    companion object {
        private const val MIN_ITERATIONS = 5_000_000
        private const val MAX_ITERATIONS = 100_000_000
        private const val TARGET_LOAD_PERCENTAGE = 0.90f
        private const val CPU_TEST_DURATION_SEC = 480f  // 8 minutes
        private const val BENCHMARK_DURATION_SEC = 10f  // QuickBenchmark duration
        private const val OLD_DEVICE_THRESHOLD_YEARS = 5.0f
        private const val OLD_DEVICE_LOAD_REDUCTION = 0.8f
        
        fun calculateDynamicIterations(
            benchmarkScore: Int,
            deviceAgeYears: Float
        ): Int {
            val targetScore = (benchmarkScore * TARGET_LOAD_PERCENTAGE).toInt()
            val baseIterations = (targetScore * (CPU_TEST_DURATION_SEC / BENCHMARK_DURATION_SEC)).toInt()
            
            val ageAdjustment = if (deviceAgeYears > OLD_DEVICE_THRESHOLD_YEARS) {
                OLD_DEVICE_LOAD_REDUCTION
            } else {
                1.0f
            }
            
            val adjustedIterations = (baseIterations * ageAdjustment).toInt()
            val finalIterations = adjustedIterations.coerceIn(MIN_ITERATIONS, MAX_ITERATIONS)
            
            Log.d("DynamicLoad", """
                ══════════════════════════════════
                Dynamic Load Calculation:
                ══════════════════════════════════
                Benchmark:       $benchmarkScore ops/10s
                Target (90%):    $targetScore ops/10s
                Duration ratio:  ${CPU_TEST_DURATION_SEC / BENCHMARK_DURATION_SEC}x
                Base iterations: $baseIterations
                Device age:      ${deviceAgeYears}y
                Age factor:      ${ageAdjustment}x
                Final:           $finalIterations
                ══════════════════════════════════
            """.trimIndent())
            
            return finalIterations
        }
    }

    private val thermalCollector = ThermalCollector(context)
    private val TAG = "StressTestEngine"

    private var stressConfig: StressTestConfig? = null
    private var deviceTier: QuickBenchmark.DeviceTier? = null
    private var quickBenchmarkScore: Int = 0
    
    private suspend fun initializeAdaptiveConfig() {
        // Quick 5-second benchmark
        quickBenchmarkScore = withContext(Dispatchers.Default) {
            QuickBenchmark.getCPUScore()
        }
        
        deviceTier = QuickBenchmark.getDeviceTier(quickBenchmarkScore)
        stressConfig = AdaptiveStressConfig.getConfig(deviceTier!!)
        
        Log.d("StressTestEngine", "Quick Benchmark: $quickBenchmarkScore")
        Log.d("StressTestEngine", "Device Tier: $deviceTier")
        Log.d("StressTestEngine", "Adaptive Config: $stressConfig")
    }
    
    /**
     * CPU Stress Test (8 minutes)
     * - Spawns N coroutines (N = core count)
     * - Each performs intensive math operations scaled to 90% of device capacity
     * - Monitors temperature and frequency every 5 seconds
     */
    suspend fun runCpuStressTest(
        onProgress: (Float) -> Unit,
        quickBenchmarkScore: Int,
        deviceAgeYears: Float
    ): CpuStressResult = withContext(Dispatchers.Default) {
        // Initialize adaptive config if not done
        if (stressConfig == null) {
            initializeAdaptiveConfig()
        }
        val config = stressConfig!!

        val iterations = calculateDynamicIterations(
            benchmarkScore = quickBenchmarkScore,
            deviceAgeYears = deviceAgeYears
        )

        Log.d("CPUStress", "Using $iterations iterations (90% load)")

        val cores = Runtime.getRuntime().availableProcessors()
        val durationSeconds = config.cpuTestDuration
        val tempReadings = mutableListOf<TempReading>()
        val freqReadings = mutableListOf<FreqReading>()

        val startTime = System.currentTimeMillis()
        var maxTemp = 0f
        var throttlingStartSec: Int? = null
        var initialFreq = 0

        // Start monitoring job
        val monitorJob = launch {
            var elapsed = 0
            while (elapsed < durationSeconds) {
                delay(5000) // Every 5 seconds
                elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()

                val temp = thermalCollector.getCpuTemperature()
                val freq = getMaxCpuFrequency()

                if (temp > maxTemp) maxTemp = temp
                if (initialFreq == 0) initialFreq = freq

                // Detect throttling (frequency drop > 20%)
                if (throttlingStartSec == null && initialFreq > 0 && freq < initialFreq * 0.8) {
                    throttlingStartSec = elapsed
                }

                tempReadings.add(TempReading(elapsed, temp))
                freqReadings.add(FreqReading(elapsed, freq))

                withContext(Dispatchers.Main) {
                    onProgress(elapsed.toFloat() / durationSeconds.toFloat())
                }
                delay(100) // Allow UI to update
            }
        }

        // Launch CPU-intensive workloads on all cores
        // Launch CPU-intensive workloads on all cores
        // Batch size is derived from iterations so load scales with device capacity
        val batchSize = (iterations / 100).coerceAtLeast(10_000)
        val jobs = List(cores) {
            launch {
                var result = 0.0
                val endTime = System.currentTimeMillis() + (durationSeconds * 1000)

                while (System.currentTimeMillis() < endTime) {
                    repeat(batchSize) { i ->
                        val x = i.toDouble()
                        result += sqrt(x) * sin(x) * cos(x) * tan(x / 100.0)
                        result += ln(abs(x + 1)) * exp(x / 1000000.0)
                    }
                    delay(10) // CRITICAL: Allow UI thread to update
                }
            }
        }

        // Wait for workload to complete
        jobs.forEach { it.join() }
        monitorJob.cancel()

        // Calculate performance metrics
        val singleCoreScore = calculateCpuScore(cores, 1)
        val multiCoreScore = calculateCpuScore(cores, cores)
        val degradation = if (initialFreq > 0) {
            ((initialFreq - (freqReadings.lastOrNull()?.freqMhz ?: initialFreq)).toFloat() / initialFreq) * 100f
        } else {
            0f
        }

        withContext(Dispatchers.Main) {
            onProgress(1.0f)
        }

        return@withContext CpuStressResult(
            durationSeconds = durationSeconds,
            maxTempCelsius = maxTemp,
            throttlingStartSec = throttlingStartSec,
            performanceDegradationPercent = degradation,
            singleCoreScore = singleCoreScore,
            multiCoreScore = multiCoreScore,
            quickBenchmarkScore = quickBenchmarkScore,
            devicePerformanceTier = deviceTier?.toString() ?: "UNKNOWN",
            cpuTestIterations = iterations.toLong(),
            tempReadings = tempReadings,
            freqReadings = freqReadings
        )
    }
    
    /**
     * Storage Stress Test (3 minutes)
     * - Writes 10 × 50MB files
     * - Measures sequential read/write speeds
     * - Tests random 4K IOPS
     */
    suspend fun runStorageStressTest(
        onProgress: (Float) -> Unit
    ): StorageStressResult = withContext(Dispatchers.IO) {
        // Initialize adaptive config if not done
        if (stressConfig == null) {
            initializeAdaptiveConfig()
        }
        val config = stressConfig!!

        val testDir = File(context.filesDir, "stress_test")
        testDir.mkdirs()
        
        val fileCount = config.storageFileCount
        val fileSizeMb = config.storageFileSizeMB
        val speedCurve = mutableListOf<SpeedPoint>()
        
        var totalWriteTime = 0L
        var totalReadTime = 0L
        var slcCacheCrash = false
        
        try {
            // Sequential Write Test
            for (i in 1..fileCount) {
                val file = File(testDir, "test_$i.dat")
                val data = ByteArray(fileSizeMb * 1024 * 1024) { Random.nextBytes(1)[0] }
                
                val writeStart = System.currentTimeMillis()
                file.writeBytes(data)
                val writeTime = System.currentTimeMillis() - writeStart
                totalWriteTime += writeTime
                
                val speed = (fileSizeMb.toFloat() / (writeTime / 1000f))
                speedCurve.add(SpeedPoint(i, speed))
                
                // Detect SLC cache crash (speed drop > 50%)
                if (i > 2 && speedCurve[i - 1].speedMbps < speedCurve[0].speedMbps * 0.5) {
                    slcCacheCrash = true
                }
                
                withContext(Dispatchers.Main) {
                    onProgress((i.toFloat() / (fileCount * 2)))
                }
                delay(100)
            }
            
            // Sequential Read Test
            for (i in 1..fileCount) {
                val file = File(testDir, "test_$i.dat")
                val readStart = System.currentTimeMillis()
                file.readBytes()
                val readTime = System.currentTimeMillis() - readStart
                totalReadTime += readTime
                
                withContext(Dispatchers.Main) {
                    onProgress((fileCount + i).toFloat() / (fileCount * 2))
                }
                delay(100)
            }
            
            val avgWriteSpeed = (fileCount * fileSizeMb).toFloat() / (totalWriteTime / 1000f)
            val avgReadSpeed = (fileCount * fileSizeMb).toFloat() / (totalReadTime / 1000f)
            
            // Fix #3: Random 4K IOPS Test
            val random4kReadIops = measureRandom4kReadIOPS()
            val random4kWriteIops = performRandom4KTest(testDir, false) // Keep legacy write test for now
            
            // Detect storage type based on speed
            val storageType = detectStorageType(avgWriteSpeed)
            
            return@withContext StorageStressResult(
                sequentialWriteMbps = avgWriteSpeed,
                sequentialReadMbps = avgReadSpeed,
                random4kReadIops = random4kReadIops,
                random4kWriteIops = random4kWriteIops,
                slcCacheCrash = slcCacheCrash,
                detectedType = storageType,
                storageTestFileCount = fileCount,
                writeSpeedCurve = speedCurve
            )
        } finally {
            // Cleanup
            testDir.listFiles()?.forEach { it.delete() }
            testDir.delete()
            onProgress(1.0f)
        }
    }
    
    /**
     * Calculate internal resistance using Ohm's Law from real measurements.
     * R = V / I where:
     * - V = voltage drop (measured)
     * - I = average current (measured from actual battery drain)
     */
    private fun calculateInternalResistance(
        voltageReadings: List<VoltageReading>,
        actualDrainMah: Float,
        durationMinutes: Float,
        deviceAgeYears: Float
    ): InternalResistanceResult {
        
        Log.d("ResistanceCalc", """
            ══════════════════════════════════════
            Physics-Based Resistance Calculation:
            ══════════════════════════════════════
            Input Data:
            - Voltage readings: ${voltageReadings.size}
            - Actual drain: $actualDrainMah mAh
            - Duration: $durationMinutes minutes
            - Device age: $deviceAgeYears years
        """.trimIndent())
        
        // Calculate average current from REAL battery drain
        val avgCurrentMa = if (durationMinutes > 0) {
            // Convert mAh over minutes to mA
            (actualDrainMah / durationMinutes) * 60f
        } else {
            0f
        }
        
        Log.d("ResistanceCalc", "Calculated average current: $avgCurrentMa mA")
        
        // Calculate baseline voltage (first 30 seconds)
        val baselineReadings = voltageReadings.take(15) // ~30 seconds at 2s intervals
        val baselineVoltage = if (baselineReadings.isNotEmpty()) {
            baselineReadings.map { it.voltageMv }.average()
        } else {
            voltageReadings.firstOrNull()?.voltageMv?.toDouble() ?: 0.0
        }
        
        // Calculate load voltage (steady state: 2-8 minutes into test)
        val loadReadings = voltageReadings.drop(60).take(180) // 2min-8min
        val loadVoltage = if (loadReadings.isNotEmpty()) {
            loadReadings.map { it.voltageMv }.average()
        } else {
            baselineVoltage
        }
        
        // Calculate voltage drop
        val voltageDrop = (baselineVoltage - loadVoltage).toFloat()
        
        Log.d("ResistanceCalc", """
            Voltage Analysis:
            - Baseline (0-30s): ${baselineVoltage.toInt()} mV
            - Under load (2-8min): ${loadVoltage.toInt()} mV
            - Voltage drop: ${voltageDrop.toInt()} mV
        """.trimIndent())
        
        // Calculate resistance using Ohm's Law: R = V / I
        val (internalResistance, method) = if (avgCurrentMa > 50 && voltageDrop > 0) {
            // Sufficient current for accurate calculation
            // R = V / I, where V in mV and I in mA gives R in Ω
            // Multiply by 1000 to get mΩ
            val resistance = (voltageDrop / avgCurrentMa) * 1000f
            val clamped = resistance.coerceIn(30f, 800f)
            
            Log.d("ResistanceCalc", """
                Ohm's Law Calculation:
                R = V / I = $voltageDrop mV / $avgCurrentMa mA = ${resistance.toInt()} mΩ
                Clamped to valid range: ${clamped.toInt()} mΩ
            """.trimIndent())
            
            Pair(clamped, "OHMS_LAW")
        } else {
            // Current too low or voltage rise instead of drop
            // Fallback to age-based estimation
            val ageBased = (50 + deviceAgeYears * 15).coerceIn(30f, 200f)
            
            Log.d("ResistanceCalc", """
                Insufficient data for Ohm's Law (current: $avgCurrentMa mA, drop: $voltageDrop mV)
                Using age-based fallback: ${ageBased.toInt()} mΩ
            """.trimIndent())
            
            Pair(ageBased, "AGE_BASED")
        }
        
        val result = InternalResistanceResult(
            baselineVoltage = baselineVoltage.toInt(),
            loadVoltage = loadVoltage.toInt(),
            voltageDrop = voltageDrop.toInt(),
            avgCurrent = avgCurrentMa.toInt(),
            internalResistance = internalResistance.toInt(),
            calculationMethod = method
        )
        
        Log.d("ResistanceCalc", """
            ══════════════════════════════════════
            Final Result:
            - Internal Resistance: ${result.internalResistance} mΩ
            - Calculation Method: ${result.calculationMethod}
            ══════════════════════════════════════
        """.trimIndent())
        
        return result
    }

    suspend fun runBatteryStressTest(
        onProgress: (Float) -> Unit,
        context: Context,
        deviceAgeYears: Float,
        designCapacityMah: Int
    ): BatteryStressResult = withContext(Dispatchers.Default) {
        if (stressConfig == null) initializeAdaptiveConfig()
        val config = stressConfig!!
        val durationSeconds = config.batteryTestDuration
        
        val baselineSamples = mutableListOf<TimeSeriesSample>()
        val loadSamples = mutableListOf<TimeSeriesSample>()
        val voltageReadings = mutableListOf<VoltageReading>()

        // ── Starting battery state ─────────────────────────────────────────────
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        val batteryLevelStart  = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val chargeCounterStart = getChargeCounterUAh()
        val batteryTempStart   = getBatteryTemperature()
        val batteryCycleCount  = getBatteryCycleCount()
        val batteryHealthSys   = getBatteryHealthStatus()
        val isChargingStart    = isCharging()

        Log.d("BatteryTest", "Start: level=$batteryLevelStart% charge=${chargeCounterStart}UAh temp=${batteryTempStart}°C cycles=$batteryCycleCount health=$batteryHealthSys charging=$isChargingStart")

        val initialVoltage = getBatteryVoltage()
        var minVoltage = initialVoltage
        var maxCurrent = 0
        var chargingDetectedMidTest = false
        val startTime = System.currentTimeMillis()
        var currentDeltaSec = 0

        // ── Phase 1: BASELINE (15 seconds) ───────────────────────────────
        for (i in 0 until 15) {
            val voltage = getBatteryVoltage()
            val current = getBatteryCurrent()
            val temp = getBatteryTemperature()
            val soc = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val pt = TimeSeriesSample(
                timestamp_ms = System.currentTimeMillis(),
                time_delta_sec = currentDeltaSec++,
                phase = "BASELINE",
                voltage_mv = voltage,
                current_ma = current,
                temp_c = temp,
                soc_percent = soc,
                cpu_load_percent = 0,
                cpu_freq_mhz = getMaxCpuFrequency()
            )
            baselineSamples.add(pt)
            // Save at 2s interval equivalent for Internal Resistance parsing
            if (i % 2 == 0) {
                voltageReadings.add(VoltageReading(currentDeltaSec - 1, voltage, current))
            }
            
            withContext(Dispatchers.Main) {
                onProgress(currentDeltaSec.toFloat() / (durationSeconds + 15).toFloat())
            }
            delay(1000)
        }

        // ── Phase 2: LOAD ─────────────────────────────────────────────
        val loadStartTime = System.currentTimeMillis()
        
        val loadJob = launch {
            val cores = Runtime.getRuntime().availableProcessors()
            val jobs = List(cores) {
                launch {
                    val endTime = loadStartTime + (durationSeconds * 1000L)
                    var result = 0.0
                    while (System.currentTimeMillis() < endTime) {
                        repeat(10_000) { i -> result += sqrt(i.toDouble()) * sin(i.toDouble()) }
                        delay(10)
                    }
                }
            }
            jobs.forEach { it.join() }
        }

        val monitorJob = launch {
            var elapsed = 0
            var nextTimeSeriesSec = 0
            var nextVoltageReadSec = 0
            
            while (elapsed < durationSeconds) {
                val voltage = getBatteryVoltage()
                val current = getBatteryCurrent()
                val temp = getBatteryTemperature()
                val soc = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                
                if (voltage < minVoltage) minVoltage = voltage
                if (current > maxCurrent) maxCurrent = current
                
                if (!isChargingStart && isCharging()) {
                    chargingDetectedMidTest = true
                }
                
                // Save time-series sample every 10 seconds
                if (elapsed >= nextTimeSeriesSec) {
                    val pt = TimeSeriesSample(
                        timestamp_ms = System.currentTimeMillis(),
                        time_delta_sec = 15 + elapsed,
                        phase = "LOAD_PULSE_START",
                        voltage_mv = voltage,
                        current_ma = current,
                        temp_c = temp,
                        soc_percent = soc,
                        cpu_load_percent = 100,
                        cpu_freq_mhz = getMaxCpuFrequency()
                    )
                    loadSamples.add(pt)
                    nextTimeSeriesSec += 10
                }
                
                // Save voltage readings every 2 seconds for legacy calculations
                if (elapsed >= nextVoltageReadSec) {
                    voltageReadings.add(VoltageReading(15 + elapsed, voltage, current))
                    nextVoltageReadSec += 2
                }
                
                withContext(Dispatchers.Main) {
                    onProgress((15 + elapsed).toFloat() / (durationSeconds + 15).toFloat())
                }
                
                delay(2000)
                elapsed = ((System.currentTimeMillis() - loadStartTime) / 1000).toInt()
            }
        }

        loadJob.join()
        monitorJob.cancel()
        
        val fullTimeSeries = baselineSamples + loadSamples
        val dataQuality = batteryCollector.analyzeDataQuality(fullTimeSeries)
        val stableFeatures = batteryCollector.extractStableFeatures(baselineSamples, loadSamples)
        
        val dropConfidence = when {
            dataQuality.confidenceScore >= 0.8f -> "HIGH"
            dataQuality.confidenceScore >= 0.6f -> "MEDIUM"
            else -> "LOW"
        }
        
        val classConfidence = if (dataQuality.pmicStalled) {
            dataQuality.confidenceScore * 0.95f
        } else {
            dataQuality.confidenceScore
        }

        // ── Ending battery state ─────────────────────────────────────────────
        val batteryLevelEnd  = batteryManager.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val chargeCounterEnd = getChargeCounterUAh()
        val batteryTempEnd   = getBatteryTemperature()
        val isChargingEnd    = isCharging()
        val endTime          = System.currentTimeMillis()
        val durationMinutes  = (endTime - startTime) / 60000.0

        val drainUAh            = if (chargeCounterStart > 0 && chargeCounterEnd > 0) chargeCounterStart - chargeCounterEnd else 0L
        val drainMAh            = drainUAh / 1000f
        
        val actualDrainPercent = if (chargeCounterStart > 0 && designCapacityMah > 0) {
            (drainMAh / designCapacityMah) * 100f
        } else {
            0f
        }
        
        val batteryDrainPercent = batteryLevelStart - batteryLevelEnd
        val drainRatePerMinute  = if (durationMinutes > 0) actualDrainPercent / durationMinutes.toFloat() else 0f

        // Calculate resistance using physics (Ohm's Law)
        val resistanceResult = calculateInternalResistance(
            voltageReadings = voltageReadings,
            actualDrainMah = drainMAh,
            durationMinutes = durationMinutes.toFloat(),
            deviceAgeYears = deviceAgeYears
        )

        val voltageSagMv = resistanceResult.voltageDrop
        val internalResistanceMohm = resistanceResult.internalResistance.toFloat()
        val peakCurrentMa = resistanceResult.avgCurrent

        // Calculate initial and load voltage for reporting
        val initialVoltageMv = resistanceResult.baselineVoltage
        val loadVoltageMv = resistanceResult.loadVoltage

        val capacityMetrics = batteryCollector.getBatteryCapacityMetrics(
            deviceAgeYears = deviceAgeYears,
            resistanceMohm = internalResistanceMohm.toInt(),
            voltageSagMv = voltageSagMv
        )

        Log.d("BatteryTest", "End: level=$batteryLevelEnd% charge=${chargeCounterEnd}UAh drain=${drainUAh}UAh=${String.format("%.1f",drainMAh)}mAh temp=${batteryTempEnd}°C sag=${voltageSagMv}mV")

        val collectedVoltageReadings = voltageReadings.map { it.voltageMv }
        val averageCurrentMa = resistanceResult.avgCurrent
        val totalTempRise = batteryTempEnd - batteryTempStart
        val cpuThrottlingPercent = 0f
        val socDropPercent = batteryDrainPercent.toFloat()

        // تحليل الجهد الهجين
        val hybridResult = HybridVoltageMonitor.analyzeVoltage(
            voltageReadings = collectedVoltageReadings,
            currentMa = averageCurrentMa,
            tempRiseCelsius = totalTempRise,
            cpuThrottlingPercent = cpuThrottlingPercent,
            socDropPercent = socDropPercent
        )

        Log.i(TAG, "Hybrid voltage analysis: source=${hybridResult.primarySource}, " +
                   "confidence=${hybridResult.confidence}, stalled=${hybridResult.isStalled}")

        val estimatedHealthPercent = if (voltageSagMv >= 10) {
            when {
                voltageSagMv < 20  -> 95 + ((20 - voltageSagMv) / 4)
                voltageSagMv < 40  -> 85 + ((40 - voltageSagMv) / 2)
                voltageSagMv < 70  -> 70 + ((70 - voltageSagMv) / 2)
                voltageSagMv < 100 -> 50 + ((100 - voltageSagMv) / 1.5).toInt()
                else             -> 30 + ((150 - voltageSagMv) / 2).toInt()
            }.coerceIn(30, 100)
        } else {
            val agePenalty    = deviceAgeYears * 3f
            val risingPenalty = if (loadVoltageMv > initialVoltage) 5f else 0f
            val cyclePenalty  = if (batteryCycleCount > 0) (batteryCycleCount / 500f) * 10f else 0f
            val health = (100f - agePenalty - risingPenalty - cyclePenalty).toInt().coerceIn(30, 100)
            Log.w("BatteryTest", "Low sag fallback: age=${deviceAgeYears}y cycles=$batteryCycleCount → health=$health%")
            health
        }
        
        val ageConfidence = 0.8f // Default static or retrieved from DeviceAgeEstimator
        val degradationPercent = capacityMetrics.degradationPercent

        // بناء metadata للتحليل المستقبلي
        val collectionMetadata = DataCollectionMetadata(
            collection_timestamp = System.currentTimeMillis(),
            app_version = "1.0.0", // Hardcoded per user request if BuildConfig is missing
            test_duration_seconds = durationSeconds,
            
            device_manufacturer = Build.MANUFACTURER,
            device_model = Build.MODEL,
            android_version = Build.VERSION.RELEASE,
            android_sdk = Build.VERSION.SDK_INT,
            
            flags = buildFlags(
                ageConfidence = ageConfidence,
                voltageStalled = hybridResult.isStalled,
                degradationPercent = degradationPercent ?: 0f
            ),
            
            raw_voltage_readings = collectedVoltageReadings,
            
            age_estimation_source = determineAgeSource(ageConfidence),
            age_estimation_confidence = ageConfidence,
            
            voltage_primary_source = hybridResult.primarySource,
            voltage_is_stalled = hybridResult.isStalled,
            voltage_variance_score = hybridResult.voltageAnalysis.varianceScore ?: 0f,
            
            has_current_readings = maxCurrent > 0,
            has_temperature_readings = batteryTempStart > -10f,
            has_resistance_readings = internalResistanceMohm > 0,
            
            notes = ""
        )

        onProgress(1.0f)

        return@withContext BatteryStressResult(
            durationSeconds          = durationSeconds,
            initialVoltageMv         = initialVoltageMv,
            loadVoltageMv            = loadVoltageMv,
            voltageSagMv             = voltageSagMv,
            peakCurrentMa            = peakCurrentMa,
            internalResistanceMohm   = internalResistanceMohm,
            calculationMethod        = resistanceResult.calculationMethod,
            estimatedHealthPercent   = estimatedHealthPercent,
            voltageReadings          = voltageReadings,
            
            voltageAnalysis          = hybridResult.voltageAnalysis,
            syntheticVoltage         = hybridResult.syntheticVoltage,
            hybridVoltageResult      = hybridResult,
            
            // NEW AI TRAINING FIELDS
            capacityMetrics          = capacityMetrics,
            timeSeriesData           = fullTimeSeries,
            dataQuality              = dataQuality,
            stableFeatures           = stableFeatures,
            dropConfidence           = dropConfidence,
            classConfidence          = classConfidence,
            
            // Legacy battery metrics
            chargeCounterStartUah    = chargeCounterStart,
            chargeCounterEndUah      = chargeCounterEnd,
            actualDrainMah           = drainMAh,
            actualDrainPercent       = actualDrainPercent,
            batteryLevelStart        = batteryLevelStart,
            batteryLevelEnd          = batteryLevelEnd,
            batteryDrainPercent      = batteryDrainPercent,
            batteryDrainRatePerMinute = drainRatePerMinute.toFloat(),
            batteryTempStartCelsius  = batteryTempStart,
            batteryTempEndCelsius    = batteryTempEnd,
            batteryCycleCount        = batteryCycleCount,
            batteryHealthStatus      = batteryHealthSys,
            isChargingStart          = isChargingStart,
            isChargingEnd            = isChargingEnd,
            chargingDetectedMidTest  = chargingDetectedMidTest,
            
            // NOTE: data_collection_metadata field is added in DiagnosticResult and DeviceScanData, not BatteryStressResult. 
            // The user requested to add it to DiagnosticResult, which is typically mapping to DeviceScanData.
            // DataCollectionRepository maps BatteryStressResult to DeviceScanData. I will make sure the metadata gets returned and mapped.
            data_collection_metadata = collectionMetadata
        )
    }
    
    // Helper Functions
    
    /**
     * بناء قائمة الأعلام للتحليل
     */
    private fun buildFlags(
        ageConfidence: Float,
        voltageStalled: Boolean,
        degradationPercent: Float
    ): List<String> {
        val flags = mutableListOf<String>()
        
        if (ageConfidence < 0.5f) flags.add("LOW_AGE_CONFIDENCE")
        if (ageConfidence < 0.7f && ageConfidence >= 0.5f) flags.add("MEDIUM_AGE_CONFIDENCE")
        if (voltageStalled) flags.add("VOLTAGE_STALLED")
        if (degradationPercent < 0) flags.add("NEGATIVE_DEGRADATION")
        if (degradationPercent > 20) flags.add("HIGH_DEGRADATION")
        
        return flags
    }

    /**
     * تحديد مصدر تقدير العمر
     */
    private fun determineAgeSource(confidence: Float): String {
        return when {
            confidence >= 0.9f -> "SERIAL_NUMBER"
            confidence >= 0.7f -> "VENDOR_PARTITION"
            confidence >= 0.5f -> "BUILD_TIME"
            else -> "UNKNOWN"
        }
    }
    
    private fun readCpuFrequency(coreId: Int): Int {
        return try {
            val file = File("/sys/devices/system/cpu/cpu$coreId/cpufreq/scaling_cur_freq")
            if (file.exists() && file.canRead()) {
                val freq = file.readText().trim().toIntOrNull() ?: 0
                freq / 1000 // Convert kHz to MHz
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    private fun getMaxCpuFrequency(): Int {
        var maxFreq = 0
        val cores = Runtime.getRuntime().availableProcessors()
        for (i in 0 until cores) {
            val freq = readCpuFrequency(i)
            if (freq > maxFreq) {
                maxFreq = freq
            }
        }
        return maxFreq
    }
    
    private fun calculateCpuScore(totalCores: Int, activeCores: Int): Int {
        // Simple scoring: cores * base frequency * architecture factor
        val baseFreq = getMaxCpuFrequency()
        return (activeCores * baseFreq * 0.8).toInt()
    }
    
    private fun performRandom4KTest(testDir: File, isRead: Boolean): Int {
        return try {
            val file = File(testDir, "random_test.dat")
            val fileSize = 10 * 1024 * 1024 // 10MB
            val blockSize = 4 * 1024 // 4KB
            val numOperations = 100
            
            if (!isRead) {
                // Write test
                val raf = RandomAccessFile(file, "rw")
                raf.setLength(fileSize.toLong())
                
                val data = ByteArray(blockSize)
                val startTime = System.currentTimeMillis()
                
                repeat(numOperations) {
                    val pos = Random.nextInt(0, fileSize - blockSize)
                    raf.seek(pos.toLong())
                    raf.write(data)
                }
                
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                raf.close()
                
                (numOperations / elapsed).toInt()
            } else {
                // Read test
                if (!file.exists()) return 0
                
                val raf = RandomAccessFile(file, "r")
                val data = ByteArray(blockSize)
                val startTime = System.currentTimeMillis()
                
                repeat(numOperations) {
                    val pos = Random.nextInt(0, fileSize - blockSize)
                    raf.seek(pos.toLong())
                    raf.read(data)
                }
                
                val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
                raf.close()
                
                (numOperations / elapsed).toInt()
            }
        } catch (e: Exception) {
            0
        }
    }
    
    private fun detectStorageType(writeSpeedMbps: Float): String {
        return when {
            writeSpeedMbps > 800 -> "UFS_3_1"
            writeSpeedMbps > 500 -> "UFS_3_0"
            writeSpeedMbps > 300 -> "UFS_2_1"
            writeSpeedMbps > 150 -> "UFS_2_0"
            writeSpeedMbps > 100 -> "eMMC_5_1"
            writeSpeedMbps > 50 -> "eMMC_5_0"
            else -> "eMMC_4_5_or_lower"
        }
    }
    
    private fun getBatteryVoltage(): Int {
        return try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    private fun getBatteryCurrent(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000) // Convert to mA
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get precise battery charge counter in microampere-hours (UAh).
     * Much more accurate than percentage for drain measurement.
     */
    private fun getChargeCounterUAh(): Long {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val counter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            Log.d(TAG, "Charge counter: $counter UAh")
            counter
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get charge counter: ${e.message}")
            -1L
        }
    }

    /**
     * Get battery charge cycle count from sysfs.
     * Returns -1 if unavailable (blocked by Knox on some Samsung devices).
     */
    private fun getBatteryCycleCount(): Int {
        return try {
            val file = java.io.File("/sys/class/power_supply/battery/cycle_count")
            if (file.exists() && file.canRead()) {
                file.readText().trim().toIntOrNull()?.also {
                    Log.d(TAG, "Battery cycle count: $it")
                } ?: -1
            } else {
                Log.w(TAG, "Cycle count file not accessible")
                -1
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get cycle count: ${e.message}")
            -1
        }
    }

    /**
     * Get battery temperature in Celsius.
     * Tries sysfs first, falls back to BatteryManager broadcast.
     */
    private fun getBatteryTemperature(): Float {
        return try {
            val sysfsFile = java.io.File("/sys/class/power_supply/battery/temp")
            if (sysfsFile.exists() && sysfsFile.canRead()) {
                val raw = sysfsFile.readText().trim().toFloatOrNull()
                if (raw != null) {
                    val celsius = raw / 10f  // sysfs reports in 0.1°C units
                    Log.d(TAG, "Battery temp (sysfs): ${celsius}°C")
                    return celsius
                }
            }
            // Fallback to BatteryManager broadcast
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val raw = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -10) ?: -10
            val celsius = raw / 10f
            Log.d(TAG, "Battery temp (broadcast): ${celsius}°C")
            celsius
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery temperature: ${e.message}")
            -1f
        }
    }

    /**
     * Get battery health status string from BatteryManager.
     */
    private fun getBatteryHealthStatus(): String {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
            when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD              -> "GOOD"
                BatteryManager.BATTERY_HEALTH_OVERHEAT          -> "OVERHEAT"
                BatteryManager.BATTERY_HEALTH_DEAD              -> "DEAD"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE      -> "OVER_VOLTAGE"
                BatteryManager.BATTERY_HEALTH_COLD              -> "COLD"
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "FAILURE"
                else                                            -> "UNKNOWN"
            }.also { Log.d(TAG, "Battery health: $it") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get battery health: ${e.message}")
            "UNKNOWN"
        }
    }

    /**
     * Check if device is currently charging.
     */
    private fun isCharging(): Boolean {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            bm.isCharging
        } catch (e: Exception) {
            false
        }
    }


    private suspend fun measureRandom4kReadIOPS(): Int = withContext(Dispatchers.IO) {
        val testFile = File(context.cacheDir, "iops_test.bin")
        
        return@withContext try {
            RandomAccessFile(testFile, "rw").use { raf ->
                raf.setLength(100 * 1024 * 1024L)
            }
            
            val iterations = 1000
            val random = java.util.Random()
            val startTime = System.nanoTime()
            
            RandomAccessFile(testFile, "r").use { raf ->
                val buffer = ByteArray(4096)
                repeat(iterations) {
                    val maxOffset = testFile.length() - 4096
                    if (maxOffset > 0) {
                        val offset = (random.nextDouble() * maxOffset).toLong()
                        raf.seek(offset)
                        raf.read(buffer)
                    }
                }
            }
            
            val durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
            (iterations / durationSeconds).toInt()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to measure random IOPS", e)
            0
        } finally {
            testFile.delete()
        }
    }
}
