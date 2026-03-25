package com.codescene.jetbrains.platform.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.jetbrains.core.delta.DeltaAnalysisResult
import com.codescene.jetbrains.core.delta.adaptDeltaResult
import com.codescene.jetbrains.core.delta.completeDeltaAnalysis
import com.codescene.jetbrains.core.models.TelemetryInfo
import com.codescene.jetbrains.core.telemetry.resolveTelemetryInfo
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
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

    /**
     * Performs delta analysis by comparing the current editor content against a baseline.
     *
     * The baseline for delta analysis is determined as follows:
     * - If available, it uses the code/content from the branch creation commit.
     * - If the branch creation commit is not found or the analysis is run in a detached HEAD state,
     *   it falls back to comparing against the best score of 10.0.
     */
    suspend fun performDeltaAnalysis(editor: Editor): DeltaAnalysisResult {
        val path = editor.virtualFile.path
        val oldCode = gitService.getBranchCreationCommitCode(path)
        val currentCode = editor.document.text

        val oldReview = ReviewParams(path, oldCode)
        val newReview = ReviewParams(path, currentCode)
        val (rawResult, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.delta(oldReview, newReview) }
        val delta = adaptDeltaResult(rawResult)

        val telemetryInfo =
            ApplicationManager.getApplication().runReadAction<TelemetryInfo> {
                val document = FileDocumentManager.getInstance().getDocument(editor.virtualFile)
                resolveTelemetryInfo(document?.lineCount, editor.virtualFile.extension)
            } ?: resolveTelemetryInfo(null, null)
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
