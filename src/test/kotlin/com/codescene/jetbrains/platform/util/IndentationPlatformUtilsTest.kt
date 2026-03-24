package com.codescene.jetbrains.platform.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class IndentationPlatformUtilsTest {
    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `adjustIndentation returns newContent when start is negative`() {
        val document = mockDocument(lineCount = 3, lineTexts = listOf("a", "b", "c"))
        val input = "  x"
        assertEquals(input, adjustIndentation(document, -1, input))
    }

    @Test
    fun `adjustIndentation returns newContent when start is out of bounds`() {
        val document = mockDocument(lineCount = 2, lineTexts = listOf("a", "b"))
        val input = "  x"
        assertEquals(input, adjustIndentation(document, 2, input))
    }

    private fun mockDocument(
        lineCount: Int,
        lineTexts: List<String>,
    ): Document {
        val offsets = IntArray(lineCount + 1)
        offsets[0] = 0
        for (i in lineTexts.indices) {
            offsets[i + 1] = offsets[i] + lineTexts[i].length + if (i < lineTexts.lastIndex) 1 else 0
        }
        return mockk {
            every { this@mockk.lineCount } returns lineCount
            for (i in 0 until lineCount) {
                val lineStart = offsets[i]
                val lineEnd = offsets[i] + lineTexts[i].length
                every { getLineStartOffset(i) } returns lineStart
                every { getLineEndOffset(i) } returns lineEnd
                every { getText(TextRange(lineStart, lineEnd)) } returns lineTexts[i]
            }
        }
    }
}
