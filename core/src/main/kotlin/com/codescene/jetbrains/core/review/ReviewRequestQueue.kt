package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.git.pathCacheKey
import java.util.concurrent.ConcurrentHashMap

class ReviewRequestQueue {
    private val ongoingReviews = ConcurrentHashMap.newKeySet<String>()
    private val queue = ConcurrentHashMap<String, () -> Unit>()

    fun requestReview(
        filePath: String,
        reviewAction: () -> Unit,
    ): Boolean {
        val key = pathCacheKey(filePath)
        if (!ongoingReviews.add(key)) {
            queue[key] = reviewAction
            return false
        }
        return true
    }

    fun finishReview(filePath: String): (() -> Unit)? {
        val key = pathCacheKey(filePath)
        ongoingReviews.remove(key)
        return queue.remove(key)
    }
}
