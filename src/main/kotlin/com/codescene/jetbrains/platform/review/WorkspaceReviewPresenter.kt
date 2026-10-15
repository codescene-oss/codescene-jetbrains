package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.cli.PresentedDelta
import com.codescene.jetbrains.core.cli.PresentedReview
import com.codescene.jetbrains.core.cli.ReviewFailedNotice
import com.codescene.jetbrains.core.cli.ReviewProgressNotice
import com.codescene.jetbrains.core.cli.WorkspaceReviewSink
import com.codescene.jetbrains.core.delta.adaptDeltaResult
import com.codescene.jetbrains.core.delta.completeDeltaAnalysis
import com.codescene.jetbrains.core.delta.presentedDeltaContents
import com.codescene.jetbrains.core.review.completeReviewAnalysis
import com.codescene.jetbrains.core.telemetry.resolveTelemetryInfo
import com.codescene.jetbrains.core.util.extractExtension
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.progress.ReviewProgressIndicator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.refreshAceFromDelta
import com.codescene.jetbrains.platform.util.refreshAceFromReview
import com.intellij.openapi.project.Project
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class WorkspaceReviewPresenter(
    private val project: Project,
    private val scope: CoroutineScope,
    private val progressIndicator: ReviewProgressIndicator,
    private val onReviewProgress: (ReviewProgressNotice) -> Unit,
) : WorkspaceReviewSink {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val serviceName = "${javaClass.simpleName} - ${project.name}"

    override fun presentReview(event: PresentedReview) {
        val content = event.content
        if (content != null) {
            val fileName = File(event.absolutePath).name
            val telemetryInfo = resolveTelemetryInfo(content.lines().size, extractExtension(fileName))
            completeReviewAnalysis(
                path = event.absolutePath,
                fileName = fileName,
                code = content,
                result = event.review,
                elapsedMs = event.elapsedMs,
                telemetryInfo = telemetryInfo,
                telemetryService = serviceProvider.telemetryService,
                reviewCacheService = serviceProvider.reviewCacheService,
                logger = Log,
                serviceName = serviceName,
            )
            scope.launch {
                UIRefreshService.getInstance(project).refreshUI(
                    event.absolutePath,
                    CodeSceneCodeVisionProvider.getProviders(),
                )
            }
            scope.launch {
                refreshAceFromReview(project, event.absolutePath, fileName, content, event.review)
            }
        }
    }

    override fun presentDelta(event: PresentedDelta) {
        val content = event.scoredContent
        val fileName = File(event.absolutePath).name
        val adapted = adaptDeltaResult(event.delta)
        val (headContent, currentContent) =
            presentedDeltaContents(event.scoredContent, event.oldGitBlobSha, event.newGitBlobSha)
        val telemetryInfo = resolveTelemetryInfo(content?.lines()?.size, extractExtension(fileName))
        completeDeltaAnalysis(
            path = event.absolutePath,
            oldCode = headContent,
            currentCode = currentContent,
            delta = adapted,
            telemetryInfo = telemetryInfo,
            elapsedMs = event.elapsedMs,
            telemetryService = serviceProvider.telemetryService,
            deltaCacheService = serviceProvider.deltaCacheService,
            logger = Log,
            serviceName = serviceName,
        )
        if (content != null) {
            scope.launch {
                UIRefreshService.getInstance(project).refreshUI(
                    event.absolutePath,
                    CodeSceneCodeVisionProvider.getProviders(),
                )
            }
            if (adapted != null) {
                scope.launch {
                    refreshAceFromDelta(project, event.absolutePath, fileName, content, adapted)
                }
            }
        }
    }

    override fun reviewFailed(event: ReviewFailedNotice) {
        Log.info("Workspace review failed for ${event.relPath}: ${event.message}", serviceName)
    }

    override fun progress(event: ReviewProgressNotice) {
        progressIndicator.onProgress(event)
        onReviewProgress(event)
    }

    override fun sessionLost() {
        progressIndicator.sessionLost()
        onReviewProgress(ReviewProgressNotice(count = 0, done = 0, files = emptyList()))
    }
}
