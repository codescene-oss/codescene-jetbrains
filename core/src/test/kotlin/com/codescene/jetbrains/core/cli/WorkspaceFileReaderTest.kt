package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceFileReaderTest {
    @Test
    fun `saved loaded text uses bytes`() {
        val bytes = "a\r\n".toByteArray()
        var readBytesCalled = false
        val reader =
            WorkspaceFileReader(
                openText = { LoadedText("a\n", unsaved = false) },
                readBytes = {
                    readBytesCalled = true
                    bytes
                },
            )

        val current = reader.current("/path")

        assertEquals(CurrentFile(GitBlobSha.ofBytes(bytes), null), current)
        assertTrue(readBytesCalled)
    }

    @Test
    fun `loaded text wins over bytes`() {
        var readBytesCalled = false
        val reader =
            WorkspaceFileReader(
                openText = { LoadedText("a\r\n", unsaved = true) },
                readBytes = {
                    readBytesCalled = true
                    "a\n".toByteArray()
                },
            )

        val current = reader.current("/path")

        assertEquals(CurrentFile(GitBlobSha.ofString("a\r\n"), "a\r\n"), current)
        assertTrue(!readBytesCalled)
    }

    @Test
    fun `no text uses bytes`() {
        val bytes = byteArrayOf(0xC3.toByte(), 0xBC.toByte())
        val reader =
            WorkspaceFileReader(
                openText = { null },
                readBytes = { bytes },
            )

        assertEquals(CurrentFile(GitBlobSha.ofBytes(bytes), null), reader.current("/path"))
    }

    @Test
    fun `empty text is hashed`() {
        val reader =
            WorkspaceFileReader(
                openText = { LoadedText("", unsaved = true) },
                readBytes = { byteArrayOf(1) },
            )

        assertEquals(CurrentFile("e69de29bb2d1d6434b8b29ae775ad8c2e48c5391", ""), reader.current("/path"))
    }

    @Test
    fun `neither text nor bytes gives null`() {
        val reader = WorkspaceFileReader(openText = { null }, readBytes = { null })

        assertNull(reader.current("/path"))
    }
}
