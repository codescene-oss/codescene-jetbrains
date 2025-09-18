package com.codescene.jetbrains.fileeditor

import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

val CWF_DOCS_DATA_KEY: Key<DocsData> = Key.create("codescene.docs.data")

@Suppress("UnstableApiUsage")
internal class CwfDocsFileEditorProvider : FileEditorProvider {

    /**
     * Method to decide when to use DOCS view in the CWF.
     * The Docs view hosts CodeScene's code smell documentation files.
     */
    override fun accept(project: Project, file: VirtualFile) =
        file is LightVirtualFile && file.getUserData(CWF_DOCS_DATA_KEY) != null

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val data = file.getUserData(CWF_DOCS_DATA_KEY) ?: error("CwfData<DocsData> is required to open this editor.")

        return CwfDocsFileEditor(project, file, data)
    }

    override fun getEditorTypeId(): String = CodeSceneFileEditor::class.java.simpleName

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }
}