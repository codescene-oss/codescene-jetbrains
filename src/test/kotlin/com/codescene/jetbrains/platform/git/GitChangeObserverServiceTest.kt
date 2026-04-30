package com.codescene.jetbrains.platform.git

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GitChangeObserverServiceTest {
    private lateinit var project: Project
    private lateinit var service: GitChangeObserverService
    private lateinit var mockLocalFileSystem: LocalFileSystem

    private val workspacePath = "/test/workspace"

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        mockLocalFileSystem = mockk(relaxed = true)

        every { project.basePath } returns workspacePath

        mockkStatic(LocalFileSystem::class)
        every { LocalFileSystem.getInstance() } returns mockLocalFileSystem

        service = GitChangeObserverService(project)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `service can be instantiated`() {
        assertNotNull(service)
    }

    @Test
    fun `start does nothing when project has no basePath`() {
        every { project.basePath } returns null

        service.start()

        verify(exactly = 0) { mockLocalFileSystem.findFileByPath(any()) }
    }

    @Test
    fun `dispose cleans up resources`() {
        service.dispose()
    }

    @Test
    fun `start does nothing when LocalFileSystem cannot find workspace path`() {
        every { mockLocalFileSystem.findFileByPath(workspacePath) } returns null

        service.start()

        verify(exactly = 1) { mockLocalFileSystem.findFileByPath(workspacePath) }
    }
}
