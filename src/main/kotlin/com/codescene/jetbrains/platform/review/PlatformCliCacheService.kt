package com.codescene.jetbrains.platform.review

import com.codescene.jetbrains.core.util.isExpiredCliCacheEntry
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration

@Service(Service.Level.PROJECT)
class PlatformCliCacheService(
    private val project: Project,
) {
    private val cacheDirectory: Path by lazy { initializeCacheDirectory() }

    companion object {
        private val maxCacheAge: Duration = Duration.ofDays(30)

        fun getInstance(project: Project): PlatformCliCacheService = project.service<PlatformCliCacheService>()
    }

    fun getCachePath(): String {
        val path = cacheDirectory.toString()
        val exists = Files.exists(cacheDirectory)
        Log.info(
            "CLI cache path=$path exists=$exists userDir=${System.getProperty("user.dir")}",
            "PlatformCliCacheService",
        )
        return path
    }

    private fun initializeCacheDirectory(): Path {
        val directory =
            Path.of(
                PathManager.getSystemPath(),
                "codescene",
                project.locationHash,
                ".review-caches",
            )

        Files.createDirectories(directory)
        pruneExpiredFiles(directory)

        return directory
    }

    private fun pruneExpiredFiles(directory: Path) {
        val nowMillis = System.currentTimeMillis()

        Files.list(directory).use { paths ->
            paths
                .filter { Files.isRegularFile(it) }
                .forEach { path ->
                    runCatching {
                        val lastModifiedMillis = Files.getLastModifiedTime(path).toMillis()
                        if (isExpiredCliCacheEntry(lastModifiedMillis, nowMillis, maxCacheAge)) {
                            Files.deleteIfExists(path)
                        }
                    }.onFailure { error ->
                        Log.warn("Unable to prune CLI cache entry ${path.fileName}: ${error.message}")
                    }
                }
        }
    }
}
