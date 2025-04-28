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
import java.awt.Color

val codeSmell = CodeSmell("", Range(394, 10, 394, 26), "")
val color = Color(170, 99, 243, 100)
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

        val result = getTextRange(codeSmell.highlightRange.startLine to codeSmell.highlightRange.endLine, document)

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

    @Test
    fun `categoryToFileName replaces spaces with dashes`() {
        val result = categoryToFileName("Some Category")
        assertEquals("some-category", result)
    }

    @Test
    fun `categoryToFileName removes commas`() {
        val result = categoryToFileName("Some, Category With Comma")
        assertEquals("some-category-with-comma", result)
    }

    @Test
    fun `categoryToFileName returns empty string when received as input`() {
        val result = categoryToFileName("")
        assertEquals("", result)
    }

    @Test
    fun `categoryToFileName returns lowercased single word when received as input`() {
        val result = categoryToFileName("Category")
        assertEquals("category", result)
    }

    @Test
    fun `webRgba returns correct rgba format`() {
        val result = color.webRgba()
        assertEquals("rgba(170, 99, 243, 100.0)", result)
    }

    @Test
    fun `webRgba returns correct rgba format with custom alpha`() {
        val result = color.webRgba(20.0)
        assertEquals("rgba(170, 99, 243, 20.0)", result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return true for single backtick`() {
        val inputString = "`"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`')))
        assertEquals(true, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return true for single backtick in the end of string`() {
        val inputString = "some string with backtick `"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`')))
        assertEquals(true, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return true for single backtick in the middle of string`() {
        val inputString = "some string with backtick ` in the middle"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`')))
        assertEquals(true, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return true for single backtick at the start of string`() {
        val inputString = "` some string with backtick at start"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`')))
        assertEquals(true, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return false for two backticks`() {
        val inputString = "``"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`')))
        assertEquals(false, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return false for two backticks when checking second`() {
        val inputString = "``"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.lastIndexOf('`')))
        assertEquals(false, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return false for three backticks when checking first`() {
        val inputString = "some text before ``` and after"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`')))
        assertEquals(false, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return false for three backticks when checking second`() {
        val inputString = "some text before ``` and after"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.indexOf('`') + 1))
        assertEquals(false, result)
    }

    @Test
    fun `surroundingCharactersNotBackticks return false for three backticks when checking third`() {
        val inputString = "some text before ``` and after"
        val result = surroundingCharactersNotBackticks(CharactersBackticksData(inputString, inputString.lastIndexOf('`')))
        assertEquals(false, result)
    }
}