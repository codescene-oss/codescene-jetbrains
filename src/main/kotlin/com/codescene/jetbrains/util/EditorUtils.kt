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
    editorWindow.closeFile(virtualFile)
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
 * Under some circumstances, refactored code may not respect the
 * current file's indentation level. This helper ensures the snippet aligns
 * with the indentation of the line being replaced.
 *
 * Behavior:
 * - Looks at the indentation (whitespace prefix) of the target line in the document.
 * - If the first line of [newContent] has less indentation than that prefix,
 *   all lines in [newContent] will be adjusted so that they start with the
 *   same indent as the target line.
 * - If [newContent] already has enough indentation, it is returned unchanged.
 *
 * Notes:
 * - Empty lines are preserved without adding indentation.
 * - Only indentation (leading whitespace) is modified; the rest of the content
 *   remains as-is.
 *
 * @param document The IntelliJ [Document] containing the file content.
 * @param start The line number (0-based) in the document where replacement begins.
 * @param newContent The new code snippet to insert.
 * @return A string with adjusted indentation if needed.
 */
fun adjustIndentation(document: Document, start: Int, newContent: String): String {
    if (start !in 0 until document.lineCount) return newContent

    val firstLineStartOffset = document.getLineStartOffset(start)
    val firstLineEndOffset = document.getLineEndOffset(start)
    val lineText = document.getText(TextRange(firstLineStartOffset, firstLineEndOffset))

    val indent = lineText.takeWhile { it.isWhitespace() }

    val newLines = newContent.split("\n")
    val firstLine = newLines.firstOrNull() ?: ""
    val needsIndent = firstLine.takeWhile { it.isWhitespace() }.length < indent.length

    return if (needsIndent)
        newLines.joinToString("\n") { line ->
            if (line.isNotBlank()) indent + line else line // TODO: trim first line?
        }
    else
        newContent
}