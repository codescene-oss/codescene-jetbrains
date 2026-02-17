package com.codescene.jetbrains.fileeditor.ace

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.components.webview.data.view.AceData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditorProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

data class CwfAceFileEditorProviderData(
    val aceData: AceData?,
    val functionToRefactor: FnToRefactor,
    val refactorResponse: RefactorResponse?,
)

val CWF_ACE_DATA_KEY: Key<CwfAceFileEditorProviderData> = Key.create("codescene.ace.data")

/**
 * The Ace view hosts CodeScene's auto-refactoring feature.
 */
@Suppress("UnstableApiUsage")
internal class CwfAceFileEditorProvider : BaseCwfFileEditorProvider<CwfAceFileEditorProviderData>(CWF_ACE_DATA_KEY) {
    override fun getEditorTypeId(): String = CwfAceFileEditorProvider::class.java.simpleName

    override fun createEditorInstance(
        project: Project,
        file: VirtualFile,
        data: CwfAceFileEditorProviderData,
    ): FileEditor = CwfAceFileEditor(project, file, data)
}
