package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class JsonRpcException(
    val rpcCode: Int?,
    override val message: String,
) : RuntimeException(message)

class JsonRpcConnection(
    private val input: InputStream,
    private val output: OutputStream,
    private val onNotification: (method: String, params: JsonNode?) -> Unit,
    private val onTransportError: (Throwable) -> Unit,
) : AutoCloseable {
    private val nextId = AtomicLong(1)
    private val pending = ConcurrentHashMap<Long, CompletableFuture<JsonNode>>()
    private val closed = AtomicBoolean(false)
    private val writeLock = Any()
    private val reader =
        Thread({
            try {
                readLoop()
            } catch (error: Throwable) {
                if (!closed.get()) {
                    onTransportError(error)
                }
            }
        }, "cs-ide-jsonrpc-reader").apply {
            isDaemon = true
            start()
        }

    fun sendRequest(
        method: String,
        params: Any?,
        timeoutMs: Long = 300_000,
    ): JsonNode {
        val id = nextId.getAndIncrement()
        val future = CompletableFuture<JsonNode>()
        pending[id] = future
        val payload = RpcJson.objectNode()
        payload.put("jsonrpc", "2.0")
        payload.put("id", id)
        payload.put("method", method)
        payload.set<JsonNode>("params", toParamsNode(params))
        write(payload)
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (error: Exception) {
            pending.remove(id)
            throw error
        }
    }

    fun sendNotification(
        method: String,
        params: Any?,
    ) {
        val payload = RpcJson.objectNode()
        payload.put("jsonrpc", "2.0")
        payload.put("method", method)
        payload.set<JsonNode>("params", toParamsNode(params))
        write(payload)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pending.values.forEach { it.completeExceptionally(JsonRpcException(null, "JSON-RPC connection closed")) }
        pending.clear()
        reader.interrupt()
        runCatching { input.close() }
        runCatching { output.close() }
    }

    private fun write(payload: Any) {
        val body = RpcJson.mapper.writeValueAsBytes(payload)
        synchronized(writeLock) {
            LspFraming.write(output, body)
        }
    }

    private fun toParamsNode(params: Any?): JsonNode =
        when (params) {
            null -> RpcJson.objectNode()
            is JsonNode -> params
            else -> RpcJson.mapper.valueToTree(params)
        }

    private fun readLoop() {
        while (!closed.get()) {
            val body = LspFraming.read(input) ?: break
            val node = RpcJson.mapper.readTree(body)
            val idNode = node.get("id")
            val method = node.get("method")?.asText()
            when {
                method != null && (idNode == null || idNode.isNull) ->
                    onNotification(method, node.get("params"))
                idNode != null && !idNode.isNull -> completeRequest(idNode.asLong(), node)
            }
        }
        if (!closed.get()) {
            onTransportError(JsonRpcException(null, "JSON-RPC stream closed"))
        }
    }

    private fun completeRequest(
        id: Long,
        node: JsonNode,
    ) {
        val future = pending.remove(id) ?: return
        val error = node.get("error")
        if (error != null && !error.isNull) {
            val code = error.get("code")?.asInt()
            val message = error.get("message")?.asText() ?: "JSON-RPC error"
            future.completeExceptionally(JsonRpcException(code, message))
            return
        }
        val result = node.get("result") ?: RpcJson.mapper.nullNode()
        future.complete(result)
    }
}
