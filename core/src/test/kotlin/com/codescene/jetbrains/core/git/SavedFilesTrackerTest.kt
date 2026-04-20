package com.codescene.jetbrains.core.git

import java.util.TreeSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SavedFilesTrackerTest {
    private lateinit var openFiles: MutableSet<String>
    private lateinit var tracker: SavedFilesTracker

    @Before
    fun setup() {
        openFiles = TreeSet(String.CASE_INSENSITIVE_ORDER)
        tracker =
            SavedFilesTracker { filePath ->
                openFiles.any { it.equals(filePath, ignoreCase = true) }
            }
    }

    @Test
    fun `onFileSaved adds file when open in editor`() {
        val filePath = "/path/to/file.kt"
        openFiles.add(filePath)

        tracker.onFileSaved(filePath)

        assertTrue(tracker.getSavedFiles().contains(filePath))
    }

    @Test
    fun `onFileSaved ignores file when not open in editor`() {
        val filePath = "/path/to/file.kt"

        tracker.onFileSaved(filePath)

        assertFalse(tracker.getSavedFiles().contains(filePath))
    }

    @Test
    fun `getSavedFiles returns copy of set`() {
        val filePath = "/path/to/file.kt"
        openFiles.add(filePath)
        tracker.onFileSaved(filePath)

        val savedFiles = tracker.getSavedFiles()
        tracker.clearSavedFiles()

        assertTrue(savedFiles.contains(filePath))
        assertTrue(tracker.getSavedFiles().isEmpty())
    }

    @Test
    fun `clearSavedFiles empties the set`() {
        val filePath1 = "/path/to/file1.kt"
        val filePath2 = "/path/to/file2.kt"
        openFiles.addAll(listOf(filePath1, filePath2))
        tracker.onFileSaved(filePath1)
        tracker.onFileSaved(filePath2)

        tracker.clearSavedFiles()

        assertTrue(tracker.getSavedFiles().isEmpty())
    }

    @Test
    fun `removeFromTracker removes specific file`() {
        val filePath1 = "/path/to/file1.kt"
        val filePath2 = "/path/to/file2.kt"
        openFiles.addAll(listOf(filePath1, filePath2))
        tracker.onFileSaved(filePath1)
        tracker.onFileSaved(filePath2)

        tracker.removeFromTracker(filePath1)

        assertFalse(tracker.getSavedFiles().contains(filePath1))
        assertTrue(tracker.getSavedFiles().contains(filePath2))
    }

    @Test
    fun `thread safety - concurrent operations do not cause exceptions`() {
        val executor = Executors.newFixedThreadPool(4)
        val latch = CountDownLatch(400)
        val files = (1..100).map { "/path/to/file$it.kt" }
        openFiles.addAll(files)

        files.forEach { filePath ->
            executor.submit {
                tracker.onFileSaved(filePath)
                latch.countDown()
            }
            executor.submit {
                tracker.getSavedFiles()
                latch.countDown()
            }
            executor.submit {
                tracker.removeFromTracker(filePath)
                latch.countDown()
            }
            executor.submit {
                tracker.clearSavedFiles()
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        executor.shutdown()

        assertEquals(0, latch.count)
    }

    @Test
    fun `onFileSaved ignores empty path`() {
        tracker.onFileSaved("")
        assertTrue(tracker.getSavedFiles().isEmpty())
    }

    @Test
    fun `removeFromTracker handles empty path gracefully`() {
        val filePath = "/path/to/file.kt"
        openFiles.add(filePath)
        tracker.onFileSaved(filePath)

        tracker.removeFromTracker("")

        assertEquals(1, tracker.getSavedFiles().size)
    }

    @Test
    fun `onFileSaved uses case-insensitive matching`() {
        openFiles.add("/PATH/TO/FILE.kt")
        tracker.onFileSaved("/path/to/file.kt")

        assertEquals(1, tracker.getSavedFiles().size)
    }

    @Test
    fun `onFileSaved does not duplicate files with different case`() {
        val filePath = "/path/to/file.kt"
        openFiles.add(filePath)
        openFiles.add(filePath.uppercase())

        tracker.onFileSaved(filePath)
        tracker.onFileSaved(filePath.uppercase())
        tracker.onFileSaved("/PATH/TO/File.kt")

        assertEquals(1, tracker.getSavedFiles().size)
    }
}
