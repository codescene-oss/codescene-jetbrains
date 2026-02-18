package com.codescene.jetbrains.fileeditor.documentation

import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditorProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

val CWF_DOCS_DATA_KEY: Key<DocsData> = Key.create("codescene.documentation.data")

/**
 * The Docs view hosts CodeScene's code smell documentation files.
 */
@Suppress("UnstableApiUsage")
internal class CwfDocsFileEditorProvider : BaseCwfFileEditorProvider<DocsData>(CWF_DOCS_DATA_KEY) {
    override fun getEditorTypeId(): String = CwfDocsFileEditor::class.java.simpleName

    override fun createEditorInstance(
        project: Project,
        file: VirtualFile,
        data: DocsData,
    ): FileEditor = CwfDocsFileEditor(project, file, data)
}
