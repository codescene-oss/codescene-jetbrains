package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.ILogger

data class BaselineReviewCacheItem(
    val fileContents: String,
    val score: Double?,
)

data class BaselineReviewCacheQuery(
    val fileContents: String,
    val filePath: String,
)

data class BaselineReviewCacheEntry(
    val fileContents: String,
    val filePath: String,
    val score: Double?,
)

open class BaselineReviewCacheService(
    log: ILogger,
) : CacheService<
        BaselineReviewCacheQuery,
        BaselineReviewCacheEntry,
        BaselineReviewCacheItem,
        Pair<Boolean, Double?>,
    >(log),
    IBaselineReviewCacheService {
    override fun get(query: BaselineReviewCacheQuery): Pair<Boolean, Double?> {
        val (fileContents, filePath) = query
        val hash = hash(fileContents)
        val entry = cache[filePath]
        val cacheHit = cache.containsKey(filePath) && entry?.fileContents == hash
        return cacheHit to entry?.score
    }

    override fun put(entry: BaselineReviewCacheEntry) {
        val (fileContents, filePath, score) = entry
        cache[filePath] = BaselineReviewCacheItem(hash(fileContents), score)
    }
}
