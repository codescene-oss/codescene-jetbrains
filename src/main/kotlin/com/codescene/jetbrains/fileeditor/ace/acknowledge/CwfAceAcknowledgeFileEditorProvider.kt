package com.codescene.jetbrains.fileeditor.ace.acknowledge

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.components.webview.data.view.AceAcknowledgeData
import com.codescene.jetbrains.fileeditor.BaseCwfFileEditorProvider
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile

data class CwfAceAcknowledgeEditorProviderData(
    val fnToRefactor: FnToRefactor,
    val aceAcknowledgeData: AceAcknowledgeData,
)

val CWF_ACE_ACKNOWLEDGE_DATA_KEY: Key<CwfAceAcknowledgeEditorProviderData> =
    Key.create("codescene.ace.acknowledge.data")

/**
 * The `ACE_ACKNOWLEDGE` view is used when the user has not acknowledged ACE yet,
 * but attempts to perform a refactoring.
 */
@Suppress("UnstableApiUsage")
internal class CwfAceAcknowledgeEditorProvider :
    BaseCwfFileEditorProvider<CwfAceAcknowledgeEditorProviderData>(CWF_ACE_ACKNOWLEDGE_DATA_KEY) {
    override fun getEditorTypeId(): String = CwfAceAcknowledgeEditorProvider::class.java.simpleName

    override fun createEditorInstance(
        project: Project,
        file: VirtualFile,
        data: CwfAceAcknowledgeEditorProviderData,
    ): FileEditor = CwfAceAcknowledgeFileEditor(project, file, data)
}
