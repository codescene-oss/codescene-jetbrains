package com.codescene.jetbrains.core.cli

import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.fasterxml.jackson.databind.JsonNode

data class ReviewFileWire(
    val relPath: String,
    val id: String? = null,
    val content: String? = null,
)

fun interface ReviewFilesSender {
    fun reviewFiles(
        repoRoot: String,
        files: List<ReviewFileWire>,
    )
}

data class FileReviewNotice(
    val id: String?,
    val relPath: String,
    val repoRoot: String,
    val review: Review,
    val gitBlobSha: String?,
)

data class DeltaReviewNotice(
    val id: String?,
    val relPath: String,
    val repoRoot: String,
    val delta: Delta?,
    val oldGitBlobSha: String?,
    val newGitBlobSha: String?,
)

data class ReviewFailedNotice(
    val id: String?,
    val relPath: String,
    val repoRoot: String,
    val message: String,
)

data class ReviewProgressNotice(
    val count: Int,
    val done: Int,
    val files: List<String>,
)

fun progressTitle(progress: ReviewProgressNotice): String? =
    if (progress.count == 0) null else "CodeScene analysis (${progress.count} remaining)"

object WorkspaceReviewNotices {
    fun fileReview(params: JsonNode): FileReviewNotice? {
        val target = target(params) ?: return null
        val result = resultObject(params) ?: return null
        val sha = RpcJson.text(result, "gitBlobSha")
        return FileReviewNotice(
            id = target.id,
            relPath = target.relPath,
            repoRoot = target.repoRoot,
            review = RpcJson.read(result, Review::class.java),
            gitBlobSha = sha,
        )
    }

    fun deltaReview(params: JsonNode): DeltaReviewNotice? {
        val target = target(params) ?: return null
        val result = resultObject(params)
        val oldSha = result?.let { RpcJson.text(it, "oldGitBlobSha") }
        val newSha = result?.let { RpcJson.text(it, "newGitBlobSha") }
        return DeltaReviewNotice(
            id = target.id,
            relPath = target.relPath,
            repoRoot = target.repoRoot,
            delta = result?.let { RpcJson.read(it, Delta::class.java) },
            oldGitBlobSha = oldSha,
            newGitBlobSha = newSha,
        )
    }

    fun reviewFailed(params: JsonNode): ReviewFailedNotice? {
        val target = target(params) ?: return null
        return ReviewFailedNotice(
            id = target.id,
            relPath = target.relPath,
            repoRoot = target.repoRoot,
            message = RpcJson.text(params, "message").orEmpty(),
        )
    }

    fun reviewProgress(params: JsonNode): ReviewProgressNotice? {
        if (!params.isObject) return null
        val files = RpcJson.field(params, "files")?.takeIf { it.isArray }?.map { it.asText() }.orEmpty()
        return ReviewProgressNotice(
            count = intField(params, "count"),
            done = intField(params, "done"),
            files = files,
        )
    }

    private fun target(params: JsonNode): NoticeTarget? {
        val path = RpcJson.text(params, "path") ?: return null
        val repoRoot = RpcJson.text(params, "repoRoot") ?: return null
        return NoticeTarget(RpcJson.text(params, "id"), WorkspacePaths.normalizeRel(path), repoRoot)
    }

    private fun resultObject(params: JsonNode): JsonNode? = RpcJson.field(params, "result")?.takeIf { it.isObject }

    private fun intField(
        params: JsonNode,
        name: String,
    ): Int = RpcJson.field(params, name)?.takeIf { it.isNumber }?.asInt() ?: 0

    private data class NoticeTarget(
        val id: String?,
        val relPath: String,
        val repoRoot: String,
    )
}
