package com.codescene.jetbrains.codeInsight.codehealth

import com.codescene.jetbrains.util.webRgba
import com.intellij.lang.Language
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory

object CodeHighlighter {

    fun generateHighlightedHtml(code: String, languageId: String, delimiter: MarkdownCodeDelimiter): String {
        val language = Language.findLanguageByID(languageId) ?: Language.ANY

        // Syntax highlighting
        val highlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, null, null)
        val lexer = highlighter.highlightingLexer
        lexer.start(code)
        val colorScheme: EditorColorsScheme = EditorColorsManager.getInstance().globalScheme
        val highlightedCode = StringBuilder()
        when (delimiter) {
            MarkdownCodeDelimiter.MULTI_LINE -> highlightedCode.append("<pre>")
            MarkdownCodeDelimiter.SINGLE_LINE -> highlightedCode.append("<code>")
        }

        while (lexer.tokenType != null) {
            val tokenText = code.substring(lexer.tokenStart, lexer.tokenEnd)
            val colorAttributes =
                highlighter.getTokenHighlights(lexer.tokenType).firstOrNull()?.let { colorScheme.getAttributes(it) }
            val colorStyle = colorAttributes?.foregroundColor?.let {
                "color: ${it.webRgba()};"
            } ?: ""

            highlightedCode.append("<span style=\"$colorStyle\">").append(tokenText).append("</span>")
            lexer.advance()
        }

        when (delimiter) {
            MarkdownCodeDelimiter.MULTI_LINE -> highlightedCode.append("</pre>")
            MarkdownCodeDelimiter.SINGLE_LINE -> highlightedCode.append("</code>")
        }

        return highlightedCode.toString().replace("\t", "    ")
    }
}

enum class MarkdownCodeDelimiter(val value: String) {
    SINGLE_LINE("`"),
    MULTI_LINE("```")
}