package com.codescene.jetbrains.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project

fun getSelectedTextEditor(project: Project, filePath: String, source: String = ""): Editor? {
    val editorManager = FileEditorManager.getInstance(project)
    val openEditor = editorManager.allEditors.firstOrNull { it.file.path == filePath }
        ?: editorManager.allEditors[0]

    if (editorManager.selectedTextEditor == null && openEditor != null) {
        Log.debug("Selected editor was null, opening file: ${openEditor.file.path}", source)
        editorManager.openFile(openEditor.file, true, true)
    }

    return editorManager.selectedTextEditor
}