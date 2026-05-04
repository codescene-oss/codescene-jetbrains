package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileEventType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VfsEventBridgeTest {
    private lateinit var project: Project
    private lateinit var observer: GitChangeObserverAdapter
    private lateinit var bridge: VfsEventBridge

    private val workspacePath = "/test/workspace"

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        observer = mockk(relaxed = true)
        bridge = VfsEventBridge(project, workspacePath, observer)
    }

    @Test
    fun `convertEvent converts VFileCreateEvent to CREATE`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "$workspacePath/new-file.kt"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.CREATE, result?.type)
        assertEquals("$workspacePath/new-file.kt", result?.path)
    }

    @Test
    fun `convertEvent converts VFileDeleteEvent to DELETE`() {
        val event = mockk<VFileDeleteEvent>()
        every { event.path } returns "$workspacePath/deleted-file.kt"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.DELETE, result?.type)
        assertEquals("$workspacePath/deleted-file.kt", result?.path)
    }

    @Test
    fun `convertEvent converts VFileContentChangeEvent to CHANGE`() {
        val event = mockk<VFileContentChangeEvent>()
        every { event.path } returns "$workspacePath/changed-file.kt"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.CHANGE, result?.type)
        assertEquals("$workspacePath/changed-file.kt", result?.path)
    }

    @Test
    fun `convertEvent returns null for VFileMoveEvent`() {
        val event = mockk<VFileMoveEvent>()
        every { event.path } returns "$workspacePath/moved-file.kt"

        val result = bridge.convertEvent(event)

        assertNull(result)
    }

    @Test
    fun `convertEvent returns null for VFilePropertyChangeEvent`() {
        val event = mockk<VFilePropertyChangeEvent>()
        every { event.path } returns "$workspacePath/renamed-file.kt"

        val result = bridge.convertEvent(event)

        assertNull(result)
    }

    @Test
    fun `convertEvent filters paths outside project workspace`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "/other/path/file.kt"

        val result = bridge.convertEvent(event)

        assertNull(result)
    }

    @Test
    fun `isWithinWorkspace returns true for paths inside workspace`() {
        assertTrue(bridge.isWithinWorkspace("$workspacePath/src/file.kt"))
        assertTrue(bridge.isWithinWorkspace("$workspacePath/deeply/nested/path/file.kt"))
    }

    @Test
    fun `isWithinWorkspace returns true for workspace path itself`() {
        assertTrue(bridge.isWithinWorkspace(workspacePath))
    }

    @Test
    fun `isWithinWorkspace returns false for paths outside workspace`() {
        assertFalse(bridge.isWithinWorkspace("/other/path/file.kt"))
        assertFalse(bridge.isWithinWorkspace("/test/workspaceOther/file.kt"))
    }

    @Test
    fun `isWithinWorkspace handles workspace path with trailing slash`() {
        val bridgeWithSlash = VfsEventBridge(project, "$workspacePath/", observer)
        assertTrue(bridgeWithSlash.isWithinWorkspace("$workspacePath/src/file.kt"))
    }

    @Test
    fun `isWithinWorkspace handles Windows separator differences`() {
        val windowsBridge = VfsEventBridge(project, "C:\\repo", observer)
        assertTrue(windowsBridge.isWithinWorkspace("C:/repo/src/file.kt"))
        assertTrue(windowsBridge.isWithinWorkspace("c:/repo/src/file.kt"))
        assertFalse(windowsBridge.isWithinWorkspace("C:/repo-other/src/file.kt"))
    }

    @Test
    fun `start subscribes to MessageBus VFS_CHANGES`() {
        val messageBus = mockk<MessageBus>(relaxed = true)
        val connection = mockk<MessageBusConnection>(relaxed = true)

        every { project.messageBus } returns messageBus
        every { messageBus.connect(bridge) } returns connection

        bridge.start()

        verify(exactly = 1) { messageBus.connect(bridge) }
        verify(exactly = 1) { connection.subscribe(any(), any()) }
    }

    @Test
    fun `batch event processing queues all valid events`() {
        val createEvent1 = mockk<VFileCreateEvent>()
        val createEvent2 = mockk<VFileCreateEvent>()
        val changeEvent = mockk<VFileContentChangeEvent>()
        val deleteEvent = mockk<VFileDeleteEvent>()
        val moveEvent = mockk<VFileMoveEvent>()

        every { createEvent1.path } returns "$workspacePath/file1.kt"
        every { createEvent2.path } returns "$workspacePath/file2.kt"
        every { changeEvent.path } returns "$workspacePath/file3.kt"
        every { deleteEvent.path } returns "$workspacePath/file4.kt"
        every { moveEvent.path } returns "$workspacePath/file5.kt"

        val events = listOf(createEvent1, createEvent2, changeEvent, deleteEvent, moveEvent)

        for (event in events) {
            bridge.convertEvent(event)?.let { observer.queueEvent(it) }
        }

        verify(exactly = 1) {
            observer.queueEvent(match { it.path == "$workspacePath/file1.kt" && it.type == FileEventType.CREATE })
        }
        verify(exactly = 1) {
            observer.queueEvent(match { it.path == "$workspacePath/file2.kt" && it.type == FileEventType.CREATE })
        }
        verify(exactly = 1) {
            observer.queueEvent(match { it.path == "$workspacePath/file3.kt" && it.type == FileEventType.CHANGE })
        }
        verify(exactly = 1) {
            observer.queueEvent(match { it.path == "$workspacePath/file4.kt" && it.type == FileEventType.DELETE })
        }
        verify(exactly = 0) { observer.queueEvent(match { it.path == "$workspacePath/file5.kt" }) }
    }

    @Test
    fun `dispose disconnects MessageBusConnection`() {
        val messageBus = mockk<MessageBus>(relaxed = true)
        val connection = mockk<MessageBusConnection>(relaxed = true)

        every { project.messageBus } returns messageBus
        every { messageBus.connect(bridge) } returns connection

        bridge.start()
        bridge.dispose()

        verify(exactly = 1) { connection.disconnect() }
    }

    @Test
    fun `convertEvent handles paths with special characters`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "$workspacePath/file with spaces.kt"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.CREATE, result?.type)
        assertEquals("$workspacePath/file with spaces.kt", result?.path)
    }

    @Test
    fun `convertEvent handles paths with unicode characters`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "$workspacePath/文件.kt"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.CREATE, result?.type)
        assertEquals("$workspacePath/文件.kt", result?.path)
    }

    @Test
    fun `convertEvent handles paths with special symbols`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "$workspacePath/file-with_special.chars.kt"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.CREATE, result?.type)
        assertEquals("$workspacePath/file-with_special.chars.kt", result?.path)
    }

    @Test
    fun `convertEvent handles empty path`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns ""

        val result = bridge.convertEvent(event)

        assertNull(result)
    }

    @Test
    fun `isWithinWorkspace handles empty path`() {
        assertFalse(bridge.isWithinWorkspace(""))
    }

    @Test
    fun `isWithinWorkspace handles relative paths`() {
        assertFalse(bridge.isWithinWorkspace("relative/path/file.kt"))
    }

    @Test
    fun `convertEvent handles paths with dots in filename`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "$workspacePath/.gitignore"

        val result = bridge.convertEvent(event)

        assertEquals(FileEventType.CREATE, result?.type)
        assertEquals("$workspacePath/.gitignore", result?.path)
    }

    @Test
    fun `isWithinWorkspace handles paths with multiple slashes`() {
        assertTrue(bridge.isWithinWorkspace("$workspacePath//file.kt"))
    }

    @Test
    fun `isGitInternalPath returns true for git directory paths`() {
        assertTrue(bridge.isGitInternalPath("$workspacePath/.git/index"))
        assertTrue(bridge.isGitInternalPath("$workspacePath/.git/AUTO_MERGE"))
        assertTrue(bridge.isGitInternalPath("$workspacePath/.git/objects/pack/something"))
        assertTrue(bridge.isGitInternalPath("$workspacePath/.git/refs/heads/main"))
    }

    @Test
    fun `isGitInternalPath returns true for git directory itself`() {
        assertTrue(bridge.isGitInternalPath("$workspacePath/.git"))
    }

    @Test
    fun `isGitInternalPath returns false for regular files`() {
        assertFalse(bridge.isGitInternalPath("$workspacePath/src/file.kt"))
        assertFalse(bridge.isGitInternalPath("$workspacePath/.gitignore"))
        assertFalse(bridge.isGitInternalPath("$workspacePath/.github/workflows/ci.yml"))
    }

    @Test
    fun `isGitInternalPath handles Windows-style backslashes`() {
        assertTrue(bridge.isGitInternalPath("$workspacePath\\.git\\index"))
        assertTrue(bridge.isGitInternalPath("$workspacePath\\.git\\objects\\pack"))
    }

    @Test
    fun `convertEvent filters git internal paths`() {
        val event = mockk<VFileCreateEvent>()
        every { event.path } returns "$workspacePath/.git/index"

        val result = bridge.convertEvent(event)

        assertNull(result)
    }

    @Test
    fun `convertEvent filters git internal paths for change events`() {
        val event = mockk<VFileContentChangeEvent>()
        every { event.path } returns "$workspacePath/.git/AUTO_MERGE"

        val result = bridge.convertEvent(event)

        assertNull(result)
    }
}
