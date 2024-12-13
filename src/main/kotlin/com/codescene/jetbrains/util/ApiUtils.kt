package com.codescene.jetbrains.util

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.services.api.CodeDeltaService
import com.codescene.jetbrains.services.api.CodeReviewService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

fun cancelPendingReviews(file: VirtualFile, project: Project) {
    val codeDeltaService = CodeDeltaService.getInstance(project)
    val codeReviewService = CodeReviewService.getInstance(project)

    codeDeltaService.cancelFileReview(file.path, CodeSceneCodeVisionProvider.activeDeltaApiCalls)
    codeReviewService.cancelFileReview(file.path, CodeSceneCodeVisionProvider.activeReviewApiCalls)
}