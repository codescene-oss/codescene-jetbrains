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

    @Test
    fun `adjustIndentation returns newContent when newContent has only blank lines`() {
        val document = mockDocument(lineCount = 1, lineTexts = listOf("  anchor"))
        assertEquals("\n\n", adjustIndentation(document, 0, "\n\n"))
    }

    @Test
    fun `adjustIndentation aligns unindented snippet to target line indent`() {
        val document = mockDocument(lineCount = 1, lineTexts = listOf("  hello"))
        val result = adjustIndentation(document, 0, "body")
        assertEquals("  body", result)
    }

    @Test
    fun `adjustIndentation adjusts relative indent for multi-line snippet`() {
        val document = mockDocument(lineCount = 2, lineTexts = listOf("    outer", "x"))
        val result = adjustIndentation(document, 0, "  line1\n  line2")
        assertEquals("    line1\n    line2", result)
    }

    @Test
    fun `adjustIndentation preserves empty lines in snippet`() {
        val document = mockDocument(lineCount = 1, lineTexts = listOf("\tbase"))
        val result = adjustIndentation(document, 0, "a\n\nb")
        assertEquals("\ta\n\n\tb", result)
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
        val fullText = lineTexts.joinToString("\n")
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
