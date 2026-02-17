package com.codescene.jetbrains.codeInsight.codehealth

import com.codescene.jetbrains.services.htmlviewer.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.services.htmlviewer.codehealth.MarkdownCodeDelimiter
import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory.getSyntaxHighlighter
import com.intellij.psi.tree.IElementType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.awt.Color
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test


class CodeHighlighterTest {

    private val mockHighlighter = mockk<SyntaxHighlighter>()
    private val mockLexer = mockk<Lexer>()
    private val mockColorScheme = mockk<EditorColorsScheme>()
    private val mockColorsManager = mockk<EditorColorsManager>()
    private val mockTextAttributes = mockk<TextAttributesKey>()

    @Before
    fun setup() {
        mockkStatic(SyntaxHighlighterFactory::class, EditorColorsManager::class, Language::class)

        every { EditorColorsManager.getInstance() } returns mockColorsManager
        every { mockColorsManager.globalScheme } returns mockColorScheme
        every { getSyntaxHighlighter(any(Language::class), null, null) } returns mockHighlighter
        every { mockHighlighter.highlightingLexer } returns mockLexer
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `generateHighlightedHtml with multi-line delimiter returns correct output`() {
        val code = "public class Test {}"
        val expectedOutput = "<pre><span style=\"color: rgba(0, 0, 0, 255.0);\">public class Test {}</span></pre>"

        val result = doTestWithSingleLineDelimiter(code, "java", MarkdownCodeDelimiter.MULTI_LINE)
        assertEquals(expectedOutput, result)
    }

    @Test
    fun `generateHighlightedHtml with single-line delimiter returns`() {
        val code = "const x = 42;"
        val expectedOutput = "<code><span style=\"color: rgba(0, 0, 0, 255.0);\">const x = 42;</span></code>"

        val result = doTestWithSingleLineDelimiter(code, "javascript", MarkdownCodeDelimiter.SINGLE_LINE)
        assertEquals(expectedOutput, result)
    }

    @Test
    fun `generateHighlightedHtml with multiple tokens returns correct output`() {
        val code = "let x = 10;"
        val expectedOutput = "<code><span style=\"color: rgba(0, 0, 255, 255.0);\">let</span><span style=\"\"> x = </span><span style=\"color: rgba(255, 0, 0, 255.0);\">10</span><span style=\"\">;</span></code>"

        setupMocksForMultipleTokens()

        val result = generateHighlightedHtml(code, "javascript", MarkdownCodeDelimiter.SINGLE_LINE)
        assertEquals(expectedOutput, result)
    }

    private fun doTestWithSingleLineDelimiter(code: String, languageId: String, delimiter: MarkdownCodeDelimiter): String {
        setupMocksForSimpleHighlighting(code)

        return generateHighlightedHtml(code, languageId, delimiter)
    }

    private fun setupMocksForSimpleHighlighting(code: String) {
        every { mockLexer.start(any()) } returns Unit
        every { mockLexer.tokenType } returnsMany listOf(mockk(), null)
        every { mockLexer.tokenStart } returns 0
        every { mockLexer.tokenEnd } returns code.length
        every { mockLexer.advance() } returns Unit
        every { mockHighlighter.getTokenHighlights(any()) } returns arrayOf(mockTextAttributes)
        every { mockColorScheme.getAttributes(any()) } returns mockk {
            every { foregroundColor } returns Color.BLACK
        }
    }

    private fun setupMocksForMultipleTokens() {
        val tokens = listOf(
            Triple(0, 3, Color.BLUE),  // "let"
            Triple(3, 8, null),        // " x = "
            Triple(8, 10, Color.RED),   // "10"
            Triple(10, 11, null),       // ";"
        )
        var tokenIndex = 0
        every { mockLexer.start(any()) } returns Unit
        every { mockLexer.tokenType } returnsMany (List<IElementType>(tokens.size * 2) { mockk() } + listOf(null))
        every { mockLexer.tokenStart } answers { tokens[tokenIndex].first }
        every { mockLexer.tokenEnd } answers { tokens[tokenIndex].second }
        every { mockLexer.advance() } answers { tokenIndex++ }

        every { mockHighlighter.getTokenHighlights(any()) } answers {
            arrayOf(mockTextAttributes)
        }

        every { mockColorScheme.getAttributes(any()) } answers {
            val color = tokens[tokenIndex].third
            if (color != null) {
                mockk {
                    every { foregroundColor } returns color
                }
            } else {
                null
            }
        }
    }
}
