package com.codescene.jetbrains.platform.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.jetbrains.core.delta.DeltaAnalysisResult
import com.codescene.jetbrains.core.delta.adaptDeltaResult
import com.codescene.jetbrains.core.delta.completeDeltaAnalysis
import com.codescene.jetbrains.core.telemetry.resolveTelemetryInfo
import com.codescene.jetbrains.core.util.extractExtension
import com.codescene.jetbrains.core.util.resolveBaselineCliCacheFileName
import com.codescene.jetbrains.core.util.resolveCliCacheFileName
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.telemetry.StatsCollectorService
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeDeltaService(private val project: Project) : com.codescene.jetbrains.core.review.BaseService(Log) {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val gitService = serviceProvider.gitService
    private val deltaCacheService = serviceProvider.deltaCacheService
    private val telemetryService = serviceProvider.telemetryService

    companion object {
        fun getInstance(project: Project): CodeDeltaService = project.service<CodeDeltaService>()
    }

    suspend fun performDeltaAnalysis(editor: Editor): DeltaAnalysisResult {
        val path = editor.virtualFile.path
        val fileName = editor.virtualFile.name
        val extension = editor.virtualFile.extension
        val currentCode = editor.document.text
        val lineCount =
            ApplicationManager.getApplication().runReadAction<Int?> {
                FileDocumentManager.getInstance().getDocument(editor.virtualFile)?.lineCount
            }
        return performDeltaAnalysisInternal(path, fileName, extension, currentCode, lineCount)
    }

    suspend fun performDeltaAnalysisByPath(
        path: String,
        fileName: String,
        currentCode: String,
    ): DeltaAnalysisResult {
        val extension = extractExtension(fileName)
        val lineCount = currentCode.lines().size
        return performDeltaAnalysisInternal(path, fileName, extension, currentCode, lineCount)
    }

    private suspend fun performDeltaAnalysisInternal(
        path: String,
        fileName: String,
        extension: String?,
        currentCode: String,
        lineCount: Int?,
    ): DeltaAnalysisResult {
        val oldCode = gitService.getBranchCreationCommitCode(path)
        val repoRelativePath = gitService.getRepoRelativePath(path)
        val baselineReviewPath =
            resolveBaselineCliCacheFileName(
                filePath = path,
                repoRelativePath = repoRelativePath,
                commitSha = gitService.getBranchCreationCommitHash(path),
            )
        val currentReviewPath = resolveCliCacheFileName(path, repoRelativePath)

        val oldReview = ReviewParams(baselineReviewPath, oldCode)
        val newReview = ReviewParams(currentReviewPath, currentCode)
        val cacheParams = CacheParams(serviceProvider.cliCacheService.getCachePath())
        val (rawResult, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.delta(oldReview, newReview, cacheParams) }
        val delta = adaptDeltaResult(rawResult)
        StatsCollectorService.getInstance().recordAnalysis(fileName, elapsedMs.toDouble())

        val telemetryInfo = resolveTelemetryInfo(lineCount, extension)
        val result =
            completeDeltaAnalysis(
                path = path,
                oldCode = oldCode,
                currentCode = currentCode,
                delta = delta,
                telemetryInfo = telemetryInfo,
                elapsedMs = elapsedMs,
                telemetryService = telemetryService,
                deltaCacheService = deltaCacheService,
                logger = Log,
                serviceName = "${this::class.java.simpleName} - ${project.name}",
            )
        return result
    }
}
