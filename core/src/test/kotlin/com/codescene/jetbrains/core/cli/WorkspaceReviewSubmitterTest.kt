package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceReviewSubmitterTest {
    private val sent = mutableListOf<Pair<String, List<ReviewFileWire>>>()
    private val pipeline =
        WorkspaceReviewPipeline(
            ReviewFilesSender { root, files -> sent += root to files },
            object : WorkspaceReviewSink {
                override fun presentReview(event: PresentedReview) = Unit

                override fun presentDelta(event: PresentedDelta) = Unit

                override fun reviewFailed(event: ReviewFailedNotice) = Unit

                override fun progress(event: ReviewProgressNotice) = Unit

                override fun sessionLost() = Unit
            },
            newId = { "fixed-id" },
        )

    private val repoA = "/repos/a"
    private val openPath = "$repoA/src/Open.kt"
    private val unsupportedPath = "$repoA/ignored.bin"
    private val outsidePath = "/tmp/nowhere.kt"

    private fun submitter(
        supported: Set<String> = setOf(openPath),
        locate: (String) -> RepoLocation? = { path ->
            when (path) {
                openPath -> RepoLocation(repoA, "src/Open.kt")
                else -> null
            }
        },
    ) = WorkspaceReviewSubmitter(
        pipeline = pipeline,
        locate = locate,
        isSupported = { supported.contains(it) },
    )

    @Test
    fun `submitEditor sends one buffer`() {
        sent.clear()
        submitter().submitEditor(openPath, "editor text")

        assertEquals(1, sent.size)
        val wire = sent.single().second.single()
        assertEquals("src/Open.kt", wire.relPath)
        assertEquals("fixed-id", wire.id)
        assertEquals("editor text", wire.content)
    }

    @Test
    fun `submitEditor skips unsupported and unlocated paths`() {
        sent.clear()
        submitter(supported = setOf(openPath)).submitEditor(unsupportedPath, "x")
        submitter(supported = setOf(unsupportedPath, outsidePath)).submitEditor(outsidePath, "x")

        assertTrue(sent.isEmpty())
    }
}
