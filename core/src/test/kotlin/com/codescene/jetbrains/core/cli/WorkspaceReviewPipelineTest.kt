package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceReviewPipelineTest {
    private val sent = mutableListOf<Pair<String, List<ReviewFileWire>>>()
    private val events = mutableListOf<Any>()
    private val reviews get() = events.filterIsInstance<PresentedReview>()
    private val deltas get() = events.filterIsInstance<PresentedDelta>()
    private val failures get() = events.filterIsInstance<ReviewFailedNotice>()
    private val progress get() = events.filterIsInstance<ReviewProgressNotice>()
    private var ids = 0
    private var now = 1_000L

    private fun record(event: Any) {
        events += event
    }

    private val sink =
        object : WorkspaceReviewSink {
            override fun presentReview(event: PresentedReview) = record(event)

            override fun presentDelta(event: PresentedDelta) = record(event)

            override fun reviewFailed(event: ReviewFailedNotice) = record(event)

            override fun progress(event: ReviewProgressNotice) = record(event)

            override fun sessionLost() = record(SESSION_LOST)
        }

    private fun pipeline(
        windows: Boolean = false,
        sender: ReviewFilesSender = ReviewFilesSender { root, files -> sent += root to files },
    ) = WorkspaceReviewPipeline(sender, sink, newId = { "id${++ids}" }, windows = windows, clock = { now })

    private val app = "src/App.kt"
    private val disk = "src/Disk.kt"

    private fun buffer(content: String) = BufferSubmission("/repo/$app", app, content)

    private fun disk(sha: String) = DiskSubmission("/repo/$disk", disk, sha)

    private fun sha(content: String) = GitBlobSha.ofString(content)

    private fun params(
        id: String?,
        path: String,
        root: String,
    ): ObjectNode =
        RpcJson.mapper.createObjectNode().apply {
            id?.let { put("id", it) }
            put("path", path)
            put("repoRoot", root)
        }

    private fun fileReview(
        id: String?,
        gitBlobSha: String?,
        path: String = app,
        root: String = "/repo",
    ): JsonNode =
        params(id, path, root).apply {
            putObject("result").put("score", 9.0).put("rawScore", "raw").put("gitBlobSha", gitBlobSha)
        }

    private fun deltaReview(
        id: String?,
        newSha: String?,
        path: String = app,
        nullResult: Boolean = false,
    ): JsonNode =
        params(id, path, "/repo").apply {
            if (nullResult) {
                putNull("result")
            } else {
                putObject("result").put("scoreChange", -1.0).put("oldGitBlobSha", "old").put("newGitBlobSha", newSha)
            }
        }

    private fun reviewFailed(
        id: String?,
        path: String = app,
    ): JsonNode = params(id, path, "/repo").put("message", "boom").apply { if (id == null) putNull("id") }

    private fun reviewProgress(count: Int): JsonNode =
        RpcJson.mapper.createObjectNode().put("count", count).put("done", 0)

    private fun WorkspaceReviewPipeline.notify(
        method: String,
        params: JsonNode,
    ) = onNotification("cs-ide/$method", params)

    @Test
    fun `buffer submit sends one wire with a new id, normalized path and content`() {
        pipeline().submit("/repo", listOf(BufferSubmission("/repo/$app", "src\\App.kt", "a")), emptyList())

        assertEquals(listOf("/repo" to listOf(ReviewFileWire(app, "id1", "a"))), sent)
    }

    @Test
    fun `buffer and disk go in one call and resubmitting in-flight content does not send`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))

        assertEquals(listOf(listOf(ReviewFileWire(app, "id1", "a"), ReviewFileWire(disk))), sent.map { it.second })
    }

    @Test
    fun `accepted content is left out of a later batch`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.notify("fileReview", fileReview("id1", sha("a")))
        pipeline.notify("deltaReview", deltaReview("id1", sha("a")))
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))

        assertEquals(listOf(ReviewFileWire(disk)), sent[1].second)
        assertTrue(pipeline.activePaths() == setOf("/repo/$disk"))
    }

    @Test
    fun `fileReview with the current id and SHA presents once`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        now = 1_250L
        pipeline.notify("fileReview", fileReview("id1", sha("a")))
        pipeline.notify("fileReview", fileReview("id1", "other"))

        val event = reviews.single()
        val target = listOf(event.absolutePath, event.relPath, event.repoRoot, event.content)
        assertEquals(listOf("/repo/$app", app, "/repo", "a"), target)
        assertEquals("raw", event.review.rawScore)
        assertEquals(sha("a"), event.gitBlobSha)
        assertEquals(250L, event.elapsedMs)
    }

    @Test
    fun `fileReview with a different SHA does not present and new content sends again`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.notify("fileReview", fileReview("id1", sha("b")))
        pipeline.submit("/repo", listOf(buffer("b")), emptyList())

        assertTrue(reviews.isEmpty())
        assertEquals(ReviewFileWire(app, "id2", "b"), sent[1].second.single())
    }

    @Test
    fun `a superseded id neither presents nor clears the newer submission`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.submit("/repo", listOf(buffer("b")), emptyList())
        pipeline.notify("fileReview", fileReview("id1", sha("a")))
        pipeline.notify("deltaReview", deltaReview("id1", sha("a")))

        assertTrue(reviews.isEmpty() && deltas.isEmpty())
        assertEquals(setOf("/repo/$app"), pipeline.activePaths())
        pipeline.notify("fileReview", fileReview("id2", sha("b")))
        assertEquals("b", reviews.single().content)
    }

    @Test
    fun `deltaReview with the current id presents and a null result presents a null delta`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.notify("deltaReview", deltaReview("id1", sha("a")))
        pipeline.submit("/repo", listOf(buffer("b")), emptyList())
        pipeline.notify("deltaReview", deltaReview("id2", null, nullResult = true))

        assertEquals(listOf("a", "b"), deltas.map { it.scoredContent })
        assertEquals(-1.0, deltas[0].delta!!.scoreChange, 0.0)
        assertEquals(listOf("old", sha("a")), listOf(deltas[0].oldGitBlobSha, deltas[0].newGitBlobSha))
        assertNull(deltas[1].delta)
    }

    @Test
    fun `a buffer stays active until its delta, not its fileReview without a SHA`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.notify("fileReview", fileReview("id1", null))
        pipeline.notify("reviewProgress", reviewProgress(0))

        assertEquals(1, reviews.size)
        assertEquals(setOf("/repo/$app"), pipeline.activePaths())
        pipeline.notify("deltaReview", deltaReview("id1", sha("a")))
        assertTrue(pipeline.activePaths().isEmpty())
    }

    @Test
    fun `reviewFailed for the current id clears the path and an older id does not`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.submit("/repo", listOf(buffer("b")), emptyList())
        pipeline.notify("reviewFailed", reviewFailed("id1"))

        assertEquals(setOf("/repo/$app"), pipeline.activePaths())
        pipeline.notify("reviewFailed", reviewFailed("id2"))
        assertTrue(pipeline.activePaths().isEmpty())
        assertEquals(listOf("id2"), failures.map { it.id })
        assertTrue(reviews.isEmpty() && deltas.isEmpty())
    }

    @Test
    fun `reviewFailed without an id clears only the in-flight disk submit`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))
        pipeline.notify("reviewFailed", reviewFailed(null, app))
        pipeline.notify("reviewFailed", reviewFailed(null, disk))

        assertEquals(setOf("/repo/$app"), pipeline.activePaths())
        assertEquals(listOf(disk), failures.map { it.relPath })
    }

    @Test
    fun `id-less results are accepted only for the disk SHA`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))
        pipeline.notify("fileReview", fileReview(null, sha("a")))
        pipeline.notify("fileReview", fileReview(null, "d2", disk))
        pipeline.notify("fileReview", fileReview(null, null, disk))
        pipeline.notify("deltaReview", deltaReview(null, null, disk, nullResult = true))

        assertTrue(reviews.isEmpty() && deltas.isEmpty())
        assertEquals(setOf("/repo/$app", "/repo/$disk"), pipeline.activePaths())
        pipeline.notify("fileReview", fileReview(null, "d1", disk))
        pipeline.notify("deltaReview", deltaReview(null, "d1", disk))

        assertNull(reviews.single().content)
        assertNull(deltas.single().scoredContent)
        assertEquals(setOf("/repo/$app"), pipeline.activePaths())
    }

    @Test
    fun `progress count 0 releases disk paths so the same SHA sends again`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", emptyList(), listOf(disk("d1")))
        pipeline.notify("reviewProgress", reviewProgress(1))

        assertEquals(setOf("/repo/$disk"), pipeline.activePaths())
        pipeline.notify("reviewProgress", reviewProgress(0))
        assertTrue(pipeline.activePaths().isEmpty())
        assertEquals(listOf(1, 0), progress.map { it.count })
        pipeline.submit("/repo", emptyList(), listOf(disk("d1")))
        assertEquals(2, sent.size)
    }

    @Test
    fun `an accepted disk SHA is not sent again but a buffer with that SHA is`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", emptyList(), listOf(DiskSubmission("/repo/$app", app, sha("a"))))
        pipeline.notify("fileReview", fileReview(null, sha("a")))
        pipeline.submit("/repo", emptyList(), listOf(DiskSubmission("/repo/$app", app, sha("a"))))
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())

        assertEquals(listOf(ReviewFileWire(app, "id1", "a")), sent[1].second)
    }

    @Test
    fun `cancel drops later results for that id`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.cancel("/repo/$app")
        pipeline.notify("fileReview", fileReview("id1", sha("a")))
        pipeline.notify("deltaReview", deltaReview("id1", sha("a")))

        assertTrue(reviews.isEmpty() && deltas.isEmpty())
        assertTrue(pipeline.activePaths().isEmpty())
    }

    @Test
    fun `sessionLost resends content whose delta was not accepted and keeps accepted deltas`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))
        pipeline.notify("fileReview", fileReview("id1", sha("a")))
        pipeline.notify("deltaReview", deltaReview(null, "d1", disk))
        pipeline.sessionLost()

        assertEquals(SESSION_LOST, events.last())
        assertTrue(pipeline.activePaths().isEmpty())
        pipeline.submit("/repo", listOf(buffer("a")), listOf(disk("d1")))
        assertEquals(listOf(ReviewFileWire(app, "id2", "a")), sent[1].second)
    }

    @Test
    fun `a failed send releases the paths`() {
        val pipeline = pipeline(sender = { _, _ -> throw CsIdeUnavailableException("down") })

        val thrown = runCatching { pipeline.submit("/repo", listOf(buffer("a")), emptyList()) }.exceptionOrNull()

        assertTrue(thrown is CsIdeUnavailableException)
        assertTrue(pipeline.activePaths().isEmpty())
    }

    @Test
    fun `another repository root is dropped and on Windows the root ignores case and slashes`() {
        val pipeline = pipeline(windows = true)
        pipeline.submit("C:\\repo", listOf(BufferSubmission("C:\\repo\\src\\App.kt", app, "a")), emptyList())
        pipeline.notify("fileReview", fileReview("id1", sha("a"), root = "d:/repo"))
        pipeline.notify("fileReview", fileReview("id1", sha("a"), root = "c:/repo"))

        assertEquals("C:\\repo", reviews.single().repoRoot)
    }

    @Test
    fun `unknown methods are ignored`() {
        val pipeline = pipeline()
        pipeline.submit("/repo", listOf(buffer("a")), emptyList())
        pipeline.notify("watchInventoryChanged", fileReview("id1", sha("a")))

        assertTrue(events.isEmpty())
        assertEquals(setOf("/repo/$app"), pipeline.activePaths())
    }

    private companion object {
        const val SESSION_LOST = "sessionLost"
    }
}
