package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CopyOnWriteArrayList

class IdeServerFixture(
    private val input: InputStream,
    private val output: OutputStream,
    private val sha: String = "fixture-sha",
    private val version: String = "fixture-version",
) {
    private val writeLock = Any()
    private val watchRoots = CopyOnWriteArrayList<String>()

    @Volatile
    var running = true
        private set

    fun start() {
        Thread({
            sendNotification(
                "cs-ide/start",
                mapOf("sha" to sha, "version" to version),
            )
            while (running) {
                val body = LspFraming.read(input) ?: break
                handle(RpcJson.mapper.readTree(body))
            }
        }, "cs-ide-fixture").apply {
            isDaemon = true
            start()
        }
    }

    fun emitFileReview(
        path: String,
        repoRoot: String,
        content: String,
        id: String? = null,
        fail: Boolean = false,
    ) {
        if (fail) {
            sendNotification(
                "cs-ide/reviewFailed",
                buildIdentity(id, path, repoRoot) + mapOf("message" to "fixture review failed"),
            )
            return
        }
        val gitBlobSha = GitBlobSha.ofUtf8(content)
        sendNotification(
            "cs-ide/fileReview",
            buildIdentity(id, path, repoRoot) +
                mapOf(
                    "result" to
                        mapOf(
                            "fileLevelCodeSmells" to emptyList<Any>(),
                            "functionLevelCodeSmells" to emptyList<Any>(),
                            "rawScore" to "raw",
                            "score" to 9.68,
                            "gitBlobSha" to gitBlobSha,
                        ),
                ),
        )
        sendNotification(
            "cs-ide/deltaReview",
            buildIdentity(id, path, repoRoot) +
                mapOf(
                    "result" to
                        mapOf(
                            "fileLevelFindings" to emptyList<Any>(),
                            "functionLevelFindings" to emptyList<Any>(),
                            "oldScore" to 10,
                            "newScore" to 9.68,
                            "scoreChange" to -0.32,
                            "oldGitBlobSha" to "old-sha",
                            "newGitBlobSha" to gitBlobSha,
                        ),
                ),
        )
    }

    fun watchedRoots(): List<String> = watchRoots.toList()

    fun stop() {
        running = false
        runCatching { input.close() }
        runCatching { output.close() }
    }

    private fun handle(node: JsonNode) {
        val method = node.get("method")?.asText() ?: return
        val id = node.get("id")
        val params = node.get("params") ?: RpcJson.objectNode()
        when (method) {
            "cs-ide/review" ->
                reply(
                    id,
                    mapOf(
                        "fileLevelCodeSmells" to emptyList<Any>(),
                        "functionLevelCodeSmells" to emptyList<Any>(),
                        "rawScore" to "raw",
                        "score" to 9.68,
                        "gitBlobSha" to "review-sha",
                    ),
                )
            "cs-ide/delta" ->
                reply(
                    id,
                    mapOf(
                        "fileLevelFindings" to emptyList<Any>(),
                        "functionLevelFindings" to emptyList<Any>(),
                        "oldScore" to 10,
                        "newScore" to 9.68,
                        "scoreChange" to -0.32,
                    ),
                )
            "cs-ide/preflight" ->
                reply(
                    id,
                    mapOf(
                        "version" to 2,
                        "fileTypes" to listOf("ts"),
                        "languageCommon" to mapOf("maxInputLoc" to 100, "codeSmells" to listOf("Complex Method")),
                    ),
                )
            "cs-ide/fns-to-refactor" -> {
                val fileName = RpcJson.text(params, "fileName", "file-name")
                if (fileName.isNullOrBlank()) {
                    replyError(id, "Missing file-name")
                    return
                }
                reply(
                    id,
                    listOf(
                        mapOf(
                            "body" to "function f() {}",
                            "name" to "f",
                            "fileType" to "TypeScript",
                            "functionType" to "Function",
                            "range" to mapOf("startLine" to 1, "startColumn" to 1, "endLine" to 1, "endColumn" to 16),
                            "refactoringTargets" to listOf(mapOf("category" to "Complex Method", "line" to 1)),
                        ),
                    ),
                )
            }
            "cs-ide/refactor" ->
                reply(
                    id,
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
                    ),
                )
            "cs-ide/telemetry" -> reply(id, mapOf("status" to 202))
            "cs-ide/device-id" -> reply(id, mapOf("deviceId" to "device-42"))
            "cs-ide/reviewFiles" -> {
                val repoRoot = RpcJson.text(params, "repoRoot", "repo-root").orEmpty()
                val files = params.get("files") ?: return
                files.forEach { file ->
                    val relPath = RpcJson.text(file, "relPath", "rel-path").orEmpty()
                    val content = RpcJson.text(file, "content")
                    val fileId = RpcJson.text(file, "id")
                    if (content == "fail") {
                        emitFileReview(relPath, repoRoot, content.orEmpty(), fileId, fail = true)
                    } else {
                        emitFileReview(relPath, repoRoot, content.orEmpty(), fileId)
                    }
                }
            }
            "cs-ide/watchFiles" -> {
                RpcJson.text(params, "repoRoot", "repo-root")?.let(watchRoots::add)
            }
            "cs-ide/stopWatchFiles" -> {
                RpcJson.text(params, "repoRoot", "repo-root")?.let(watchRoots::remove)
            }
        }
    }

    private fun reply(
        id: JsonNode?,
        result: Any,
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

    private fun sendNotification(
        method: String,
        params: Any,
    ) {
        send(mapOf("jsonrpc" to "2.0", "method" to method, "params" to params))
    }

    private fun send(payload: Any) {
        val body = RpcJson.mapper.writeValueAsBytes(payload)
        synchronized(writeLock) {
            LspFraming.write(output, body)
        }
    }

    private fun buildIdentity(
        id: String?,
        path: String,
        repoRoot: String,
    ): MutableMap<String, Any> {
        val values = mutableMapOf<String, Any>("path" to path, "repoRoot" to repoRoot)
        if (id != null) values["id"] = id
        return values
    }
}
