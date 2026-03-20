package com.codescene.jetbrains.platform.listeners

import com.codescene.jetbrains.core.contracts.FileChangeCallback
import com.codescene.jetbrains.core.contracts.IFileWatcher
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection

@Service
class VfsFileWatcher : IFileWatcher, Disposable {
    private val callbacks = linkedSetOf<FileChangeCallback>()
    private val connection: MessageBusConnection = ApplicationManager.getApplication().messageBus.connect(this)

    init {
        connection.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    val paths = events.map { it.path }.distinct()
                    if (paths.isEmpty()) return
                    callbacks.forEach { it(paths) }
                }
            },
        )
    }

    override fun addListener(callback: FileChangeCallback) {
        callbacks.add(callback)
    }

    override fun removeListener(callback: FileChangeCallback) {
        callbacks.remove(callback)
    }

    override fun dispose() {
        callbacks.clear()
    }
}
