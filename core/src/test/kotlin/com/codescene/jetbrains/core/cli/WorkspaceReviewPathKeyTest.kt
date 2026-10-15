package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WorkspaceReviewPathKeyTest {
    private val fixture = WorkspaceReviewPipelineFixture()
    private val rel = "src/A.kt"
    private val winRoot = "C:\\repo"
    private val winDisk = "C:\\repo\\src\\A.kt"
    private val winSlash = "C:/repo/src/A.kt"

    @Before
    fun reset() {
        fixture.sent.clear()
        fixture.events.clear()
    }

    @Test
    fun `a backslash buffer and a forward-slash watch result share one path state on Windows`() {
        val sha = "disk-sha"
        val observe: (String, String) -> ObservedFile? = { root, path ->
            if (WorkspacePaths.sameRoot(root, winRoot, true) && path == rel) {
                ObservedFile(winSlash, winRoot, CurrentFile(sha, null))
            } else {
                null
            }
        }
        val pipeline = fixture.pipeline(windows = true, observe = observe)
        pipeline.submit(winRoot, listOf(BufferSubmission(winDisk, rel, "buf")))
        fixture.notify(
            pipeline,
            "fileReview",
            fixture.fileReview("id1", fixture.sha("buf"), path = rel, root = winRoot),
        )
        fixture.notify(
            pipeline,
            "deltaReview",
            fixture.deltaReview("id1", fixture.sha("buf"), path = rel, root = winRoot),
        )

        assertEquals(1, fixture.reviews.size)
        assertEquals(1, fixture.deltas.size)

        fixture.notify(pipeline, "fileReview", fixture.fileReview(null, sha, path = rel, root = winRoot))
        assertEquals(2, fixture.reviews.size)

        pipeline.submit(winRoot, listOf(BufferSubmission(winDisk, rel, "buf")))
        assertEquals(2, fixture.sent.size)
        assertEquals(setOf(winDisk), pipeline.activePaths())
    }

    @Test
    fun `buffer path forms collapse to the latest submission path on Windows`() {
        val pipeline = fixture.pipeline(windows = true)
        pipeline.submit(winRoot, listOf(BufferSubmission(winSlash, rel, "buf")))
        pipeline.submit(winRoot, listOf(BufferSubmission(winDisk, rel, "buf2")))

        assertEquals(setOf(winDisk), pipeline.activePaths())
    }

    @Test
    fun `paths differing only by case stay separate when not on Windows`() {
        val pipeline = fixture.pipeline(windows = false)
        pipeline.submit("/repo", listOf(BufferSubmission("/repo/A.kt", "A.kt", "a")))
        pipeline.submit("/repo", listOf(BufferSubmission("/repo/a.kt", "a.kt", "b")))

        assertEquals(setOf("/repo/A.kt", "/repo/a.kt"), pipeline.activePaths())
    }
}
