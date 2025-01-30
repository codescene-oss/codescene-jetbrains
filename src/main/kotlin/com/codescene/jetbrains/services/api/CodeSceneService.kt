package com.codescene.jetbrains.services.api

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

abstract class CodeSceneService : BaseService(), Disposable {
    abstract val scope: CoroutineScope
    abstract val activeReviewCalls: MutableMap<String, Job>

    private val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)
    protected val serviceImplementation: String = this::class.java.simpleName

    abstract fun review(editor: Editor)

    /**
     * Shared logic for reviewing a file.
     * @param editor The editor instance.
     * @param timeout The action timeout, defaulted to 10s.
     * @param performAction A lambda containing subclass-specific actions.
     */
    protected fun reviewFile(
        editor: Editor,
        timeout: Long = 15_000,
        performAction: suspend () -> Unit
    ) {
        val service = "$serviceImplementation - ${editor.project!!.name}"
        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name

        activeReviewCalls[filePath]?.cancel()

        try {
            activeReviewCalls[filePath] = scope.launch {
                withTimeoutOrNull(timeout) {
                    delay(debounceDelay)

                    Log.info("Initiating review for file $fileName at path $filePath.", service)
                    performAction()

                    CodeSceneCodeVisionProvider.markApiCallComplete(
                        filePath,
                        getActiveApiCalls()
                    )
                } ?: Log.warn("Review task timed out for file: $filePath", service)
            }
        } catch (e: CancellationException) {
            Log.info("Review canceled for file $fileName.", service)
        } catch (e: Exception) {
            Log.error("Error during review for file $fileName - ${e.message}", service)
        } finally {
            activeReviewCalls.remove(filePath)
        }
    }

    protected abstract fun getActiveApiCalls(): MutableSet<String>



    fun cancelFileReview(filePath: String, calls: MutableSet<String>) {
        activeReviewCalls[filePath]?.let { job ->
            job.cancel()

            Log.info(
                "Cancelling active $CODESCENE review for file '$filePath' because it was closed.",
                serviceImplementation
            )

            activeReviewCalls.remove(filePath)

            CodeSceneCodeVisionProvider.markApiCallComplete(filePath, calls)
        } ?: Log.debug("No active $CODESCENE review found for file: $filePath", serviceImplementation)
    }

    override fun dispose() {
        activeReviewCalls.values.forEach { it.cancel() }

        activeReviewCalls.clear()

        scope.cancel()
    }
}