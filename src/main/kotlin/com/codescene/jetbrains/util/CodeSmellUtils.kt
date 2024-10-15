package com.codescene.jetbrains.util

import com.codescene.jetbrains.codeInsight.CodeSmell
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

fun getTextRange(codeSmell: CodeSmell, editor: Editor): TextRange {
    val start = editor.document.getLineStartOffset(codeSmell.range.startLine - 1)
    val end = editor.document.getLineEndOffset(codeSmell.range.endLine - 1)

    return TextRange(start, end)
}