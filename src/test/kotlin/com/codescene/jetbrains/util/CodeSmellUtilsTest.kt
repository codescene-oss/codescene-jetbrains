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

class CodeSmellUtilsTest {

    @Test
    fun `getTextRange returns correct TextRange`() {
        val codeSmell = CodeSmell(
            category = "",
            details = "",
            highlightRange = HighlightRange(394, 10, 394, 26)
        )
        val document = mockk<Document>()

        every { document.getLineStartOffset(codeSmell.highlightRange.startLine) } returns 10
        every { document.getLineEndOffset(codeSmell.highlightRange.endLine) } returns 50

        val result = getTextRange(codeSmell, document)

        assertEquals(TextRange(10, 50), result)
        verify(exactly = 1) { document.getLineStartOffset(393) }
        verify(exactly = 1) { document.getLineEndOffset(393) }
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
        val details = ""

        val result = formatCodeSmellMessage(category, details)

        assertEquals("Overall Code Complexity", result)
    }
}