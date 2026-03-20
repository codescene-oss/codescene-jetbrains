package com.codescene.jetbrains.core.review

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.contracts.ILogger
import org.apache.commons.codec.digest.DigestUtils

data class ReviewCacheItem(
    val fileContents: String,
    val response: Review,
)

data class ReviewCacheQuery(
    val fileContents: String,
    val filePath: String,
)

data class ReviewCacheEntry(
    val fileContents: String,
    val filePath: String,
    val response: Review,
)

open class ReviewCacheService(
    log: ILogger,
) : CacheService<ReviewCacheQuery, ReviewCacheEntry, ReviewCacheItem, Review>(log) {
    override fun get(query: ReviewCacheQuery): Review? {
        val (fileContents, filePath) = query

        val hash = DigestUtils.sha256Hex(fileContents)

        val apiResponse = cache[filePath]?.response
        val cacheHit = cache.containsKey(filePath) && cache[filePath]?.fileContents == hash

        return if (cacheHit) apiResponse else null
    }

    override fun put(entry: ReviewCacheEntry) {
        val (fileContents, filePath, response) = entry

        val contentHash = DigestUtils.sha256Hex(fileContents)

        cache[filePath] = ReviewCacheItem(contentHash, response)
    }
}
