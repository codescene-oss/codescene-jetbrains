package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.ignore.GitRepositoryIgnoredFilesHolder
import git4idea.index.GitFileStatus
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.status.GitStagingAreaHolder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class Git4IdeaChangeListerTest {
    private val gitRoot = "/test/repo"
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
    fun `getAllChangedFiles returns empty set when repository is null`() =
        runBlocking {
            every { mockLocalFileSystem.findFileByPath(any()) } returns null

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles("/test/repo", "/test/repo")

            assertEquals(0, changedFiles.size)
        }

    @Test
    fun `getAllChangedFiles returns empty set for clean repository`() =
        runBlocking {
            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                "/test/repo",
            )
            Git4IdeaTestSupport.setupCleanRepository(mockRepository, mockStagingArea, mockGitExecutor)

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles("/test/repo", "/test/repo")

            assertEquals(0, changedFiles.size)
        }

    @Test
    fun `getAllChangedFiles detects new untracked files`() =
        runBlocking {
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

            val untrackedFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { untrackedFile.path } returns "test.ts"
            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns listOf(untrackedFile)

            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, gitRoot, "test.ts", "ts")

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(gitRoot, gitRoot)

            assertTrue(changedFiles.size > 0)
            assertTrue(changedFiles.any { it.endsWith("test.ts") })
        }

    @Test
    fun `getAllChangedFiles detects modified files in staging area`() =
        runBlocking {
            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                gitRoot,
            )
            Git4IdeaTestSupport.setupEmptyUntrackedFiles(mockRepository)
            every { mockRepository.currentBranchName } returns "master"
            every { mockGitExecutor.runRevParse(mockRepository) } returns "abc123"

            val mockFilePath = mockk<com.intellij.openapi.vcs.FilePath>()
            every { mockFilePath.path } returns "index.js"
            val mockRecord = mockk<GitFileStatus>()
            every { mockRecord.path } returns mockFilePath
            every { mockRecord.index } returns 'M'
            every { mockRecord.workTree } returns ' '
            every { mockRepository.stagingAreaHolder } returns mockStagingArea
            every { mockStagingArea.allRecords } returns listOf(mockRecord)
            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, gitRoot, "index.js", "js")

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(gitRoot, gitRoot)

            assertTrue(changedFiles.size > 0)
            assertTrue(changedFiles.any { it.endsWith("index.js") })
        }

    @Test
    fun `getAllChangedFiles filters unsupported file types`() =
        runBlocking {
            val gitRoot = "/test/repo"
            val workspace = "/test/repo"

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

            val txtFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { txtFile.path } returns "notes.txt"
            val tsFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { tsFile.path } returns "code.ts"

            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns listOf(txtFile, tsFile)

            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, "notes.txt", "txt")
            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, "code.ts", "ts")

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(gitRoot, workspace)

            assertEquals(1, changedFiles.size)
            assertTrue(changedFiles.any { it.endsWith("code.ts") })
            assertFalse(changedFiles.any { it.endsWith("notes.txt") })
        }

    @Test
    fun `getAllChangedFiles detects committed files via merge-base diff on feature branch`() =
        runBlocking {
            val gitRoot = "/test/repo"
            val workspace = "/test/repo"

            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                gitRoot,
            )
            Git4IdeaTestSupport.setupEmptyStagingArea(mockRepository, mockStagingArea)
            Git4IdeaTestSupport.setupEmptyUntrackedFiles(mockRepository)
            Git4IdeaTestSupport.setupFeatureBranch(mockRepository, mockVirtualFile, mockGitExecutor)

            every { mockGitExecutor.runDiff(mockRepository, "base123") } returns listOf("committed-only.ts")

            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, "committed-only.ts", "ts")

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(gitRoot, workspace)

            assertTrue(
                "Should include committed file via merge-base diff",
                changedFiles.any { it.endsWith("committed-only.ts") },
            )
        }

    @Test
    fun `getAllChangedFiles combines status and diff changes on feature branch`() =
        runBlocking {
            val gitRoot = "/test/repo"
            val workspace = "/test/repo"

            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                gitRoot,
            )
            Git4IdeaTestSupport.setupEmptyStagingArea(mockRepository, mockStagingArea)
            Git4IdeaTestSupport.setupFeatureBranch(mockRepository, mockVirtualFile, mockGitExecutor)

            val untrackedFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { untrackedFile.path } returns "uncommitted.ts"
            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns listOf(untrackedFile)

            every { mockGitExecutor.runDiff(mockRepository, "base123") } returns listOf("committed.ts")

            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, "uncommitted.ts", "ts")
            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, "committed.ts", "ts")

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(gitRoot, workspace)

            val fileNames = changedFiles.map { File(it).name }
            assertTrue("Should include uncommitted file", fileNames.contains("uncommitted.ts"))
            assertTrue("Should include committed file", fileNames.contains("committed.ts"))
        }

    @Test
    fun `getAllChangedFiles respects filesToExcludeFromHeuristic for untracked files`() =
        runBlocking {
            val gitRoot = "/test/repo"
            val workspace = "/test/repo"

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
                (1..10).map { i ->
                    val file = mockk<com.intellij.openapi.vcs.FilePath>()
                    every { file.path } returns "untracked$i.ts"
                    file
                }
            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns files

            files.forEach { file ->
                val fileName = file.path
                Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, fileName, "ts")
                every { mockFileSystem.getParent(fileName) } returns null
            }

            val file2Path = "$gitRoot/untracked2.ts"
            val file5Path = "$gitRoot/untracked5.ts"
            val filesToExcludeFromHeuristic = setOf(file2Path, file5Path)

            val changedFiles =
                git4IdeaChangeLister.getAllChangedFiles(
                    gitRoot,
                    workspace,
                    filesToExcludeFromHeuristic,
                )

            val fileNames = changedFiles.map { File(it).name }
            assertTrue("Should include untracked2.ts", fileNames.contains("untracked2.ts"))
            assertTrue("Should include untracked5.ts", fileNames.contains("untracked5.ts"))
        }

    @Test
    fun `getAllChangedFiles handles untracked files with absolute paths`() =
        runBlocking {
            val gitRoot = "/test/repo"
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

            val untrackedFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { untrackedFile.path } returns "$gitRoot/test.ts"
            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns listOf(untrackedFile)

            val absolutePath = normalizeAbsolutePath("$gitRoot/test.ts")
            every { mockFileSystem.getAbsolutePath(gitRoot, "$gitRoot/test.ts") } returns "$gitRoot/test.ts"
            every { mockFileSystem.fileExists(absolutePath) } returns true
            every { mockFileSystem.getExtension(absolutePath) } returns "ts"
            every { mockFileSystem.getParent("$gitRoot/test.ts") } returns gitRoot
            every { mockFileSystem.getRelativePath(gitRoot, absolutePath) } returns "test.ts"

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(gitRoot, gitRoot)

            assertTrue("Should find untracked file with absolute path", changedFiles.isNotEmpty())
            assertTrue("Should resolve absolute path correctly", changedFiles.any { it.endsWith("test.ts") })
        }
}
