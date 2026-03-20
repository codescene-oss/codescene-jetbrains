package com.codescene.jetbrains.core.contracts

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.review.ReviewCacheEntry
import com.codescene.jetbrains.core.review.ReviewCacheQuery

interface IReviewCacheService {
    fun get(query: ReviewCacheQuery): Review?

    fun put(entry: ReviewCacheEntry)

    fun invalidate(filePath: String)

    fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    )
}
