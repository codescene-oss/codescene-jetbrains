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
    val documentText: String? = null,
) : Submission {
    override fun wire() = ReviewFileWire(relPath)
}

private val Submission.text: String?
    get() =
        when (this) {
            is SubmittedBuffer -> content
            is SubmittedDisk -> documentText
        }

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
    ): Boolean {
        if (state == null || state.current.gitBlobSha != gitBlobSha) return true
        if (state.current !is SubmittedBuffer) return true
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
    ): Boolean = state.current.let { it is SubmittedBuffer && it.id == key.id }

    fun afterReview(state: PathState): PathState = state.copy(accepted = true)

    fun afterDelta(state: PathState): PathState = state.copy(accepted = true, deltaAccepted = true, inFlight = false)

    fun afterSessionLost(state: PathState): PathState = state.copy(inFlight = false, accepted = state.deltaAccepted)

    fun awaitingDelta(state: PathState?): String? {
        if (state == null) return null
        if (state.current !is SubmittedDisk) return null
        if (!state.accepted || state.deltaAccepted) return null
        return state.current.gitBlobSha
    }

    fun observedState(
        previous: PathState?,
        observed: ObservedFile,
        relPath: String,
        now: Long,
    ): PathState {
        val normalized = WorkspacePaths.normalizeRel(relPath)
        val same = previous?.takeIf { it.current.gitBlobSha == observed.current.gitBlobSha }
        val submittedAt = same?.current?.submittedAt ?: now
        val submission =
            SubmittedDisk(
                observed.absolutePath,
                observed.repoRoot,
                normalized,
                observed.current.gitBlobSha,
                submittedAt,
                documentText = observed.current.text,
            )
        return same?.copy(current = submission) ?: PathState(submission, inFlight = false, accepted = false)
    }
}
