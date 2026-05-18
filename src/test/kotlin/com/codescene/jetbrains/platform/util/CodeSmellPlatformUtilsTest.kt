package com.codescene.jetbrains.platform.util

import com.intellij.openapi.editor.Document
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeSmellPlatformUtilsTest {
    @Test
    fun `getTextRangeOrNull maps valid line range to text range`() {
        val document = mockk<Document>()
        every { document.lineCount } returns 5
        every { document.getLineStartOffset(1) } returns 10
        every { document.getLineEndOffset(2) } returns 30

        val range = getTextRangeOrNull(2 to 3, document)

        assertEquals(10, range?.startOffset)
        assertEquals(30, range?.endOffset)
    }

    @Test
    fun `getTextRangeOrNull returns null for stale line range beyond document`() {
        val document = mockk<Document>()
        every { document.lineCount } returns 5

        val range = getTextRangeOrNull(4 to 6, document)

        assertNull(range)
        verify(exactly = 0) { document.getLineStartOffset(any()) }
        verify(exactly = 0) { document.getLineEndOffset(any()) }
    }

    @Test
    fun `getTextRangeOrNull returns null for invalid line order`() {
        val document = mockk<Document>()
        every { document.lineCount } returns 5

        val range = getTextRangeOrNull(4 to 3, document)

        assertNull(range)
        verify(exactly = 0) { document.getLineStartOffset(any()) }
        verify(exactly = 0) { document.getLineEndOffset(any()) }
    }

    @Test
    fun `getTextRangeOrNull returns null for zero start line`() {
        val document = mockk<Document>()
        every { document.lineCount } returns 5

        val range = getTextRangeOrNull(0 to 1, document)

        assertNull(range)
        verify(exactly = 0) { document.getLineStartOffset(any()) }
        verify(exactly = 0) { document.getLineEndOffset(any()) }
    }
}
