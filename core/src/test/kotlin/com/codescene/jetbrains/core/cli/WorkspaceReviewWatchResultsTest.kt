package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkspaceReviewWatchResultsTest {
    private val fixture = WorkspaceReviewPipelineFixture()
    private var current = CurrentFile("s1", null)
    private var observeCalls = 0

    @Before
    fun reset() {
        fixture.sent.clear()
        fixture.events.clear()
        current = CurrentFile("s1", null)
        observeCalls = 0
    }

    private fun observe(
        repoRoot: String,
        relPath: String,
    ): ObservedFile? {
        observeCalls++
        return if (repoRoot == "/repo" && relPath == fixture.app) {
            ObservedFile(fixture.appPath, "/repo", current)
        } else {
            null
        }
    }

    @Test
    fun `a watch result for a never-submitted file presents once without content`() {
        current = CurrentFile("s1", null)
        val pipeline = fixture.pipeline(observe = ::observe)

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "s1"))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, "s1"))

        val review = fixture.reviews.single()
        assertEquals(fixture.appPath, review.absolutePath)
        assertNull(review.content)
        assertEquals("s1", review.gitBlobSha)
        val delta = fixture.deltas.single()
        assertNull(delta.scoredContent)
        assertTrue(fixture.sent.isEmpty())
        assertTrue(pipeline.activePaths().isEmpty())
    }

    @Test
    fun `a watch result for a loaded document presents with the document text`() {
        current = CurrentFile(fixture.sha("t"), "t")
        val pipeline = fixture.pipeline(observe = ::observe)

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, fixture.sha("t")))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, fixture.sha("t")))

        assertEquals("t", fixture.reviews.single().content)
        assertEquals("t", fixture.deltas.single().scoredContent)
    }

    @Test
    fun `a watch result whose SHA is not the current bytes is dropped`() {
        current = CurrentFile("s2", null)
        val pipeline = fixture.pipeline(observe = ::observe)

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "s1"))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview(null, "s1"))

        assertTrue(fixture.reviews.isEmpty() && fixture.deltas.isEmpty())
    }

    @Test
    fun `a disk SHA that is no longer current is dropped and the current one is accepted`() {
        val pipeline = fixture.pipeline(observe = ::observe)
        pipeline.submit("/repo", emptyList(), listOf(fixture.disk("d1")))
        current = CurrentFile("d2", null)

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "d1"))
        assertTrue(fixture.reviews.isEmpty())

        current = CurrentFile("d2", null)
        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "d2"))
        assertEquals(1, fixture.reviews.size)
    }

    @Test
    fun `an id-less result is dropped while a buffer is in flight`() {
        current = CurrentFile(fixture.sha("a"), "a")
        val pipeline = fixture.pipeline(observe = ::observe)
        pipeline.submit("/repo", listOf(fixture.buffer("a")), emptyList())

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, fixture.sha("a")))
        assertTrue(fixture.reviews.isEmpty())
        assertEquals(0, observeCalls)
        assertEquals(setOf(fixture.appPath), pipeline.activePaths())

        fixture.notify(pipeline, "fileReview", fixture.fileReview("id1", fixture.sha("a")))
        assertEquals(1, fixture.reviews.size)
    }

    @Test
    fun `without an observed file a never-submitted path is dropped`() {
        val pipeline = fixture.pipeline(observe = { _, _ -> null })

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, "s1"))

        assertTrue(fixture.reviews.isEmpty())
    }

    @Test
    fun `observe is not called for results with an id`() {
        val pipeline = fixture.pipeline(observe = ::observe)
        pipeline.submit("/repo", listOf(fixture.buffer("a")), emptyList())

        fixture.notify(pipeline, "fileReview", fixture.fileReview("id1", fixture.sha("a")))
        fixture.notify(pipeline, "deltaReview", fixture.deltaReview("id1", fixture.sha("a")))

        assertEquals(0, observeCalls)
    }
}
