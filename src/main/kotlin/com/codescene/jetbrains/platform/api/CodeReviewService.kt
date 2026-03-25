package com.codescene.jetbrains.platform.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.review.completeReviewAnalysis
import com.codescene.jetbrains.core.telemetry.resolveTelemetryInfo
import com.codescene.jetbrains.core.util.Constants.REVIEW
import com.codescene.jetbrains.core.util.TelemetryEvents
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeReviewService(private val project: Project) : com.codescene.jetbrains.core.review.BaseService(Log) {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val reviewCacheService = serviceProvider.reviewCacheService
    private val telemetryService = serviceProvider.telemetryService

    companion object {
        fun getInstance(project: Project): CodeReviewService = project.service<CodeReviewService>()
    }

    suspend fun performCodeReview(editor: Editor): Review? {
        val file = editor.virtualFile
        val review =
            review(
                path = file.path,
                fileName = file.name,
                code = editor.document.text,
                cacheResult = true,
            )
        return review
    }

    suspend fun reviewBaseline(
        path: String,
        fileName: String,
        code: String,
    ): Double? = review(path, fileName, code, cacheResult = false)?.score?.orElse(null)

    private fun logReviewTelemetry(
        elapsedMs: Long,
        telemetryInfo: TelemetryInfo,
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
        Log.debug("Review response processed.", serviceName)
    }

    private suspend fun review(
        path: String,
        fileName: String,
        code: String,
        cacheResult: Boolean,
    ): Review? {
        val params = ReviewParams(path, code)
        val (result, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.review(params) }
        result ?: return null

        val serviceName = "${this::class.java.simpleName} - ${project.name}"
        val telemetryInfo = resolveTelemetryInfo(code.lines().size, path.substringAfterLast('.', ""))

        if (cacheResult) {
            completeReviewAnalysis(
                path = path,
                fileName = fileName,
                code = code,
                result = result,
                elapsedMs = elapsedMs,
                telemetryInfo = telemetryInfo,
                telemetryService = telemetryService,
                reviewCacheService = reviewCacheService,
                logger = Log,
                serviceName = serviceName,
            )
        } else {
            logReviewTelemetry(
                elapsedMs = elapsedMs,
                telemetryInfo = telemetryInfo,
                serviceName = serviceName,
            )
        }

        return result
    }
}
