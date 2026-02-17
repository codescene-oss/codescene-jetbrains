package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.util.acceptedFileNames
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
abstract class HtmlViewer<T>(private val project: Project) : LafManagerListener {
    private var data: T? = null
    private var editor: Editor? = null

    init {
        // Subscribe to theme updates when the service is initialized
        val bus = ApplicationManager.getApplication().messageBus.connect()
        bus.subscribe(LafManagerListener.TOPIC, this)
    }

    fun open(
        editor: Editor?,
        params: T,
    ) {
        data = params
        this.editor = editor

        val document = prepareFile(params)
        val fileEditorManager = FileEditorManager.getInstance(project)

        if (editor != null) {
            splitWindow(document, fileEditorManager)
        } else {
            openDocumentationWithoutActiveEditor(document, fileEditorManager)
        }

        sendTelemetry(params)
    }

    protected abstract fun prepareFile(params: T): LightVirtualFile

    protected open fun sendTelemetry(params: T) {}

    /**
     * Opens the given documentation file in a right-split editor.
     * Closes any other currently opened document files that match names in `acceptedFileNames` before opening the new file.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    private fun splitWindow(
        file: LightVirtualFile,
        fileEditorManager: FileEditorManager,
    ) {
        val editorManagerEx = FileEditorManagerEx.getInstanceEx(project)
        val docWindow =
            editorManagerEx.windows
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
    private fun openDocumentationWithoutActiveEditor(
        file: LightVirtualFile,
        fileEditorManager: FileEditorManager,
    ) {
        fileEditorManager.openFiles
            .filterIsInstance<LightVirtualFile>()
            .filter { shouldCloseFile(it, file) }
            .forEach { fileEditorManager.closeFile(it) }

        val docNotOpen = fileEditorManager.openFiles.none { it.name == file.name }
        if (docNotOpen) {
            fileEditorManager.openFile(file, false, true)
        }
    }

    private fun shouldCloseFile(
        existing: LightVirtualFile,
        new: LightVirtualFile,
    ) = existing != new && acceptedFileNames.contains(existing.nameWithoutExtension)

    override fun lookAndFeelChanged(p0: LafManager) {
        data?.let { open(editor, data!!) }
    }
}
