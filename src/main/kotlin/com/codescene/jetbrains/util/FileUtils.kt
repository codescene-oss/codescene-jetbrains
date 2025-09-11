package com.codescene.jetbrains.util

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

object FileUtils {
    /**
     * Opens a standalone documentation file (e.g., Code Health Monitor docs)
     * if there are no other open files and the documentation file is not already open.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    fun openDocumentationWithoutActiveEditor(file: LightVirtualFile, fileEditorManager: FileEditorManager) {
        fileEditorManager.openFiles
            .filterIsInstance<LightVirtualFile>()
            .filter { shouldCloseFile(it, file) }
            .forEach { fileEditorManager.closeFile(it) }

        val docNotOpen = fileEditorManager.openFiles.none { it.name == file.name }
        if (docNotOpen)
            fileEditorManager.openFile(file, false, true)
    }

    /**
     * Opens the given documentation file in a right-split editor.
     * Closes any other currently opened document files that match names in `acceptedFileNames` before opening the new file.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    fun splitWindow(file: LightVirtualFile, fileEditorManager: FileEditorManager, project: Project) {
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

    private fun shouldCloseFile(existing: LightVirtualFile, new: LightVirtualFile) =
        existing != new && acceptedFileNames.contains(existing.nameWithoutExtension)
}