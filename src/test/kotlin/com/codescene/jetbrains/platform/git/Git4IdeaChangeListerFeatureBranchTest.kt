package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class Git4IdeaChangeListerFeatureBranchTest : Git4IdeaChangeListerTestFixture() {
    @Test
    fun `getAllChangedFiles uses closest merge base when main and develop disagree`() =
        runBlocking {
            val workspace = "/test/repo"
            val branch = "feature-stacked"

            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                gitRoot,
            )
            Git4IdeaTestSupport.setupEmptyStagingArea(mockRepository, mockStagingArea)
            Git4IdeaTestSupport.setupEmptyUntrackedFiles(mockRepository)
            every { mockRepository.currentBranchName } returns branch
            every { mockRepository.root } returns mockVirtualFile

            every { mockGitExecutor.runMergeBase(mockRepository, branch, "main") } returns "oldMainMb"
            every { mockGitExecutor.runMergeBase(mockRepository, branch, "develop") } returns "developMb"
            every { mockGitExecutor.runIsAncestor(mockRepository, "oldMainMb", "developMb") } returns true
            every { mockGitExecutor.runIsAncestor(mockRepository, "developMb", "oldMainMb") } returns false

            every { mockGitExecutor.runDiff(mockRepository, "developMb") } returns listOf("stacked.ts")

            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, gitRoot, workspace, "stacked.ts", "ts")

            git4IdeaChangeLister.getAllChangedFiles(gitRoot, workspace)

            verify(exactly = 1) { mockGitExecutor.runDiff(mockRepository, "developMb") }
        }

    @Test
    fun `getAllChangedFiles detects committed files via merge-base diff on feature branch`() =
        runBlocking {
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
