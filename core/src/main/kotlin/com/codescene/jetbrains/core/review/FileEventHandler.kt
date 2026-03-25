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
        deltaCache.invalidate(path)
        reviewCache.invalidate(path)
        baselineReviewCache.invalidate(path)
    }

    fun handleMove(
        oldPath: String,
        newPath: String,
    ) {
        deltaCache.updateKey(oldPath, newPath)
        reviewCache.updateKey(oldPath, newPath)
        baselineReviewCache.updateKey(oldPath, newPath)
    }
}
