package com.codescene.jetbrains.core.git

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitPathUtilsTest {
    private lateinit var testRepoPath: File

    @Before
    fun setup() {
        testRepoPath = Files.createTempDirectory("test-git-repo-path-utils").toFile()
        GitTestSupport.initGitRepo(testRepoPath)
    }

    @After
    fun teardown() {
        testRepoPath.deleteRecursively()
    }

    @Test
    fun `createWorkspacePrefix adds separator to path without trailing separator`() {
        val inputPath = File.separator + "foo" + File.separator + "bar"
        val result = createWorkspacePrefix(inputPath)
        val resolvedPath = Paths.get(inputPath).toAbsolutePath().normalize().toString()
        assertEquals("${resolvedPath}${File.separator}", result.workspacePrefix)
        assertEquals(resolvedPath, result.normalizedWorkspacePath)
    }

    @Test
    fun `createWorkspacePrefix preserves separator for path with trailing separator`() {
        val inputPath = File.separator + "foo" + File.separator + "bar" + File.separator
        val result = createWorkspacePrefix(inputPath)
        val resolvedPath = Paths.get(inputPath).toAbsolutePath().normalize().toString() + File.separator
        assertEquals(resolvedPath, result.workspacePrefix)
    }

    @Test
    fun `isFileInWorkspace returns true for file inside workspace`() {
        val workspaceDir = File(testRepoPath, "workspace")
        workspaceDir.mkdirs()
        File(workspaceDir, "file.ts").writeText("export const x = 1;")

        val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspaceDir.absolutePath)
        val filePath = "workspace/file.ts"
        val result = isFileInWorkspace(filePath, testRepoPath.absolutePath, normalizedWorkspacePath, workspacePrefix)
        assertTrue("Expected file to be in workspace", result)
    }

    @Test
    fun `isFileInWorkspace returns false for file outside workspace`() {
        val workspaceDir = File(testRepoPath, "workspace")
        workspaceDir.mkdirs()

        val otherDir = File(testRepoPath, "other")
        otherDir.mkdirs()
        File(otherDir, "file.ts").writeText("export const y = 1;")

        val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspaceDir.absolutePath)
        val filePath = "other/file.ts"
        val result = isFileInWorkspace(filePath, testRepoPath.absolutePath, normalizedWorkspacePath, workspacePrefix)
        assertFalse("Expected file to be outside workspace", result)
    }

    @Test
    fun `isFileInWorkspace returns false for file with similar prefix`() {
        val workspaceDir = File(testRepoPath, "workspace")
        workspaceDir.mkdirs()

        val similarDir = File(testRepoPath, "workspace-other")
        similarDir.mkdirs()
        File(similarDir, "file.ts").writeText("export const z = 1;")

        val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspaceDir.absolutePath)
        val filePath = "workspace-other/file.ts"
        val result = isFileInWorkspace(filePath, testRepoPath.absolutePath, normalizedWorkspacePath, workspacePrefix)
        assertFalse("Expected file to be outside workspace", result)
    }

    @Test
    fun `createWorkspacePrefix handles path that already ends with separator`() {
        val inputPath = File.separator + "foo" + File.separator + "bar" + File.separator
        val result = createWorkspacePrefix(inputPath)
        val resolvedPath = Paths.get(inputPath).toAbsolutePath().normalize().toString()
        assertEquals("${resolvedPath}${File.separator}", result.workspacePrefix)
        assertEquals(resolvedPath, result.normalizedWorkspacePath)
    }

    @Test
    fun `isFileInWorkspace returns false when file does not exist`() {
        val workspaceDir = File(testRepoPath, "workspace")
        workspaceDir.mkdirs()

        val (normalizedWorkspacePath, workspacePrefix) = createWorkspacePrefix(workspaceDir.absolutePath)
        val filePath = "workspace/nonexistent.ts"
        val result = isFileInWorkspace(filePath, testRepoPath.absolutePath, normalizedWorkspacePath, workspacePrefix)
        assertFalse("Expected false for non-existent file", result)
    }
}
