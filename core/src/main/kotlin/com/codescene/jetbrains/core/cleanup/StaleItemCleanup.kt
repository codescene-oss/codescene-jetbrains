package com.codescene.jetbrains.core.cleanup

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.git.pathCacheKey

class StaleItemCleanup(
    private val deltaCacheService: IDeltaCacheService,
    private val logger: ILogger,
) {
    fun cleanupStaleItems(
        gitChangedFiles: Set<String>,
        visibleEditorFiles: Set<String>,
    ): List<String> {
        val allowedFiles = gitChangedFiles + visibleEditorFiles
        val allowedKeys = allowedFiles.map { pathCacheKey(it) }.toSet()

        val cachedItems = deltaCacheService.getAll()
        val removedPaths = mutableListOf<String>()

        for ((filePath, _) in cachedItems) {
            val fileKey = pathCacheKey(filePath)
            if (!allowedKeys.contains(fileKey)) {
                logger.info("Removing stale item: $filePath", "StaleItemCleanup")
                deltaCacheService.setIncludeInCodeHealthMonitor(filePath, false)
                removedPaths.add(filePath)
            }
        }
        return removedPaths
    }
}
