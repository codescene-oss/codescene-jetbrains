package com.codescene.jetbrains.core.git

import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.contracts.ISavedFilesTracker
import java.util.TreeSet

class SavedFilesTracker(
    private val isFileOpenInEditor: (String) -> Boolean,
    private val logger: ILogger,
) : ISavedFilesTracker {
    private val savedFiles: MutableSet<String> = TreeSet(String.CASE_INSENSITIVE_ORDER)

    fun onFileSaved(filePath: String) {
        if (filePath.isEmpty()) return
        if (isFileOpenInEditor(filePath)) {
            synchronized(savedFiles) { savedFiles.add(filePath) }
            logger.info("Tracked saved file", "SavedFilesTracker")
        } else {
            logger.info("Ignoring save for file not open in editor", "SavedFilesTracker")
        }
    }

    override fun getSavedFiles(): Set<String> {
        synchronized(savedFiles) { return savedFiles.toSet() }
    }

    override fun clearSavedFiles() {
        val count = synchronized(savedFiles) { savedFiles.size }
        logger.info("Clearing $count saved files", "SavedFilesTracker")
        synchronized(savedFiles) { savedFiles.clear() }
    }

    override fun removeFromTracker(filePath: String) {
        if (filePath.isEmpty()) return
        synchronized(savedFiles) { savedFiles.remove(filePath) }
    }
}
