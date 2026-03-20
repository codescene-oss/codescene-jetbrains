package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService

class FileEventHandler(
    private val deltaCache: IDeltaCacheService,
    private val reviewCache: IReviewCacheService,
) {
    fun handleRename(
        oldPath: String,
        newPath: String,
    ) {
        deltaCache.updateKey(oldPath, newPath)
        reviewCache.updateKey(oldPath, newPath)
    }

    fun handleDelete(path: String) {
        deltaCache.invalidate(path)
        reviewCache.invalidate(path)
    }

    fun handleMove(
        oldPath: String,
        newPath: String,
    ) {
        deltaCache.updateKey(oldPath, newPath)
        reviewCache.updateKey(oldPath, newPath)
    }
}
