package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.TestLogger
import com.fasterxml.jackson.databind.JsonNode
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

object CsIdeLiveServer {
    private var lastPid: Long = 0

    fun start(): CsIdeServer {
        val launchSpec = {
            CsIdeLaunch.spec(
                CsIdeLaunchEnvironment(),
                javaHomeForServer(),
                CsIdeLaunch.extensionApiJar(),
                CsIdeLaunch.defaultThreads(Runtime.getRuntime().availableProcessors()),
                System.getProperty("os.name"),
                System.getenv(),
            )
        }
        val delegate = SystemCsIdeProcessFactory(launchSpec)
        val server =
            CsIdeServer(CsIdeProcessFactory { delegate.start().also { lastPid = it.pid } }, TestLogger)
        assertTrue(CsIdeClient(CsIdeTransport(server::request)).deviceId().isNotBlank())
        return server
    }

    fun stop(server: CsIdeServer) {
        val pid = lastPid
        server.close()
        if (pid != 0L) awaitProcessDead(pid)
    }

    fun windows(): Boolean = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)

    fun watchClient(server: CsIdeServer): WatchClient =
        WatchClient(server::notifyInSession, server::notifyIfRunning, CsIdeTransport(server::request))

    fun <T : Any> awaitNotice(
        received: LinkedBlockingQueue<Pair<String, JsonNode>>,
        deadline: Long,
        expectedMethod: String,
        stderr: () -> String,
        parse: (JsonNode) -> T?,
        accept: (T) -> Boolean,
    ): T {
        val tracker = NoticeAcceptTracker(accept)
        while (true) {
            val remaining = deadline - System.nanoTime()
            val (method, params) =
                received.poll(remaining.coerceAtLeast(0), TimeUnit.NANOSECONDS)
                    ?: throw AssertionError(
                        awaitNoticeTimeoutMessage(expectedMethod, tracker.lastRejected, stderr),
                    )
            if (method != expectedMethod) continue
            parse(params)?.takeIf(tracker::accept)?.let { return it }
        }
    }

    fun awaitInventory(
        received: LinkedBlockingQueue<Pair<String, JsonNode>>,
        deadline: Long,
        repoRoot: String,
        stderr: () -> String,
    ): WatchInventory =
        awaitNotice(received, deadline, WatchNotices.INVENTORY_CHANGED, stderr, WatchNotices::inventory) {
            WorkspacePaths.sameRoot(repoRoot, it.repoRoot, windows())
        }

    private class NoticeAcceptTracker<T : Any>(private val predicate: (T) -> Boolean) {
        var lastRejected: T? = null

        fun accept(candidate: T): Boolean {
            if (predicate(candidate)) return true
            lastRejected = candidate
            return false
        }
    }

    private fun awaitNoticeTimeoutMessage(
        expectedMethod: String,
        lastRejected: Any?,
        stderr: () -> String,
    ): String =
        buildString {
            append("No matching $expectedMethod within timeout")
            lastRejected?.let { append("; last rejected: $it") }
            append("; stderr: ${stderr()}")
        }

    private fun javaHomeForServer(): Path {
        val override = System.getProperty("csIde.java")?.trim()?.takeIf { it.isNotEmpty() }
        if (override != null) return Path.of(override)
        val home = Path.of(System.getProperty("java.home"))
        return if (home.fileName.toString().equals("jre", ignoreCase = true)) home.parent else home
    }

    private fun awaitProcessDead(pid: Long) {
        val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15)
        while (System.currentTimeMillis() < deadline) {
            val alive = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)
            if (!alive) return
            Thread.sleep(100)
        }
        val stillAlive = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)
        assertFalse("cs-ide process $pid survived server close", stillAlive)
    }
}
