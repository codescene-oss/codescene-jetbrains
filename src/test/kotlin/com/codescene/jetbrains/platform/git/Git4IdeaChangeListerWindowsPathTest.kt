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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class Git4IdeaChangeListerWindowsPathTest {
    private lateinit var project: Project
    private lateinit var git4IdeaChangeLister: Git4IdeaChangeLister
    private lateinit var mockRepository: GitRepository
    private lateinit var mockRepoManager: GitRepositoryManager
    private lateinit var mockLocalFileSystem: LocalFileSystem
    private lateinit var mockVirtualFile: VirtualFile
    private lateinit var mockStagingArea: GitStagingAreaHolder
    private lateinit var mockFileSystem: IFileSystem
    private lateinit var mockGitExecutor: GitCommandExecutor
    private lateinit var mockIgnoredFilesHolder: GitRepositoryIgnoredFilesHolder

    @Before
    fun setup() {
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
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getAllChangedFiles matches heuristic exclusions with Windows separator differences`() =
        runBlocking {
            val gitRoot = "C:/test/repo"
            val workspace = "C:/test/repo"

            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                gitRoot,
            )
            Git4IdeaTestSupport.setupEmptyStagingArea(mockRepository, mockStagingArea)
            every { mockRepository.currentBranchName } returns "master"
            every { mockGitExecutor.runRevParse(mockRepository) } returns "abc123"

            val files =
                (1..6).map { i ->
                    val file = mockk<com.intellij.openapi.vcs.FilePath>()
                    every { file.path } returns "untracked$i.ts"
                    file
                }
            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns files

            files.forEach { file ->
                val fileName = file.path
                val absolutePath = "C:\\test\\repo\\$fileName"
                every { mockFileSystem.getAbsolutePath(gitRoot, fileName) } returns absolutePath
                every { mockFileSystem.fileExists(absolutePath) } returns true
                every { mockFileSystem.getExtension(absolutePath) } returns "ts"
                every { mockFileSystem.getParent(fileName) } returns null
            }

            val changedFiles =
                git4IdeaChangeLister.getAllChangedFiles(
                    gitRoot,
                    workspace,
                    setOf("C:/test/repo/untracked2.ts"),
                )

            assertEquals(setOf("C:\\test\\repo\\untracked2.ts"), changedFiles)
        }
}
