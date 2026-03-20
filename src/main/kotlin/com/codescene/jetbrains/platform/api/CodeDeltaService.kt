package com.codescene.jetbrains.platform.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.jetbrains.core.delta.adaptDeltaResult
import com.codescene.jetbrains.core.delta.completeDeltaAnalysis
import com.codescene.jetbrains.core.review.CodeReviewer
import com.codescene.jetbrains.core.review.ReviewOrchestrator
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.getTelemetryInfo
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Service(Service.Level.PROJECT)
class CodeDeltaService(private val project: Project) : CodeSceneService() {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val gitService = serviceProvider.gitService
    private val deltaCacheService = serviceProvider.deltaCacheService
    private val telemetryService = serviceProvider.telemetryService
    private val uiRefreshService = project.service<UIRefreshService>()

    companion object {
        fun getInstance(project: Project): CodeDeltaService = project.service<CodeDeltaService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)
    override val codeReviewer = CodeReviewer(scope)
    override val reviewOrchestrator: ReviewOrchestrator by lazy {
        ReviewOrchestrator(
            codeReviewer = codeReviewer,
            scope = scope,
            logger = Log,
            telemetryService = serviceProvider.telemetryService,
            progressService = serviceProvider.progressService,
            onApiCallComplete = { filePath ->
                CodeSceneCodeVisionProvider.markApiCallComplete(
                    filePath,
                    CodeSceneCodeVisionProvider.activeDeltaApiCalls,
                )
            },
        )
    }

    override fun review(editor: Editor) {
        reviewFile(editor) {
            performDeltaAnalysis(editor)
        }
    }

    override fun onReviewFinished(filePath: String) {
        updateMonitor(project)
    }

    override fun onReviewScheduled(filePath: String) {
        updateMonitor(project)
    }

    /**
     * Performs delta analysis by comparing the current editor content against a baseline.
     *
     * The baseline for delta analysis is determined as follows:
     * - If available, it uses the code/content from the branch creation commit.
     * - If the branch creation commit is not found or the analysis is run in a detached HEAD state,
     *   it falls back to comparing against the best score of 10.0.
     */
    private suspend fun performDeltaAnalysis(editor: Editor) {
        val path = editor.virtualFile.path
        val oldCode = gitService.getBranchCreationCommitCode(path)
        val currentCode = editor.document.text

        val oldReview = ReviewParams(path, oldCode)
        val newReview = ReviewParams(path, currentCode)
        val (rawResult, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.delta(oldReview, newReview) }
        val delta = adaptDeltaResult(rawResult)

        val telemetryInfo = getTelemetryInfo(editor.virtualFile)
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
                serviceName = "$serviceImplementation - ${project.name}",
            )

        if (result.shouldRefreshUi) {
            uiRefreshService.refreshCodeVision(editor, listOf("CodeHealthCodeVisionProvider"))
        }
    }
}
