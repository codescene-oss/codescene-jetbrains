package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.ILogger
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GitChangeObserverTest {
    private lateinit var observer: GitChangeObserver
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

        observer =
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
    }

    @Test
    fun `tracker tracks added files`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("$workspacePath/test.ts")
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test.ts"))
            observer.processQueuedEvents()

            assertTrue(observer.getTrackedFiles().contains("/workspace/test.ts"))
        }

    @Test
    fun `removeFromTracker removes file from tracking`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("$workspacePath/test.ts")
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test.ts"))
            observer.processQueuedEvents()
            assertTrue(observer.getTrackedFiles().contains("/workspace/test.ts"))

            observer.removeFromTracker("/workspace/test.ts")

            assertFalse(observer.getTrackedFiles().contains("/workspace/test.ts"))
        }

    @Test
    fun `handleFileDelete removes tracked file`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("$workspacePath/test.ts")
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test.ts"))
            observer.processQueuedEvents()
            assertTrue(observer.getTrackedFiles().contains("/workspace/test.ts"))

            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.DELETE, "/workspace/test.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/test.ts"))
            assertTrue(deletedFiles.contains("/workspace/test.ts"))
        }

    @Test
    fun `handleFileDelete handles directory deletion`() =
        runBlocking {
            mockGitChangeLister.changedFiles =
                setOf(
                    "$workspacePath/subdir/file1.ts",
                    "$workspacePath/subdir/file2.ts",
                    "$workspacePath/other.ts",
                )
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/subdir/file1.ts"))
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/subdir/file2.ts"))
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/other.ts"))
            observer.processQueuedEvents()

            assertEquals(3, observer.getTrackedFiles().size)

            mockFileSystem.extensionOverrides["/workspace/subdir"] = ""
            mockGitChangeLister.changedFiles = setOf("$workspacePath/other.ts")
            observer.queueEvent(FileEvent(FileEventType.DELETE, "/workspace/subdir"))
            observer.processQueuedEvents()

            assertEquals(1, observer.getTrackedFiles().size)
            assertTrue(observer.getTrackedFiles().contains("/workspace/other.ts"))
            assertTrue(deletedFiles.contains("/workspace/subdir/file1.ts"))
            assertTrue(deletedFiles.contains("/workspace/subdir/file2.ts"))
        }

    @Test
    fun `shouldProcessFile rejects unsupported file types`() =
        runBlocking {
            val changedFilesSet = setOf("$workspacePath/test.txt")
            assertFalse(observer.shouldProcessFile("/workspace/test.txt", changedFilesSet))
        }

    @Test
    fun `shouldProcessFile accepts supported file types`() =
        runBlocking {
            val changedFilesSet = setOf("$workspacePath/test.ts")
            assertTrue(observer.shouldProcessFile("/workspace/test.ts", changedFilesSet))
        }

    @Test
    fun `handleFileChange filters files not in changed list`() =
        runBlocking {
            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.CHANGE, "/workspace/committed.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/committed.ts"))
        }

    @Test
    fun `change event removes tracked file when no longer changed`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("$workspacePath/test.ts")
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test.ts"))
            observer.processQueuedEvents()
            assertTrue(observer.getTrackedFiles().contains("/workspace/test.ts"))

            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.CHANGE, "/workspace/test.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/test.ts"))
            assertTrue(deletedFiles.contains("/workspace/test.ts"))
        }

    @Test
    fun `events are queued instead of processed immediately`() {
        observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test1.ts"))
        observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test2.ts"))

        assertEquals(2, observer.getQueuedEventCount())
        assertTrue(observer.getTrackedFiles().isEmpty())
    }

    @Test
    fun `getChangedFilesVsBaseline is called once per batch`() =
        runBlocking {
            mockGitChangeLister.changedFiles =
                setOf(
                    "$workspacePath/test1.ts",
                    "$workspacePath/test2.ts",
                    "$workspacePath/test3.ts",
                )
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test1.ts"))
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test2.ts"))
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/test3.ts"))

            val callCountBefore = mockGitChangeLister.callCount
            observer.processQueuedEvents()
            val callCountAfter = mockGitChangeLister.callCount

            assertEquals(1, callCountAfter - callCountBefore)
        }

    @Test
    fun `empty queue does not trigger unnecessary processing`() =
        runBlocking {
            val callCountBefore = mockGitChangeLister.callCount
            observer.processQueuedEvents()
            val callCountAfter = mockGitChangeLister.callCount

            assertEquals(0, callCountAfter - callCountBefore)
        }

    @Test
    fun `dispose cleans up resources`() {
        observer.start()
        observer.dispose()
    }

    @Test
    fun `dispose cleans up scheduled executor`() {
        observer.start()
        observer.dispose()
        observer.dispose()
    }

    @Test
    fun `duplicate change events in same batch only trigger one callback`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("$workspacePath/test.ts")
            observer.queueEvent(FileEvent(FileEventType.CHANGE, "/workspace/test.ts"))
            observer.queueEvent(FileEvent(FileEventType.CHANGE, "/workspace/test.ts"))
            observer.queueEvent(FileEvent(FileEventType.CHANGE, "/workspace/test.ts"))
            observer.processQueuedEvents()

            assertEquals(1, changedFiles.size)
        }

    @Test
    fun `CREATE event tracks file without requiring git4idea confirmation`() =
        runBlocking {
            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/new-file.ts"))
            observer.processQueuedEvents()

            assertTrue(observer.getTrackedFiles().contains("/workspace/new-file.ts"))
            assertTrue(changedFiles.contains("/workspace/new-file.ts"))
        }

    @Test
    fun `CREATE event skips unsupported file extensions`() =
        runBlocking {
            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/readme.txt"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/readme.txt"))
            assertFalse(changedFiles.contains("/workspace/readme.txt"))
        }

    @Test
    fun `CREATE event skips non-existent files`() =
        runBlocking {
            mockFileSystem.fileExistsOverrides["/workspace/ghost.ts"] = false
            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/ghost.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/ghost.ts"))
            assertFalse(changedFiles.contains("/workspace/ghost.ts"))
        }

    @Test
    fun `CREATE event processes file even when not in git changed list`() =
        runBlocking {
            mockGitChangeLister.changedFiles = setOf("$workspacePath/other.ts")
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/brand-new.ts"))
            observer.processQueuedEvents()

            assertTrue(observer.getTrackedFiles().contains("/workspace/brand-new.ts"))
            assertTrue(changedFiles.contains("/workspace/brand-new.ts"))
        }

    @Test
    fun `CREATE event skips gitignored files`() =
        runBlocking {
            mockGitService.ignoredFiles.add("/workspace/ignored.ts")
            mockGitChangeLister.changedFiles = emptySet()
            observer.queueEvent(FileEvent(FileEventType.CREATE, "/workspace/ignored.ts"))
            observer.processQueuedEvents()

            assertFalse(observer.getTrackedFiles().contains("/workspace/ignored.ts"))
            assertFalse(changedFiles.contains("/workspace/ignored.ts"))
        }
}
