package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.review.cancelPendingReviews as coreCancelPendingReviews
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.intellij.openapi.vfs.VirtualFile

fun cancelPendingReviews(
    file: VirtualFile,
    codeDeltaService: CodeDeltaService,
    codeReviewService: CodeReviewService,
) {
    coreCancelPendingReviews(
        filePath = file.path,
        cancelDelta = { codeDeltaService.cancelFileReview(it) },
        cancelReview = { codeReviewService.cancelFileReview(it) },
    )
}
