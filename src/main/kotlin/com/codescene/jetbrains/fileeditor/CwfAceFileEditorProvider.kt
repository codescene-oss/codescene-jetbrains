package com.codescene.jetbrains.fileeditor

import com.codescene.jetbrains.components.webview.data.view.AceData
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

val CWF_ACE_DATA_KEY: Key<AceData> = Key.create("codescene.ace.data")

@Suppress("UnstableApiUsage")
internal class CwfAceFileEditorProvider : FileEditorProvider {

    /**
     * Method to decide when to use ACE view in the CWF.
     * The Ace view hosts CodeScene's auto-refactoring feature.
     */
    override fun accept(project: Project, file: VirtualFile): Boolean =
        file is LightVirtualFile && file.getUserData(CWF_ACE_DATA_KEY) != null

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val data = file.getUserData(CWF_ACE_DATA_KEY) ?: error("CwfData<AceData> is required to open this editor.")

        return CwfAceFileEditor(project, file, data)
    }

    override fun getEditorTypeId(): String = CwfAceFileEditorProvider::class.java.simpleName

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }
}