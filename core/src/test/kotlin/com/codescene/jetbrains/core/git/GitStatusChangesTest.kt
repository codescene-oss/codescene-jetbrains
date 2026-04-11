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

class GitStatusChangesTest {
    private lateinit var testRepoPath: File
    private lateinit var gitChangeLister: IGitChangeLister

    @Before
    fun setup() {
        testRepoPath = Files.createTempDirectory("test-git-repo-status").toFile()
        GitTestSupport.initGitRepoWithExtraFiles(testRepoPath)
        gitChangeLister = TestGitChangeListerForStatus(testRepoPath)
    }

    @After
    fun teardown() {
        testRepoPath.deleteRecursively()
    }

    private fun exec(vararg command: String): String = GitTestSupport.exec(testRepoPath, *command)

    @Test
    fun `getAllChangedFiles detects all file statuses`() =
        runBlocking {
            File(testRepoPath, "untracked.ts").writeText("export const x = 1;")
            File(testRepoPath, "added.js").writeText("console.log(\"new\");")
            exec("git", "add", "added.js")
            File(testRepoPath, "to-modify.py").writeText("print(\"modified\")")
            File(testRepoPath, "existing.ts").writeText("export const foo = 2;")
            exec("git", "add", "existing.ts")
            File(testRepoPath, "existing.ts").writeText("export const foo = 3;")
            File(testRepoPath, "new-modified.ts").writeText("export const a = 1;")
            exec("git", "add", "new-modified.ts")
            File(testRepoPath, "new-modified.ts").writeText("export const a = 2;")
            File(testRepoPath, "to-rename.js").renameTo(File(testRepoPath, "renamed.js"))
            exec("git", "add", "-A")
            exec("git", "config", "core.ignoreCase", "false")
            val srcPath = File(testRepoPath, "README.ts")
            val copiedPath = File(testRepoPath, "copied.ts")
            srcPath.copyTo(copiedPath)
            exec("git", "add", "-A")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )
            val fileNames = changes.toList()

