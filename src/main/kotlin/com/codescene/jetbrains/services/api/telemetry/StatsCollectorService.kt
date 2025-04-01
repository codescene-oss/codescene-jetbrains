package com.codescene.jetbrains.services.api.telemetry

import com.codescene.jetbrains.services.api.BaseService
import com.intellij.openapi.components.Service
import com.intellij.util.PathUtil.getFileExtension

@Service
class StatsCollectorService(): BaseService() {

    val stats: Stats = Stats()

    // TODO: Uncomment this code when review stats would be needed and use this service to record review stats data
//    companion object {
//        fun getInstance(): StatsCollectorService = service<StatsCollectorService>()
//    }

    fun recordAnalysis(fileName: String, time: Double) {
        // Skip record if time is negative or zero. Must be some kind of error.
        if (time <= 0) return

        val language = getFileExtension(fileName)

        val analysis = stats.analysis.find { it.language == language }
        if (analysis != null) {
            analysis.runs++
            analysis.sum += time
            analysis.avgTime = analysis.sum / analysis.runs
            analysis.maxTime = maxOf(analysis.maxTime, time)
        } else {
            language?.let {
                stats.analysis.add(
                    AnalysisStats(
                        language = language,
                        runs = 1,
                        sum = time,
                        avgTime = time,
                        maxTime = time
                    )
                )
            }
        }
    }

    fun clear() {
        stats.analysis.clear()
    }

}

data class Stats(
    val analysis: MutableList<AnalysisStats> = mutableListOf<AnalysisStats>()
)

data class AnalysisStats(
    val language: String,
    var runs: Int,
    var sum: Double,
    var avgTime: Double,
    var maxTime: Double
)

