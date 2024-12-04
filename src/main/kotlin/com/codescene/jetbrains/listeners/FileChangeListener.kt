package com.codescene.jetbrains.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

class FileChangeListener(private val project: Project) : AsyncFileListener {
    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val renameEvents = events.filterIsInstance<VFilePropertyChangeEvent>()
            .filter { it.propertyName == VirtualFile.PROP_NAME }
        val deleteEvents = events.filterIsInstance<VFileDeleteEvent>()
        val moveEvent = events.filterIsInstance<VFileMoveEvent>()

        if (renameEvents.isEmpty() && deleteEvents.isEmpty() && moveEvent.isEmpty()) {
            return null // No relevant events to process
        }

        return FileEventProcessor(project, renameEvents, deleteEvents, moveEvent)
    }
}