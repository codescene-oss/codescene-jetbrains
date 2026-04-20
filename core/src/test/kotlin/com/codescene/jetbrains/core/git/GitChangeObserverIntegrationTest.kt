package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.IFileSystem
import com.codescene.jetbrains.core.contracts.IOpenFilesObserver
import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitChangeObserverIntegrationTest {
    private lateinit var testRepoPath: File
    private lateinit var observer: GitChangeObserver
    private lateinit var gitChangeLister: TestGitChangeLister
    private var deletedFiles: MutableList<String> = mutableListOf()
    private var changedFiles: MutableList<String> = mutableListOf()

    @Before
    fun setup() {
        testRepoPath = Files.createTempDirectory("test-git-repo-observer").toFile()
        GitTestSupport.initGitRepoWithExtraFiles(testRepoPath)

        deletedFiles = mutableListOf()
        changedFiles = mutableListOf()
        gitChangeLister = TestGitChangeLister(testRepoPath)

        observer =
            GitChangeObserver(
                gitChangeLister = gitChangeLister,
                savedFilesTracker = NoOpSavedFilesTracker(),
                openFilesObserver = NoOpOpenFilesObserver(),
                fileSystem = RealFileSystem(testRepoPath.absolutePath),
                onFileDeleted = { deletedFiles.add(it) },
                onFileChanged = { changedFiles.add(it) },
                workspacePath = testRepoPath.absolutePath,
                gitRootPath = testRepoPath.absolutePath,
            )
    }

    @After
    fun teardown() {
        observer.dispose()
        testRepoPath.deleteRecursively()
    }

    private fun exec(vararg command: String): String = GitTestSupport.exec(testRepoPath, *command)

    @Test
    fun `returns empty set for clean repository`() =
        runBlocking {
            val changedFilesSet = observer.getChangedFilesVsBaseline()
            assertEquals(0, changedFilesSet.size)
        }

    @Test
    fun `detects new untracked files`() =
        runBlocking {
            val newFile = File(testRepoPath, "new-file.ts")
            newFile.writeText("export const x = 1;")

            val changedFilesSet = observer.getChangedFilesVsBaseline()

            assertTrue(changedFilesSet.any { it.endsWith("new-file.ts") })
        }

    @Test
    fun `detects modified files`() =
        runBlocking {
            val existingFile = File(testRepoPath, "existing.ts")
            existingFile.writeText("export const modified = true;")

            val changedFilesSet = observer.getChangedFilesVsBaseline()

            assertTrue(changedFilesSet.any { it.endsWith("existing.ts") })
        }

    @Test
    fun `detects staged files`() =
        runBlocking {
            val newFile = File(testRepoPath, "staged.py")
            newFile.writeText("print('staged')")
            exec("git", "add", "staged.py")

            val changedFilesSet = observer.getChangedFilesVsBaseline()

            assertTrue(changedFilesSet.any { it.endsWith("staged.py") })
        }

    @Test
    fun `combines status and diff changes`() =
        runBlocking {
            exec("git", "checkout", "-b", "feature-branch")

            val committedFile = File(testRepoPath, "committed.ts")
            committedFile.writeText("export const foo = 1;")
            exec("git", "add", "committed.ts")
            exec("git", "commit", "-m", "Add committed.ts")

            val uncommittedFile = File(testRepoPath, "uncommitted.ts")
            uncommittedFile.writeText("export const bar = 2;")

            val changedFilesSet = observer.getChangedFilesVsBaseline()

            assertTrue(changedFilesSet.any { it.endsWith("committed.ts") })
            assertTrue(changedFilesSet.any { it.endsWith("uncommitted.ts") })
        }

    @Test
    fun `handles files with whitespace in names`() =
        runBlocking {
            val fileWithSpaces = File(testRepoPath, "my file.ts")
            fileWithSpaces.writeText("export const x = 1;")

            val changedFilesSet = observer.getChangedFilesVsBaseline()

            assertTrue(changedFilesSet.any { it.contains("my file.ts") })
        }

    @Test
    fun `gitignored files are not tracked`() =
        runBlocking {
            val gitignore = File(testRepoPath, ".gitignore")
            gitignore.writeText("ignored.ts")
            exec("git", "add", ".gitignore")
            exec("git", "commit", "-m", "Add .gitignore")

            val ignoredFile = File(testRepoPath, "ignored.ts")
            ignoredFile.writeText("export const ignored = true;")

            observer.queueEvent(FileEvent(FileEventType.CREATE, ignoredFile.absolutePath))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains(ignoredFile.absolutePath))
        }

    @Test
    fun `file becomes tracked after gitignore removal`() =
        runBlocking {
            val gitignore = File(testRepoPath, ".gitignore")
            gitignore.writeText("previously-ignored.ts")
            exec("git", "add", ".gitignore")
            exec("git", "commit", "-m", "Add .gitignore")

            val previouslyIgnoredFile = File(testRepoPath, "previously-ignored.ts")
            previouslyIgnoredFile.writeText("export const x = 1;")

            observer.queueEvent(FileEvent(FileEventType.CREATE, previouslyIgnoredFile.absolutePath))
            observer.processQueuedEvents()
            assertFalse(observer.getTrackedFiles().contains(previouslyIgnoredFile.absolutePath))

            gitignore.writeText("")
            exec("git", "add", ".gitignore")
            exec("git", "commit", "-m", "Clear .gitignore")

            observer.queueEvent(FileEvent(FileEventType.CHANGE, previouslyIgnoredFile.absolutePath))
            observer.processQueuedEvents()

            assertTrue(observer.getTrackedFiles().contains(previouslyIgnoredFile.absolutePath))
        }

    @Test
    fun `integration - file modification and revert cycle`() =
        runBlocking {
            val testFile = File(testRepoPath, "cycle-test.ts")
            testFile.writeText("export const original = 1;")
            exec("git", "add", "cycle-test.ts")
            exec("git", "commit", "-m", "Add cycle-test.ts")

            testFile.writeText("export const modified = 2;")

            observer.queueEvent(FileEvent(FileEventType.CHANGE, testFile.absolutePath))
            observer.processQueuedEvents()
            assertTrue(observer.getTrackedFiles().contains(testFile.absolutePath))

            testFile.writeText("export const original = 1;")

            observer.queueEvent(FileEvent(FileEventType.CHANGE, testFile.absolutePath))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains(testFile.absolutePath))
        }
}

private class NoOpSavedFilesTracker : ISavedFilesTracker {
    override fun getSavedFiles(): Set<String> = emptySet()
}

private class NoOpOpenFilesObserver : IOpenFilesObserver {
    override fun getAllVisibleFileNames(): Set<String> = emptySet()
}

private class RealFileSystem(private val basePath: String) : IFileSystem {
    override fun readFile(path: String): String? {
        val file = File(path)
        return if (file.exists()) file.readText() else null
    }

    override fun fileExists(path: String): Boolean = File(path).exists()

    override fun getRelativePath(
        basePath: String,
        filePath: String,
    ): String {
        val base = Paths.get(basePath).toAbsolutePath().normalize()
        val file = Paths.get(filePath).toAbsolutePath().normalize()
        return base.relativize(file).toString()
    }

    override fun getAbsolutePath(
        parent: String,
        child: String,
    ): String = Paths.get(parent, child).toAbsolutePath().normalize().toString()

    override fun getExtension(path: String): String {
        val file = File(path)
        return file.extension
    }

    override fun getParent(path: String): String? = File(path).parent
}
