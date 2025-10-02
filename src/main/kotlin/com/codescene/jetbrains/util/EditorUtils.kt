package com.codescene.jetbrains.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun getSelectedTextEditor(project: Project, filePath: String, source: String = ""): Editor? {
    var result: Editor? = null

    ApplicationManager.getApplication().invokeAndWait {
        val editorManager = FileEditorManager.getInstance(project)
        val openEditor = editorManager.allEditors.firstOrNull { it.file.path == filePath }
            ?: editorManager.allEditors.firstOrNull()

        if (editorManager.selectedTextEditor == null && openEditor != null) {
            Log.debug("Selected editor was null, opening file: ${openEditor.file.path}", source)
            editorManager.openFile(openEditor.file, true, true)
        }

        result = editorManager.selectedTextEditor
    }

    return result
}

fun closeWindow(fileName: String, project: Project) {
    val editorManager = FileEditorManagerEx.getInstanceEx(project)
    val docFile = editorManager.windows
        .firstOrNull { editorWindow ->
            editorWindow
                .fileList
                .any { it.nameWithoutExtension == fileName }
        }?.let { window ->
            val matchingFile = window
                .fileList
                .firstOrNull { it.nameWithoutExtension == fileName }
            matchingFile?.let { window to it }
        }

    val (editorWindow, virtualFile) = docFile ?: return

    CoroutineScope(Dispatchers.Main).launch { editorWindow.closeFile(virtualFile) }
}

data class ReplaceCodeSnippetArgs(
    val project: Project,
    val filePath: String,
    val startLine: Int,
    val endLine: Int,
    val newContent: String
)

fun replaceCodeSnippet(args: ReplaceCodeSnippetArgs) {
    val (project, filePath, startLine, endLine, newContent) = args

    val file = File(filePath)
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return
    val openFileDescriptor = OpenFileDescriptor(project, virtualFile)

    // Always run UI/editor actions on EDT
    ApplicationManager.getApplication().invokeLater {
        val editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)
        editor?.document?.let { document ->
            val start = maxOf(0, startLine - 1)
            val end = maxOf(0, endLine - 1)

            val firstLineStartOffset = document.getLineStartOffset(start)
            val lastLineEndOffset = document.getLineEndOffset(end)

            val content = adjustIndentation(document, start, newContent)

            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(firstLineStartOffset, lastLineEndOffset, content)
            }
        }
    }
}

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
fun adjustIndentation(document: Document, start: Int, newContent: String): String {
    if (start !in 0 until document.lineCount) return newContent

    val currentCodeFirstLine = getFirstLineText(start, document)
    val targetIndent = currentCodeFirstLine.takeWhile { it.isWhitespace() }

    val newContentLines = newContent.split("\n")

    // smallest indent among non-blank new content lines, which will serve as a reference point
    val newContentIndent = newContentLines
        .filter { it.isNotBlank() }
        .map { it.takeWhile { ch -> ch.isWhitespace() } }
        .filter { it.isNotEmpty() }
        .minByOrNull { it.length }
        ?: ""

    val newContentFirstNonBlankLine = newContentLines.firstOrNull { it.isNotBlank() } ?: return newContent
    val shouldSkipAdditionalRepetition = newContentFirstNonBlankLine.firstOrNull()?.isWhitespace() ?: false

    return adjustLines(newContentLines, shouldSkipAdditionalRepetition, targetIndent, newContentIndent)
}

private fun adjustLines(
    newContentLines: List<String>,
    shouldSkipAdditionalRepetition: Boolean,
    targetIndent: String,
    newContentIndent: String
) =
    newContentLines.joinToString("\n") { line ->
        if (line.isBlank()) {
            line
        } else {
            val prefixRepeats = countPrefixRepeats(line, newContentIndent)
            val repetition = if (shouldSkipAdditionalRepetition) prefixRepeats else prefixRepeats + 1

            targetIndent.repeat(repetition) + line.trimStart()
        }
    }

private fun countPrefixRepeats(text: String, prefix: String): Int {
    if (prefix.isEmpty()) return 0

    var count = 0
    var index = 0

    while (text.startsWith(prefix, index)) {
        count++
        index += prefix.length
    }

    return count
}

private fun getFirstLineText(start: Int, document: Document): String {
    val firstLineStartOffset = document.getLineStartOffset(start)
    val firstLineEndOffset = document.getLineEndOffset(start)
    return document.getText(TextRange(firstLineStartOffset, firstLineEndOffset))
}