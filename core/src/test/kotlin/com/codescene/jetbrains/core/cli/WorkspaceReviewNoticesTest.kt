package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WorkspaceReviewNoticesTest {
    private fun json(text: String): JsonNode = RpcJson.mapper.readTree(text)

    @Test
    fun `fileReview parses a camelCase payload`() {
        val notice =
            WorkspaceReviewNotices.fileReview(
                json(
                    """
                    {"id": "u1", "path": "src/App.kt", "repoRoot": "/repo",
                     "result": {"score": 9.5, "rawScore": "raw-b64", "fileLevelCodeSmells": [],
                                "functionLevelCodeSmells": [], "gitBlobSha": "sha1"}}
                    """.trimIndent(),
                ),
            )!!

        assertEquals("u1", notice.id)
        assertEquals("src/App.kt", notice.relPath)
        assertEquals("/repo", notice.repoRoot)
        assertEquals("sha1", notice.gitBlobSha)
        assertEquals("raw-b64", notice.review.rawScore)
        assertEquals(9.5, notice.review.score.get(), 0.0)
    }

    @Test
    fun `fileReview parses a kebab-case payload`() {
        val notice =
            WorkspaceReviewNotices.fileReview(
                json(
                    """
                    {"id": "u1", "path": "src/App.kt", "repo-root": "/repo",
                     "result": {"score": 9.5, "raw-score": "raw-b64", "file-level-code-smells": [],
                                "function-level-code-smells": [], "git-blob-sha": "sha1"}}
                    """.trimIndent(),
                ),
            )!!

        assertEquals("/repo", notice.repoRoot)
        assertEquals("sha1", notice.gitBlobSha)
        assertEquals("raw-b64", notice.review.rawScore)
    }

    @Test
    fun `fileReview without id has a null id`() {
        val notice =
            WorkspaceReviewNotices.fileReview(
                json("""{"path": "a.kt", "repoRoot": "/repo", "result": {"rawScore": "r", "gitBlobSha": "s"}}"""),
            )!!

        assertNull(notice.id)
    }

    @Test
    fun `fileReview without path is null`() {
        assertNull(WorkspaceReviewNotices.fileReview(json("""{"repoRoot": "/repo", "result": {"rawScore": "r"}}""")))
    }

    @Test
    fun `fileReview without repoRoot is null`() {
        assertNull(WorkspaceReviewNotices.fileReview(json("""{"path": "a.kt", "result": {"rawScore": "r"}}""")))
    }

    @Test
    fun `fileReview without result is null`() {
        assertNull(WorkspaceReviewNotices.fileReview(json("""{"path": "a.kt", "repoRoot": "/repo"}""")))
    }

    @Test
    fun `fileReview normalizes the relative path`() {
        val notice =
            WorkspaceReviewNotices.fileReview(
                json("""{"path": "src\\App.kt", "repoRoot": "/repo", "result": {"rawScore": "r"}}"""),
            )!!

        assertEquals("src/App.kt", notice.relPath)
    }

    @Test
    fun `deltaReview with a null result has a null delta and null SHAs`() {
        val notice =
            WorkspaceReviewNotices.deltaReview(
                json("""{"id": "u1", "path": "a.kt", "repoRoot": "/repo", "result": null}"""),
            )!!

        assertEquals("u1", notice.id)
        assertNull(notice.delta)
        assertNull(notice.oldGitBlobSha)
        assertNull(notice.newGitBlobSha)
    }

    @Test
    fun `deltaReview without result has a null delta`() {
        val notice = WorkspaceReviewNotices.deltaReview(json("""{"path": "a.kt", "repoRoot": "/repo"}"""))!!

        assertNull(notice.id)
        assertNull(notice.delta)
    }

    @Test
    fun `deltaReview with a non-object result has a null delta`() {
        val notice = WorkspaceReviewNotices.deltaReview(json("""{"path": "a.kt", "repoRoot": "/r", "result": 1}"""))!!

        assertNull(notice.delta)
        assertNull(notice.oldGitBlobSha)
    }

    @Test
    fun `deltaReview reads SHAs and maps the delta`() {
        val notice =
            WorkspaceReviewNotices.deltaReview(
                json(
                    """
                    {"path": "a.kt", "repoRoot": "/repo",
                     "result": {"oldScore": 9.0, "newScore": 8.0, "scoreChange": -1.0,
                                "fileLevelFindings": [], "functionLevelFindings": [],
                                "oldGitBlobSha": "old", "newGitBlobSha": "new"}}
                    """.trimIndent(),
                ),
            )!!

        assertEquals("old", notice.oldGitBlobSha)
        assertEquals("new", notice.newGitBlobSha)
        assertEquals(-1.0, notice.delta!!.scoreChange, 0.0)
        assertEquals(8.0, notice.delta!!.newScore.get(), 0.0)
    }

    @Test
    fun `deltaReview reads kebab-case SHAs`() {
        val notice =
            WorkspaceReviewNotices.deltaReview(
                json(
                    """
                    {"path": "a.kt", "repo-root": "/repo",
                     "result": {"score-change": 0.0, "old-git-blob-sha": "old", "new-git-blob-sha": "new"}}
                    """.trimIndent(),
                ),
            )!!

        assertEquals("/repo", notice.repoRoot)
        assertEquals("old", notice.oldGitBlobSha)
        assertEquals("new", notice.newGitBlobSha)
    }

    @Test
    fun `deltaReview without path is null`() {
        assertNull(WorkspaceReviewNotices.deltaReview(json("""{"repoRoot": "/repo", "result": null}""")))
    }

    @Test
    fun `reviewFailed with a null id keeps the message`() {
        val notice =
            WorkspaceReviewNotices.reviewFailed(
                json("""{"id": null, "path": "a.kt", "repoRoot": "/repo", "message": "boom"}"""),
            )!!

        assertNull(notice.id)
        assertEquals("a.kt", notice.relPath)
        assertEquals("/repo", notice.repoRoot)
        assertEquals("boom", notice.message)
    }

    @Test
    fun `reviewFailed without repoRoot is null`() {
        assertNull(WorkspaceReviewNotices.reviewFailed(json("""{"id": "u1", "path": "a.kt", "message": "boom"}""")))
    }

    @Test
    fun `reviewFailed without a message has an empty message`() {
        val notice = WorkspaceReviewNotices.reviewFailed(json("""{"id": "u1", "path": "a.kt", "repo-root": "/r"}"""))

        assertEquals("", notice!!.message)
    }

    @Test
    fun `reviewProgress parses count, done and files`() {
        val notice = WorkspaceReviewNotices.reviewProgress(json("""{"count": 2, "done": 1, "files": ["a.kt"]}"""))

        assertEquals(ReviewProgressNotice(count = 2, done = 1, files = listOf("a.kt")), notice)
    }

    @Test
    fun `reviewProgress tolerates malformed fields`() {
        val notice = WorkspaceReviewNotices.reviewProgress(json("""{"count": "x", "files": "a.kt"}"""))

        assertEquals(ReviewProgressNotice(count = 0, done = 0, files = emptyList()), notice)
    }

    @Test
    fun `reviewProgress that is not an object is null`() {
        assertNull(WorkspaceReviewNotices.reviewProgress(json("""[1, 2]""")))
        assertNull(WorkspaceReviewNotices.reviewProgress(json("null")))
    }

    @Test
    fun `progressTitle is null when no work remains`() {
        assertNull(progressTitle(ReviewProgressNotice(count = 0, done = 3, files = emptyList())))
    }

    @Test
    fun `progressTitle shows the remaining count`() {
        val title = progressTitle(ReviewProgressNotice(count = 2, done = 1, files = listOf("a.kt", "b.kt")))

        assertNotNull(title)
        assertEquals("CodeScene analysis (2 remaining)", title)
    }

    @Test
    fun `ReviewFileWire defaults id and content to null`() {
        val wire = ReviewFileWire(relPath = "a.kt")

        assertNull(wire.id)
        assertNull(wire.content)
    }
}
