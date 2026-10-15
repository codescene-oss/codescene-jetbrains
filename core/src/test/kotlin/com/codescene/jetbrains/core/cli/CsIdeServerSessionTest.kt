package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.TestLogger
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeServerSessionTest {
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
    fun `notifyInSession returns 1 for the first session and 2 after a respawn`() {
        val params = RpcJson.objectNode().put("repo-root", "/repo")

        assertEquals(1, server.notifyInSession("cs-ide/reviewFiles", params))
        factory.currentPeer().readClientMessage()

        factory.killCurrentProcess()
        awaitCondition { server.liveSession() == 0L }
        clock.set(101)

        assertEquals(2, server.notifyInSession("cs-ide/reviewFiles", params))
        assertEquals(2, factory.starts.get())
    }

    @Test
    fun `liveSession is 0 before start, the session id while running, and 0 after close`() {
        assertEquals(0, server.liveSession())

        server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()
        assertEquals(1, server.liveSession())

        server.close()
        assertEquals(0, server.liveSession())
    }

    @Test
    fun `start listeners receive each session id after metadata is set`() {
        val records = CopyOnWriteArrayList<Pair<Long, CsIdeServerMetadata?>>()
        server.addSessionStartListener { id -> records.add(id to server.metadata()) }

        server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()

        factory.killCurrentProcess()
        awaitCondition { server.liveSession() == 0L }
        clock.set(101)

        server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()

        val expected = CsIdeServerMetadata("abc-sha", "1.2.3")
        assertEquals(listOf(1L to expected, 2L to expected), records.toList())
    }

    @Test
    fun `a start listener runs before notification handlers see cs-ide start`() {
        val order = CopyOnWriteArrayList<String>()
        server.addSessionStartListener { order.add("session $it") }
        server.addNotificationHandler { method, _ -> order.add(method) }

        server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()

        assertEquals(listOf("session 1", "cs-ide/start"), order.toList())
    }

    @Test
    fun `a throwing start listener does not block other listeners or startup`() {
        val received = AtomicInteger(0)
        server.addSessionStartListener { error("listener failed") }
        server.addSessionStartListener { received.set(it.toInt()) }

        assertEquals(1, server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode()))
        factory.currentPeer().readClientMessage()

        assertEquals(1, received.get())
    }

    @Test
    fun `a closed start listener registration is not called`() {
        val calls = AtomicInteger(0)
        server.addSessionStartListener { calls.incrementAndGet() }.close()

        server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()

        assertEquals(0, calls.get())
    }

    @Test
    fun `notifyIfRunning without a server returns false and starts nothing`() {
        val ok =
            server.notifyIfRunning(
                "cs-ide/stopWatchFiles",
                RpcJson.objectNode().put("repo-root", "/repo"),
            )

        assertFalse(ok)
        assertEquals(0, factory.starts.get())
    }

    @Test
    fun `notifyIfRunning writes a frame without an id when a session is live`() {
        val params = RpcJson.objectNode().put("repo-root", "/repo")
        server.notifyInSession("cs-ide/reviewFiles", RpcJson.objectNode())
        factory.currentPeer().readClientMessage()

        assertTrue(server.notifyIfRunning("cs-ide/stopWatchFiles", params))

        val frame = factory.currentPeer().readClientMessage()!!
        assertEquals("cs-ide/stopWatchFiles", frame.path("method").asText())
        assertEquals(params, frame.path("params"))
        assertFalse(frame.has("id"))
    }

    @Test
    fun `notifyIfRunning returns false after close and during cooldown without spawning`() {
        backoff.recordFailure(clock.get())
        val params = RpcJson.objectNode().put("repo-root", "/repo")

        assertFalse(server.notifyIfRunning("cs-ide/stopWatchFiles", params))
        assertEquals(0, factory.starts.get())

        server.close()
        assertFalse(server.notifyIfRunning("cs-ide/stopWatchFiles", params))
    }

    @Test
    fun `cooldownRemainingMs is 0 initially and follows the backoff`() {
        assertEquals(0, server.cooldownRemainingMs())

        backoff.recordFailure(0)
        clock.set(40)
        assertEquals(60, server.cooldownRemainingMs())
    }

    @Test
    fun `notify still throws CsIdeUnavailableException during cooldown`() {
        backoff.recordFailure(clock.get())

        val error = runCatching { server.notify("cs-ide/reviewFiles", RpcJson.objectNode()) }.exceptionOrNull()

        assertTrue(error is CsIdeUnavailableException)
        assertEquals(0, factory.starts.get())
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
