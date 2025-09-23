package com.codescene.jetbrains.fileeditor.ace.acknowledge

import com.codescene.jetbrains.components.webview.data.view.AceAcknowledgeData
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

val CWF_ACE_ACKNOWLEDGE_DATA_KEY: Key<AceAcknowledgeData> = Key.create("codescene.ace.acknowledge.data")

@Suppress("UnstableApiUsage")
internal class CwfAceAcknowledgeEditorProvider : FileEditorProvider {

    /**
     * Method to decide when to use ACE acknowledge view in the CWF.
     */
    override fun accept(project: Project, file: VirtualFile): Boolean =
        file is LightVirtualFile && file.getUserData(CWF_ACE_ACKNOWLEDGE_DATA_KEY) != null

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val data = file.getUserData(CWF_ACE_ACKNOWLEDGE_DATA_KEY)
            ?: error("CwfData<AceAcknowledgeData> is required to open this editor.")

        return CwfAceAcknowledgeFileEditor(project, file, data)
    }

    override fun getEditorTypeId(): String = CwfAceAcknowledgeEditorProvider::class.java.simpleName

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }
}