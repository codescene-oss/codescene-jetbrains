package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.Constants.DELTA
import com.codescene.jetbrains.core.util.TelemetryEvents

fun adaptDeltaResult(raw: Delta?): Delta? {
    if (raw == null) return null
    if (raw.oldScore?.isEmpty == true && raw.newScore?.isEmpty == false) {
        return Delta(
            10.0,
            raw.newScore.get(),
            raw.scoreChange,
            raw.fileLevelFindings,
            raw.functionLevelFindings,
        )
    }
    return raw
}

data class DeltaAnalysisResult(
    val delta: Delta?,
    val shouldRefreshUi: Boolean,
)

fun completeDeltaAnalysis(
    path: String,
    oldCode: String,
    currentCode: String,
    delta: Delta?,
    telemetryInfo: TelemetryInfo,
    elapsedMs: Long,
    telemetryService: ITelemetryService,
    deltaCacheService: IDeltaCacheService,
    logger: ILogger,
    serviceName: String,
): DeltaAnalysisResult {
    telemetryService.logUsage(
        TelemetryEvents.ANALYSIS_PERFORMANCE,
        mutableMapOf(
            Pair("type", DELTA),
            Pair("elapsedMs", elapsedMs),
            Pair("loc", telemetryInfo.loc),
            Pair("language", telemetryInfo.language),
        ),
    )

    if (delta == null) {
        logger.info("Received null response from $CODESCENE delta API.", serviceName)
        deltaCacheService.put(DeltaCacheEntry(path, oldCode, currentCode, null))
        return DeltaAnalysisResult(delta = null, shouldRefreshUi = false)
    }

    deltaCacheService.put(DeltaCacheEntry(path, oldCode, currentCode, delta))
    return DeltaAnalysisResult(delta = delta, shouldRefreshUi = true)
}
