package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.review.BaseService
import com.codescene.jetbrains.core.review.CodeReviewer
import com.codescene.jetbrains.core.review.ReviewOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

abstract class CodeSceneService :
    BaseService(Log),
    Disposable {
    abstract val scope: CoroutineScope
    abstract val codeReviewer: CodeReviewer
    protected abstract val reviewOrchestrator: ReviewOrchestrator

    val activeReviewCalls: Set<String>
        get() = reviewOrchestrator.activeFilePaths()

    protected val serviceImplementation: String = this::class.java.simpleName

    abstract fun review(editor: Editor)

    protected fun reviewFile(
        editor: Editor,
        timeout: Long = 300_000,
        debounceDelayMs: Long? = null,
        performAction: suspend () -> Unit,
    ) {
        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name
        val serviceName = "$serviceImplementation - ${editor.project!!.name}"

        reviewOrchestrator.reviewFile(
            filePath = filePath,
            fileName = fileName,
            serviceName = serviceName,
            isCodeReview = isCodeReview(),
            timeout = timeout,
            debounceDelayMs = debounceDelayMs,
            performAction = performAction,
            onScheduled = { onReviewScheduled(filePath) },
            onFinished = { onReviewFinished(filePath) },
        )
    }

    protected open fun onReviewScheduled(filePath: String) {}

    protected open fun onReviewFinished(filePath: String) {}

    protected open fun isCodeReview(): Boolean = false

    fun cancelFileReview(filePath: String) {
        reviewOrchestrator.cancel(filePath, serviceImplementation)
    }

    override fun dispose() {
        reviewOrchestrator.dispose()
        scope.cancel()
    }
}
