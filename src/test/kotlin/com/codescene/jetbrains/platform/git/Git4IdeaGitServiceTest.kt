package com.codescene.jetbrains.platform.git

import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import git4idea.ignore.GitRepositoryIgnoredFilesHolder
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
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
    fun `resolveRepository uses getRepositoryForRootQuick off EDT`() {
        val vcsRoot = mockk<VirtualFile>(relaxed = true)

        every { mockApplication.isDispatchThread } returns false
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

    @Test
    fun `resolveRepository picks deepest nested repo when quick lookup misses`() {
        val filePath = "/workspace/monorepo/submodule/pkg/File.kt"
        val outerRoot = mockRepositoryRoot("/workspace/monorepo")
        val innerRoot = mockRepositoryRoot("/workspace/monorepo/submodule")
        val outerRepo = mockRepositoryAt(outerRoot)
        val innerRepo = mockRepositoryAt(innerRoot)
        val nestedFile = mockFileAt(filePath)

        setupEdtRepositoryFallback(nestedFile, outerRoot, listOf(outerRepo, innerRepo))

        val result = gitService.resolveRepository(nestedFile)

        assertSame(innerRepo, result)
    }

    @Test
    fun `nested repo file resolves relative path and ignore status from nested repo`() {
        val filePath = "/workspace/monorepo/submodule/pkg/File.kt"
        val outerRoot = mockRepositoryRoot("/workspace/monorepo")
        val innerRoot = mockRepositoryRoot("/workspace/monorepo/submodule")
        val outerRepo = mockRepositoryAt(outerRoot)
        val innerRepo = mockRepositoryAt(innerRoot)
        val nestedFile = mockFileAt(filePath)
        val ignoredFilesHolder = mockk<GitRepositoryIgnoredFilesHolder>(relaxed = true)

        every { innerRepo.ignoredFilesHolder } returns ignoredFilesHolder
        every { ignoredFilesHolder.containsFile(any<FilePath>()) } returns true

        setupEdtRepositoryFallback(nestedFile, outerRoot, listOf(outerRepo, innerRepo))

        mockkStatic(LocalFileSystem::class)
        mockkStatic(VcsUtil::class)
        val mockLocalFileSystem = mockk<LocalFileSystem>(relaxed = true)
        every { LocalFileSystem.getInstance() } returns mockLocalFileSystem
        every { mockLocalFileSystem.findFileByPath(filePath) } returns nestedFile
        every { VcsUtil.getFilePath(nestedFile) } returns mockk(relaxed = true)

        assertEquals("pkg/File.kt", gitService.getRepoRelativePath(filePath))
        assertTrue(gitService.isIgnored(filePath))
        verify(exactly = 1) { ignoredFilesHolder.containsFile(any<FilePath>()) }
    }

    private fun mockRepositoryRoot(path: String): VirtualFile {
        val root = mockk<VirtualFile>(relaxed = true)
        every { root.path } returns path
        return root
    }

    private fun mockRepositoryAt(root: VirtualFile): GitRepository {
        val repo = mockk<GitRepository>(relaxed = true)
        every { repo.root } returns root
        return repo
    }

    private fun mockFileAt(path: String): VirtualFile {
        val nestedFile = mockk<VirtualFile>(relaxed = true)
        every { nestedFile.path } returns path
        return nestedFile
    }

    private fun setupEdtRepositoryFallback(
        file: VirtualFile,
        outerVcsRoot: VirtualFile,
        repositories: List<GitRepository>,
    ) {
        every { mockApplication.isDispatchThread } returns true
        mockkStatic(ProjectLevelVcsManager::class)
        val mockVcsManager = mockk<ProjectLevelVcsManager>(relaxed = true)
        every { ProjectLevelVcsManager.getInstance(project) } returns mockVcsManager
        every { mockVcsManager.getVcsRootFor(file) } returns outerVcsRoot
        every { mockRepoManager.getRepositoryForRootQuick(outerVcsRoot) } returns null
        every { mockRepoManager.repositories } returns repositories
    }
}
