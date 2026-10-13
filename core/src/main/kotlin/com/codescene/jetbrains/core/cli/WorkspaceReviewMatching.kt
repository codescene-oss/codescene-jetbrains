package com.codescene.jetbrains.core.cli

internal sealed interface Submission {
    val absolutePath: String
    val repoRoot: String
    val relPath: String
    val gitBlobSha: String
    val submittedAt: Long

    fun wire(): ReviewFileWire
}

internal data class SubmittedBuffer(
    override val absolutePath: String,
    override val repoRoot: String,
    override val relPath: String,
    override val gitBlobSha: String,
    override val submittedAt: Long,
    val id: String,
    val content: String,
) : Submission {
    override fun wire() = ReviewFileWire(relPath, id, content)
}

internal data class SubmittedDisk(
    override val absolutePath: String,
    override val repoRoot: String,
    override val relPath: String,
    override val gitBlobSha: String,
    override val submittedAt: Long,
) : Submission {
    override fun wire() = ReviewFileWire(relPath)
}

private val Submission.text: String? get() = (this as? SubmittedBuffer)?.content

internal fun FileReviewNotice.presentFor(
    s: Submission,
    elapsedMs: Long,
) = PresentedReview(s.absolutePath, s.relPath, s.repoRoot, s.text, review, gitBlobSha, elapsedMs)

internal fun DeltaReviewNotice.presentFor(
    s: Submission,
    elapsedMs: Long,
) = PresentedDelta(s.absolutePath, s.relPath, s.repoRoot, s.text, delta, oldGitBlobSha, newGitBlobSha, elapsedMs)

internal data class PathState(
    val current: Submission,
    val inFlight: Boolean,
    val accepted: Boolean,
    val deltaAccepted: Boolean = false,
)

internal data class NoticeKey(
    val id: String?,
    val repoRoot: String,
    val relPath: String,
)

internal object WorkspaceReviewMatching {
    fun shouldSend(
        state: PathState?,
        gitBlobSha: String,
        buffer: Boolean,
    ): Boolean {
        if (state == null || state.current.gitBlobSha != gitBlobSha) return true
        if (buffer && state.current !is SubmittedBuffer) return true
        return !state.inFlight && !state.accepted
    }

    fun targets(
        submission: Submission,
        key: NoticeKey,
        windows: Boolean,
    ): Boolean =
        submission.relPath == key.relPath &&
            WorkspacePaths.sameRoot(submission.repoRoot, key.repoRoot, windows)

    fun acceptsResult(
        submission: Submission,
        key: NoticeKey,
        gitBlobSha: String?,
    ): Boolean =
        when (submission) {
            is SubmittedBuffer -> key.id == submission.id && (gitBlobSha == null || gitBlobSha == submission.gitBlobSha)
            is SubmittedDisk -> key.id == null && gitBlobSha != null && gitBlobSha == submission.gitBlobSha
        }

    fun failureMatches(
        state: PathState,
        key: NoticeKey,
    ): Boolean =
        when (val submission = state.current) {
            is SubmittedBuffer -> key.id == submission.id
            is SubmittedDisk -> key.id == null && state.inFlight
        }

    fun afterReview(state: PathState): PathState =
        state.copy(accepted = true, inFlight = state.inFlight && state.current is SubmittedBuffer)

    fun afterDelta(state: PathState): PathState = state.copy(accepted = true, deltaAccepted = true, inFlight = false)

    fun afterSessionLost(state: PathState): PathState = state.copy(inFlight = false, accepted = state.deltaAccepted)

    fun releasedByIdleProgress(state: PathState): Boolean = state.inFlight && state.current is SubmittedDisk
}
