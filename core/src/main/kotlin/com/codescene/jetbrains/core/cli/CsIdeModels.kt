package com.codescene.jetbrains.core.cli

import com.codescene.data.delta.Delta
import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Review

data class ServerMetadata(
    val sha: String,
    val version: String,
)

data class ReviewFile(
    val relPath: String,
    val id: String? = null,
    val content: String? = null,
)

data class FileReviewEvent(
    val id: String?,
    val path: String,
    val repoRoot: String,
    val result: Review,
)

data class DeltaReviewEvent(
    val id: String?,
    val path: String,
    val repoRoot: String,
    val result: Delta?,
)

data class ReviewFailedEvent(
    val id: String?,
    val path: String,
    val repoRoot: String,
    val message: String,
)

data class AceFileParams(
    val content: String,
    val fileName: String,
)

data class CliCacheParams(
    val path: String,
)

data class ReviewRequest(
    val path: String,
    val fileContent: String?,
    val cachePath: String?,
    val repoPath: String?,
)

data class TelemetryResponse(
    val status: Int?,
)

interface CsIdeListener {
    fun onFileReview(event: FileReviewEvent)

    fun onDeltaReview(event: DeltaReviewEvent)

    fun onReviewFailed(event: ReviewFailedEvent)

    fun onError(error: Throwable)
}

data class FnsToRefactorRequest(
    val fileName: String,
    val fileContent: String,
    val cachePath: String?,
    val codeSmells: List<CodeSmell>? = null,
    val delta: Delta? = null,
    val preflight: com.codescene.data.ace.PreflightResponse? = null,
)
