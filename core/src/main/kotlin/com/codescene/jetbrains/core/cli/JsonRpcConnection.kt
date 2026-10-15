package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class JsonRpcException(
    val rpcCode: Int?,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class JsonRpcConnection(
    private val input: InputStream,
    private val output: OutputStream,
    private val onNotification: (String, JsonNode) -> Unit,
    private val onClosed: (Throwable) -> Unit,
    threadName: String = "cs-ide-jsonrpc-reader",
) : AutoCloseable {
    private val nextId = AtomicLong(1)
    private val pending = ConcurrentHashMap<Long, CompletableFuture<JsonNode>>()
    private val closed = AtomicBoolean(false)
    private val writeLock = Any()

    init {
        Thread({ runReader() }, threadName).apply {
            isDaemon = true
            start()
        }
    }

    fun request(
        method: String,
        params: JsonNode,
    ): CompletableFuture<JsonNode> {
        val future = CompletableFuture<JsonNode>()
        if (closed.get()) {
            future.completeExceptionally(JsonRpcException(null, "JSON-RPC connection is closed"))
            return future
        }
        val id = nextId.getAndIncrement()
        pending[id] = future
        future.whenComplete { _, _ ->
            pending.remove(id)
            if (future.isCancelled) sendCancel(id)
        }
        try {
            write(envelope(method, params).put("id", id))
        } catch (e: Exception) {
            future.completeExceptionally(JsonRpcException(null, "Failed to send $method: ${e.message}", e))
        }
        return future
    }

    fun notify(
        method: String,
        params: JsonNode,
    ) {
        if (closed.get()) throw JsonRpcException(null, "JSON-RPC connection is closed")
        write(envelope(method, params))
    }

    fun failPending(error: Throwable) {
        pending.values.toList().forEach { it.completeExceptionally(error) }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        failPending(JsonRpcException(null, "JSON-RPC connection closed"))
        runCatching { output.close() }
        runCatching { input.close() }
    }

    private fun envelope(
        method: String,
        params: JsonNode,
    ): ObjectNode =
        RpcJson.objectNode().apply {
            put("jsonrpc", "2.0")
            put("method", method)
            set<JsonNode>("params", params)
        }

    private fun sendCancel(id: Long) {
        if (closed.get()) return
        runCatching { write(envelope("\$/cancelRequest", RpcJson.objectNode().put("id", id))) }
    }

    private fun write(payload: JsonNode) {
        val body = RpcJson.mapper.writeValueAsBytes(payload)
        synchronized(writeLock) { LspFraming.write(output, body) }
    }

    private fun runReader() {
        val failure =
            try {
                readLoop()
                JsonRpcException(null, "JSON-RPC stream ended")
            } catch (e: Throwable) {
                JsonRpcException(null, "JSON-RPC stream failed: ${e.message}", e)
            }
        if (closed.compareAndSet(false, true)) {
            failPending(failure)
            onClosed(failure)
        }
    }

    private fun readLoop() {
        while (!closed.get()) {
            val body = LspFraming.read(input) ?: return
            dispatch(RpcJson.mapper.readTree(body))
        }
    }

    private fun dispatch(node: JsonNode) {
        val id = node.get("id")?.takeUnless { it.isNull }
        val method = node.get("method")?.asText()
        when {
            method != null && id == null -> deliverNotification(method, node.get("params") ?: RpcJson.objectNode())
            method != null -> rejectServerRequest(id!!)
            id != null -> complete(id.asLong(), node)
        }
    }

    private fun deliverNotification(
        method: String,
        params: JsonNode,
    ) {
        runCatching { onNotification(method, params) }
    }

    private fun rejectServerRequest(id: JsonNode) {
        val error = RpcJson.objectNode().put("code", -32601).put("message", "Method not supported by client")
        val reply = RpcJson.objectNode().put("jsonrpc", "2.0")
        reply.set<JsonNode>("id", id)
        reply.set<JsonNode>("error", error)
        runCatching { write(reply) }
    }

    private fun complete(
        id: Long,
        node: JsonNode,
    ) {
        val future = pending.remove(id) ?: return
        val error = node.get("error")?.takeUnless { it.isNull }
        if (error != null) {
            val code = error.get("code")?.takeIf { it.isNumber }?.asInt()
            future.completeExceptionally(JsonRpcException(code, error.get("message")?.asText() ?: "JSON-RPC error"))
            return
        }
        future.complete(node.get("result") ?: RpcJson.mapper.nullNode())
    }
}
