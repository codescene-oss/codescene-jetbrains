package com.codescene.jetbrains.platform.telemetry

import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.telemetry.Stats
import com.codescene.jetbrains.core.telemetry.StatsCollector
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class StatsCollectorService :
    BaseService(Log),
    Disposable {
    private val collector = StatsCollector()
    val stats: Stats = collector.stats
    private val scheduler =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "codescene-stats-telemetry").apply { isDaemon = true }
        }

    init {
        scheduler.scheduleAtFixedRate({ flushStats() }, 30, 30, TimeUnit.MINUTES)
    }

    companion object {
        fun getInstance(): StatsCollectorService = service()
    }

    fun recordAnalysis(
        fileName: String,
        time: Double,
    ) = collector.recordAnalysis(fileName, time)

    private fun flushStats() {
        val snapshot = stats.analysis.toList()
        if (snapshot.isEmpty()) return
        val telemetry = TelemetryService.getInstance()
        snapshot.forEach { analysis ->
            val analysisMap =
                mapOf(
                    "language" to analysis.language,
                    "runs" to analysis.runs,
                    "avgTime" to analysis.avgTime,
                    "maxTime" to analysis.maxTime,
                )
            telemetry.logUsage(
                TelemetryEvents.STATS,
                mapOf("stats" to mapOf("analysis" to analysisMap)),
            )
        }
        collector.clear()
    }

    override fun dispose() {
        scheduler.shutdown()
    }
}
