package com.codescene.jetbrains.core.contracts

import com.codescene.jetbrains.core.review.BaselineReviewCacheEntry
import com.codescene.jetbrains.core.review.BaselineReviewCacheQuery

interface IBaselineReviewCacheService {
    fun get(query: BaselineReviewCacheQuery): Pair<Boolean, Double?>

    fun put(entry: BaselineReviewCacheEntry)

    fun invalidate(filePath: String)

    fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    )
}
