package com.codescene.jetbrains.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

fun getSelectedTextEditor(project: Project, filePath: String, source: String = ""): Editor? {
    val editorManager = FileEditorManager.getInstance(project)
    val openEditor = editorManager.allEditors.firstOrNull { it.file.path == filePath }
        ?: editorManager.allEditors.firstOrNull()

    if (editorManager.selectedTextEditor == null && openEditor != null) {
        Log.debug("Selected editor was null, opening file: ${openEditor.file.path}", source)
        editorManager.openFile(openEditor.file, true, true)
    }

    return editorManager.selectedTextEditor
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

    val file = java.io.File(filePath)
    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file) ?: return
    val openFileDescriptor = OpenFileDescriptor(project, virtualFile)

    // Always run UI/editor actions on EDT
    ApplicationManager.getApplication().invokeLater {
        val editor = FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true)
        editor?.document?.let { document ->
            val startOffset = document.getLineStartOffset(startLine - 1)
            val endOffset = document.getLineEndOffset(endLine - 1)

            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(startOffset, endOffset, newContent)
            }
        }
    }
}