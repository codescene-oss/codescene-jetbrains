package com.codescene.jetbrains.util

import com.codescene.jetbrains.data.CodeSmell
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

fun getTextRange(codeSmell: CodeSmell, editor: Editor): TextRange {
    val start = editor.document.getLineStartOffset(codeSmell.highlightRange.startLine - 1)
    val end = editor.document.getLineEndOffset(codeSmell.highlightRange.endLine - 1)

    return TextRange(start, end)
}

fun formatCodeSmellMessage(category: String, details: String): String =
    if (details.isNotEmpty()) "$category ($details)" else category