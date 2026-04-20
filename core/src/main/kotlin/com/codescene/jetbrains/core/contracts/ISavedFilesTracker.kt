package com.codescene.jetbrains.core.contracts

interface ISavedFilesTracker {
    fun getSavedFiles(): Set<String>

    fun clearSavedFiles()

    fun removeFromTracker(filePath: String)
}
