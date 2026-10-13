package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class WorkspaceReviewPipelineFixture {
    val sent = mutableListOf<Pair<String, List<ReviewFileWire>>>()
    val events = mutableListOf<Any>()
    val reviews get() = events.filterIsInstance<PresentedReview>()
    val deltas get() = events.filterIsInstance<PresentedDelta>()
    private var ids = 0
    var now = 1_000L

    private fun record(event: Any) {
        events += event
    }

    val sink =
        object : WorkspaceReviewSink {
            override fun presentReview(event: PresentedReview) = record(event)

            override fun presentDelta(event: PresentedDelta) = record(event)

            override fun reviewFailed(event: ReviewFailedNotice) = record(event)

            override fun progress(event: ReviewProgressNotice) = record(event)

            override fun sessionLost() = record(SESSION_LOST)
        }

    fun pipeline(
        windows: Boolean = false,
        observe: (String, String) -> ObservedFile? = { _, _ -> null },
        sender: ReviewFilesSender = ReviewFilesSender { root, files -> sent += root to files },
    ) = WorkspaceReviewPipeline(sender, sink, newId = {
        "id${++ids}"
    }, windows = windows, clock = { now }, observe = observe)

    val app = "src/App.kt"
    val appPath = "/repo/$app"

    fun buffer(content: String) = BufferSubmission(appPath, app, content)

    fun disk(sha: String) = DiskSubmission(appPath, app, sha)

    fun sha(content: String) = GitBlobSha.ofString(content)

    fun params(
        id: String?,
        path: String,
        root: String,
    ): ObjectNode =
        RpcJson.mapper.createObjectNode().apply {
            id?.let { put("id", it) }
            put("path", path)
            put("repoRoot", root)
        }

    fun fileReview(
        id: String?,
        gitBlobSha: String?,
        path: String = app,
        root: String = "/repo",
    ): JsonNode =
        params(id, path, root).apply {
            putObject("result").put("score", 9.0).put("rawScore", "raw").put("gitBlobSha", gitBlobSha)
        }

    fun deltaReview(
        id: String?,
        newSha: String?,
        path: String = app,
        nullResult: Boolean = false,
        root: String = "/repo",
    ): JsonNode =
        params(id, path, root).apply {
            if (nullResult) {
                putNull("result")
            } else {
                putObject("result").put("scoreChange", -1.0).put("oldGitBlobSha", "old").put("newGitBlobSha", newSha)
            }
        }

    fun reviewProgress(count: Int): JsonNode = RpcJson.mapper.createObjectNode().put("count", count).put("done", 0)

    fun notify(
        pipeline: WorkspaceReviewPipeline,
        method: String,
        params: JsonNode,
    ) = pipeline.onNotification("cs-ide/$method", params)

    companion object {
        const val SESSION_LOST = "sessionLost"
    }
}
