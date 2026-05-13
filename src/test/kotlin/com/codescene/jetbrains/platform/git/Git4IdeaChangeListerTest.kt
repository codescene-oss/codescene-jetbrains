package com.codescene.jetbrains.platform.git

import git4idea.index.GitFileStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Git4IdeaChangeListerTest : Git4IdeaChangeListerTestFixture() {
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
            val localGitRoot = "/test/repo"
            val workspace = "/test/repo"

            Git4IdeaTestSupport.setupRepositoryAccess(
                mockLocalFileSystem,
                mockRepoManager,
                mockRepository,
                mockVirtualFile,
                localGitRoot,
            )
            Git4IdeaTestSupport.setupEmptyStagingArea(mockRepository, mockStagingArea)
            every { mockRepository.currentBranchName } returns "master"
            every { mockGitExecutor.runRevParse(mockRepository) } returns "abc123"

            val txtFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { txtFile.path } returns "notes.txt"
            val tsFile = mockk<com.intellij.openapi.vcs.FilePath>()
            every { tsFile.path } returns "code.ts"

            every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns listOf(txtFile, tsFile)

            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, localGitRoot, workspace, "notes.txt", "txt")
            Git4IdeaTestSupport.setupFileSystemForFile(mockFileSystem, localGitRoot, workspace, "code.ts", "ts")

            val changedFiles = git4IdeaChangeLister.getAllChangedFiles(localGitRoot, workspace)

            assertEquals(1, changedFiles.size)
            assertTrue(changedFiles.any { it.endsWith("code.ts") })
            assertFalse(changedFiles.any { it.endsWith("notes.txt") })
        }
}
