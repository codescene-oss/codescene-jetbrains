package com.codescene.jetbrains.util

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

val codeSmell = CodeSmell("", Range(394, 10, 394, 26), "")
const val START_OFFSET = 10
const val END_OFFSET = 50
val startLine = codeSmell.highlightRange.startLine - 1
val endLine = codeSmell.highlightRange.endLine - 1

class CodeSmellUtilsTest {
    @Test
    fun `getTextRange returns correct TextRange`() {
        val document = mockk<Document>()

        every { document.getLineStartOffset(startLine) } returns START_OFFSET
        every { document.getLineEndOffset(endLine) } returns END_OFFSET

        val result = getTextRange(codeSmell.highlightRange.startLine to codeSmell.highlightRange.endLine, document)

        assertEquals(TextRange(START_OFFSET, END_OFFSET), result)
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
