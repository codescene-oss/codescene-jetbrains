package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JsonRpcConnectionTest {
    private lateinit var peer: ScriptedPeer
    private lateinit var connection: JsonRpcConnection
    private val notifications = LinkedBlockingQueue<Pair<String, JsonNode>>()
    private val closedWith = CompletableFuture<Throwable>()

    @Before
    fun setUp() {
        peer = ScriptedPeer()
        connection =
            JsonRpcConnection(
                input = peer.clientInput,
                output = peer.clientOutput,
                onNotification = { method, params -> notifications.add(method to params) },
                onClosed = { closedWith.complete(it) },
            )
    }

    @After
    fun tearDown() {
        connection.close()
    }

    @Test
    fun `request is framed as JSON-RPC 2 with params`() {
        connection.request("cs-ide/review", RpcJson.objectNode().put("path", "a.kt"))

        val message = peer.nextMessage()
        assertEquals("2.0", message.path("jsonrpc").asText())
        assertEquals("cs-ide/review", message.path("method").asText())
        assertEquals("a.kt", message.path("params").path("path").asText())
        assertTrue(message.path("id").isNumber)
    }

    @Test
    fun `responses are correlated by id`() {
        val first = connection.request("first", RpcJson.objectNode())
        val second = connection.request("second", RpcJson.objectNode())
        val firstId = peer.nextMessage().path("id").asLong()
        val secondId = peer.nextMessage().path("id").asLong()

        peer.reply(secondId, mapOf("name" to "second"))
        peer.reply(firstId, mapOf("name" to "first"))

        assertEquals("first", first.get(5, TimeUnit.SECONDS).path("name").asText())
        assertEquals("second", second.get(5, TimeUnit.SECONDS).path("name").asText())
    }

    @Test
    fun `error replies fail the request with the rpc code and message`() {
        val future = connection.request("cs-ide/refactor", RpcJson.objectNode())
        peer.replyError(peer.nextMessage().path("id").asLong(), "Request failed [401]")

        val error = assertFails(future)

        assertTrue(error is JsonRpcException)
        assertEquals(-32603, (error as JsonRpcException).rpcCode)
        assertEquals("Request failed [401]", error.message)
    }

    @Test
    fun `null result completes with a null node`() {
        val future = connection.request("cs-ide/delta", RpcJson.objectNode())
        peer.reply(peer.nextMessage().path("id").asLong(), null)

        assertTrue(future.get(5, TimeUnit.SECONDS).isNull)
    }

    @Test
    fun `notifications are dispatched`() {
        peer.notify("cs-ide/start", mapOf("sha" to "abc", "version" to "1.0"))

        val (method, params) = notifications.poll(5, TimeUnit.SECONDS)!!
        assertEquals("cs-ide/start", method)
        assertEquals("abc", params.path("sha").asText())
    }

    @Test
    fun `client notifications carry no id`() {
        connection.notify("cs-ide/reviewFiles", RpcJson.objectNode().put("repo-root", "/repo"))

        val message = peer.nextMessage()
        assertEquals("cs-ide/reviewFiles", message.path("method").asText())
        assertTrue(message.path("id").isMissingNode)
    }

    @Test
    fun `messages leave in submission order`() {
        connection.notify("one", RpcJson.objectNode())
        connection.request("two", RpcJson.objectNode())
        connection.notify("three", RpcJson.objectNode())

        val methods = (1..3).map { peer.nextMessage().path("method").asText() }
        assertEquals(listOf("one", "two", "three"), methods)
    }

    @Test
    fun `cancelling a request sends cancelRequest and ignores the late reply`() {
        val future = connection.request("cs-ide/refactor", RpcJson.objectNode())
        val id = peer.nextMessage().path("id").asLong()

        future.cancel(true)

        val cancel = peer.nextMessage()
        assertEquals("\$/cancelRequest", cancel.path("method").asText())
        assertEquals(id, cancel.path("params").path("id").asLong())
        peer.reply(id, mapOf("late" to true))
        assertTrue(future.isCancelled)
    }

    @Test
    fun `end of stream fails pending requests and reports closure`() {
        val future = connection.request("cs-ide/review", RpcJson.objectNode())
        peer.nextMessage()

        peer.closeOutput()

        assertTrue(assertFails(future) is JsonRpcException)
        assertNotNull(closedWith.get(5, TimeUnit.SECONDS))
    }

    @Test
    fun `server to client requests get a method not found reply`() {
        peer.send(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to 99,
                "method" to "workspace/configuration",
                "params" to emptyMap<String, Any>(),
            ),
        )

        val reply = peer.nextMessage()
        assertEquals(99, reply.path("id").asInt())
        assertEquals(-32601, reply.path("error").path("code").asInt())
    }

    @Test
    fun `requests after close fail immediately`() {
        connection.close()

        assertTrue(assertFails(connection.request("cs-ide/review", RpcJson.objectNode())) is JsonRpcException)
        assertNull(peer.pollMessage(200))
    }

    @Test
    fun `failPending fails every pending request with the given error`() {
        val future = connection.request("cs-ide/review", RpcJson.objectNode())
        peer.nextMessage()

        connection.failPending(IllegalStateException("server exited"))

        assertEquals("server exited", assertFails(future).message)
    }

    private fun assertFails(future: CompletableFuture<JsonNode>): Throwable =
        try {
            future.get(5, TimeUnit.SECONDS)
            throw AssertionError("Expected failure")
        } catch (e: ExecutionException) {
            e.cause!!
        }
}
