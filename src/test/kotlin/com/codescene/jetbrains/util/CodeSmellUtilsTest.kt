package com.codescene.jetbrains.util

import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.data.HighlightRange
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

val codeSmell = CodeSmell(
    category = "",
    details = "",
    highlightRange = HighlightRange(394, 10, 394, 26)
)
const val startOffset = 10
const val endOffset = 50
val startLine = codeSmell.highlightRange.startLine - 1
val endLine = codeSmell.highlightRange.endLine - 1

class CodeSmellUtilsTest {
    @Test
    fun `getTextRange returns correct TextRange`() {
        val document = mockk<Document>()

        every { document.getLineStartOffset(startLine) } returns startOffset
        every { document.getLineEndOffset(endLine) } returns endOffset

        val result = getTextRange(codeSmell, document)

        assertEquals(TextRange(startOffset, endOffset), result)
        verify(exactly = 1) { document.getLineStartOffset(startLine) }
        verify(exactly = 1) { document.getLineEndOffset(endLine) }
    }

    @Test
    fun `formatCodeSmellMessage returns category and details when details are provided`() {
        val category = "Excess Number of Function Arguments"
        val details = "Arguments = 5"

        val result = formatCodeSmellMessage(category, details)

        assertEquals("Excess Number of Function Arguments (Arguments = 5)", result)
    }

    @Test
    fun `formatCodeSmellMessage returns category only when details are empty`() {
        val category = "Overall Code Complexity"

        val result = formatCodeSmellMessage(category, "")

        assertEquals("Overall Code Complexity", result)
    }
}