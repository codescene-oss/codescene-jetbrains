package com.codescene.jetbrains.platform.git

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class Git4IdeaGitServiceTest {
    private lateinit var project: Project
    private lateinit var file: VirtualFile
    private lateinit var mockRepoManager: GitRepositoryManager
    private lateinit var mockRepository: GitRepository
    private lateinit var mockApplication: Application
    private lateinit var gitService: Git4IdeaGitService

    @Before
    fun setup() {
        project = mockk(relaxed = true)
        file = mockk(relaxed = true)
        mockRepoManager = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockApplication = mockk(relaxed = true)

        mockkStatic(ApplicationManager::class)
        mockkStatic(GitRepositoryManager::class)
        every { ApplicationManager.getApplication() } returns mockApplication
        every { GitRepositoryManager.getInstance(project) } returns mockRepoManager

        gitService = Git4IdeaGitService(project)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `resolveRepository uses getRepositoryForFile off EDT`() {
        every { mockApplication.isDispatchThread } returns false
        every { mockRepoManager.getRepositoryForFile(file) } returns mockRepository

        val result = gitService.resolveRepository(file)

        assertSame(mockRepository, result)
        verify(exactly = 1) { mockRepoManager.getRepositoryForFile(file) }
        verify(exactly = 0) { mockRepoManager.getRepositoryForRootQuick(any<VirtualFile>()) }
    }

    @Test
    fun `resolveRepository uses getRepositoryForRootQuick on EDT`() {
        val vcsRoot = mockk<VirtualFile>(relaxed = true)

        every { mockApplication.isDispatchThread } returns true
        mockkStatic(ProjectLevelVcsManager::class)
        val mockVcsManager = mockk<ProjectLevelVcsManager>(relaxed = true)
        every { ProjectLevelVcsManager.getInstance(project) } returns mockVcsManager
        every { mockVcsManager.getVcsRootFor(file) } returns vcsRoot
        every { mockRepoManager.getRepositoryForRootQuick(vcsRoot) } returns mockRepository

        val result = gitService.resolveRepository(file)

        assertSame(mockRepository, result)
        verify(exactly = 0) { mockRepoManager.getRepositoryForFile(any<VirtualFile>()) }
        verify(exactly = 1) { mockRepoManager.getRepositoryForRootQuick(vcsRoot) }
    }
}
