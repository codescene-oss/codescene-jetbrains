package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchRegistryTest {
    private var now = 0L

    private fun registry(windows: Boolean = false): WatchRegistry = WatchRegistry(windows) { now }

    private fun ack(
        registry: WatchRegistry,
        commands: List<WatchCommand>,
        session: Long = 1L,
    ) {
        commands.filterIsInstance<WatchCommand.Watch>().forEach {
            registry.sent(it.repoRoot, it.relativePaths, session)
        }
    }

    @Test
    fun `the first project emits one watch per root`() {
        val registry = registry()
        val commands = registry.setProject("p1", mapOf("/a" to listOf("."), "/b" to listOf("src")))
        assertEquals(
            listOf(WatchCommand.Watch("/a", listOf(".")), WatchCommand.Watch("/b", listOf("src"))),
            commands,
        )
    }

    @Test
    fun `unchanged scopes after sent emit nothing`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/r" to listOf("src"))))
        assertEquals(emptyList<WatchCommand>(), registry.setProject("p1", mapOf("/r" to listOf("src"))))
    }

    @Test
    fun `a second project on the same root sends the union`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/r" to listOf("a"))))
        var commands = registry.setProject("p2", mapOf("/r" to listOf("b")))
        assertEquals(listOf(WatchCommand.Watch("/r", listOf("a", "b"))), commands)
        ack(registry, commands)
        commands = registry.setProject("p3", mapOf("/r" to listOf(".")))
        assertEquals(listOf(WatchCommand.Watch("/r", listOf("."))), commands)
        ack(registry, commands)
        commands = registry.setProject("p4", mapOf("/r" to listOf("a/x")))
        assertEquals(emptyList<WatchCommand>(), commands)
    }

    @Test
    fun `Windows root keys ignore case and slashes`() {
        val windowsRegistry = registry(true)
        ack(windowsRegistry, windowsRegistry.setProject("p1", mapOf("C:\\Repo" to listOf("src"))))
        val union =
            windowsRegistry.setProject(
                "p2",
                mapOf("c:/repo/" to listOf("lib")),
            )
        assertEquals(listOf(WatchCommand.Watch("C:\\Repo", listOf("lib", "src"))), union)

        val unixRegistry = registry(false)
        ack(unixRegistry, unixRegistry.setProject("p1", mapOf("/Repo" to listOf("a"))))
        assertEquals(
            listOf(WatchCommand.Watch("/repo", listOf("b"))),
            unixRegistry.setProject("p2", mapOf("/repo" to listOf("b"))),
        )
    }

    @Test
    fun `removing one of two projects shrinks the union`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/r" to listOf("a"))))
        val union = registry.setProject("p2", mapOf("/r" to listOf("b")))
        assertEquals(listOf(WatchCommand.Watch("/r", listOf("a", "b"))), union)
        ack(registry, union)
        assertEquals(listOf(WatchCommand.Watch("/r", listOf("a"))), registry.removeProject("p2"))
    }

    @Test
    fun `removing the last project stops a sent root and not an unsent one`() {
        val registry = registry()
        val initial = registry.setProject("p1", mapOf("/r" to listOf("a"), "/never" to listOf("x")))
        assertEquals(
            listOf(WatchCommand.Watch("/r", listOf("a")), WatchCommand.Watch("/never", listOf("x"))),
            initial,
        )
        registry.sent("/r", listOf("a"), 1)
        assertEquals(listOf(WatchCommand.Stop("/r")), registry.removeProject("p1"))
    }

    @Test
    fun `a scope change that drops a root stops it`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/keep" to listOf("a"), "/drop" to listOf("b"))))
        assertEquals(listOf(WatchCommand.Stop("/drop")), registry.setProject("p1", mapOf("/keep" to listOf("a"))))
    }

    @Test
    fun `sessionStarted re-emits roots sent in another session`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/a" to listOf("."), "/b" to listOf("src"))))
        assertEquals(
            listOf(WatchCommand.Watch("/a", listOf(".")), WatchCommand.Watch("/b", listOf("src"))),
            registry.sessionStarted(2),
        )
        registry.sent("/a", listOf("."), 2)
        assertEquals(listOf(WatchCommand.Watch("/b", listOf("src"))), registry.sessionStarted(2))
    }

    @Test
    fun `pending with no live session emits every desired root and skips roots sent in the live session`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/a" to listOf("."), "/b" to listOf("src"))))
        assertEquals(
            listOf(WatchCommand.Watch("/a", listOf(".")), WatchCommand.Watch("/b", listOf("src"))),
            registry.pending(0),
        )
        registry.sent("/a", listOf("."), 1)
        registry.sent("/b", listOf("src"), 1)
        assertEquals(emptyList<WatchCommand>(), registry.pending(1))
        assertEquals(
            listOf(WatchCommand.Watch("/a", listOf(".")), WatchCommand.Watch("/b", listOf("src"))),
            registry.pending(2),
        )
    }

    @Test
    fun `lost re-watches once per minute`() {
        val registry = registry()
        ack(registry, registry.setProject("p1", mapOf("/r" to listOf("."))))
        now = 0
        assertEquals(listOf(WatchCommand.Watch("/r", listOf("."))), registry.lost("/r"))
        now = 59_999
        assertEquals(emptyList<WatchCommand>(), registry.lost("/r"))
        now = 60_000
        assertEquals(listOf(WatchCommand.Watch("/r", listOf("."))), registry.lost("/r"))
        assertEquals(emptyList<WatchCommand>(), registry.lost("/unknown"))
    }

    @Test
    fun `retryDelayMs clamps to 2 to 30 seconds`() {
        assertEquals(2_000L, WatchRegistry.retryDelayMs(0))
        assertEquals(5_000L, WatchRegistry.retryDelayMs(5_000))
        assertEquals(30_000L, WatchRegistry.retryDelayMs(60_000))
    }

    @Test
    fun `hasDesired is false after every project is removed`() {
        val registry = registry()
        assertFalse(registry.hasDesired())
        ack(registry, registry.setProject("p1", mapOf("/r" to listOf("."))))
        assertTrue(registry.hasDesired())
        registry.removeProject("p1")
        assertFalse(registry.hasDesired())
    }
}
