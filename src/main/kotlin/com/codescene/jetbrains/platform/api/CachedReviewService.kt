package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.delta.DeltaCacheQuery
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.review.BaselineReviewCacheEntry
import com.codescene.jetbrains.core.review.BaselineReviewCacheQuery
import com.codescene.jetbrains.core.review.CodeReviewer
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.review.ReviewOrchestrator
import com.codescene.jetbrains.core.review.resolveDeltaExecutionPlan
import com.codescene.jetbrains.core.review.shouldRefreshAfterReviewFlow
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.UIRefreshService
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.refreshAceFromDelta
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
    private val applicationServiceProvider = CodeSceneApplicationServiceProvider.getInstance()
    private val reviewService = project.service<CodeReviewService>()
    private val deltaService = project.service<CodeDeltaService>()
    private val uiRefreshService = project.service<UIRefreshService>()
    private val pathBasedReviewHandler by lazy { PathBasedReviewHandler.getInstance(project) }
    private val aceEntryOrchestrator by lazy { AceEntryOrchestrator.getInstance(project) }

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
            performCachedReview(editor)
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
            performCachedReview(editor)
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
            performAction = { pathBasedReviewHandler.performCachedReviewByPath(filePath, fileName) },
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

    private suspend fun performCachedReview(editor: Editor) {
        val file = editor.virtualFile
        val path = file.path
        val fileName = file.name
        val currentCode = editor.document.text

        val cachedReview = serviceProvider.reviewCacheService.get(ReviewCacheQuery(currentCode, path))
        val reviewMiss = cachedReview == null
        var review = cachedReview
        if (review == null) {
            review = reviewService.performCodeReview(editor)
        }
        if (review == null) {
            val f = pathFileName(path)
            Log.debug("cached review no result file=$f len=${currentCode.length}", "CodeSceneCachedReview")
            return
        }

        val f = pathFileName(path)
        Log.debug("cached review file=$f reviewMiss=$reviewMiss len=${currentCode.length}", "CodeSceneCachedReview")

        val aceUpdated =
            if (reviewMiss) {
                aceEntryOrchestrator.checkContainsRefactorableFunctions(editor, review)
            } else {
                false
            }
        val deltaHandled =
            handleDelta(editor, fileName, currentCode, review.score.orElse(null), reviewMiss)

        if (shouldRefreshAfterReviewFlow(reviewMiss, deltaHandled.didHandleDelta, aceUpdated)) {
            uiRefreshService.refreshUI(path, CodeSceneCodeVisionProvider.getProviders())
        }
    }

    private suspend fun handleDelta(
        editor: Editor,
        fileName: String,
        currentCode: String,
        currentScore: Double?,
        reviewMiss: Boolean,
    ): DeltaHandlingResult {
        val path = normalizeAbsolutePath(editor.virtualFile.path)
        Log.info("handleDelta editorPath=$path", "CachedReviewService")
        val baselineCode = serviceProvider.gitService.getBranchCreationCommitCode(path)
        val baselineScore = getBaselineScore(path, fileName, baselineCode)
        val plan = resolveDeltaExecutionPlan(baselineCode, currentCode, currentScore, baselineScore)

        if (plan.shouldCacheEmptyDelta) {
            serviceProvider.deltaCacheService.put(
                DeltaCacheEntry(
                    filePath = path,
                    headContent = baselineCode,
                    currentFileContent = currentCode,
                    deltaApiResponse = null,
                ),
            )
            return DeltaHandlingResult(didHandleDelta = true)
        }

        if (!plan.shouldRunDelta) {
            return DeltaHandlingResult(didHandleDelta = false)
        }

        if (!reviewMiss) {
            val query = DeltaCacheQuery(path, baselineCode, currentCode)
            val (deltaHit, _) = serviceProvider.deltaCacheService.get(query)
            if (deltaHit) {
                val df = pathFileName(path)
                Log.debug(
                    "handleDelta hit skip file=$df baseLen=${baselineCode.length}",
                    "CodeSceneCachedReview",
                )
                return DeltaHandlingResult(didHandleDelta = false)
            }
            val df = pathFileName(path)
            Log.debug(
                "handleDelta miss cachedReview file=$df baseLen=${baselineCode.length}",
                "CodeSceneCachedReview",
            )
        }

        val deltaResult = deltaService.performDeltaAnalysis(editor)
        val df2 = pathFileName(path)
        Log.debug(
            "handleDelta done file=$df2 reviewMiss=$reviewMiss " +
                "deltaNull=${deltaResult.delta == null}",
            "CodeSceneCachedReview",
        )
        val delta = deltaResult.delta
        if (delta != null && !reviewMiss) {
            refreshAceFromDelta(project, path, fileName, currentCode, delta)
        }
        return DeltaHandlingResult(didHandleDelta = true)
    }

    private suspend fun getBaselineScore(
        path: String,
        fileName: String,
        baselineCode: String,
    ): Double? {
        if (baselineCode.isBlank()) {
            return null
        }

        val query = BaselineReviewCacheQuery(fileContents = baselineCode, filePath = path)
        val (found, score) = serviceProvider.baselineReviewCacheService.get(query)
        if (found) {
            return score
        }

        val baselineScore = reviewService.reviewBaseline(path, fileName, baselineCode)
        serviceProvider.baselineReviewCacheService.put(
            BaselineReviewCacheEntry(
                fileContents = baselineCode,
                filePath = path,
                score = baselineScore,
            ),
        )
        return baselineScore
    }
}

private data class DeltaHandlingResult(
    val didHandleDelta: Boolean,
)
