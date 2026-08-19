package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.delta.DeltaCacheQuery
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.review.CodeReviewer
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.review.ReviewOrchestrator
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.codescene.jetbrains.platform.cli.WorkspaceReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.isFileSupported
import com.codescene.jetbrains.platform.util.isPathSupportedForReview
import com.codescene.jetbrains.platform.webview.util.updateMonitor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

@Service(Service.Level.PROJECT)
class CachedReviewService(
    private val project: Project,
) : CodeSceneService() {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)

    companion object {
        fun getInstance(project: Project): CachedReviewService = project.service<CachedReviewService>()
    }

    override val scope = CoroutineScope(Dispatchers.IO)
    override val codeReviewer = CodeReviewer(scope, Log)
    override val reviewOrchestrator: ReviewOrchestrator by lazy {
        ReviewOrchestrator(
            codeReviewer = codeReviewer,
            scope = scope,
            logger = Log,
            telemetryService = serviceProvider.telemetryService,
            progressService = serviceProvider.progressService,
        )
    }

    override fun review(editor: Editor) {
        if (!isFileSupported(project, editor.virtualFile)) {
            return
        }
        val filePath = editor.virtualFile.path
        Log.info("review(editor) entryPath=$filePath", "CachedReviewService")
        reviewFile(
            editor,
            onQueuedCallback = { replayQueuedReview(filePath) },
        ) {
            submitIfNeeded(editor)
        }
    }

    fun reviewFromCodeVision(
        editor: Editor,
        debounceDelayMs: Long?,
    ) {
        if (!isFileSupported(project, editor.virtualFile)) {
            return
        }
        val filePath = editor.virtualFile.path
        reviewFile(
            editor,
            debounceDelayMs = debounceDelayMs,
            onQueuedCallback = { replayQueuedReview(filePath) },
        ) {
            submitIfNeeded(editor)
        }
    }

    fun reviewByPath(filePath: String) {
        if (!isPathSupportedForReview(project, filePath)) {
            return
        }
        Log.info("reviewByPath entryPath=$filePath", "CachedReviewService")
        val fileName = pathFileName(filePath)
        reviewOrchestrator.reviewFile(
            filePath = filePath,
            fileName = fileName,
            serviceName = "$serviceImplementation - ${project.name}",
            isCodeReview = true,
            timeout = 300_000,
            debounceDelayMs = null,
            showProgress = false,
            performAction = { WorkspaceReviewService.getInstance(project).submitDiskPath(filePath) },
            onScheduled = null,
            onFinished = { onReviewFinished(filePath) },
            onQueuedCallback = { reviewByPath(filePath) },
        )
    }

    override fun onReviewScheduled(filePath: String) {
        Log.debug(
            "review scheduled path=$filePath activeJobs=${codeReviewer.activeFilePaths()}",
            "CodeSceneCachedReview",
        )
        updateMonitor(project)
    }

    override fun onReviewFinished(filePath: String) {
        Log.debug("review finished path=$filePath", "CodeSceneCachedReview")
        updateMonitor(project)
    }

    override fun isCodeReview(): Boolean = true

    private fun replayQueuedReview(filePath: String) {
        val fileName = pathFileName(filePath)
        val editor = findEditorForPath(filePath)
        if (editor != null && !editor.isDisposed) {
            Log.info("Replaying queued review with editor file=$fileName", "CodeSceneCachedReview")
            review(editor)
        } else {
            Log.info("Replaying queued review by path (no editor) file=$fileName", "CodeSceneCachedReview")
            reviewByPath(filePath)
        }
    }

    private fun findEditorForPath(filePath: String): Editor? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return null
        return FileEditorManager.getInstance(project).getEditors(virtualFile)
            .firstNotNullOfOrNull { (it as? TextEditor)?.editor }
    }

    private fun submitIfNeeded(editor: Editor) {
        val path = editor.virtualFile.path
        val currentCode = editor.document.text
        val cachedReview = serviceProvider.reviewCacheService.get(ReviewCacheQuery(currentCode, path))
        val baselineCode = serviceProvider.gitService.getBranchCreationCommitCode(normalizeAbsolutePath(path))
        val (deltaHit, _) =
            serviceProvider.deltaCacheService.get(DeltaCacheQuery(path, baselineCode, currentCode))
        if (cachedReview != null && deltaHit) {
            Log.debug("cached review+delta hit file=${pathFileName(path)}", "CodeSceneCachedReview")
            return
        }
        WorkspaceReviewService.getInstance(project).submitEditor(editor)
    }
}
