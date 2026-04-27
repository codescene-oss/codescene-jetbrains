package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.ILogger
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitChangeObserverPrePopulationTest {
    private lateinit var mockGitChangeLister: MockGitChangeLister
    private lateinit var mockSavedFilesTracker: MockSavedFilesTracker
    private lateinit var mockOpenFilesObserver: MockOpenFilesObserver
    private lateinit var mockFileSystem: MockFileSystem
    private lateinit var mockGitService: MockGitService
    private lateinit var logger: ILogger
    private var deletedFiles: MutableList<String> = mutableListOf()
    private var changedFiles: MutableList<String> = mutableListOf()

    private val workspacePath = "/workspace"
    private val gitRootPath = "/workspace"

    @Before
    fun setup() {
        deletedFiles = mutableListOf()
        changedFiles = mutableListOf()
        mockGitChangeLister = MockGitChangeLister()
        mockSavedFilesTracker = MockSavedFilesTracker()
        mockOpenFilesObserver = MockOpenFilesObserver()
        mockFileSystem = MockFileSystem()
        mockGitService = MockGitService()
        logger = mockk(relaxed = true)
    }

    private fun createObserver(): GitChangeObserver =
        GitChangeObserver(
            gitChangeLister = mockGitChangeLister,
            savedFilesTracker = mockSavedFilesTracker,
            openFilesObserver = mockOpenFilesObserver,
            fileSystem = mockFileSystem,
            gitService = mockGitService,
            onFileDeleted = { deletedFiles.add(it) },
            onFileChanged = { changedFiles.add(it) },
            workspacePath = workspacePath,
            gitRootPath = gitRootPath,
            logger = logger,
        )

    @Test
    fun `tracker is pre-populated when populateTrackerFromRepoState is called`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("src/file1.ts", "src/file2.ts")

            val observer = createObserver()
            observer.populateTrackerFromRepoState()

            val trackedFiles = observer.getTrackedFiles()
            assertTrue(trackedFiles.contains("/workspace/src/file1.ts"))
            assertTrue(trackedFiles.contains("/workspace/src/file2.ts"))
            assertEquals(2, trackedFiles.size)
        }

    @Test
    fun `pre-population converts relative paths to absolute paths`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("src/file.ts")

            val observer = createObserver()
            observer.populateTrackerFromRepoState()

            val trackedFiles = observer.getTrackedFiles()
            assertTrue(trackedFiles.contains("/workspace/src/file.ts"))
            assertFalse(trackedFiles.contains("src/file.ts"))
        }

    @Test
    fun `pre-populated files can be deleted without prior change event`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("src/file.ts")

            val observer = createObserver()
            observer.populateTrackerFromRepoState()

            assertTrue(observer.getTrackedFiles().contains("/workspace/src/file.ts"))

            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.DELETE, "/workspace/src/file.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/src/file.ts"))
            assertTrue(deletedFiles.contains("/workspace/src/file.ts"))
        }

    @Test
    fun `pre-population handles empty changed files list`() =
        runBlocking {
            mockGitChangeLister.changedFiles = emptySet()

            val observer = createObserver()
            observer.populateTrackerFromRepoState()

            assertTrue(observer.getTrackedFiles().isEmpty())
        }

    @Test
    fun `delete event fires for file that was open in editor when populateTrackerFromRepoState is called`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("src/open-in-editor.ts")
            mockOpenFilesObserver.files = setOf("/workspace/src/open-in-editor.ts")

            val observer = createObserver()
            observer.populateTrackerFromRepoState()

            assertTrue(observer.getTrackedFiles().contains("/workspace/src/open-in-editor.ts"))

            mockGitChangeLister.changedFiles = emptySet()

            observer.queueEvent(FileEvent(FileEventType.DELETE, "/workspace/src/open-in-editor.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/src/open-in-editor.ts"))
            assertTrue(deletedFiles.contains("/workspace/src/open-in-editor.ts"))
        }
}
