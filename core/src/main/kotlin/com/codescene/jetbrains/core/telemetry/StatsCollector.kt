package com.codescene.jetbrains.core.telemetry

class StatsCollector {
    val stats: Stats = Stats()

    fun recordAnalysis(
        fileName: String,
        time: Double,
    ) {
        if (time <= 0) return

        val language = getFileExtension(fileName) ?: return
        val analysis = stats.analysis.find { it.language == language }

        if (analysis != null) {
            analysis.runs++
            analysis.sum += time
            analysis.avgTime = analysis.sum / analysis.runs
            analysis.maxTime = maxOf(analysis.maxTime, time)
        } else {
            stats.analysis.add(
                AnalysisStats(
                    language = language,
                    runs = 1,
                    sum = time,
                    avgTime = time,
                    maxTime = time,
                ),
            )
        }
    }

    fun clear() {
        stats.analysis.clear()
    }

    private fun getFileExtension(fileName: String): String? {
        val trimmed = fileName.trim()
        if (trimmed.isEmpty()) return null
        val slashIndex = maxOf(trimmed.lastIndexOf('/'), trimmed.lastIndexOf('\\'))
        val baseName = if (slashIndex >= 0) trimmed.substring(slashIndex + 1) else trimmed
        val dotIndex = baseName.lastIndexOf('.')
        if (dotIndex <= 0 || dotIndex == baseName.length - 1) return null
        return baseName.substring(dotIndex + 1)
    }
}

data class Stats(
    val analysis: MutableList<AnalysisStats> = mutableListOf(),
)

data class AnalysisStats(
    val language: String,
    var runs: Int,
    var sum: Double,
    var avgTime: Double,
    var maxTime: Double,
)
