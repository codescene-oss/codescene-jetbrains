package com.codescene.jetbrains.core.cli

import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.PreflightResponse
import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.delta.Delta
import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Review
import com.codescene.data.telemetry.TelemetryEvent
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

fun interface CsIdeTransport {
    fun request(
        method: String,
        params: JsonNode,
    ): CompletableFuture<JsonNode>
}

class CsIdeClient(
    private val transport: CsIdeTransport,
    private val timeoutMs: Long = 300_000,
) {
    @Volatile
    private var cachedPreflight: PreflightResponse? = null

    fun review(
        params: ReviewParams,
        cache: CacheParams,
    ): Review? {
        val code = params.code ?: return null
        return rpc("cs-ide/review") {
            put("path", params.path)
            put("file-content", code)
            cache.path?.let { put("cache-path", it) }
            params.repoPath?.let { put("repo-path", it) }
        }.let { node ->
            if (node.isNull || node.isMissingNode) null else RpcJson.read(node, Review::class.java)
        }
    }

    fun delta(
        oldScore: String?,
        newScore: String?,
    ): Delta? =
        rpc("cs-ide/delta") {
            oldScore?.let { put("old-score", it) }
            newScore?.let { put("new-score", it) }
        }.let { node ->
            if (node.isNull || node.isMissingNode) null else RpcJson.read(node, Delta::class.java)
        }

    fun reviewDelta(
        old: ReviewParams,
        new: ReviewParams,
        cache: CacheParams,
    ): Delta? {
        val oldReview = review(old, cache)
        val newReview = review(new, cache)
        return delta(oldReview?.rawScore, newReview?.rawScore)
    }

    fun preflight(force: Boolean): PreflightResponse? =
        rpc("cs-ide/preflight") { put("force", force) }
            .let { node ->
                if (node.isNull || node.isMissingNode) {
                    null
                } else {
                    RpcJson.read(node, PreflightResponse::class.java).also { cachedPreflight = it }
                }
            }

    fun fnsToRefactor(
        code: CodeParams,
        cache: CacheParams,
        codeSmells: List<CodeSmell>,
    ): List<FnToRefactor> =
        fnsToRefactorInternal(code, cache) {
            set<JsonNode>("code-smells", RpcJson.tree(codeSmells))
        }

    fun fnsToRefactor(
        code: CodeParams,
        cache: CacheParams,
        delta: Delta,
    ): List<FnToRefactor> =
        fnsToRefactorInternal(code, cache) {
            set<JsonNode>("delta-result", RpcJson.tree(delta))
        }

    fun refactor(
        fn: FnToRefactor,
        options: RefactoringOptions?,
    ): RefactorResponse? =
        rpc("cs-ide/refactor") {
            set<JsonNode>("fn-to-refactor", RpcJson.tree(fn))
            options?.token?.orElse(null)?.let { put("token", it) }
            options?.skipCache?.orElse(null)?.let { put("skip-cache", it) }
        }.let { node ->
            if (node.isNull || node.isMissingNode) null else RpcJson.read(node, RefactorResponse::class.java)
        }

    fun telemetry(event: TelemetryEvent): Int {
        val node = rpc("cs-ide/telemetry") { set<JsonNode>("event", RpcJson.tree(event)) }
        return RpcJson.field(node, "status")?.asInt() ?: 0
    }

    fun deviceId(): String {
        val node = rpc("cs-ide/device-id") { }
        return RpcJson.text(node, "deviceId", "device-id").orEmpty()
    }

    private fun fnsToRefactorInternal(
        code: CodeParams,
        cache: CacheParams,
        extra: ObjectNode.() -> Unit,
    ): List<FnToRefactor> =
        rpc("cs-ide/fns-to-refactor") {
            put("file-name", code.fileName)
            put("file-content", code.content)
            cache.path?.let { put("cache-path", it) }
            cachedPreflight?.let { set<JsonNode>("preflight", RpcJson.tree(it)) }
            extra()
        }.let { readFnList(it) }

    private fun readFnList(node: JsonNode): List<FnToRefactor> {
        if (node.isNull || node.isMissingNode) return emptyList()
        if (node.isArray) return RpcJson.read(node, FnListType)
        return listOf(RpcJson.read(node, FnToRefactor::class.java))
    }

    private fun rpc(
        method: String,
        params: ObjectNode.() -> Unit,
    ): JsonNode {
        val future = transport.request(method, RpcJson.objectNode().apply(params))
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            throw failure(future, e)
        }
    }

    private fun failure(
        future: CompletableFuture<JsonNode>,
        error: Exception,
    ): Exception {
        if (error is InterruptedException) Thread.currentThread().interrupt()
        if (error !is ExecutionException) future.cancel(true)
        return CsIdeErrors.unwrap(error)
    }

    private companion object {
        private val FnListType = object : TypeReference<List<FnToRefactor>>() {}
    }
}
