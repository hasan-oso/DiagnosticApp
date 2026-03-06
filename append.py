import sys
import codecs
path = 'app/src/main/java/com/diagnostics/app/benchmark/QuickBenchmark.kt'

with codecs.open(path, 'r', 'utf-8') as f:
    c = f.read()

idx = c.rfind('}')
if idx == -1: sys.exit(1)

code = """
    /**
     * Classify device into performance tier dynamically
     */
    fun calculatePerformanceTier(
        cpuMaxFreqMhz: Int,
        cpuCoreCount: Int,
        ramTotalMb: Int,
        gpuRenderer: String,
        androidSdk: Int
    ): String {
        val score = buildList {
            // CPU Frequency
            add(when {
                cpuMaxFreqMhz >= 3000 -> 40
                cpuMaxFreqMhz >= 2700 -> 35
                cpuMaxFreqMhz >= 2400 -> 28
                cpuMaxFreqMhz >= 2000 -> 20
                else -> 10
            })
            // RAM
            add(when {
                ramTotalMb >= 10000 -> 25
                ramTotalMb >= 8000  -> 20
                ramTotalMb >= 6000  -> 15
                ramTotalMb >= 4000  -> 10
                else -> 5
            })
            // GPU
            add(when {
                gpuRenderer.contains("Adreno 7", ignoreCase = true) -> 25
                gpuRenderer.contains("Adreno 6", ignoreCase = true) -> 20
                gpuRenderer.contains("Mali-G7", ignoreCase = true)  -> 18
                gpuRenderer.contains("Mali-G6", ignoreCase = true)  -> 15
                gpuRenderer.contains("Mali-G5", ignoreCase = true)  -> 10
                else -> 8
            })
            // Android SDK
            add(when {
                androidSdk >= 34 -> 10
                androidSdk >= 31 -> 8
                androidSdk >= 28 -> 5
                else -> 2
            })
        }.sum()

        return when {
            score >= 85 -> "FLAGSHIP"
            score >= 65 -> "HIGH_END"
            score >= 45 -> "MID_RANGE"
            score >= 25 -> "BUDGET"
            else        -> "ENTRY_LEVEL"
        }
    }

    /**
     * Calculate normalized score 0-100 based on iterations per second
     */
    fun calculateNormalizedScore(
        cpuTestIterations: Long,
        scanDurationSeconds: Int,
        cpuMaxFreqMhz: Int,
        ramTotalMb: Int
    ): Int {
        val iterationsPerSecond = if (scanDurationSeconds > 0) {
            cpuTestIterations / scanDurationSeconds.toFloat()
        } else {
            cpuTestIterations.toFloat()
        }

        // Reference: Snapdragon 8 Gen 3 ? 50,000 iter/sec
        val maxReference = 50_000f
        val cpuScore = (iterationsPerSecond / maxReference * 70f).coerceIn(0f, 70f)

        // RAM score (0-15)
        val ramScore = (ramTotalMb / 12_000f * 15f).coerceIn(0f, 15f)

        // CPU freq score (0-15)
        val freqScore = (cpuMaxFreqMhz / 3_500f * 15f).coerceIn(0f, 15f)

        return (cpuScore + ramScore + freqScore).toInt().coerceIn(0, 100)
    }
}
"""

with codecs.open(path, 'w', 'utf-8') as f:
    f.write(c[:idx] + code)
