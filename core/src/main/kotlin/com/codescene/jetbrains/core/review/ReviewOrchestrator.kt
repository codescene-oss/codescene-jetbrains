package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.IProgressService
import com.codescene.jetbrains.core.contracts.ITelemetryService
import com.codescene.jetbrains.core.models.FailureType
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.core.util.TelemetryEvents
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReviewOrchestrator(
    private val codeReviewer: CodeReviewer,
    private val scope: CoroutineScope,
    private val logger: ILogger,
    private val telemetryService: ITelemetryService,
    private val progressService: IProgressService,
    private val onApiCallComplete: (filePath: String) -> Unit = {},
) {
    private val failureIndicatorDelay = TimeUnit.SECONDS.toMillis(2)

    fun reviewFile(
        filePath: String,
        fileName: String,
        serviceName: String,
        isCodeReview: Boolean,
        timeout: Long = 60_000,
        debounceDelayMs: Long? = null,
        showProgress: Boolean = true,
        performAction: suspend () -> Unit,
        onScheduled: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null,
    ) {
        val progressMessage = resolveProgressMessage(fileName, isCodeReview)
        codeReviewer.reviewFile(
            filePath = filePath,
            timeout = timeout,
            debounceDelayMs = debounceDelayMs,
            runWithProgress = { action ->
                if (showProgress) {
                    progressService.runWithProgress(progressMessage) { action() }
                } else {
                    action()
                }
            },
            performAction = {
                logger.info("Initiating review for file $fileName at path $filePath.", serviceName)
                performAction()
            },
            onError = { failureType, exceptionMessage ->
                handleError(
                    filePath = filePath,
                    fileName = fileName,
                    serviceName = serviceName,
                    isCodeReview = isCodeReview,
                    failureType = failureType,
                    exceptionMessage = exceptionMessage,
                )
            },
            onScheduled = onScheduled,
            onFinished = {
                onApiCallComplete(filePath)
                onFinished?.invoke()
            },
        )
    }

    fun cancel(
        filePath: String,
        serviceName: String,
    ): Boolean {
        if (codeReviewer.cancel(filePath)) {
            logger.info(
                "Cancelling active $CODESCENE review for file '$filePath' because it was closed.",
                serviceName,
            )
            onApiCallComplete(filePath)
            return true
        }
        logger.debug("No active $CODESCENE review found for file: $filePath", serviceName)
        return false
    }

    fun activeFilePaths(): Set<String> = codeReviewer.activeFilePaths()

    fun dispose() {
        codeReviewer.dispose()
    }

    private fun handleError(
        filePath: String,
        fileName: String,
        serviceName: String,
        isCodeReview: Boolean,
        failureType: FailureType,
        exceptionMessage: String?,
    ) {
        val failure =
            resolveReviewFailureHandling(
                failureType = failureType,
                fileName = fileName,
                filePath = filePath,
                exceptionMessage = exceptionMessage,
            )
        val newProgressMessage = resolveProgressMessage(fileName, isCodeReview) + failure.progressSuffix
        scope.launch {
            progressService.runWithProgress(newProgressMessage) {
                delay(failureIndicatorDelay)
            }
        }
        when (failure.logLevel) {
            ReviewLogLevel.INFO -> logger.info(failure.logMessage, serviceName)
            ReviewLogLevel.WARN -> logger.warn(failure.logMessage, serviceName)
            ReviewLogLevel.ERROR -> logger.error(failure.logMessage, serviceName)
        }
        if (failure.shouldLogTimeoutTelemetry) {
            telemetryService.logUsage(TelemetryEvents.REVIEW_OR_DELTA_TIMEOUT)
        }
    }
}
