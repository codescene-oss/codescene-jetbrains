package com.codescene.jetbrains.core.util

enum class CodeVisionAction { NOT_READY, READY_EMPTY, READY }

data class CodeVisionDecisionInput(
    val codeVisionEnabled: Boolean,
    val monitorEnabled: Boolean,
    val hasCachedReview: Boolean,
    val hasCachedDelta: Boolean,
)

data class CodeVisionDecision(
    val action: CodeVisionAction,
    val needsReviewApiCall: Boolean,
    val needsDeltaApiCall: Boolean,
)

fun resolveCodeVisionDecision(input: CodeVisionDecisionInput): CodeVisionDecision {
    val needsDelta = !input.hasCachedDelta && input.monitorEnabled
    val needsReview = !input.hasCachedReview

    val action =
        when {
            needsReview -> CodeVisionAction.NOT_READY
            !input.codeVisionEnabled -> CodeVisionAction.READY_EMPTY
            else -> CodeVisionAction.READY
        }

    return CodeVisionDecision(
        action = action,
        needsReviewApiCall = needsReview,
        needsDeltaApiCall = needsDelta,
    )
}
