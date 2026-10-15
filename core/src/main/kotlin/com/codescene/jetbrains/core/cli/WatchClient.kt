package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CompletableFuture

data class WatchInventory(
    val repoRoot: String,
    val files: Set<String>,
)

object WatchNotices {
    const val INVENTORY_CHANGED = "cs-ide/watchInventoryChanged"

    fun inventory(params: JsonNode): WatchInventory? {
        val repoRoot = RpcJson.text(params, "repoRoot") ?: return null
        val filesNode = RpcJson.field(params, "files") ?: return null
        if (!filesNode.isArray) return null
        val files = filesNode.map { WorkspacePaths.normalizeRel(it.asText()) }.toSet()
        return WatchInventory(repoRoot, files)
    }
}

class WatchClient(
    private val notifyInSession: (String, JsonNode) -> Long,
    private val notifyIfRunning: (String, JsonNode) -> Boolean,
    private val transport: CsIdeTransport,
) {
    fun watchFiles(
        repoRoot: String,
        relativePaths: List<String>,
    ): Long {
        val params = RpcJson.objectNode().put("repo-root", repoRoot)
        params.putArray("relative-paths").apply { relativePaths.forEach(::add) }
        return notifyInSession(WATCH_FILES, params)
    }

    fun stopWatchFiles(repoRoot: String): Boolean {
        val params = RpcJson.objectNode().put("repo-root", repoRoot)
        return notifyIfRunning(STOP_WATCH_FILES, params)
    }

    fun getWatchInventory(repoRoot: String): CompletableFuture<WatchInventory> {
        val params = RpcJson.objectNode().put("repo-root", repoRoot)
        return transport.request(GET_WATCH_INVENTORY, params).thenApply { node ->
            WatchNotices.inventory(node)
                ?: throw IllegalStateException("Malformed cs-ide/getWatchInventory result")
        }
    }

    companion object {
        private const val WATCH_FILES = "cs-ide/watchFiles"
        private const val STOP_WATCH_FILES = "cs-ide/stopWatchFiles"
        private const val GET_WATCH_INVENTORY = "cs-ide/getWatchInventory"

        fun isNotWatched(error: Throwable): Boolean {
            val unwrapped = CsIdeErrors.unwrap(error)
            return unwrapped is JsonRpcException &&
                unwrapped.message.orEmpty().contains("not watched", ignoreCase = true)
        }
    }
}
