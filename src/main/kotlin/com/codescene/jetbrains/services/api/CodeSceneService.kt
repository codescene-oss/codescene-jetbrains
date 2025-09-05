package com.codescene.jetbrains.services.api

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.TelemetryEvents
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.platform.ide.progress.withBackgroundProgress
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

abstract class CodeSceneService : BaseService(), Disposable {
    abstract val scope: CoroutineScope
    abstract val activeReviewCalls: MutableMap<String, Job>

    private val debounceDelay: Long = TimeUnit.SECONDS.toMillis(3)
    private val failureIndicatorDelay = TimeUnit.SECONDS.toMillis(2)
    protected val serviceImplementation: String = this::class.java.simpleName

    abstract fun review(editor: Editor)

    /**
     * Shared logic for reviewing a file.
     * @param editor The editor instance.
     * @param timeout The action timeout, defaulted to 60s.
     * @param performAction A lambda containing subclass-specific actions.
     */
    protected fun reviewFile(
        editor: Editor,
        timeout: Long = 60_000,
        performAction: suspend () -> Unit
    ) {
        val service = getServiceForLogging(editor)
        val filePath = editor.virtualFile.path
        val fileName = editor.virtualFile.name

        activeReviewCalls[filePath]?.cancel()

        val progressMessage = getProgressMessage(fileName)
        activeReviewCalls[filePath] = scope.launch {
            withTimeout(timeout) {
                withBackgroundProgress(editor.project!!, progressMessage, cancellable = false) {
                    try {
                        delay(debounceDelay)

                        Log.info("Initiating review for file $fileName at path $filePath.", service)
                        performAction()

                        CodeSceneCodeVisionProvider.markApiCallComplete(
                            filePath,
                            getActiveApiCalls()
                        )
                    } catch (e: TimeoutCancellationException) {
                        handleError(editor, FailureType.TIMED_OUT, e.message)
                    } catch (e: CancellationException) {
                        // because of Intellij's bug https://youtrack.jetbrains.com/issue/IJPL-5335/Non-cancellable-progress-indicator-can-be-cancelled
                        // even if we have cancellable false, we should handle cancellation
                        handleError(editor, FailureType.CANCELLED, e.message)
                    } catch (e: Exception) {
                        handleError(editor, FailureType.FAILED, e.message)
                    } finally {
                        activeReviewCalls.remove(filePath)
                    }
                }
            }
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

    private fun getServiceForLogging(editor: Editor): String {
        return "$serviceImplementation - ${editor.project!!.name}"
    }

    private fun handleError(editor: Editor, failureType: FailureType, exceptionMessage: String?) {
        val newProgressMessage = getProgressMessage(editor.virtualFile.name) + failureType.value
        val service = getServiceForLogging(editor)
        scope.launch {
            withBackgroundProgress(editor.project!!, newProgressMessage, cancellable = false) {
                delay(failureIndicatorDelay)
            }
        }
        when (failureType) {
            FailureType.CANCELLED -> Log.info("Review canceled for file ${editor.virtualFile.name}.", service)
            FailureType.FAILED -> Log.error("Error during review for file ${editor.virtualFile.name} - $exceptionMessage", service)
            FailureType.TIMED_OUT -> logTimeout(editor)
        }
    }

    private fun logTimeout(editor: Editor) {
        Log.warn("Review task timed out for file: ${editor.virtualFile.path}", getServiceForLogging(editor))
        TelemetryService.getInstance().logUsage(TelemetryEvents.REVIEW_OR_DELTA_TIMEOUT)
    }

    private fun getProgressMessage(fileName: String): String {
        return when (this) {
            is CodeReviewService -> "CodeScene: Reviewing file $fileName..."
            else -> "CodeScene: Updating monitor for file $fileName..."
        }
    }
}

enum class FailureType(val value: String) {
    CANCELLED("Cancelled"),
    FAILED("Failed"),
    TIMED_OUT("Timed out")
}