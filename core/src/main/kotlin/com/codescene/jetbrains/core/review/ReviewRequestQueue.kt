package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.git.pathCacheKey

class ReviewRequestQueue {
    private val lock = Any()
    private val ongoingReviews = mutableSetOf<String>()
    private val queue = mutableMapOf<String, () -> Unit>()

    fun requestReview(
        filePath: String,
        reviewAction: () -> Unit,
    ): Boolean {
        val key = pathCacheKey(filePath)
        synchronized(lock) {
            if (!ongoingReviews.add(key)) {
                queue[key] = reviewAction
                return false
            }
            return true
        }
    }

    fun finishReview(filePath: String): (() -> Unit)? {
        val key = pathCacheKey(filePath)
        synchronized(lock) {
            ongoingReviews.remove(key)
            return queue.remove(key)
        }
    }
}
