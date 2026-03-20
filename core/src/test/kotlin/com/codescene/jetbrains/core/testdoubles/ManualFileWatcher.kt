package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.FileChangeCallback
import com.codescene.jetbrains.core.contracts.IFileWatcher

class ManualFileWatcher : IFileWatcher {
    private val callbacks = linkedSetOf<FileChangeCallback>()

    override fun addListener(callback: FileChangeCallback) {
        callbacks.add(callback)
    }

    override fun removeListener(callback: FileChangeCallback) {
        callbacks.remove(callback)
    }

    fun trigger(changedPaths: List<String>) {
        callbacks.forEach { it(changedPaths) }
    }
}
