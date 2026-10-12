package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.TestLogger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeServerTest {
    @Test
    fun `first request starts the process and resolves after cs-ide start`() {
        val factory = FakeCsIdeProcessFactory { peer -> startPeer(peer, sendStart = true) }
        val server = server(factory)

        val future = server.request("cs-ide/device-id", RpcJson.objectNode())
        val peer = factory.currentPeer()
        val request = readRequest(peer)
        peer.writeToClient(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to request.path("id").asLong(),
                "result" to mapOf("deviceId" to "device-1"),
            ),
        )

        assertEquals("device-1", future.get(5, TimeUnit.SECONDS).path("deviceId").asText())
        assertEquals(1, factory.starts.get())
        assertEquals(CsIdeServerMetadata("abc-sha", "1.2.3"), server.metadata())
        server.close()
    }

    @Test
    fun `startup timeout fails with stderr tail and destroys the process`() {
        val factory =
            FakeCsIdeProcessFactory { peer ->
                peer.appendStderr("boom-from-stderr")
            }
        val server = server(factory, startupTimeoutMs = 200)

        val error = assertFails(server.request("cs-ide/review", RpcJson.objectNode()))

        assertTrue(error is CsIdeUnavailableException)
        assertTrue(error.message!!.contains("boom-from-stderr"))
        assertEquals(1, factory.starts.get())
        server.close()
    }

    @Test
    fun `process exit fails pending requests and respawns after backoff`() {
        val clock = AtomicLong(0)
        val backoff = CsIdeBackoff(initialMs = 50, maxMs = 50)
        val factory = FakeCsIdeProcessFactory { peer -> startPeer(peer, sendStart = true) }
        val server =
            server(
                factory,
                backoff = backoff,
                clock = { clock.get() },
            )

        server.request("cs-ide/device-id", RpcJson.objectNode()).let { ready ->
            val peer = factory.currentPeer()
            val id = readRequest(peer).path("id").asLong()
            peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "warm")))
            ready.get(5, TimeUnit.SECONDS)
        }

        val peer = factory.currentPeer()
        val pending = server.request("cs-ide/review", RpcJson.objectNode())
        readRequest(peer)
        factory.killCurrentProcess()
        assertFails(pending)

        clock.set(10)
        assertFails(server.request("cs-ide/review", RpcJson.objectNode()))
        assertEquals(1, factory.starts.get())

        clock.set(60)
        val second = server.request("cs-ide/device-id", RpcJson.objectNode())
        assertEquals(2, factory.starts.get())
        val peer2 = factory.currentPeer()
        val secondId = readRequest(peer2).path("id").asLong()
        peer2.writeToClient(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to secondId,
                "result" to mapOf("deviceId" to "again"),
            ),
        )
        assertEquals("again", second.get(5, TimeUnit.SECONDS).path("deviceId").asText())
        server.close()
    }

    @Test
    fun `cooldown fails fast without spawning`() {
        var now = 0L
        val clock = AtomicLong(now)
        val backoff = CsIdeBackoff(initialMs = 1_000, maxMs = 1_000)
        val factory = FakeCsIdeProcessFactory { peer -> startPeer(peer, sendStart = true) }
        val server = server(factory, backoff = backoff, clock = { clock.get() })

        server.request("cs-ide/device-id", RpcJson.objectNode()).let { future ->
            val peer = factory.currentPeer()
            val id = readRequest(peer).path("id").asLong()
            peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "x")))
            future.get(5, TimeUnit.SECONDS)
        }

        peerClose(factory.currentPeer())
        waitForTransportClose()
        assertEquals(1, factory.starts.get())

        assertFails(server.request("cs-ide/device-id", RpcJson.objectNode()))
        assertEquals(1, factory.starts.get())

        clock.set(1_500)
        server.request("cs-ide/device-id", RpcJson.objectNode()).let { future ->
            assertEquals(2, factory.starts.get())
            val peer = factory.currentPeer()
            val id = readRequest(peer).path("id").asLong()
            peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "y")))
            assertEquals("y", future.get(5, TimeUnit.SECONDS).path("deviceId").asText())
        }
        server.close()
    }

    @Test
    fun `successful response resets backoff`() {
        var now = 0L
        val clock = AtomicLong(now)
        val backoff = CsIdeBackoff(initialMs = 1_000, maxMs = 1_000)
        val factory = FakeCsIdeProcessFactory { peer -> startPeer(peer, sendStart = true) }
        val server = server(factory, backoff = backoff, clock = { clock.get() })

        backoff.recordFailure(clock.get())
        assertEquals(1_000, backoff.remainingMs(clock.get()))
        clock.set(1_001)

        val future = server.request("cs-ide/device-id", RpcJson.objectNode())
        val peer = factory.currentPeer()
        val id = readRequest(peer).path("id").asLong()
        peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "ok")))
        future.get(5, TimeUnit.SECONDS)

        assertEquals(0, backoff.remainingMs(clock.get()))
        server.close()
    }

    @Test
    fun `close closes stdin waits then destroys`() {
        val factory = FakeCsIdeProcessFactory { peer -> startPeer(peer, sendStart = true) }
        val server = server(factory, shutdownGraceMs = 50)
        server.request("cs-ide/device-id", RpcJson.objectNode()).let { future ->
            val peer = factory.currentPeer()
            val id = readRequest(peer).path("id").asLong()
            peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "z")))
            future.get(5, TimeUnit.SECONDS)
        }
        val process = factory.currentProcess()
        server.close()
        assertFalse(process.isAlive())
        server.close()
    }

    @Test
    fun `close during startup aborts the start and leaves no running process`() {
        val factory = FakeCsIdeProcessFactory()
        val server = server(factory, startupTimeoutMs = 10_000)
        val pending = CompletableFuture.supplyAsync { server.request("cs-ide/review", RpcJson.objectNode()) }
        awaitStarts(factory, 1)

        val startedAt = System.nanoTime()
        server.close()

        assertTrue(assertFails(pending.get(5, TimeUnit.SECONDS)) is CsIdeUnavailableException)
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) < 2_000)
        assertFalse(factory.currentProcess().isAlive())
    }

    @Test
    fun `process exit during startup fails fast`() {
        val factory = FakeCsIdeProcessFactory()
        val server = server(factory, startupTimeoutMs = 10_000)
        val pending = CompletableFuture.supplyAsync { server.request("cs-ide/review", RpcJson.objectNode()) }
        awaitStarts(factory, 1)

        val startedAt = System.nanoTime()
        factory.killCurrentProcess()

        val error = assertFails(pending.get(5, TimeUnit.SECONDS))
        assertTrue(error is CsIdeUnavailableException)
        assertTrue(error.message!!.contains("exited"))
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt) < 2_000)
        server.close()
    }

    @Test
    fun `requests after close fail`() {
        val factory = FakeCsIdeProcessFactory()
        val server = server(factory)
        server.close()
        assertFails(server.request("cs-ide/review", RpcJson.objectNode()))
    }

    @Test
    fun `stderr tail is exposed`() {
        val factory =
            FakeCsIdeProcessFactory { peer ->
                peer.appendStderr("line-one\n")
            }
        val server = server(factory, startupTimeoutMs = 200)
        runCatching { server.request("cs-ide/review", RpcJson.objectNode()).get(1, TimeUnit.SECONDS) }
        assertTrue(server.stderrTail().contains("line-one"))
        server.close()
    }

    private fun server(
        factory: FakeCsIdeProcessFactory,
        startupTimeoutMs: Long = 5_000,
        shutdownGraceMs: Long = 50,
        backoff: CsIdeBackoff = CsIdeBackoff(),
        clock: () -> Long = System::currentTimeMillis,
    ): CsIdeServer =
        CsIdeServer(
            processFactory = factory,
            logger = TestLogger,
            startupTimeoutMs = startupTimeoutMs,
            shutdownGraceMs = shutdownGraceMs,
            backoff = backoff,
            clock = clock,
        )

    private fun startPeer(
        peer: FakeCsIdePeer,
        sendStart: Boolean,
    ) {
        if (!sendStart) return
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

    private fun readRequest(peer: FakeCsIdePeer): com.fasterxml.jackson.databind.JsonNode {
        var message: com.fasterxml.jackson.databind.JsonNode? = null
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (message == null && System.nanoTime() < deadline) {
            message = peer.readClientMessage()
            if (message == null) Thread.sleep(5)
        }
        return message ?: throw AssertionError("No request from client")
    }

    private fun peerClose(peer: FakeCsIdePeer) {
        peer.closeClientInput()
    }

    private fun waitForTransportClose() {
        Thread.sleep(100)
    }

    private fun awaitStarts(
        factory: FakeCsIdeProcessFactory,
        count: Int,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (factory.starts.get() < count && System.nanoTime() < deadline) Thread.sleep(5)
        assertEquals(count, factory.starts.get())
    }

    private fun assertFails(future: CompletableFuture<*>): Throwable {
        try {
            future.get(5, TimeUnit.SECONDS)
            throw AssertionError("Expected failure")
        } catch (e: ExecutionException) {
            assertNotNull(e.cause)
            return e.cause!!
        }
    }
}
