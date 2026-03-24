package com.codescene.jetbrains.platform.util

import com.codescene.jetbrains.core.util.adjustIndentation as adjustIndentationFromAnchorLine
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange

/**
 * Adjusts the indentation of a code snippet before inserting it into a document.
 *
 * This helper aligns [newContent] with the indentation of the line at [start] in [document].
 * It preserves the relative indentation of all non-blank lines and ensures the snippet
 * integrates correctly with the surrounding code.
 *
 * Behavior:
 * - Determines the target indentation from the line at [start] in [document].
 * - Detects the smallest leading whitespace among non-blank lines in [newContent] (i.e. refactored code).
 * - For each non-blank line, it calculates how many times the original snippet's
 *   leading whitespace prefix repeats and adjusts it relative to the target indentation.
 * - Empty lines are preserved without adding indentation.
 * - The rest of the content (non-whitespace characters) is unchanged.
 */
fun adjustIndentation(
    document: Document,
    start: Int,
    newContent: String,
): String {
    if (start !in 0 until document.lineCount) return newContent

    val currentCodeFirstLine = getFirstLineText(start, document)
    return adjustIndentationFromAnchorLine(currentCodeFirstLine, newContent)
}

private fun getFirstLineText(
    start: Int,
    document: Document,
): String {
    val firstLineStartOffset = document.getLineStartOffset(start)
    val firstLineEndOffset = document.getLineEndOffset(start)
    return document.getText(TextRange(firstLineStartOffset, firstLineEndOffset))
}
