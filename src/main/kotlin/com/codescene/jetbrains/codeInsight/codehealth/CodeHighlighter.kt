package com.codescene.jetbrains.codeInsight.codehealth

import com.codescene.jetbrains.util.webRgba
import com.intellij.lang.Language
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory

object CodeHighlighter {
    /**
     * Method used to highlight code for different languages and replace non-highlighted code with highlighted one.
     * It is using Intellij's [SyntaxHighlighter] and [Lexer] to do the job.
     * Internally, code is being split into words and Lexer is then checking if the word is keyword,
     * or any other special kind which needs highlighting.
     * If needed, highlighting will be applied, if not default coloring will be left.
     * Besides highlighting, code will be converted to HTML code block by manually adding HTML tags.
     */
    fun generateHighlightedHtml(code: String, languageId: String, delimiter: MarkdownCodeDelimiter): String {
        var language = Language.ANY
        // currently used languages for code examples in our documentation
        // in case of new language used, it's mapping needs to be added here
        // this is because language ids don't follow same pattern
        // printSupportedLanguages method can be used for troubleshooting
        when (languageId) {
            "java" -> language = Language.findLanguageByID(languageId.uppercase()) ?: Language.ANY
            "javascript" -> language = Language.findLanguageByID("JavaScript") ?: Language.ANY
        }

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

    private fun printSupportedLanguages() {
        Language.getRegisteredLanguages().forEach {
            println(it)
        }
    }
}

enum class MarkdownCodeDelimiter(val value: String) {
    SINGLE_LINE("`"),
    MULTI_LINE("```")
}