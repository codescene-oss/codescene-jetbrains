package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IGitChangeLister
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitUntrackedFilesTest {
    private lateinit var testRepoPath: File
    private lateinit var gitChangeLister: IGitChangeLister

    @Before
    fun setup() {
        testRepoPath = Files.createTempDirectory("test-git-repo-untracked").toFile()
        GitTestSupport.initGitRepo(testRepoPath)
        gitChangeLister = TestGitChangeListerForStatus(testRepoPath)
    }

    @After
    fun teardown() {
        testRepoPath.deleteRecursively()
    }

    private fun exec(vararg command: String): String = GitTestSupport.exec(testRepoPath, *command)

    @Test
    fun `getAllChangedFiles excludes untracked files when more than MAX_UNTRACKED_FILES_PER_LOCATION at root level`() =
        runBlocking {
            val count = MAX_UNTRACKED_FILES_PER_LOCATION + 1
            for (i in 1..count) {
                File(testRepoPath, "untracked$i.ts").writeText("export const x$i = 1;")
            }

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            for (i in 1..count) {
                assertFalse("Should not include untracked$i.ts", changes.any { it.endsWith("untracked$i.ts") })
            }
        }

    @Test
    fun `getAllChangedFiles includes untracked files when MAX_UNTRACKED_FILES_PER_LOCATION or fewer at root level`() =
        runBlocking {
            val count = MAX_UNTRACKED_FILES_PER_LOCATION
            for (i in 1..count) {
                File(testRepoPath, "untracked$i.ts").writeText("export const x$i = 1;")
            }

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            for (i in 1..count) {
                assertTrue("Should include untracked$i.ts", changes.any { it.endsWith("untracked$i.ts") })
            }
        }

    @Test
    fun `getAllChangedFiles excludes untracked files in directory when more than MAX_UNTRACKED_FILES_PER_LOCATION`() =
        runBlocking {
            val untrackedDir = File(testRepoPath, "untracked-dir")
            untrackedDir.mkdirs()

            val count = MAX_UNTRACKED_FILES_PER_LOCATION + 1
            for (i in 1..count) {
                File(untrackedDir, "file$i.ts").writeText("export const x$i = 1;")
            }

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            for (i in 1..count) {
                assertFalse("Should not include untracked-dir/file$i.ts", changes.any { it.contains("file$i.ts") })
            }
        }

    @Test
    fun `getAllChangedFiles includes untracked files in directory when MAX_UNTRACKED_FILES_PER_LOCATION or fewer`() =
        runBlocking {
            val untrackedDir = File(testRepoPath, "untracked-dir")
            untrackedDir.mkdirs()

            val count = MAX_UNTRACKED_FILES_PER_LOCATION
            for (i in 1..count) {
                File(untrackedDir, "file$i.ts").writeText("export const x$i = 1;")
            }

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            for (i in 1..count) {
                assertTrue("Should include file$i.ts", changes.any { it.endsWith("file$i.ts") })
            }
        }

    @Test
    fun `getAllChangedFiles handles multiple directories with different untracked file counts`() =
        runBlocking {
            val dir1 = File(testRepoPath, "many-files")
            dir1.mkdirs()
            val manyCount = MAX_UNTRACKED_FILES_PER_LOCATION + 1
            for (i in 1..manyCount) {
                File(dir1, "file$i.ts").writeText("export const x$i = 1;")
            }

            val dir2 = File(testRepoPath, "few-files")
            dir2.mkdirs()
            val fewCount = MAX_UNTRACKED_FILES_PER_LOCATION - 2
            for (i in 1..fewCount) {
                File(dir2, "file$i.ts").writeText("export const y$i = 1;")
            }

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            for (i in 1..manyCount) {
                assertFalse(
                    "Should not include many-files/file$i.ts",
                    changes.any {
                        it.contains("many-files") && it.contains("file$i.ts")
                    },
                )
            }

            for (i in 1..fewCount) {
                assertTrue(
                    "Should include file$i.ts from few-files",
                    changes.any { it.contains("few-files") && it.endsWith("file$i.ts") },
                )
            }
        }

    @Test
    fun `getAllChangedFiles always includes tracked modified files regardless of count`() =
        runBlocking {
            val count = MAX_UNTRACKED_FILES_PER_LOCATION * 2
            for (i in 1..count) {
                File(testRepoPath, "tracked$i.ts").writeText("export const x$i = 1;")
            }
            exec("git", "add", ".")
            exec("git", "commit", "-m", "Add tracked files")

            for (i in 1..count) {
                File(testRepoPath, "tracked$i.ts").writeText("export const x$i = 2;")
            }

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    emptySet(),
                )

            for (i in 1..count) {
                assertTrue("Should include tracked$i.ts", changes.any { it.endsWith("tracked$i.ts") })
            }
        }

    @Test
    fun `getAllChangedFiles includes untracked files in filesToExcludeFromHeuristic even when exceeding limit`() =
        runBlocking {
            val count = MAX_UNTRACKED_FILES_PER_LOCATION + 3
            for (i in 1..count) {
                File(testRepoPath, "untracked$i.ts").writeText("export const x$i = 1;")
            }

            val file2Path = File(testRepoPath, "untracked2.ts").absolutePath
            val file5Path = File(testRepoPath, "untracked5.ts").absolutePath
            val filesToExcludeFromHeuristic = setOf(file2Path, file5Path)

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    filesToExcludeFromHeuristic,
                )

            assertTrue("Should include untracked2.ts", changes.any { it.endsWith("untracked2.ts") })
            assertTrue("Should include untracked5.ts", changes.any { it.endsWith("untracked5.ts") })

            for (i in 1..count) {
                if (i != 2 && i != 5) {
                    assertFalse("Should not include untracked$i.ts", changes.any { it.endsWith("untracked$i.ts") })
                }
            }
        }

    @Test
    fun `getAllChangedFiles includes untracked files in directory in filesToExcludeFromHeuristic even when exceeding limit`() =
        runBlocking {
            val untrackedDir = File(testRepoPath, "untracked-dir")
            untrackedDir.mkdirs()

            val count = MAX_UNTRACKED_FILES_PER_LOCATION + 3
            for (i in 1..count) {
                File(untrackedDir, "file$i.ts").writeText("export const x$i = 1;")
            }

            val file1Path = File(untrackedDir, "file1.ts").absolutePath
            val file4Path = File(untrackedDir, "file4.ts").absolutePath
            val filesToExcludeFromHeuristic = setOf(file1Path, file4Path)

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    filesToExcludeFromHeuristic,
                )

            assertTrue("Should include file1.ts", changes.any { it.contains("file1.ts") })
            assertTrue("Should include file4.ts", changes.any { it.contains("file4.ts") })

            for (i in 1..count) {
                if (i != 1 && i != 4) {
                    assertFalse("Should not include file$i.ts", changes.any { it.contains("file$i.ts") })
                }
            }
        }

    @Test
    fun `getAllChangedFiles includes mix of tracked modified files and untracked files from filesToExcludeFromHeuristic`() =
        runBlocking {
            File(testRepoPath, "tracked.ts").writeText("export const tracked = 1;")
            exec("git", "add", "tracked.ts")
            exec("git", "commit", "-m", "Add tracked file")

            File(testRepoPath, "tracked.ts").writeText("export const tracked = 2;")

            val count = MAX_UNTRACKED_FILES_PER_LOCATION + 2
            for (i in 1..count) {
                File(testRepoPath, "untracked$i.ts").writeText("export const x$i = 1;")
            }

            val file3Path = File(testRepoPath, "untracked3.ts").absolutePath
            val filesToExcludeFromHeuristic = setOf(file3Path)

            val changes =
                gitChangeLister.getAllChangedFiles(
                    testRepoPath.absolutePath,
                    testRepoPath.absolutePath,
                    filesToExcludeFromHeuristic,
                )

            assertTrue("Should include tracked modified file", changes.any { it.endsWith("tracked.ts") })
            assertTrue("Should include untracked3.ts", changes.any { it.endsWith("untracked3.ts") })

            for (i in 1..count) {
                if (i != 3) {
                    assertFalse("Should not include untracked$i.ts", changes.any { it.endsWith("untracked$i.ts") })
                }
            }
        }
}
