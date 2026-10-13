package com.codescene.jetbrains.core.cli

import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class BufferSubmission(
    val absolutePath: String,
    val relPath: String,
    val content: String,
)

data class DiskSubmission(
    val absolutePath: String,
    val relPath: String,
    val gitBlobSha: String,
)

data class PresentedReview(
    val absolutePath: String,
    val relPath: String,
    val repoRoot: String,
    val content: String?,
    val review: Review,
    val gitBlobSha: String?,
    val elapsedMs: Long,
)

data class PresentedDelta(
    val absolutePath: String,
    val relPath: String,
    val repoRoot: String,
    val scoredContent: String?,
    val delta: Delta?,
    val oldGitBlobSha: String?,
    val newGitBlobSha: String?,
    val elapsedMs: Long,
)

interface WorkspaceReviewSink {
    fun presentReview(event: PresentedReview)

    fun presentDelta(event: PresentedDelta)

    fun reviewFailed(event: ReviewFailedNotice)

    fun progress(event: ReviewProgressNotice)

    fun sessionLost()
}

class WorkspaceReviewPipeline(
    private val sender: ReviewFilesSender,
    private val sink: WorkspaceReviewSink,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val windows: Boolean = false,
    private val clock: () -> Long = System::currentTimeMillis,
    private val observe: (repoRoot: String, relPath: String) -> ObservedFile? = { _, _ -> null },
) {
    private val lock = Any()
    private val states = mutableMapOf<String, PathState>()

    fun submit(
        repoRoot: String,
        buffers: List<BufferSubmission>,
        disks: List<DiskSubmission>,
    ) {
        val batch =
            synchronized(lock) {
                val now = clock()
                val queuedBuffers = buffers.mapNotNull { queueBuffer(repoRoot, it, now) }
                queuedBuffers + disks.mapNotNull { queueDisk(repoRoot, it, now) }
            }
        if (batch.isEmpty()) return
        try {
            sender.reviewFiles(repoRoot, batch.map { it.wire() })
        } catch (e: Exception) {
            synchronized(lock) { batch.forEach { update(it) { state -> state.copy(inFlight = false) } } }
            throw e
        }
    }

    fun cancel(absolutePath: String) {
        synchronized(lock) { states.remove(pathKey(absolutePath)) }
    }

    fun sessionLost() {
        synchronized(lock) { states.replaceAll { _, state -> WorkspaceReviewMatching.afterSessionLost(state) } }
        sink.sessionLost()
    }

    fun forgetAccepted(repoRoot: String) {
        synchronized(lock) {
            states.replaceAll { _, state ->
                val sameRoot = WorkspacePaths.sameRoot(state.current.repoRoot, repoRoot, windows)
                if (sameRoot) state.copy(accepted = false, deltaAccepted = false) else state
            }
        }
    }

    fun activePaths(): Set<String> =
        synchronized(lock) { states.values.filter { it.inFlight }.map { it.current.absolutePath }.toSet() }

    fun onNotification(
        method: String,
        params: JsonNode,
    ) {
        when (method) {
            FILE_REVIEW -> WorkspaceReviewNotices.fileReview(params)?.let(::onFileReview)
            DELTA_REVIEW -> WorkspaceReviewNotices.deltaReview(params)?.let(::onDeltaReview)
            REVIEW_FAILED -> WorkspaceReviewNotices.reviewFailed(params)?.let(::onReviewFailed)
            REVIEW_PROGRESS -> WorkspaceReviewNotices.reviewProgress(params)?.let(::onProgress)
        }
    }

    private fun queueBuffer(
        repoRoot: String,
        buffer: BufferSubmission,
        now: Long,
    ): Submission? {
        val sha = GitBlobSha.ofString(buffer.content)
        if (!WorkspaceReviewMatching.shouldSend(states[pathKey(buffer.absolutePath)], sha, buffer = true)) return null
        val relPath = WorkspacePaths.normalizeRel(buffer.relPath)
        return track(SubmittedBuffer(buffer.absolutePath, repoRoot, relPath, sha, now, newId(), buffer.content))
    }

    private fun queueDisk(
        repoRoot: String,
        disk: DiskSubmission,
        now: Long,
    ): Submission? {
        val state = states[pathKey(disk.absolutePath)]
        if (!WorkspaceReviewMatching.shouldSend(state, disk.gitBlobSha, buffer = false)) return null
        val relPath = WorkspacePaths.normalizeRel(disk.relPath)
        return track(SubmittedDisk(disk.absolutePath, repoRoot, relPath, disk.gitBlobSha, now))
    }

    private fun track(submission: Submission): Submission {
        states[pathKey(submission.absolutePath)] = PathState(submission, inFlight = true, accepted = false)
        return submission
    }

    private fun update(
        submission: Submission,
        change: (PathState) -> PathState,
    ) {
        val key = pathKey(submission.absolutePath)
        val state = states[key] ?: return
        if (state.current === submission) states[key] = change(state)
    }

    private fun pathKey(absolutePath: String) = WorkspacePaths.rootKey(absolutePath, windows)

    private fun find(key: NoticeKey): PathState? =
        states.values.firstOrNull { WorkspaceReviewMatching.targets(it.current, key, windows) }

    private fun accept(
        key: NoticeKey,
        gitBlobSha: String?,
        change: (PathState) -> PathState,
    ): Submission? =
        synchronized(lock) {
            val state = find(key) ?: return null
            if (!WorkspaceReviewMatching.acceptsResult(state.current, key, gitBlobSha)) return null
            update(state.current, change)
            state.current
        }

    private fun acceptIdLess(
        key: NoticeKey,
        expectedSha: (PathState?) -> String?,
        change: (PathState) -> PathState,
    ): Submission? =
        synchronized(lock) {
            val state = find(key)
            if (state != null && state.inFlight && state.current is SubmittedBuffer) return null
            val sha = expectedSha(state) ?: return null
            val observed = observe(key.repoRoot, key.relPath)
            val target =
                if (observed == null) {
                    state ?: return null
                } else {
                    WorkspaceReviewMatching.observedState(state, observed, key.relPath, clock())
                }
            if (!WorkspaceReviewMatching.acceptsResult(target.current, key, sha)) return null
            state?.let { states.remove(pathKey(it.current.absolutePath)) }
            states[pathKey(target.current.absolutePath)] = change(target)
            target.current
        }

    private fun onFileReview(notice: FileReviewNotice) {
        val key = NoticeKey(notice.id, notice.repoRoot, notice.relPath)
        val submission =
            if (notice.id == null) {
                acceptIdLess(key, { notice.gitBlobSha }, WorkspaceReviewMatching::afterReview)
            } else {
                accept(key, notice.gitBlobSha, WorkspaceReviewMatching::afterReview)
            } ?: return
        sink.presentReview(notice.presentFor(submission, elapsed(submission)))
    }

    private fun onDeltaReview(notice: DeltaReviewNotice) {
        val key = NoticeKey(notice.id, notice.repoRoot, notice.relPath)
        val submission =
            if (notice.id == null) {
                acceptIdLess(
                    key,
                    { s ->
                        notice.newGitBlobSha
                            ?: if (notice.delta == null) WorkspaceReviewMatching.awaitingDelta(s) else null
                    },
                    WorkspaceReviewMatching::afterDelta,
                )
            } else {
                accept(key, notice.newGitBlobSha, WorkspaceReviewMatching::afterDelta)
            } ?: return
        sink.presentDelta(notice.presentFor(submission, elapsed(submission)))
    }

    private fun onReviewFailed(notice: ReviewFailedNotice) {
        val key = NoticeKey(notice.id, notice.repoRoot, notice.relPath)
        val matched =
            synchronized(lock) {
                val state = find(key)?.takeIf { WorkspaceReviewMatching.failureMatches(it, key) }
                state?.let { update(it.current) { current -> current.copy(inFlight = false) } } != null
            }
        if (matched) sink.reviewFailed(notice)
    }

    private fun onProgress(notice: ReviewProgressNotice) {
        if (notice.count == 0) {
            synchronized(lock) {
                states.replaceAll { _, state ->
                    if (WorkspaceReviewMatching.releasedByIdleProgress(state)) state.copy(inFlight = false) else state
                }
            }
        }
        sink.progress(notice)
    }

    private fun elapsed(submission: Submission): Long = (clock() - submission.submittedAt).coerceAtLeast(0)

    private companion object {
        const val FILE_REVIEW = "cs-ide/fileReview"
        const val DELTA_REVIEW = "cs-ide/deltaReview"
        const val REVIEW_FAILED = "cs-ide/reviewFailed"
        const val REVIEW_PROGRESS = "cs-ide/reviewProgress"
    }
}
