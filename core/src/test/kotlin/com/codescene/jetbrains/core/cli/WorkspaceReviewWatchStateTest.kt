package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkspaceReviewWatchStateTest {
    private val fixture = WorkspaceReviewPipelineFixture()
    private var current = CurrentFile("s1", null)

    @Before
    fun reset() {
        fixture.sent.clear()
        fixture.events.clear()
        current = CurrentFile("s1", null)
    }

    private fun observe(
        repoRoot: String,
        relPath: String,
    ): ObservedFile? =
        if (repoRoot == "/repo" && relPath == fixture.app) {
            ObservedFile(fixture.appPath, repoRoot, current)
        } else {
            null
        }

    @Test
    fun `a null id-less delta pairs with the accepted review`() {
        current = CurrentFile("s1", null)
        val pipeline = fixture.pipeline(observe = ::observe)

        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, null, nullResult = true))
        assertTrue(fixture.deltas.isEmpty())

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "s1"))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, null, nullResult = true))

        val delta = fixture.deltas.single()
        assertNull(delta.delta)
        assertNull(delta.oldGitBlobSha)
        assertNull(delta.newGitBlobSha)

        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, null, nullResult = true))
        assertEquals(1, fixture.deltas.size)
    }

    @Test
    fun `a null id-less delta is dropped when the bytes changed after the review`() {
        current = CurrentFile("s1", null)
        val pipeline = fixture.pipeline(observe = ::observe)
        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "s1"))

        current = CurrentFile("s2", null)
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, null, nullResult = true))

        assertTrue(fixture.deltas.isEmpty())
    }

    @Test
    fun `an accepted watch result dedups the same disk SHA but not a buffer`() {
        current = CurrentFile(fixture.sha("a"), null)
        val pipeline = fixture.pipeline(observe = ::observe)
        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, fixture.sha("a")))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, fixture.sha("a")))

        pipeline.submit("/repo", emptyList(), listOf(DiskSubmission(fixture.appPath, fixture.app, fixture.sha("a"))))
        assertTrue(fixture.sent.isEmpty())

        pipeline.submit("/repo", listOf(fixture.buffer("a")), emptyList())
        assertEquals(listOf(ReviewFileWire(fixture.app, "id1", "a")), fixture.sent.single().second)
    }

    @Test
    fun `forgetAccepted resends accepted content for that repository only`() {
        val otherApp = "other/App.kt"
        val pipeline = fixture.pipeline()
        pipeline.submit("/repo", listOf(fixture.buffer("a")), emptyList())
        pipeline.submit("/other", listOf(BufferSubmission("/other/$otherApp", otherApp, "b")), emptyList())
        fixture.notify(pipeline, "fileReview", fixture.fileReview("id1", fixture.sha("a")))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview("id1", fixture.sha("a")))
        fixture.notify(
            pipeline,
            "fileReview",
            fixture.fileReview("id2", fixture.sha("b"), path = otherApp, root = "/other"),
        )
        fixture.notify(
            pipeline,
            "deltaReview",
            fixture.deltaReview("id2", fixture.sha("b"), path = otherApp, root = "/other"),
        )
        assertEquals(2, fixture.deltas.size)

        fixture.sent.clear()
        pipeline.forgetAccepted("/repo")
        pipeline.submit("/repo", listOf(fixture.buffer("a")), emptyList())
        pipeline.submit("/other", listOf(BufferSubmission("/other/$otherApp", otherApp, "b")), emptyList())

        assertEquals(listOf(ReviewFileWire(fixture.app, "id3", "a")), fixture.sent.single().second)
    }

    @Test
    fun `forgetAccepted matches the root ignoring case and slashes on Windows`() {
        val pipeline = fixture.pipeline(windows = true)
        pipeline.submit("C:\\repo", listOf(BufferSubmission("C:\\repo\\src\\App.kt", fixture.app, "a")), emptyList())
        fixture.notify(pipeline, "fileReview", fixture.fileReview("id1", fixture.sha("a"), root = "C:\\repo"))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview("id1", fixture.sha("a"), root = "C:\\repo"))

        fixture.sent.clear()
        pipeline.forgetAccepted("c:/repo/")
        pipeline.submit("C:\\repo", listOf(BufferSubmission("C:\\repo\\src\\App.kt", fixture.app, "a")), emptyList())

        assertEquals(1, fixture.sent.size)
    }

    @Test
    fun `sessionLost keeps an observed file whose delta was accepted`() {
        current = CurrentFile(fixture.sha("a"), null)
        val pipeline = fixture.pipeline(observe = ::observe)
        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, fixture.sha("a")))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, fixture.sha("a")))
        pipeline.sessionLost()

        fixture.sent.clear()
        pipeline.submit("/repo", emptyList(), listOf(DiskSubmission(fixture.appPath, fixture.app, fixture.sha("a"))))
        assertTrue(fixture.sent.isEmpty())
    }
}
