package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class WatchInventoryParityLiveTest {
    private val tempRoots = mutableListOf<Path>()

    @After
    fun tearDown() {
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun `origin default merge base only when main and develop exist`() {
        val fixture = freshFixture()
        fixture.write("base.ts", TS_BODY)
        fixture.commitAll("base on main")
        fixture.setOriginHead("main")
        fixture.checkoutNew("develop")
        fixture.write("develop-only.ts", TS_BODY)
        fixture.commitAll("develop only")
        fixture.checkoutNew("feature-stacked")
        fixture.write("stacked.ts", TS_BODY)
        fixture.commitAll("stacked")

        assertInventoryParity(fixture, setOf("develop-only.ts", "stacked.ts"))
    }

    @Test
    fun `committed files via merge-base diff on feature branch`() {
        val fixture = freshFixture()
        fixture.write("base.ts", TS_BODY)
        fixture.commitAll("base on main")
        fixture.setOriginHead("main")
        fixture.checkoutNew("feature")
        fixture.write("committed-only.ts", TS_BODY)
        fixture.commitAll("feature commit")

        assertInventoryParity(fixture, setOf("committed-only.ts"))
    }

    @Test
    fun `combines status and diff changes on feature branch`() {
        val fixture = freshFixture()
        fixture.write("base.ts", TS_BODY)
        fixture.commitAll("base on main")
        fixture.setOriginHead("main")
        fixture.checkoutNew("feature")
        fixture.write("committed.ts", TS_BODY)
        fixture.commitAll("committed")
        fixture.write("uncommitted.ts", TS_BODY)

        assertInventoryParity(fixture, setOf("committed.ts", "uncommitted.ts"))
    }

    @Test
    fun `untracked file in the repository root`() {
        val fixture = freshFixture()
        fixture.write("base.ts", TS_BODY)
        fixture.commitAll("base on main")
        fixture.write("test.ts", TS_BODY)

        assertInventoryParity(fixture, setOf("test.ts"))
    }

    @Test
    fun `modified tracked file on the main line and deleted file excluded`() {
        val fixture = freshFixture()
        fixture.write("a.ts", TS_BODY)
        fixture.write("b.ts", TS_BODY)
        fixture.commitAll("a and b")
        fixture.write("a.ts", TS_BODY + "\n")
        fixture.delete("b.ts")

        assertInventoryParity(fixture, setOf("a.ts"))
    }

    @Test
    fun `many untracked files in one directory are all listed`() {
        val fixture = freshFixture()
        fixture.write("base.ts", TS_BODY)
        fixture.commitAll("base on main")
        for (i in 1..10) {
            fixture.write("untracked$i.ts", TS_BODY)
        }

        val expected = (1..10).map { "untracked$it.ts" }.toSet()
        assertInventoryParity(fixture, expected)
    }

    private fun assertInventoryParity(
        fixture: WatchGitFixture,
        expected: Set<String>,
    ) {
        val received = LinkedBlockingQueue<Pair<String, JsonNode>>()
        val registration = server.addNotificationHandler { method, params -> received.add(method to params) }
        val repoRoot = fixture.root.toString()
        val client = CsIdeLiveServer.watchClient(server)
        var stopped = false
        try {
            client.watchFiles(repoRoot, listOf("."))

            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120)
            val pushed = CsIdeLiveServer.awaitInventory(received, deadline, repoRoot, server::stderrTail)
            assertEquals("push inventory", expected, pushed.files)

            val fetched = client.getWatchInventory(repoRoot).get(60, TimeUnit.SECONDS)
            assertEquals("getWatchInventory", expected, fetched.files)

            assertTrue(client.stopWatchFiles(repoRoot))
            stopped = true
        } finally {
            if (!stopped) {
                client.stopWatchFiles(repoRoot)
            }
            registration.close()
        }
    }

    private fun freshFixture(): WatchGitFixture {
        val root = Files.createTempDirectory("codescene-watch-parity").toRealPath()
        tempRoots.add(root)
        return WatchGitFixture(root).also { it.init() }
    }

    companion object {
        private const val TS_BODY = "export const x = 1;\n"
        private lateinit var server: CsIdeServer

        @BeforeClass
        @JvmStatic
        fun startServer() {
            server = CsIdeLiveServer.start()
        }

        @AfterClass
        @JvmStatic
        fun stopServer() {
            CsIdeLiveServer.stop(server)
        }
    }
}
