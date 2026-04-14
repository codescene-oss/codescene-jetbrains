package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IGitChangeLister
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitChangeListerTest {
    private lateinit var testRepoPath: File
    private lateinit var gitChangeLister: IGitChangeLister

    @Before
    fun setup() {
        testRepoPath = Files.createTempDirectory("test-git-repo").toFile()
        GitTestSupport.initGitRepo(testRepoPath)
        gitChangeLister = TestGitChangeLister(testRepoPath)
    }

    @After
    fun teardown() {
        testRepoPath.deleteRecursively()
    }

    private fun exec(vararg command: String): String = GitTestSupport.exec(testRepoPath, *command)

    @Test
    fun `getAllChangedFiles returns empty set for clean repository`() =
        runBlocking {
            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)
            assertEquals(0, changedFiles.size)
        }

    @Test
    fun `getAllChangedFiles detects new untracked files`() =
        runBlocking {
            val newFile = File(testRepoPath, "test.ts")
            newFile.writeText("console.log(\"test\");")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            assertTrue(changedFiles.size > 0)
            assertTrue(changedFiles.any { it.endsWith("test.ts") })
        }

    @Test
    fun `getAllChangedFiles detects modified files`() =
        runBlocking {
            val testFile = File(testRepoPath, "index.js")
            testFile.writeText("console.log(\"hello\");")
            exec("git", "add", "index.js")
            exec("git", "commit", "-m", "Add index.js")

            testFile.writeText("console.log(\"modified\");")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            assertTrue(changedFiles.size > 0)
            assertTrue(changedFiles.any { it.endsWith("index.js") })
        }

    @Test
    fun `getAllChangedFiles detects staged files`() =
        runBlocking {
            val newFile = File(testRepoPath, "script.py")
            newFile.writeText("print(\"hello\")")
            exec("git", "add", "script.py")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            assertTrue(changedFiles.size > 0)
            assertTrue(changedFiles.any { it.endsWith("script.py") })
        }

    @Test
    fun `getAllChangedFiles filters unsupported file types`() =
        runBlocking {
            val txtFile = File(testRepoPath, "notes.txt")
            val mdFile = File(testRepoPath, "docs.md")
            val tsFile = File(testRepoPath, "code.ts")
            txtFile.writeText("Some notes")
            mdFile.writeText("# Documentation")
            tsFile.writeText("export const x = 1;")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)
            val fileNames = changedFiles.map { File(it).name }

            assertEquals(1, changedFiles.size)
            assertTrue(fileNames.contains("code.ts"))
        }

    @Test
    fun `getAllChangedFiles detects renamed files`() =
        runBlocking {
            val originalFile = File(testRepoPath, "original.js")
            originalFile.writeText("console.log(\"test\");")
            exec("git", "add", "original.js")
            exec("git", "commit", "-m", "Add original.js")

            val renamedFile = File(testRepoPath, "renamed.js")
            originalFile.renameTo(renamedFile)
            exec("git", "add", "-A")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            assertTrue(changedFiles.size > 0)
            assertTrue(changedFiles.any { it.endsWith("renamed.js") })
        }

    @Test
    fun `getAllChangedFiles combines status and diff changes`() =
        runBlocking {
            exec("git", "checkout", "-b", "feature-branch")

            val committedFile = File(testRepoPath, "committed.ts")
            committedFile.writeText("export const foo = 1;")
            exec("git", "add", "committed.ts")
            exec("git", "commit", "-m", "Add committed.ts")

            val uncommittedFile = File(testRepoPath, "uncommitted.ts")
            uncommittedFile.writeText("export const bar = 2;")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            val fileNames = changedFiles.map { File(it).name }
            assertTrue("Should include uncommitted file", fileNames.contains("uncommitted.ts"))
        }

    @Test
    fun `getAllChangedFiles handles files with whitespace in names`() =
        runBlocking {
            val fileWithSpaces = File(testRepoPath, "my file.ts")
            val anotherFileWithSpaces = File(testRepoPath, "test file with spaces.js")
            fileWithSpaces.writeText("console.log(\"has spaces\");")
            anotherFileWithSpaces.writeText("console.log(\"also has spaces\");")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            val fileNames = changedFiles.map { File(it).name }
            assertTrue("Should include file with spaces: my file.ts", fileNames.contains("my file.ts"))
            assertTrue(
                "Should include file with spaces: test file with spaces.js",
                fileNames.contains("test file with spaces.js"),
            )
        }

    @Test
    fun `getAllChangedFiles detects committed files via merge-base diff`() =
        runBlocking {
            exec("git", "checkout", "-b", "feature-branch")

            val committedFile = File(testRepoPath, "committed-only.ts")
            committedFile.writeText("export const foo = 1;")
            exec("git", "add", "committed-only.ts")
            exec("git", "commit", "-m", "Add committed-only.ts")

            val changedFiles = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, testRepoPath.absolutePath)

            val fileNames = changedFiles.map { File(it).name }
            assertTrue("Should include committed file via merge-base diff", fileNames.contains("committed-only.ts"))
        }

    @Test
    fun `shouldReviewFile checks against supported extensions`() {
        assertTrue(shouldReviewFile("test.js"))
        assertTrue(shouldReviewFile("test.ts"))
        assertTrue(shouldReviewFile("test.py"))
        assertTrue(shouldReviewFile("test.java"))
        assertTrue(shouldReviewFile("test.kt"))
        assertFalse(shouldReviewFile("test.txt"))
        assertFalse(shouldReviewFile("test.md"))
        assertFalse(shouldReviewFile("test.json"))
    }
}
