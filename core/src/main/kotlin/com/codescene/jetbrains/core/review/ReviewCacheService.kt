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

private const val REVIEW_CACHE_LOG = "CodeSceneReviewCache"

open class ReviewCacheService(
    log: ILogger,
) : CacheService<ReviewCacheQuery, ReviewCacheEntry, ReviewCacheItem, Review>(log) {
    override fun get(query: ReviewCacheQuery): Review? {
        val (fileContents, filePath) = query

        val hash = DigestUtils.sha256Hex(fileContents)

        val apiResponse = cache[filePath]?.response
        val storedHash = cache[filePath]?.fileContents
        val cacheHit = cache.containsKey(filePath) && storedHash == hash

        if (!cacheHit) {
            val shortPath = filePath.substringAfterLast('/')
            val reason =
                when {
                    !cache.containsKey(filePath) -> "no_entry"
                    else -> "content_hash_mismatch"
                }
            log.debug(
                "review cache miss file=$shortPath reason=$reason qHash=${hash.take(8)} " +
                    "sHash=${storedHash?.take(8)} lenContent=${fileContents.length}",
                REVIEW_CACHE_LOG,
            )
        }

        return if (cacheHit) apiResponse else null
    }

    override fun put(entry: ReviewCacheEntry) {
        val (fileContents, filePath, response) = entry

        val contentHash = DigestUtils.sha256Hex(fileContents)

        cache[filePath] = ReviewCacheItem(contentHash, response)
    }
}
