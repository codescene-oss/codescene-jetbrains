package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.git.pathCacheKey

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
        val cacheKey = key(filePath)
        val entry = cache[cacheKey]
        val cacheHit = cache.containsKey(cacheKey) && entry?.fileContents == hash
        return cacheHit to entry?.score
    }

    override fun put(entry: BaselineReviewCacheEntry) {
        val (fileContents, filePath, score) = entry
        cache[key(filePath)] = BaselineReviewCacheItem(hash(fileContents), score)
    }

    override fun key(filePath: String): String = pathCacheKey(filePath)
}
