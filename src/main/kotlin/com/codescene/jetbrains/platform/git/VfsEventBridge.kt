package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.git.FileEvent
import com.codescene.jetbrains.core.git.FileEventType
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.MessageBusConnection

class VfsEventBridge(
    private val project: Project,
    private val workspacePath: String,
    private val observer: GitChangeObserverAdapter,
) : Disposable {
    private var connection: MessageBusConnection? = null

    fun start() {
        Log.info("Starting VFS listener", "VfsEventBridge")
        connection?.disconnect()
        connection = project.messageBus.connect(this)
        connection?.subscribe(
            VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun after(events: List<VFileEvent>) {
                    var queuedCount = 0
                    for (event in events) {
                        val fileEvent = convertEvent(event) ?: continue
                        observer.queueEvent(fileEvent)
                        queuedCount++
                    }
                    Log.info("Received ${events.size} VFS events, queued $queuedCount", "VfsEventBridge")
                }
            },
        )
    }

    internal fun convertEvent(event: VFileEvent): FileEvent? {
        val path = event.path
        if (!isWithinWorkspace(path)) return null

        return when (event) {
            is VFileCreateEvent -> FileEvent(FileEventType.CREATE, path)
            is VFileDeleteEvent -> FileEvent(FileEventType.DELETE, path)
            is VFileContentChangeEvent -> FileEvent(FileEventType.CHANGE, path)
            else -> {
                Log.info("Ignoring event type=${event::class.simpleName}", "VfsEventBridge")
                null
            }
        }
    }

    internal fun isWithinWorkspace(path: String): Boolean {
        val normalizedWorkspace = if (workspacePath.endsWith("/")) workspacePath else "$workspacePath/"
        return path.startsWith(normalizedWorkspace) || path == workspacePath
    }

    override fun dispose() {
        Log.info("Disposing", "VfsEventBridge")
        connection?.disconnect()
        connection = null
    }
}
