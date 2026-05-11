package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService

class FileEventHandler(
    private val deltaCache: IDeltaCacheService,
    private val reviewCache: IReviewCacheService,
    private val baselineReviewCache: IBaselineReviewCacheService,
) {
    fun handleRename(
        oldPath: String,
        newPath: String,
    ) {
        deltaCache.updateKey(oldPath, newPath)
        reviewCache.updateKey(oldPath, newPath)
        baselineReviewCache.updateKey(oldPath, newPath)
    }

    fun handleDelete(path: String) {
        // All three caches are content-based: they use file paths as keys but validate content
        // hashes on lookup. This means cache hits require BOTH path match AND content match.
        //
        // When files are temporarily deleted (e.g., git stash) then restored (git stash apply)
        // with identical content, the caches should hit. Not invalidating avoids expensive
        // 4-9 second re-analysis for unchanged files.
        //
        // If a file is deleted and a different file is created at the same path, the cache
        // will correctly miss because the content hash won't match.
        //
        // We hide the file from the Code Health Monitor UI by setting includeInCodeHealthMonitor
        // to false. When the file is restored and re-analyzed, put() will reset this to true.
        deltaCache.setIncludeInCodeHealthMonitor(path, false)
    }

    fun handleMove(
        oldPath: String,
        newPath: String,
    ) {
        deltaCache.updateKey(oldPath, newPath)
        reviewCache.updateKey(oldPath, newPath)
        baselineReviewCache.updateKey(oldPath, newPath)
    }

    fun handleFileCacheUpdate(update: FileCacheUpdate) {
        when (update) {
            is FileCacheUpdate.Rename -> handleRename(update.oldPath, update.newPath)
            is FileCacheUpdate.Move -> handleMove(update.oldPath, update.newPath)
            is FileCacheUpdate.Delete -> handleDelete(update.path)
        }
    }
}
