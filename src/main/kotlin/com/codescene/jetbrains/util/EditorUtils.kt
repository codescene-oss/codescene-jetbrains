package com.codescene.jetbrains.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project

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