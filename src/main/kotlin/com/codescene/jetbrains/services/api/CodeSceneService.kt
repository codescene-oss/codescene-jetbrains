package com.codescene.jetbrains.services.api

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.services.BaseService
import com.codescene.jetbrains.services.telemetry.TelemetryService
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
     * @param timeout The action timeout, defaulted to 15s.
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
                        handleTimeout(service, progressMessage, editor)
                    } catch (e: CancellationException) {
                        handleCancellation(service, progressMessage, editor)
                    } catch (e: Exception) {
                        handleFailure(e.message, service, progressMessage, editor)
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

    private fun handleTimeout(service: String, progressMessage: String, editor: Editor) {
        val newProgressMessage = progressMessage + "Timed out"
        scope.launch {
            withBackgroundProgress(editor.project!!, newProgressMessage, cancellable = false) {
                delay(failureIndicatorDelay)
            }
        }
        Log.warn("Review task timed out for file: ${editor.virtualFile.path}", service)
        TelemetryService.getInstance().logUsage(TelemetryEvents.REVIEW_OR_DELTA_TIMEOUT)
    }

    private fun handleCancellation(service: String, progressMessage: String, editor: Editor) {
        val newProgressMessage = progressMessage + "Cancelled"
        scope.launch {
            withBackgroundProgress(editor.project!!, newProgressMessage, cancellable = false) {
                delay(failureIndicatorDelay)
            }
        }
        Log.info("Review canceled for file ${editor.virtualFile.name}.", service)
    }

    private fun handleFailure(exceptionMessage: String?, service: String, progressMessage: String, editor: Editor) {
        val newProgressMessage = progressMessage + "Failed"
        scope.launch {
            withBackgroundProgress(editor.project!!, newProgressMessage, cancellable = false) {
                delay(failureIndicatorDelay)
            }
        }
        Log.error("Error during review for file ${editor.virtualFile.name} - $exceptionMessage", service)
    }

    private fun getProgressMessage(fileName: String): String {
        return if (this is CodeReviewService) {
            return "CodeScene: Reviewing file $fileName..."
        } else {
            return "CodeScene: Updating monitor for file $fileName..."
        }
    }
}