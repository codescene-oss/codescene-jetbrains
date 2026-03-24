package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileChangeListenerTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun stubProjectForFileEventProcessor(project: Project) {
        val provider = mockk<CodeSceneProjectServiceProvider>()
        every { provider.deltaCacheService } returns mockk<IDeltaCacheService>(relaxed = true)
        every { provider.reviewCacheService } returns mockk<IReviewCacheService>(relaxed = true)
        mockkObject(CodeSceneProjectServiceProvider.Companion)
        every { CodeSceneProjectServiceProvider.getInstance(project) } returns provider
        every { project.getService(CodeDeltaService::class.java) } returns mockk(relaxed = true)
        every { project.getService(CodeReviewService::class.java) } returns mockk(relaxed = true)
    }

    @Test
    fun `prepareChange returns null for empty events`() {
        val listener = FileChangeListener(mockk())
        assertNull(listener.prepareChange(emptyList()))
    }

    @Test
    fun `prepareChange returns null when no rename delete or move events`() {
        val listener = FileChangeListener(mockk())
        val otherProperty =
            mockk<VFilePropertyChangeEvent> {
                every { propertyName } returns "other"
            }
        assertNull(listener.prepareChange(listOf(otherProperty)))
    }

    @Test
    fun `prepareChange returns FileEventProcessor for delete events`() {
        val project = mockk<Project>()
        stubProjectForFileEventProcessor(project)
        val listener = FileChangeListener(project)
        val deleteEvent = mockk<VFileDeleteEvent>(relaxed = true)
        val applier = listener.prepareChange(listOf(deleteEvent))
        assertNotNull(applier)
        assertTrue(applier is FileEventProcessor)
    }

    @Test
    fun `prepareChange returns FileEventProcessor for move events`() {
        val project = mockk<Project>()
        stubProjectForFileEventProcessor(project)
        val listener = FileChangeListener(project)
        val moveEvent = mockk<VFileMoveEvent>(relaxed = true)
        val applier = listener.prepareChange(listOf(moveEvent))
        assertNotNull(applier)
        assertTrue(applier is FileEventProcessor)
    }

    @Test
    fun `prepareChange returns FileEventProcessor for name property change`() {
        val project = mockk<Project>()
        stubProjectForFileEventProcessor(project)
        val listener = FileChangeListener(project)
        val renameEvent =
            mockk<VFilePropertyChangeEvent> {
                every { propertyName } returns VirtualFile.PROP_NAME
            }
        val applier = listener.prepareChange(listOf(renameEvent))
        assertNotNull(applier)
        assertTrue(applier is FileEventProcessor)
    }
}
