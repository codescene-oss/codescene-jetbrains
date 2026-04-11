package com.codescene.jetbrains.platform.fs

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class VfsFileSystemTest {
    private lateinit var vfsFileSystem: VfsFileSystem
    private lateinit var mockLocalFileSystem: LocalFileSystem

    @Before
    fun setup() {
        mockLocalFileSystem = mockk(relaxed = true)
        mockkStatic(LocalFileSystem::class)
        every { LocalFileSystem.getInstance() } returns mockLocalFileSystem
        vfsFileSystem = VfsFileSystem()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getExtension returns extension for indexed file`() {
        val mockFile = mockk<VirtualFile>()
        every { mockFile.extension } returns "kt"
        every { mockLocalFileSystem.findFileByPath("/test/file.kt") } returns mockFile

        val extension = vfsFileSystem.getExtension("/test/file.kt")

        assertEquals("kt", extension)
    }

    @Test
    fun `getExtension works for unindexed file`() {
        every { mockLocalFileSystem.findFileByPath("/test/untracked.ts") } returns null

        val extension = vfsFileSystem.getExtension("/test/untracked.ts")

        assertEquals("ts", extension)
    }

    @Test
    fun `getParent returns parent for indexed file`() {
        val mockFile = mockk<VirtualFile>()
        val mockParent = mockk<VirtualFile>()
        every { mockFile.parent } returns mockParent
        every { mockParent.path } returns "/test"
        every { mockLocalFileSystem.findFileByPath("/test/file.kt") } returns mockFile

        val parent = vfsFileSystem.getParent("/test/file.kt")

        assertEquals("/test", parent)
    }

    @Test
    fun `getParent fails for unindexed file`() {
        every { mockLocalFileSystem.findFileByPath("/test/subdir/untracked.ts") } returns null

        val parent = vfsFileSystem.getParent("/test/subdir/untracked.ts")

        assertEquals("/test/subdir", parent)
    }

    @Test
    fun `getParent returns null for root path`() {
        every { mockLocalFileSystem.findFileByPath("/") } returns null

        val parent = vfsFileSystem.getParent("/")

        assertEquals(null, parent)
    }

    @Test
    fun `getAbsolutePath combines parent and child for indexed parent`() {
        val mockFile = mockk<VirtualFile>()
        every { mockFile.path } returns "/test/parent"
        every { mockLocalFileSystem.findFileByPath("/test/parent") } returns mockFile

        val absolutePath = vfsFileSystem.getAbsolutePath("/test/parent", "child.kt")

        assertEquals("/test/parent/child.kt", absolutePath)
    }

    @Test
    fun `getAbsolutePath fails for unindexed parent`() {
        every { mockLocalFileSystem.findFileByPath("/test/unindexed") } returns null

        val absolutePath = vfsFileSystem.getAbsolutePath("/test/unindexed", "child.kt")

        assertEquals("/test/unindexed/child.kt", absolutePath)
    }

    @Test
    fun `getExtension handles deleted file path`() {
        every { mockLocalFileSystem.findFileByPath("/test/deleted.java") } returns null

        val extension = vfsFileSystem.getExtension("/test/deleted.java")

        assertEquals("java", extension)
    }

    @Test
    fun `getParent handles deleted file path`() {
        every { mockLocalFileSystem.findFileByPath("/project/src/main/deleted.kt") } returns null

        val parent = vfsFileSystem.getParent("/project/src/main/deleted.kt")

        assertEquals("/project/src/main", parent)
    }

    @Test
    fun `getAbsolutePath handles paths with multiple separators`() {
        every { mockLocalFileSystem.findFileByPath("/test/parent") } returns null

        val absolutePath = vfsFileSystem.getAbsolutePath("/test/parent", "sub/child.kt")

        assertEquals("/test/parent/sub/child.kt", absolutePath)
    }
}
