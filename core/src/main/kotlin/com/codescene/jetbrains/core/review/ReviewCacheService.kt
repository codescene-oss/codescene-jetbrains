package com.codescene.jetbrains.core.review

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.git.pathCacheKey
import com.codescene.jetbrains.core.git.pathFileName
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
        val cacheKey = key(filePath)

        val apiResponse = cache[cacheKey]?.response
        val storedHash = cache[cacheKey]?.fileContents
        val cacheHit = cache.containsKey(cacheKey) && storedHash == hash

        if (!cacheHit) {
            val shortPath = pathFileName(filePath)
            val reason =
                when {
                    !cache.containsKey(cacheKey) -> "no_entry"
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

    fun getLastKnown(filePath: String): Review? = cache[key(filePath)]?.response

    override fun put(entry: ReviewCacheEntry) {
        val (fileContents, filePath, response) = entry

        val contentHash = DigestUtils.sha256Hex(fileContents)

        cache[key(filePath)] = ReviewCacheItem(contentHash, response)
    }

    override fun key(filePath: String): String = pathCacheKey(filePath)
}
