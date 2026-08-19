package com.codescene.jetbrains.core.cli

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.data.telemetry.TelemetryEvent
import com.fasterxml.jackson.databind.JsonNode
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class CsIdeClient(
    private val input: InputStream,
    private val output: OutputStream,
    private val process: Process? = null,
) : AutoCloseable {
    private val listeners = CopyOnWriteArrayList<CsIdeListener>()
    private val startFuture = CompletableFuture<ServerMetadata>()
    private val connection =
        JsonRpcConnection(
            input = input,
            output = output,
            onNotification = ::handleNotification,
            onTransportError = ::handleTransportError,
        )
    private val metadata = AtomicReference<ServerMetadata>()
    private val closed = AtomicBoolean(false)

    fun addListener(listener: CsIdeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CsIdeListener) {
        listeners.remove(listener)
    }

    fun start(timeoutMs: Long = CsIdeDistribution.STARTUP_TIMEOUT_MS): ServerMetadata {
        process?.onExit()?.thenAccept {
            handleTransportError(JsonRpcException(null, "cs-ide server exited with code ${it.exitValue()}"))
        }
        return startFuture.get(timeoutMs, TimeUnit.MILLISECONDS)
    }

    fun metadata(): ServerMetadata? = metadata.get()

    fun review(request: ReviewRequest): Review {
        val params = RpcJson.objectNode()
        params.put("path", request.path)
        request.fileContent?.let { params.put("file-content", it) }
        request.cachePath?.let { params.put("cache-path", it) }
        request.repoPath?.let { params.put("repo-path", it) }
        return RpcJson.read(connection.sendRequest("cs-ide/review", params))
    }

    fun delta(
        oldScore: String?,
        newScore: String?,
    ): Delta? {
        val params = RpcJson.objectNode()
        oldScore?.let { params.put("old-score", it) }
        newScore?.let { params.put("new-score", it) }
        val result = connection.sendRequest("cs-ide/delta", params)
        if (result.isNull) return null
        return RpcJson.read(result)
    }

    fun preflight(force: Boolean = false): PreflightResponse {
        val params = RpcJson.objectNode()
        params.put("force", force)
        return RpcJson.read(connection.sendRequest("cs-ide/preflight", params))
    }

    fun fnsToRefactor(request: FnsToRefactorRequest): List<FnToRefactor> {
        val params = RpcJson.objectNode()
        params.put("file-name", request.fileName)
        params.put("file-content", request.fileContent)
        request.cachePath?.let { params.put("cache-path", it) }
        request.preflight?.let { params.set<JsonNode>("preflight", RpcJson.mapper.valueToTree(it)) }
        request.delta?.let { params.set<JsonNode>("delta-result", RpcJson.mapper.valueToTree(it)) }
        request.codeSmells?.let { params.set<JsonNode>("code-smells", RpcJson.mapper.valueToTree(it)) }
        val result = connection.sendRequest("cs-ide/fns-to-refactor", params)
        if (result.isArray) {
            return result.map { RpcJson.read<FnToRefactor>(it) }
        }
        if (result.isNull) return emptyList()
        return listOf(RpcJson.read(result))
    }

    fun refactor(
        function: com.codescene.data.ace.FnToRefactor,
        options: RefactoringOptions?,
    ): RefactorResponse {
        val params = RpcJson.objectNode()
        params.set<JsonNode>("fn-to-refactor", RpcJson.mapper.valueToTree(function))
        options?.token?.orElse(null)?.let { params.put("token", it) }
        options?.skipCache?.orElse(null)?.let { params.put("skip-cache", it) }
        return RpcJson.read(connection.sendRequest("cs-ide/refactor", params))
    }

    fun telemetry(event: TelemetryEvent): TelemetryResponse {
        val params = RpcJson.objectNode()
        params.set<JsonNode>("event", RpcJson.mapper.valueToTree(event))
        val result = connection.sendRequest("cs-ide/telemetry", params)
        val status = RpcJson.field(result, "status")?.asInt()
        return TelemetryResponse(status)
    }

    fun deviceId(): String {
        val result = connection.sendRequest("cs-ide/device-id", RpcJson.objectNode())
        return RpcJson.text(result, "deviceId", "device-id").orEmpty()
    }

    fun reviewFiles(
        repoRoot: String,
        files: List<ReviewFile>,
        baselineRevision: String? = null,
    ) {
        val params = RpcJson.objectNode()
        params.put("repo-root", repoRoot)
        baselineRevision?.let { params.put("baseline-revision", it) }
        val filesNode = RpcJson.arrayNode()
        files.forEach { file ->
            val node = RpcJson.objectNode()
            node.put("rel-path", toPosixRelPath(file.relPath))
            file.id?.let { node.put("id", it) }
            file.content?.let { node.put("content", it) }
            filesNode.add(node)
        }
        params.set<JsonNode>("files", filesNode)
        connection.sendNotification("cs-ide/reviewFiles", params)
    }

    fun watchFiles(
        repoRoot: String,
        baselineRevision: String? = null,
    ) {
        val params = RpcJson.objectNode()
        params.put("repo-root", repoRoot)
        baselineRevision?.let { params.put("baseline-revision", it) }
        connection.sendNotification("cs-ide/watchFiles", params)
    }

    fun stopWatchFiles(repoRoot: String) {
        val params = RpcJson.objectNode()
        params.put("repo-root", repoRoot)
        connection.sendNotification("cs-ide/stopWatchFiles", params)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        connection.close()
        process?.let { CsIdeProcess.kill(it) }
        if (!startFuture.isDone) {
            startFuture.completeExceptionally(JsonRpcException(null, "cs-ide client closed"))
        }
    }

    private fun handleNotification(
        method: String,
        params: JsonNode?,
    ) {
        val node = params ?: RpcJson.objectNode()
        when (method) {
            "cs-ide/start" -> {
                val value =
                    ServerMetadata(
                        sha = RpcJson.text(node, "sha").orEmpty(),
                        version = RpcJson.text(node, "version").orEmpty(),
                    )
                metadata.set(value)
                startFuture.complete(value)
            }
            "cs-ide/fileReview" ->
                identity(node)?.let { identity ->
                    val resultNode = node.get("result") ?: return
                    val event =
                        FileReviewEvent(
                            id = identity.id,
                            path = identity.path,
                            repoRoot = identity.repoRoot,
                            result = RpcJson.read(resultNode),
                        )
                    listeners.forEach { it.onFileReview(event) }
                }
            "cs-ide/deltaReview" ->
                identity(node)?.let { identity ->
                    val resultNode = node.get("result")
                    val event =
                        DeltaReviewEvent(
                            id = identity.id,
                            path = identity.path,
                            repoRoot = identity.repoRoot,
                            result = if (resultNode == null || resultNode.isNull) null else RpcJson.read(resultNode),
                        )
                    listeners.forEach { it.onDeltaReview(event) }
                }
            "cs-ide/reviewFailed" ->
                identity(node)?.let { identity ->
                    val event =
                        ReviewFailedEvent(
                            id = identity.id,
                            path = identity.path,
                            repoRoot = identity.repoRoot,
                            message = RpcJson.text(node, "message").orEmpty(),
                        )
                    listeners.forEach { it.onReviewFailed(event) }
                }
        }
    }

    private fun identity(node: JsonNode): NotificationIdentity? {
        val path = RpcJson.text(node, "path") ?: return null
        val repoRoot = RpcJson.text(node, "repoRoot", "repo-root") ?: return null
        return NotificationIdentity(
            id = RpcJson.text(node, "id"),
            path = path,
            repoRoot = repoRoot,
        )
    }

    private fun handleTransportError(error: Throwable) {
        if (closed.get()) return
        if (!startFuture.isDone) {
            startFuture.completeExceptionally(error)
        }
        listeners.forEach { it.onError(error) }
    }

    companion object {
        fun fromDistribution(
            distribution: Path,
            threads: Int,
            onStdErr: (String) -> Unit = {},
        ): CsIdeClient {
            val process = CsIdeProcess.startDistribution(distribution, threads, onStdErr)
            return CsIdeClient(
                input = process.inputStream,
                output = process.outputStream,
                process = process,
            )
        }
    }
}

private data class NotificationIdentity(
    val id: String?,
    val path: String,
    val repoRoot: String,
)
