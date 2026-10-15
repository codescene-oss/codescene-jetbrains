package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class IdeServerFixture(
    private val input: InputStream,
    private val output: OutputStream,
) {
    private val writeLock = Any()
    private val lastParams = ConcurrentHashMap<String, JsonNode>()
    private val paramsHistory = ConcurrentHashMap<String, CopyOnWriteArrayList<JsonNode>>()

    @Volatile
    var deltaResultNull: Boolean = false

    @Volatile
    var reviewErrorMessage: String? = null

    @Volatile
    var running = true
        private set

    private val handlers: Map<String, (JsonNode?, JsonNode) -> Unit> =
        mapOf(
            "cs-ide/review" to { id, _ -> replyReview(id) },
            "cs-ide/delta" to { id, _ -> reply(id, if (deltaResultNull) null else DELTA) },
            "cs-ide/preflight" to { id, _ -> reply(id, PREFLIGHT) },
            "cs-ide/fns-to-refactor" to { id, params -> replyFnsToRefactor(id, params) },
            "cs-ide/refactor" to { id, _ -> reply(id, REFACTOR_RESPONSE) },
            "cs-ide/telemetry" to { id, _ -> reply(id, mapOf("status" to 202)) },
            "cs-ide/device-id" to { id, _ -> reply(id, mapOf("deviceId" to "device-42")) },
        )

    fun lastParams(method: String): JsonNode? = lastParams[method]

    fun paramsHistory(method: String): List<JsonNode> = paramsHistory[method]?.toList() ?: emptyList()

    fun start() {
        Thread({
            while (running) {
                val body = LspFraming.read(input) ?: break
                handle(RpcJson.mapper.readTree(body))
            }
        }, "cs-ide-fixture").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        runCatching { input.close() }
        runCatching { output.close() }
    }

    private fun handle(node: JsonNode) {
        val method = node.get("method")?.asText() ?: return
        val id = node.get("id")
        val params = node.get("params") ?: RpcJson.objectNode()
        if (id != null && id.isNumber) record(method, params)
        handlers[method]?.invoke(id, params)
    }

    private fun record(
        method: String,
        params: JsonNode,
    ) {
        val copy: JsonNode = params.deepCopy()
        lastParams[method] = copy
        paramsHistory.computeIfAbsent(method) { CopyOnWriteArrayList<JsonNode>() }.add(copy)
    }

    private fun replyReview(id: JsonNode?) {
        val error = reviewErrorMessage
        if (error != null) replyError(id, error) else reply(id, REVIEW)
    }

    private fun replyFnsToRefactor(
        id: JsonNode?,
        params: JsonNode,
    ) {
        val fileName = RpcJson.text(params, "fileName", "file-name")
        if (fileName.isNullOrBlank()) replyError(id, "Missing file-name") else reply(id, listOf(FN_TO_REFACTOR))
    }

    private fun reply(
        id: JsonNode?,
        result: Any?,
    ) {
        if (id == null || id.isNull) return
        send(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to id.asLong(),
                "result" to result,
            ),
        )
    }

    private fun replyError(
        id: JsonNode?,
        message: String,
    ) {
        if (id == null || id.isNull) return
        send(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to id.asLong(),
                "error" to mapOf("code" to -32603, "message" to message),
            ),
        )
    }

    private fun send(payload: Any) {
        val body = RpcJson.mapper.writeValueAsBytes(payload)
        synchronized(writeLock) {
            LspFraming.write(output, body)
        }
    }

    private companion object {
        val REVIEW =
            mapOf(
                "fileLevelCodeSmells" to emptyList<Any>(),
                "functionLevelCodeSmells" to emptyList<Any>(),
                "rawScore" to "raw-score",
                "score" to 9.68,
                "gitBlobSha" to "review-sha",
            )

        val DELTA =
            mapOf(
                "fileLevelFindings" to emptyList<Any>(),
                "functionLevelFindings" to emptyList<Any>(),
                "oldScore" to 10,
                "newScore" to 9.68,
                "scoreChange" to -0.32,
            )

        val PREFLIGHT =
            mapOf(
                "version" to 2,
                "fileTypes" to listOf("ts"),
                "languageCommon" to mapOf("maxInputLoc" to 100, "codeSmells" to listOf("Complex Method")),
            )

        val FN_TO_REFACTOR =
            mapOf(
                "body" to "function f() {}",
                "name" to "f",
                "fileType" to "TypeScript",
                "functionType" to "Function",
                "range" to mapOf("startLine" to 1, "startColumn" to 1, "endLine" to 1, "endColumn" to 16),
                "refactoringTargets" to listOf(mapOf("category" to "Complex Method", "line" to 1)),
            )

        val REFACTOR_RESPONSE =
            mapOf(
                "code" to "function f() {}",
                "confidence" to
                    mapOf(
                        "level" to 1,
                        "title" to "High confidence",
                        "recommendedAction" to mapOf("description" to "Apply", "details" to "Safe change"),
                        "reviewHeader" to "Review",
                    ),
                "reasons" to emptyList<Any>(),
                "refactoringProperties" to
                    mapOf(
                        "addedCodeSmells" to emptyList<Any>(),
                        "removedCodeSmells" to listOf("Complex Method"),
                    ),
                "traceId" to "trace-1",
            )
    }
}
