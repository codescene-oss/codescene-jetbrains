package com.codescene.jetbrains.services

import com.codescene.jetbrains.data.CodeReview
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

data class CacheQuery(
    val fileContents: String,
    val filePath: String
)

data class CacheEntry(
    val fileContents: String,
    val filePath: String,
    val response: CodeReview
)

@Service(Service.Level.PROJECT)
class ReviewCacheService {
    private val cache = ConcurrentHashMap<String, Pair<String, CodeReview>>()

    companion object {
        fun getInstance(project: Project): ReviewCacheService = project.service<ReviewCacheService>()
    }

    fun getCachedResponse(query: CacheQuery): CodeReview? {
        val (fileContents, filePath) = query

        val hash = DigestUtils.sha256Hex(fileContents)

        val apiResponse = cache[filePath]?.second
        val cacheHit = cache.containsKey(filePath) && cache[filePath]?.first == hash

        return if (cacheHit) apiResponse else null
    }

    fun cacheResponse(entry: CacheEntry) {
        val (fileContents, filePath, response) = entry

        val contentHash = DigestUtils.sha256Hex(fileContents)

        cache[filePath] = Pair(contentHash, response)
    }
}