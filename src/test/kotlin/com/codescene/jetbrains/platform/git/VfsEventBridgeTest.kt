package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileEventType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import io.mockk.every
import io.mockk.mockk
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
}
