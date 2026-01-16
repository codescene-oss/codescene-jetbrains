package com.codescene.jetbrains.fileeditor

import com.codescene.jetbrains.util.acceptedFileNames
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
@Suppress("UnstableApiUsage")
class CodeSceneFileEditorProvider : FileEditorProvider {

    /**
     * Method to decide when to use CodeSceneHtmlViewer.
     * It will be used for internal documentation files and ACE.
     * This behaviour is controlled by `acceptedFileNames` list which needs to be updated in case of newly added file.
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension == "md" && acceptedFileNames.contains(file.nameWithoutExtension)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return CodeSceneFileEditor(project, file)
    }

    override fun getEditorTypeId(): String = CodeSceneFileEditor::class.java.simpleName

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }
}
