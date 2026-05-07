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

class GitCommittedChangesTest {
    private lateinit var testRepoPath: File
    private lateinit var gitChangeLister: IGitChangeLister

    @Before
    fun setup() {
        testRepoPath = Files.createTempDirectory("test-git-repo-committed").toFile()
        GitTestSupport.initGitRepoWithFeatureBranch(testRepoPath)
        gitChangeLister = TestGitChangeListerForCommitted(testRepoPath)
    }

    @After
    fun teardown() {
        testRepoPath.deleteRecursively()
    }

    private fun exec(vararg command: String): String = GitTestSupport.exec(testRepoPath, *command)

    @Test
    fun `getAllChangedFiles filters committed files outside workspacePath when gitRootPath differs`() =
        runBlocking {
            val workspaceDir = File(testRepoPath, "workspace")
            workspaceDir.mkdirs()

            File(workspaceDir, "inside.ts").writeText("export const inside = 1;")
            exec("git", "add", "workspace/inside.ts")
            exec("git", "commit", "-m", "Add inside file")

            val baseCommit = exec("git", "rev-parse", "HEAD~1").trim()

            File(testRepoPath, "outside.ts").writeText("export const outside = 1;")
            exec("git", "add", "outside.ts")
            exec("git", "commit", "-m", "Add outside file")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    workspaceDir.absolutePath,
                )

            assertTrue("Should include committed file inside workspacePath", changes.any { it.endsWith("inside.ts") })
            assertFalse(
                "Should not include committed file outside workspacePath",
                changes.any { it.endsWith("outside.ts") },
            )
            assertTrue("All paths should be absolute", changes.all { File(it).isAbsolute })
        }

    @Test
    fun `getAllChangedFiles handles committed files with trailing slash in workspacePath`() =
        runBlocking {
            val workspaceDir = File(testRepoPath, "workspace")
            workspaceDir.mkdirs()

            val baseCommit = exec("git", "rev-parse", "HEAD").trim()

            File(workspaceDir, "file.ts").writeText("export const a = 1;")
            exec("git", "add", "workspace/file.ts")
            exec("git", "commit", "-m", "Add file")

            val workspacePathWithSlash = workspaceDir.absolutePath + File.separator
            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    workspacePathWithSlash,
                    emptySet(),
                )

            assertTrue("Should handle committed files with trailing slash", changes.any { it.endsWith("file.ts") })
        }

    @Test
    fun `getAllChangedFiles returns empty set when baseCommit is empty`() =
        runBlocking {
            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )
            assertEquals(0, changes.size)
        }

    @Test
    fun `getAllChangedFiles filters and strips prefix for committed files with same name in different locations`() =
        runBlocking {
            val uiDir = File(testRepoPath, "ui")
            uiDir.mkdirs()

            val baseCommit = exec("git", "rev-parse", "HEAD").trim()

            File(testRepoPath, "gc.cpp").writeText("// bad gc.cpp at root")
            exec("git", "add", "gc.cpp")
            exec("git", "commit", "-m", "Add gc.cpp at root")

            File(uiDir, "gc.cpp").writeText("// good gc.cpp in ui")
            exec("git", "add", "ui/gc.cpp")
            exec("git", "commit", "-m", "Add gc.cpp in ui")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    uiDir.absolutePath,
                    emptySet(),
                )

            assertEquals(1, changes.size)
            assertTrue("Should include gc.cpp from ui directory", changes.any { it.endsWith("gc.cpp") })

            val returnedFilePath = File(changes.first())
            val content = returnedFilePath.readText()
            assertTrue("Should return gc.cpp from ui", content.contains("good gc.cpp in ui"))
            assertFalse("Should not return gc.cpp from root", content.contains("bad gc.cpp at root"))
        }

    @Test
    fun `getAllChangedFiles returns all committed files when gitRootPath equals workspacePath`() =
        runBlocking {
            val baseCommit = exec("git", "rev-parse", "HEAD").trim()

            File(testRepoPath, "root-file.ts").writeText("export const a = 1;")
            val subDir = File(testRepoPath, "subdir")
            subDir.mkdirs()
            File(subDir, "sub-file.ts").writeText("export const b = 1;")

            exec("git", "add", ".")
            exec("git", "commit", "-m", "Add multiple files")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            assertTrue("Should include root-file.ts", changes.any { it.endsWith("root-file.ts") })
            assertTrue("Should include sub-file.ts in subdir", changes.any { it.endsWith("sub-file.ts") })
        }

    @Test
    fun `getAllChangedFiles handles renamed files and excludes old filename`() =
        runBlocking {
            val baseCommit = exec("git", "rev-parse", "HEAD").trim()

            File(testRepoPath, "old-name.ts").writeText("export const value = 1;")
            exec("git", "add", "old-name.ts")
            exec("git", "commit", "-m", "Add file to rename")

            File(testRepoPath, "old-name.ts").renameTo(File(testRepoPath, "new-name.ts"))
            exec("git", "add", "-A")
            exec("git", "commit", "-m", "Rename file")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            assertTrue("Should include new filename", changes.any { it.endsWith("new-name.ts") })
            assertFalse("Should not include old filename", changes.any { it.endsWith("old-name.ts") })

            for (filePath in changes) {
                assertTrue("File should exist: $filePath", File(filePath).exists())
            }
        }
}
