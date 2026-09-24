package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService

object ReviewCacheProbe {
    fun isFullyCached(
        filePath: String,
        currentCode: String,
        reviewCache: IReviewCacheService,
        deltaCache: IDeltaCacheService,
    ): Boolean {
        if (reviewCache.get(ReviewCacheQuery(currentCode, filePath)) == null) {
            return false
        }
        return deltaCache.isCachedForCurrentContent(filePath, currentCode)
    }
}
