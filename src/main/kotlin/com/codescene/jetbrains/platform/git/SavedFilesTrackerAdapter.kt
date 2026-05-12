package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
import com.codescene.jetbrains.core.git.SavedFilesTracker
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection

class SavedFilesTrackerAdapter(
    private val project: Project,
) : ISavedFilesTracker, Disposable {
    private var connection: MessageBusConnection? = null

    private val tracker =
        SavedFilesTracker(
            isFileOpenInEditor = { filePath ->
                isFileOpenInEditor(filePath)
            },
            logger = Log,
        )

    private fun isFileOpenInEditor(filePath: String): Boolean {
        val fileEditorManager = FileEditorManager.getInstance(project)
        return fileEditorManager.openFiles.any { it.path.equals(filePath, ignoreCase = true) }
    }

    fun start() {
        Log.info("Starting document save listener", "SavedFilesTrackerAdapter")
        connection?.disconnect()
        connection = project.messageBus.connect(this)
        connection?.subscribe(
            FileDocumentManagerListener.TOPIC,
            object : FileDocumentManagerListener {
                override fun beforeDocumentSaving(document: Document) {
                    val file = FileDocumentManager.getInstance().getFile(document)
                    Log.info("Document saving file=${file?.name ?: "null"}", "SavedFilesTrackerAdapter")
                    if (file != null) {
                        tracker.onFileSaved(file.path)
                    }
                }
            },
        )
    }

    override fun getSavedFiles(): Set<String> = tracker.getSavedFiles()

    override fun clearSavedFiles() = tracker.clearSavedFiles()

    override fun removeFromTracker(filePath: String) = tracker.removeFromTracker(filePath)

    override fun dispose() {
        Log.info("Disposing", "SavedFilesTrackerAdapter")
        connection?.disconnect()
        connection = null
    }
}
