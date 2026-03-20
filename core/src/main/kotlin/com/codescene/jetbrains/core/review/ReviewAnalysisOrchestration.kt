package com.codescene.jetbrains.core.review

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.util.Constants.REVIEW
import com.codescene.jetbrains.core.util.TelemetryEvents

fun completeReviewAnalysis(
    path: String,
    fileName: String,
    code: String,
    result: Review,
    elapsedMs: Long,
    telemetryInfo: TelemetryInfo,
    telemetryService: ITelemetryService,
    reviewCacheService: IReviewCacheService,
    logger: ILogger,
    serviceName: String,
) {
    telemetryService.logUsage(
        TelemetryEvents.ANALYSIS_PERFORMANCE,
        mutableMapOf(
            Pair("type", REVIEW),
            Pair("elapsedMs", elapsedMs),
            Pair("loc", telemetryInfo.loc),
            Pair("language", telemetryInfo.language),
        ),
    )
    reviewCacheService.put(ReviewCacheEntry(fileContents = code, filePath = path, response = result))
    logger.debug("Review response cached for file $fileName with path $path", serviceName)
}
