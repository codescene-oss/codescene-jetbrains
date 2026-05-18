package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.ignore.GitRepositoryIgnoredFilesHolder
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.status.GitStagingAreaHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before

abstract class Git4IdeaChangeListerTestFixture {
    protected val gitRoot = "/test/repo"
    protected lateinit var project: Project
    protected lateinit var git4IdeaChangeLister: Git4IdeaChangeLister
    protected lateinit var mockRepository: GitRepository
    protected lateinit var mockRepoManager: GitRepositoryManager
    protected lateinit var mockLocalFileSystem: LocalFileSystem
    protected lateinit var mockVirtualFile: VirtualFile
    protected lateinit var mockStagingArea: GitStagingAreaHolder
    protected lateinit var mockFileSystem: IFileSystem
    protected lateinit var mockGitExecutor: GitCommandExecutor
    protected lateinit var mockIgnoredFilesHolder: GitRepositoryIgnoredFilesHolder

    @Before
    open fun setup() {
        project = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        mockRepoManager = mockk(relaxed = true)
        mockLocalFileSystem = mockk(relaxed = true)
        mockVirtualFile = mockk(relaxed = true)
        mockStagingArea = mockk(relaxed = true)
        mockFileSystem = mockk(relaxed = true)
        mockGitExecutor = mockk(relaxed = true)
        mockIgnoredFilesHolder = mockk(relaxed = true)

        every { mockRepository.ignoredFilesHolder } returns mockIgnoredFilesHolder
        every { mockIgnoredFilesHolder.containsFile(any()) } returns false

        mockkStatic(GitRepositoryManager::class)
        every { GitRepositoryManager.getInstance(project) } returns mockRepoManager

        mockkStatic(LocalFileSystem::class)
        every { LocalFileSystem.getInstance() } returns mockLocalFileSystem

        git4IdeaChangeLister = Git4IdeaChangeLister(project, mockFileSystem, mockGitExecutor)
    }

    @After
    open fun teardown() {
        unmockkAll()
    }
}
