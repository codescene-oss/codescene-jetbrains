package com.codescene.jetbrains.fileeditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

/**
 * Generic base class for CWF-based `FileEditorProviders`.
 *
 * @param T The type of data stored in the `VirtualFile`.
 */
@Suppress("UnstableApiUsage")
abstract class BaseCwfFileEditorProvider<T>(
    private val dataKey: Key<T>
) : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean =
        file is LightVirtualFile && file.getUserData(dataKey) != null

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val data = file.getUserData(dataKey)
            ?: error("CwfData<$dataKey> is required to open this editor.")

        return createEditorInstance(project, file, data)
    }

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }

    /**
     * Implementations create the correct editor type for the given data.
     */
    protected abstract fun createEditorInstance(
        project: Project,
        file: VirtualFile,
        data: T
    ): FileEditor
}
