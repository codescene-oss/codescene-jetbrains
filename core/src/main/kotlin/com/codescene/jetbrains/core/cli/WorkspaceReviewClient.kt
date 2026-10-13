package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

class WorkspaceReviewClient(
    private val notify: (String, JsonNode) -> Unit,
) : ReviewFilesSender {
    override fun reviewFiles(
        repoRoot: String,
        files: List<ReviewFileWire>,
    ) {
        if (files.isEmpty()) return
        val params = RpcJson.objectNode().put("repo-root", repoRoot)
        val wireFiles = params.putArray("files")
        files.forEach { wireFiles.add(fileNode(it)) }
        notify(METHOD, params)
    }

    private fun fileNode(file: ReviewFileWire): ObjectNode =
        RpcJson.objectNode().apply {
            put("rel-path", file.relPath)
            file.id?.let { put("id", it) }
            file.content?.let { put("content", it) }
        }

    private companion object {
        private const val METHOD = "cs-ide/reviewFiles"
    }
}
