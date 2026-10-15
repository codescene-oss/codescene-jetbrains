package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.contracts.ILogger
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeServerExitLogTest {
    @Test
    fun `stream close still logs the later process exit code once`() {
        val logger = RecordingLogger()
        val clock = AtomicLong(0)
        val backoff = CsIdeBackoff(initialMs = 1_000, maxMs = 4_000)
        val lost = AtomicInteger(0)
        val factory =
            FakeCsIdeProcessFactory { peer ->
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
        val server =
            CsIdeServer(
                processFactory = factory,
                logger = logger,
                startupTimeoutMs = 5_000,
                shutdownGraceMs = 50,
                backoff = backoff,
                clock = { clock.get() },
            )
        server.addSessionListener { lost.incrementAndGet() }
        val ready = server.request("cs-ide/device-id", RpcJson.objectNode())
        val peer = factory.currentPeer()
        val request = readRequest(peer)
        peer.writeToClient(
            mapOf(
                "jsonrpc" to "2.0",
                "id" to request.path("id").asLong(),
                "result" to mapOf("deviceId" to "device-1"),
            ),
        )
        assertEquals("device-1", ready.get(5, TimeUnit.SECONDS).path("deviceId").asText())
        factory.currentProcess().completeOnDestroy = false
        peer.closeClientInput()
        waitUntil { lost.get() == 1 }
        assertEquals(1, lost.get())
        factory.currentProcess().completeExit(137)
        waitUntil { logger.warnings.any { it == "cs-ide server exited with code 137" } }
        assertTrue(logger.warnings.any { it == "cs-ide JSON-RPC closed: JSON-RPC stream ended" })
        assertEquals(1, lost.get())
        assertEquals(1_000, backoff.remainingMs(0))
        server.close()
    }

    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(5)
        assertTrue(condition())
    }

    private class RecordingLogger : ILogger {
        val warnings = mutableListOf<String>()

        override fun info(
            message: String,
            service: String?,
        ) {}

        override fun warn(
            message: String,
            service: String?,
        ) {
            warnings += message
        }

        override fun debug(
            message: String,
            service: String?,
        ) {}

        override fun error(
            message: String,
            service: String?,
        ) {}
    }
}
