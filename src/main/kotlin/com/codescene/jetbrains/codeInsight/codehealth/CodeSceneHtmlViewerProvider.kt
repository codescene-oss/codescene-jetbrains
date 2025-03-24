package com.codescene.jetbrains.codeInsight.codehealth

import com.codescene.jetbrains.util.acceptedFileNames
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Suppress("UnstableApiUsage")
class CodeSceneHtmlViewerProvider : FileEditorProvider {

    /**
     * Method to decide when to use CodeSceneHtmlViewer.
     * It will be used only for internal Markdown documentation files.
     * This behaviour is controlled by codeSmellNames list which needs to be updated in case of newly added file.
     */
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.extension == "md" && acceptedFileNames.contains(file.nameWithoutExtension)
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return CodeSceneHtmlViewer(project, file)
    }

    override fun getEditorTypeId(): String = CodeSceneHtmlViewer::class.java.simpleName

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_OTHER_EDITORS

    override fun disposeEditor(editor: FileEditor) {
        editor.dispose()
    }
}
