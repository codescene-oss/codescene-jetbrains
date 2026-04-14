package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.status.GitStagingAreaHolder
import io.mockk.every

object Git4IdeaTestSupport {
    fun setupRepositoryAccess(
        mockLocalFileSystem: LocalFileSystem,
        mockRepoManager: GitRepositoryManager,
        mockRepository: GitRepository,
        mockVirtualFile: VirtualFile,
        gitRoot: String,
    ) {
        every { mockLocalFileSystem.findFileByPath(gitRoot) } returns mockVirtualFile
        every { mockRepoManager.getRepositoryForRoot(mockVirtualFile) } returns mockRepository
    }

    fun setupEmptyStagingArea(
        mockRepository: GitRepository,
        mockStagingArea: GitStagingAreaHolder,
    ) {
        every { mockRepository.stagingAreaHolder } returns mockStagingArea
        every { mockStagingArea.allRecords } returns emptyList()
    }

    fun setupEmptyUntrackedFiles(mockRepository: GitRepository) {
        every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns emptyList()
    }

    fun setupFeatureBranch(
        mockRepository: GitRepository,
        mockVirtualFile: VirtualFile,
        mockGitExecutor: GitCommandExecutor,
        branchName: String = "feature-branch",
        mergeBase: String = "base123",
    ) {
        every { mockRepository.currentBranchName } returns branchName
        every { mockRepository.root } returns mockVirtualFile
        every { mockGitExecutor.runMergeBase(mockRepository, branchName, "main") } returns mergeBase
    }

    fun setupCleanRepository(
        mockRepository: GitRepository,
        mockStagingArea: GitStagingAreaHolder,
        mockGitExecutor: GitCommandExecutor,
        branchName: String = "master",
    ) {
        setupEmptyStagingArea(mockRepository, mockStagingArea)
        every { mockRepository.untrackedFilesHolder.retrieveUntrackedFilePaths() } returns emptyList()
        every { mockRepository.currentBranchName } returns branchName
        every { mockGitExecutor.runRevParse(mockRepository) } returns "abc123"
    }

    fun setupFileSystemForFile(
        mockFileSystem: IFileSystem,
        gitRoot: String,
        workspace: String,
        fileName: String,
        extension: String,
    ) {
        every { mockFileSystem.getAbsolutePath(gitRoot, fileName) } returns "$gitRoot/$fileName"
        every { mockFileSystem.fileExists("$gitRoot/$fileName") } returns true
        every { mockFileSystem.getExtension("$gitRoot/$fileName") } returns extension
        every { mockFileSystem.getAbsolutePath(workspace, fileName) } returns "$workspace/$fileName"
    }
}