            assertTrue("Should detect ?? status", fileNames.contains("untracked.ts"))
            assertTrue("Should detect A status", fileNames.contains("added.js"))
            assertTrue("Should detect M status", fileNames.contains("to-modify.py"))
            assertTrue("Should detect MM status", fileNames.contains("existing.ts"))
            assertTrue("Should detect AM status", fileNames.contains("new-modified.ts"))
            assertTrue("Should detect R status", fileNames.contains("renamed.js"))
            assertTrue("Should detect C or A status", fileNames.contains("copied.ts"))
        }

    @Test
    fun `getAllChangedFiles detects files with whitespace in various statuses`() =
        runBlocking {
            File(testRepoPath, "spaced file.ts").writeText("export const x = 1;")
            exec("git", "add", "spaced file.ts")
            File(testRepoPath, "original name.ts").writeText("export const y = 1;")
            exec("git", "add", "original name.ts")
            File(testRepoPath, "staged modified.rs").writeText("fn main() { }")
            exec("git", "add", "staged modified.rs")
            File(testRepoPath, "source file.ts").writeText("// Source")
            exec("git", "add", "source file.ts")
            exec("git", "commit", "-m", "Add files for testing")
            File(testRepoPath, "spaced file.ts").writeText("export const x = 2;")
            File(testRepoPath, "original name.ts").renameTo(File(testRepoPath, "new name.ts"))
            exec("git", "add", "-A")
            File(testRepoPath, "staged modified.rs").writeText("fn main() { let x = 1; }")
            exec("git", "add", "staged modified.rs")
            File(testRepoPath, "staged modified.rs").writeText("fn main() { let x = 2; }")
            File(testRepoPath, "source file.ts").copyTo(File(testRepoPath, "copied file.ts"))
            exec("git", "add", "copied file.ts")
            File(testRepoPath, "untracked file.ts").writeText("export const x = 1;")
            File(testRepoPath, "staged file.py").writeText("print(\"hello\")")
            exec("git", "add", "staged file.py")
            File(testRepoPath, "new modified file.js").writeText("console.log(1);")
            exec("git", "add", "new modified file.js")
            File(testRepoPath, "new modified file.js").writeText("console.log(2);")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )
            val fileNames = changes.toList()

            assertTrue("Should detect ?? with spaces", fileNames.contains("untracked file.ts"))
            assertTrue("Should detect A with spaces", fileNames.contains("staged file.py"))
            assertTrue("Should detect M with spaces", fileNames.contains("spaced file.ts"))
            assertTrue("Should detect MM with spaces", fileNames.contains("staged modified.rs"))
            assertTrue("Should detect AM with spaces", fileNames.contains("new modified file.js"))
            assertTrue("Should detect R with spaces", fileNames.contains("new name.ts"))
            assertTrue("Should detect C or A with spaces", fileNames.contains("copied file.ts"))
        }

    @Test
    fun `getAllChangedFiles returns empty set for clean repository and excludes deleted files`() =
        runBlocking {
            var changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )
            assertEquals(0, changes.size)
            File(testRepoPath, "existing.ts").delete()
            changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )
            val fileNames = changes.toList()
            assertFalse("Should not include deleted file", fileNames.contains("existing.ts"))
        }

    @Test
    fun `getAllChangedFiles filters files outside workspacePath when gitRootPath differs`() =
        runBlocking {
            val subDir = File(testRepoPath, "workspace-subdir")
            subDir.mkdirs()
            File(subDir, "inside.ts").writeText("export const inside = 1;")
            File(testRepoPath, "outside.ts").writeText("export const outside = 1;")

            val changes = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, subDir.absolutePath, emptySet())
            val fileNames = changes.toList()

            assertTrue("Should include file inside workspacePath", fileNames.contains("inside.ts"))
            assertFalse("Should not include file outside workspacePath", fileNames.contains("outside.ts"))
            assertFalse(
                "Should strip workspace prefix",
                fileNames.contains("workspace-subdir${File.separator}inside.ts"),
            )
        }

    @Test
    fun `getAllChangedFiles handles workspacePath with trailing slash`() =
        runBlocking {
            val workspaceDir = File(testRepoPath, "workspace")
            workspaceDir.mkdirs()
            File(workspaceDir, "file.ts").writeText("export const a = 1;")

            val workspacePathWithSlash = workspaceDir.absolutePath + File.separator
            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    workspacePathWithSlash,
                    emptySet(),
                )
            val fileNames = changes.toList()

            assertTrue("Should handle workspacePath with trailing slash", fileNames.contains("file.ts"))
        }

    @Test
    fun `getAllChangedFiles filters and strips prefix for files with same name in different locations`() =
        runBlocking {
            val uiDir = File(testRepoPath, "ui")
            uiDir.mkdirs()
            File(testRepoPath, "gc.cpp").writeText("// bad gc.cpp at root")
            File(uiDir, "gc.cpp").writeText("// good gc.cpp in ui")

            val changes = gitChangeLister.getAllChangedFiles(testRepoPath.absolutePath, uiDir.absolutePath, emptySet())
            val fileNames = changes.toList()

            assertEquals(1, fileNames.size)
            assertTrue("Should include gc.cpp from ui directory", fileNames.contains("gc.cpp"))

            val returnedFilePath = File(uiDir, fileNames[0])
            val content = returnedFilePath.readText()
            assertTrue("Should return gc.cpp from ui", content.contains("good gc.cpp in ui"))
            assertFalse("Should not return gc.cpp from root", content.contains("bad gc.cpp at root"))
        }

    @Test
    fun `getAllChangedFiles handles renamed files and excludes old filename`() =
        runBlocking {
            File(testRepoPath, "file-to-rename.ts").writeText("export const value = 1;")
            exec("git", "add", "file-to-rename.ts")
            exec("git", "commit", "-m", "Add file to rename")
            File(testRepoPath, "file-to-rename.ts").renameTo(File(testRepoPath, "file-renamed.ts"))
            exec("git", "add", "-A")

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )
            val fileNames = changes.toList()

            assertTrue("Should include new filename", fileNames.contains("file-renamed.ts"))
            assertFalse("Should not include old filename", fileNames.contains("file-to-rename.ts"))

            for (fileName in fileNames) {
                val filePath = File(testRepoPath, fileName)
                assertTrue("File should exist: $fileName", filePath.exists())
            }
        }
}
