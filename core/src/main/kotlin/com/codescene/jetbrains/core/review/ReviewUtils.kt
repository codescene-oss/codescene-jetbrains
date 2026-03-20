package com.codescene.jetbrains.core.review

fun cancelPendingReviews(
    filePath: String,
    cancelDelta: (String) -> Unit,
    cancelReview: (String) -> Unit,
) {
    cancelDelta(filePath)
    cancelReview(filePath)
}
