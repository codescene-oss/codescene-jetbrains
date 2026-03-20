package com.codescene.jetbrains.core.testdoubles

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.review.ReviewCacheEntry
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.review.ReviewCacheService

class InMemoryReviewCacheService : IReviewCacheService {
    private val delegate = ReviewCacheService(TestLogger)

    override fun get(query: ReviewCacheQuery): Review? = delegate.get(query)

    override fun put(entry: ReviewCacheEntry) {
        delegate.put(entry)
    }

    override fun invalidate(filePath: String) {
        delegate.invalidate(filePath)
    }

    override fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    ) {
        delegate.updateKey(oldFilePath, newFilePath)
    }
}
