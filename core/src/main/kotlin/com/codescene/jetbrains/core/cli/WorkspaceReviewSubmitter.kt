package com.codescene.jetbrains.core.cli

data class RepoLocation(
    val repoRoot: String,
    val relPath: String,
)

class WorkspaceReviewSubmitter(
    private val pipeline: WorkspaceReviewPipeline,
    private val locate: (String) -> RepoLocation?,
    private val isSupported: (String) -> Boolean,
) {
    fun submitEditor(
        absolutePath: String,
        content: String,
    ) {
        if (!isSupported(absolutePath)) return
        val location = locate(absolutePath) ?: return
        pipeline.submit(
            location.repoRoot,
            listOf(BufferSubmission(absolutePath, location.relPath, content)),
        )
    }
}
