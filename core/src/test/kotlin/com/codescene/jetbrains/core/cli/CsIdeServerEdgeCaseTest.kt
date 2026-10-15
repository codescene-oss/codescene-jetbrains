package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.TestLogger
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeServerEdgeCaseTest {
    @Test
    fun `process that is dead after cs-ide start fails the start`() {
        val factory =
            CsIdeProcessFactory {
                val peer = FakeCsIdePeer()
                peer.sendStart()
                object : CsIdeProcess by FakeCsIdeProcess(peer) {
                    override fun isAlive(): Boolean = false
                }
            }
        val server = CsIdeServer(factory, TestLogger, startupTimeoutMs = 5_000)

        val error = failure(server.request("cs-ide/review", RpcJson.objectNode()))

        assertTrue(error is CsIdeUnavailableException)
        assertTrue(error.message!!.contains("exited during startup"))
        server.close()
    }

    @Test
    fun `stderr tail keeps only the most recent output`() {
        val factory = FakeCsIdeProcessFactory { peer -> peer.appendStderr("a".repeat(5_000) + "END") }
        val server = CsIdeServer(factory, TestLogger, startupTimeoutMs = 200)

        failure(server.request("cs-ide/review", RpcJson.objectNode()))
        awaitCondition { server.stderrTail().endsWith("END") }

        assertEquals(4_000, server.stderrTail().length)
        server.close()
    }

    @Test
    fun `stderr read failure does not stop the server`() {
        val factory =
            FakeCsIdeProcessFactory { peer ->
                peer.processStderr.close()
                peer.sendStart()
            }
        val server = CsIdeServer(factory, TestLogger, startupTimeoutMs = 5_000)

        val future = server.request("cs-ide/device-id", RpcJson.objectNode())
        val peer = factory.currentPeer()
        val id = peer.readClientMessage()!!.path("id").asLong()
        peer.writeToClient(mapOf("jsonrpc" to "2.0", "id" to id, "result" to mapOf("deviceId" to "ok")))

        assertEquals("ok", future.get(5, TimeUnit.SECONDS).path("deviceId").asText())
        assertEquals("", server.stderrTail())
        server.close()
    }

    private fun FakeCsIdePeer.sendStart() {
        Thread({
            writeToClient(
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

    private fun failure(future: CompletableFuture<*>): Throwable =
        try {
            future.get(5, TimeUnit.SECONDS)
            throw AssertionError("Expected failure")
        } catch (e: ExecutionException) {
            e.cause!!
        }
}
