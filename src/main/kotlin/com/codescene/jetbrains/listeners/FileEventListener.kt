package com.codescene.jetbrains.listeners

import com.codescene.jetbrains.services.cache.DeltaCacheService
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

//TODO: refactor
class FileEventListener(private val project: Project) : AsyncFileListener {
    override fun prepareChange(events: List<VFileEvent>): AsyncFileListener.ChangeApplier? {
        val renameEvents = events.filterIsInstance<VFilePropertyChangeEvent>()
            .filter { it.propertyName == VirtualFile.PROP_NAME }
        val deleteEvents = events.filterIsInstance<VFileDeleteEvent>()

        if (renameEvents.isEmpty() && deleteEvents.isEmpty()) {
            return null // No relevant events to process
        }

        return object : AsyncFileListener.ChangeApplier {
            val deltaCache = DeltaCacheService.getInstance(project)
            val reviewCache = ReviewCacheService.getInstance(project)

            override fun beforeVfsChange() {
                handleRenameEvents(renameEvents)
                handleDeleteEvents(deleteEvents)
            }

            private fun handleRenameEvents(
                renameEvents: List<VFilePropertyChangeEvent>,
                scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
            ) {
                scope.launch {
                    renameEvents.forEach {
                        ProgressManager.checkCanceled()
                        println("Before Rename: ${it.oldValue} -> ${it.newValue}.")

                        val oldKey = it.file.path
                        val newKey = "${it.file.parent.path}/${it.newValue}"

                        deltaCache.updateKey(oldKey, newKey)
                        reviewCache.updateKey(oldKey, newKey)
                    }
                }
            }

            private fun handleDeleteEvents(
                deleteEvents: List<VFileDeleteEvent>,
                scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
            ) {
                scope.launch {
                    deleteEvents.forEach {
                        ProgressManager.checkCanceled()
                        println("Before Delete: ${it.file.path} on ${Thread.currentThread().name}")

                        deltaCache.invalidate(it.file.path)
                        reviewCache.invalidate(it.file.path)
                    }
                }
            }
        }
    }
}