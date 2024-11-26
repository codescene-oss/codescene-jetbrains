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
                renameEvents.forEach {
                    ProgressManager.checkCanceled()
                    println("Before Rename: ${it.oldValue} -> ${it.newValue}")

                    deltaCache.updateKey(it.oldValue as String, it.newValue as String)
                    reviewCache.updateKey(it.oldValue as String, it.newValue as String)
                }

                deleteEvents.forEach {
                    ProgressManager.checkCanceled()
                    println("Before Delete: ${it.file.path}")

                    deltaCache.invalidate(it.file.path)
                    reviewCache.invalidate(it.file.path)
                }
            }

            override fun afterVfsChange() {
                renameEvents.forEach {
                    println("After Rename: ${it.oldValue} -> ${it.newValue}")
                }

                deleteEvents.forEach {
                    println("After Delete: ${it.file.path}")
                }
            }
        }
    }
}