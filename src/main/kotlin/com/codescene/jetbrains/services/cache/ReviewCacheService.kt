package com.codescene.jetbrains.services.cache

import com.codescene.jetbrains.data.CodeReview
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils

data class ReviewCacheItem(
    val fileContents: String,
    val response: CodeReview
)

data class ReviewCacheQuery(
    val fileContents: String,
    val filePath: String
)

data class ReviewCacheEntry(
    val fileContents: String,
    val filePath: String,
    val response: CodeReview
)

@Service(Service.Level.PROJECT)
class ReviewCacheService: CacheService<ReviewCacheQuery, ReviewCacheEntry, ReviewCacheItem, CodeReview>() {
    companion object {
        fun getInstance(project: Project): ReviewCacheService = project.service<ReviewCacheService>()
    }

    override fun getCachedResponse(query: ReviewCacheQuery): CodeReview? {
        val (fileContents, filePath) = query

        val hash = DigestUtils.sha256Hex(fileContents)

        val apiResponse = cache[filePath]?.response
        val cacheHit = cache.containsKey(filePath) && cache[filePath]?.fileContents == hash

        return if (cacheHit) apiResponse else null
    }

    override fun cacheResponse(entry: ReviewCacheEntry) {
        val (fileContents, filePath, response) = entry

        val contentHash = DigestUtils.sha256Hex(fileContents)

        cache[filePath] = ReviewCacheItem(contentHash, response)
    }
}