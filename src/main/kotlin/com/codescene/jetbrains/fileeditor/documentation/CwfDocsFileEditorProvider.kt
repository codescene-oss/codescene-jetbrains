package com.codescene.jetbrains.fileeditor.documentation

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditorProvider
import com.codescene.jetbrains.fileeditor.CodeSceneFileEditor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

data class CwfDocsFileEditorProviderData(
    val docsData: DocsData,
    val fnToRefactor: FnToRefactor?
)

val CWF_DOCS_DATA_KEY: Key<CwfDocsFileEditorProviderData> = Key.create("codescene.documentation.data")

/**
 * The Docs view hosts CodeScene's code smell documentation files.
 */
@Suppress("UnstableApiUsage")
internal class CwfDocsFileEditorProvider : BaseCwfFileEditorProvider<CwfDocsFileEditorProviderData>(CWF_DOCS_DATA_KEY) {
    override fun getEditorTypeId(): String = CodeSceneFileEditor::class.java.simpleName

    override fun createEditorInstance(
        project: Project,
        file: VirtualFile,
        data: CwfDocsFileEditorProviderData
    ): FileEditor = CwfDocsFileEditor(project, file, data.docsData)
}