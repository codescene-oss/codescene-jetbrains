package com.codescene.jetbrains.core.review

data class DeltaExecutionPlan(
    val shouldRunDelta: Boolean,
    val shouldCacheEmptyDelta: Boolean,
)

fun resolveDeltaExecutionPlan(
    baselineCode: String,
    currentScore: Double?,
    baselineScore: Double?,
): DeltaExecutionPlan =
    when {
        baselineCode.isBlank() -> DeltaExecutionPlan(shouldRunDelta = true, shouldCacheEmptyDelta = false)
        currentScore == null || baselineScore == null ->
            DeltaExecutionPlan(shouldRunDelta = true, shouldCacheEmptyDelta = false)

        currentScore == baselineScore ->
            DeltaExecutionPlan(shouldRunDelta = false, shouldCacheEmptyDelta = true)

        else -> DeltaExecutionPlan(shouldRunDelta = true, shouldCacheEmptyDelta = false)
    }

fun shouldRefreshAfterReviewFlow(
    reviewMiss: Boolean,
    deltaHandled: Boolean,
    aceUpdated: Boolean,
): Boolean = reviewMiss || deltaHandled || aceUpdated
