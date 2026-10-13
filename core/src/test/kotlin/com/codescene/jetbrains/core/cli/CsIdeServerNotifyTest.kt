package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.TestLogger
import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeServerNotifyTest {
    private val factory = FakeCsIdeProcessFactory { peer -> sendStart(peer) }
    private val clock = AtomicLong(0)
    private val backoff = CsIdeBackoff(initialMs = 100, maxMs = 10_000)
    private val server =
        CsIdeServer(
            processFactory = factory,
            logger = TestLogger,
            startupTimeoutMs = 5_000,
            shutdownGraceMs = 50,
            backoff = backoff,
            clock = { clock.get() },
        )

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `notify writes a frame without an id after cs-ide start`() {
        val params = RpcJson.objectNode().put("repo-root", "/repo")

        server.notify("cs-ide/reviewFiles", params)
        val frame = factory.currentPeer().readClientMessage()!!

        assertEquals("cs-ide/reviewFiles", frame.path("method").asText())
        assertEquals(params, frame.path("params"))
        assertFalse(frame.has("id"))
        assertEquals(CsIdeServerMetadata("abc-sha", "1.2.3"), server.metadata())
    }

    @Test
    fun `notify resets the backoff after a successful write`() {
        backoff.recordFailure(clock.get())
        clock.set(101)

        start()
        backoff.recordFailure(clock.get())

        assertEquals(100, backoff.remainingMs(clock.get()))
    }

    @Test
    fun `notify during cooldown throws without spawning`() {
        backoff.recordFailure(clock.get())

        val error = runCatching { server.notify("cs-ide/reviewFiles", RpcJson.objectNode()) }.exceptionOrNull()

        assertTrue(error is CsIdeUnavailableException)
        assertEquals(0, factory.starts.get())
    }

    @Test
    fun `notify after close throws`() {
        server.close()

        val error = runCatching { server.notify("cs-ide/reviewFiles", RpcJson.objectNode()) }.exceptionOrNull()

        assertTrue(error is CsIdeUnavailableException)
        assertEquals(0, factory.starts.get())
    }

    @Test
    fun `handlers receive cs-ide start and later notifications`() {
        val first = recorder()
        val second = recorder()

        start()
        sendFileReview()

        assertEquals("abc-sha", awaitMethod(first, "cs-ide/start").path("sha").asText())
        assertEquals("src/App.kt", awaitMethod(first, "cs-ide/fileReview").path("path").asText())
        assertEquals("src/App.kt", awaitMethod(second, "cs-ide/fileReview").path("path").asText())
    }

    @Test
    fun `closing a registration stops only that handler`() {
        val removed = CopyOnWriteArrayList<String>()
        val registration = server.addNotificationHandler { method, _ -> removed.add(method) }
        val kept = recorder()

        start()
        registration.close()
        sendFileReview()

        awaitMethod(kept, "cs-ide/fileReview")
        assertEquals(listOf("cs-ide/start"), removed.toList())
    }

    @Test
    fun `a throwing handler does not block other handlers or the next request`() {
        server.addNotificationHandler { _, _ -> error("handler failed") }
        val received = recorder()

        start()
        sendFileReview()
        awaitMethod(received, "cs-ide/fileReview")

        val future = server.request("cs-ide/device-id", RpcJson.objectNode())
        val peer = factory.currentPeer()
        val id = peer.readClientMessage()!!.path("id").asLong()
        peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "ok")))

        assertEquals("ok", future.get(5, TimeUnit.SECONDS).path("deviceId").asText())
    }

    @Test
    fun `killing the process runs the session listener once outside the server lock`() {
        val calls = AtomicInteger(0)
        val lockFree = CompletableFuture<Boolean>()
        server.addSessionListener {
            calls.incrementAndGet()
            val tail = CompletableFuture.supplyAsync { server.stderrTail() }
            lockFree.complete(runCatching { tail.get(1, TimeUnit.SECONDS) }.isSuccess)
        }

        start()
        factory.killCurrentProcess()

        assertTrue(lockFree.get(5, TimeUnit.SECONDS))
        Thread.sleep(100)
        assertEquals(1, calls.get())
    }

    @Test
    fun `transport close runs the session listener`() {
        val calls = AtomicInteger(0)
        server.addSessionListener { calls.incrementAndGet() }

        start()
        factory.currentPeer().closeClientInput()

        awaitCondition { calls.get() == 1 }
    }

    @Test
    fun `close runs the session listener`() {
        val calls = AtomicInteger(0)
        server.addSessionListener { calls.incrementAndGet() }

        start()
        server.close()

        assertEquals(1, calls.get())
    }

    @Test
    fun `closed session listener registration is not called`() {
        val calls = AtomicInteger(0)
        server.addSessionListener { calls.incrementAndGet() }.close()

        start()
        server.close()

        assertEquals(0, calls.get())
    }

    private fun start() {
        server.notify("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()
    }

    private fun recorder(): LinkedBlockingQueue<Pair<String, JsonNode>> {
        val queue = LinkedBlockingQueue<Pair<String, JsonNode>>()
        server.addNotificationHandler { method, params -> queue.add(method to params) }
        return queue
    }

    private fun awaitMethod(
        queue: LinkedBlockingQueue<Pair<String, JsonNode>>,
        method: String,
    ): JsonNode {
        while (true) {
            val (received, params) =
                queue.poll(5, TimeUnit.SECONDS) ?: throw AssertionError("No $method notification")
            if (received == method) return params
        }
    }

    private fun sendFileReview() {
        factory.currentPeer().writeToClient(
            mapOf(
                "jsonrpc" to "2.0",
                "method" to "cs-ide/fileReview",
                "params" to mapOf("path" to "src/App.kt", "repoRoot" to "/repo"),
            ),
        )
    }

    private fun sendStart(peer: FakeCsIdePeer) {
        Thread({
            peer.writeToClient(
                mapOf(
                    "jsonrpc" to "2.0",
                    "method" to "cs-ide/start",
                    "params" to mapOf("sha" to "abc-sha", "version" to "1.2.3"),
                ),
            )
        }, "fake-start").apply {
            isDaemon = true
            start()
        }
    }

    private fun awaitCondition(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue(condition())
    }
}
