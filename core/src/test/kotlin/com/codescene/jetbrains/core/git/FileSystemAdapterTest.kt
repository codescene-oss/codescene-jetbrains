package com.codescene.jetbrains.core.git

import java.io.File
import java.nio.file.Files
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileSystemAdapterTest {
    private lateinit var testDir: File
    private val adapter = FileSystemAdapter()

    @Before
    fun setup() {
        testDir = Files.createTempDirectory("test-filesystem").toFile()
    }

    @After
    fun teardown() {
        testDir.deleteRecursively()
    }

    @Test
    fun `readFile returns content when file exists`() {
        val file = File(testDir, "test.txt")
        file.writeText("Hello, World!")

        val content = adapter.readFile(file.absolutePath)

        assertEquals("Hello, World!", content)
    }

    @Test
    fun `readFile returns null when file does not exist`() {
        val nonExistentPath = File(testDir, "nonexistent.txt").absolutePath

        val content = adapter.readFile(nonExistentPath)

        assertNull(content)
    }

    @Test
    fun `readFile returns null when path is a directory`() {
        val dir = File(testDir, "subdir")
        dir.mkdirs()

        val content = adapter.readFile(dir.absolutePath)

        assertNull(content)
    }

    @Test
    fun `fileExists returns true when file exists`() {
        val file = File(testDir, "exists.txt")
        file.writeText("content")

        val exists = adapter.fileExists(file.absolutePath)

        assertTrue(exists)
    }

    @Test
    fun `fileExists returns false when file does not exist`() {
        val nonExistentPath = File(testDir, "nonexistent.txt").absolutePath

        val exists = adapter.fileExists(nonExistentPath)

        assertFalse(exists)
    }

    @Test
    fun `getRelativePath returns relative path between base and file`() {
        val basePath = testDir.absolutePath
        val filePath = File(testDir, "subdir/file.txt").absolutePath

        val relativePath = adapter.getRelativePath(basePath, filePath)

        assertEquals("subdir${File.separator}file.txt", relativePath)
    }

    @Test
    fun `getAbsolutePath combines parent and child paths`() {
        val parent = testDir.absolutePath
        val child = "subdir/file.txt"

        val absolutePath = adapter.getAbsolutePath(parent, child)

        assertTrue(absolutePath.contains("subdir"))
        assertTrue(absolutePath.contains("file.txt"))
    }

    @Test
    fun `getExtension returns file extension`() {
        val path = "/path/to/file.txt"

        val extension = adapter.getExtension(path)

        assertEquals("txt", extension)
    }

    @Test
    fun `getExtension returns empty string for file without extension`() {
        val path = "/path/to/file"

        val extension = adapter.getExtension(path)

        assertEquals("", extension)
    }

    @Test
    fun `getParent returns parent directory path`() {
        val path = File(testDir, "subdir/file.txt").absolutePath

        val parent = adapter.getParent(path)

        assertTrue(parent!!.endsWith("subdir"))
    }

    @Test
    fun `getParent returns null for root-level path`() {
        val path = "/"

        val parent = adapter.getParent(path)

        assertNull(parent)
    }
}
