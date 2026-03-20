package com.codescene.jetbrains.platform.telemetry

import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.telemetry.Stats
import com.codescene.jetbrains.core.telemetry.StatsCollector
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service

@Service
class StatsCollectorService() : BaseService(Log) {
    private val collector = StatsCollector()
    val stats: Stats = collector.stats

    // TODO: Uncomment this code when review stats would be needed and use this service to record review stats data
//    companion object {
//        fun getInstance(): StatsCollectorService = service<StatsCollectorService>()
//    }

    fun recordAnalysis(
        fileName: String,
        time: Double,
    ) = collector.recordAnalysis(fileName, time)

    fun clear() = collector.clear()
}
