package com.codescene.jetbrains.core.cleanup

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.git.pathCacheKey

class InventoryMonitorSync(
    private val deltaCacheService: IDeltaCacheService,
    private val logger: ILogger,
) {
    fun sync(
        repoRoot: String,
        inventoryPaths: Set<String>,
        openPaths: Set<String>,
        excludedRoots: Set<String> = emptySet(),
    ): Int {
        val rootPrefix = rootPrefix(repoRoot)
        val excludedPrefixes = excludedRoots.map { rootPrefix(it) }
        val allowedKeys =
            (inventoryPaths + openPaths).map { pathCacheKey(it) }.toSet()
        var hidden = 0
        for ((filePath, _) in deltaCacheService.getAll()) {
            val fileKey = pathCacheKey(filePath)
            if (!fileKey.startsWith(rootPrefix)) continue
            if (excludedPrefixes.any { fileKey.startsWith(it) }) continue
            if (!allowedKeys.contains(fileKey)) {
                deltaCacheService.setIncludeInCodeHealthMonitor(filePath, false)
                hidden++
            }
        }
        for (path in inventoryPaths) {
            deltaCacheService.setIncludeInCodeHealthMonitor(path, true)
        }
        logger.info("monitor sync for $repoRoot: hidden=$hidden", "InventoryMonitorSync")
        return hidden
    }

    private fun rootPrefix(root: String): String = pathCacheKey(root).trimEnd('/') + "/"
}
