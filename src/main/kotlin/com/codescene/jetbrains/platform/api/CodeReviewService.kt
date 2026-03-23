package com.codescene.jetbrains.platform.api

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.jetbrains.core.review.CodeReviewer
import com.codescene.jetbrains.core.review.ReviewOrchestrator
import com.codescene.jetbrains.core.review.completeReviewAnalysis
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.getTelemetryInfo
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Service(Service.Level.PROJECT)
class CodeReviewService(private val project: Project) : CodeSceneService() {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val reviewCacheService = serviceProvider.reviewCacheService
    private val telemetryService = serviceProvider.telemetryService
    private val uiRefreshService = project.service<UIRefreshService>()

    companion object {
        fun getInstance(project: Project): CodeReviewService = project.service<CodeReviewService>()
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
                    CodeSceneCodeVisionProvider.activeReviewApiCalls,
                )
            },
        )
    }

    override fun review(editor: Editor) {
        reviewFile(editor) {
            performCodeReview(editor)
            uiRefreshService.refreshUI(editor, CodeSceneCodeVisionProvider.getProviders())
        }
    }

    override fun isCodeReview(): Boolean = true

    private suspend fun performCodeReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val code = editor.document.text

        val params = ReviewParams(path, code)
        val (result, elapsedMs) = runWithClassLoaderChange { ExtensionAPI.review(params) }

        result ?: return

        val telemetryInfo = getTelemetryInfo(file)
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
            serviceName = "$serviceImplementation - ${project.name}",
        )

        AceEntryOrchestrator.getInstance(
            project,
        ).checkContainsRefactorableFunctions(editor, result)
    }
}
