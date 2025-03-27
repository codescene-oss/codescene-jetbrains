package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.util.*
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

abstract class HtmlViewer<T>(private val project: Project) : LafManagerListener {
    init {
        // Subscribe to theme updates when the service is initialized
        val bus = ApplicationManager.getApplication().messageBus.connect()
        bus.subscribe(LafManagerListener.TOPIC, this)
    }

    //add telemetry handling before docs opened
    fun open(editor: Editor?, params: T) {
        val document = prepareFile(params)
        val fileEditorManager = FileEditorManager.getInstance(project)

//        !fileEditorManager.selectedFiles.contains(documentationFile) for general docs
        if (editor != null)
            splitWindow(document, fileEditorManager)
        else
            openDocumentationWithoutActiveEditor(document, fileEditorManager)
    }

    abstract fun prepareFile(params: T): LightVirtualFile

    /**
     * Opens the given documentation file in a right-split editor.
     * Closes any other currently opened document files that match names in `acceptedFileNames` before opening the new file.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    private fun splitWindow(file: LightVirtualFile, fileEditorManager: FileEditorManager) {
        val editorManagerEx = FileEditorManagerEx.getInstanceEx(project)
        val docWindow = editorManagerEx.windows
            .firstOrNull { editorWindow ->
                editorWindow.fileList.any { acceptedFileNames.contains(it.nameWithoutExtension) }
            }

        editorManagerEx.splitters.openInRightSplit(file, false)

        fileEditorManager.openFiles
            .filterIsInstance<LightVirtualFile>()
            .filter { shouldCloseFile(it, file) }
            .forEach { docWindow?.closeFile(it) }
    }

    /**
     * Opens a standalone documentation file (e.g., Code Health Monitor docs)
     * if there are no other open files and the documentation file is not already open.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    private fun openDocumentationWithoutActiveEditor(file: LightVirtualFile, fileEditorManager: FileEditorManager) {
        fileEditorManager.openFiles
            .filterIsInstance<LightVirtualFile>()
            .filter { shouldCloseFile(it, file) }
            .forEach { fileEditorManager.closeFile(it) }

        val docNotOpen = fileEditorManager.openFiles.none { it.name == file.name }
        if (docNotOpen)
            fileEditorManager.openFile(file, false, true)
    }

    private fun shouldCloseFile(existing: LightVirtualFile, new: LightVirtualFile) =
        existing != new && acceptedFileNames.contains(existing.nameWithoutExtension)
}