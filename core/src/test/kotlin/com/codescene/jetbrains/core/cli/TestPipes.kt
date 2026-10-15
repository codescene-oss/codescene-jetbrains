package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.Pipe
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class TestPipe {
    private val pipe = Pipe.open()
    val input: InputStream = Channels.newInputStream(pipe.source())
    val output: OutputStream = Channels.newOutputStream(pipe.sink())
}

class ScriptedPeer {
    private val toClient = TestPipe()
    private val fromClient = TestPipe()
    private val received = LinkedBlockingQueue<JsonNode>()
    private val writeLock = Any()

    val clientInput: InputStream get() = toClient.input
    val clientOutput: OutputStream get() = fromClient.output

    init {
        Thread({
            while (true) {
                val body = runCatching { LspFraming.read(fromClient.input) }.getOrNull() ?: break
                received.add(RpcJson.mapper.readTree(body))
            }
        }, "scripted-peer").apply {
            isDaemon = true
            start()
        }
    }

    fun nextMessage(timeoutMs: Long = 5_000): JsonNode =
        received.poll(timeoutMs, TimeUnit.MILLISECONDS) ?: throw AssertionError("No message from client")

    fun pollMessage(timeoutMs: Long): JsonNode? = received.poll(timeoutMs, TimeUnit.MILLISECONDS)

    fun send(payload: Any) {
        val body = RpcJson.mapper.writeValueAsBytes(payload)
        synchronized(writeLock) { LspFraming.write(toClient.output, body) }
    }

    fun reply(
        id: Long,
        result: Any?,
    ) = send(mapOf("jsonrpc" to "2.0", "id" to id, "result" to result))

    fun replyError(
        id: Long,
        message: String,
        code: Int = -32603,
    ) = send(mapOf("jsonrpc" to "2.0", "id" to id, "error" to mapOf("code" to code, "message" to message)))

    fun notify(
        method: String,
        params: Any,
    ) = send(mapOf("jsonrpc" to "2.0", "method" to method, "params" to params))

    fun closeOutput() {
        toClient.output.close()
    }
}
