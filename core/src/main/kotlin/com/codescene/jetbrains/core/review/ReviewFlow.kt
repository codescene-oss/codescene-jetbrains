package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.models.FailureType

enum class ReviewLogLevel {
    INFO,
    WARN,
    ERROR,
}

data class ReviewFailureHandling(
    val progressSuffix: String,
    val logLevel: ReviewLogLevel,
    val logMessage: String,
    val shouldLogTimeoutTelemetry: Boolean = false,
)

fun resolveProgressMessage(
    fileName: String,
    isCodeReview: Boolean,
): String =
    if (isCodeReview) {
        "CodeScene: Reviewing file $fileName..."
    } else {
        "CodeScene: Updating monitor for file $fileName..."
    }

fun resolveReviewFailureHandling(
    failureType: FailureType,
    fileName: String,
    filePath: String,
    exceptionMessage: String?,
): ReviewFailureHandling =
    when (failureType) {
        FailureType.CANCELLED ->
            ReviewFailureHandling(
                progressSuffix = failureType.value,
                logLevel = ReviewLogLevel.INFO,
                logMessage = "Review canceled for file $fileName.",
            )
        FailureType.FAILED ->
            ReviewFailureHandling(
                progressSuffix = failureType.value,
                logLevel = ReviewLogLevel.ERROR,
                logMessage = "Error during review for file $fileName - $exceptionMessage",
            )
        FailureType.TIMED_OUT ->
            ReviewFailureHandling(
                progressSuffix = failureType.value,
                logLevel = ReviewLogLevel.WARN,
                logMessage = "Review task timed out for file: $filePath",
                shouldLogTimeoutTelemetry = true,
            )
    }
