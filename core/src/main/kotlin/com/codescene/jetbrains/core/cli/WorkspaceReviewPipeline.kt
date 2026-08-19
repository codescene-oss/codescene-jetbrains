package com.codescene.jetbrains.core.cli

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class WorkspaceReviewPipeline(
    private val client: CsIdeClient,
    private val contentSha: (repoRoot: String, relPath: String) -> String?,
) : CsIdeListener {
    private val pending = ConcurrentHashMap<String, PendingReview>()
    private val listeners = CopyOnWriteArrayList<CsIdeListener>()

    fun addListener(listener: CsIdeListener) {
        listeners.add(listener)
    }

    fun submit(
        repoRoot: String,
        files: List<ReviewFile>,
        baselineRevision: String? = null,
    ) {
        files.forEach { file ->
            val id = file.id
            if (id != null) {
                pending[id] =
                    PendingReview(
                        id = id,
                        repoRoot = repoRoot,
                        relPath = toPosixRelPath(file.relPath),
                        gitBlobSha = file.content?.let(GitBlobSha::ofUtf8),
                    )
            }
        }
        client.reviewFiles(repoRoot, files, baselineRevision)
    }

    fun submitBuffer(
        repoRoot: String,
        relPath: String,
        content: String,
        baselineRevision: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        submit(
            repoRoot = repoRoot,
            files = listOf(ReviewFile(relPath = relPath, id = id, content = content)),
            baselineRevision = baselineRevision,
        )
        return id
    }

    override fun onFileReview(event: FileReviewEvent) {
        if (!accept(event.id, event.repoRoot, event.path, event.result.gitBlobSha)) return
        listeners.forEach { it.onFileReview(event) }
        completeIfDone(event.id, reviewDone = true)
    }

    override fun onDeltaReview(event: DeltaReviewEvent) {
        val sha = event.result?.newGitBlobSha
        if (!accept(event.id, event.repoRoot, event.path, sha)) return
        listeners.forEach { it.onDeltaReview(event) }
        completeIfDone(event.id, deltaDone = true)
    }

    override fun onReviewFailed(event: ReviewFailedEvent) {
        if (event.id != null && pending[event.id] == null) return
        listeners.forEach { it.onReviewFailed(event) }
        event.id?.let { pending.remove(it) }
    }

    override fun onError(error: Throwable) {
        listeners.forEach { it.onError(error) }
    }

    private fun accept(
        id: String?,
        repoRoot: String,
        relPath: String,
        resultSha: String?,
    ): Boolean {
        val posixPath = toPosixRelPath(relPath)
        if (id != null) {
            val waiting = pending[id] ?: return false
            if (waiting.repoRoot != repoRoot || waiting.relPath != posixPath) return false
            if (waiting.gitBlobSha != null && resultSha != null && waiting.gitBlobSha != resultSha) return false
        }
        val currentSha = contentSha(repoRoot, posixPath)
        if (resultSha != null && currentSha != null && resultSha != currentSha) return false
        return true
    }

    private fun completeIfDone(
        id: String?,
        reviewDone: Boolean = false,
        deltaDone: Boolean = false,
    ) {
        if (id == null) return
        pending.computeIfPresent(id) { _, value ->
            val updated =
                value.copy(
                    reviewDone = value.reviewDone || reviewDone,
                    deltaDone = value.deltaDone || deltaDone,
                )
            if (updated.reviewDone && updated.deltaDone) null else updated
        }
    }

    private data class PendingReview(
        val id: String,
        val repoRoot: String,
        val relPath: String,
        val gitBlobSha: String?,
        val reviewDone: Boolean = false,
        val deltaDone: Boolean = false,
    )
}
