package com.codescene.jetbrains.services

import com.codescene.jetbrains.data.ApiResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
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
    val response: ApiResponse
)

@Service(Service.Level.PROJECT)
class ReviewCacheService {
    private val cache = ConcurrentHashMap<String, Pair<String, ApiResponse>>()

    companion object {
        fun getInstance(project: Project): ReviewCacheService = project.service<ReviewCacheService>()
    }

    fun getCachedResponse(query: CacheQuery): ApiResponse? {
        val (fileContents, filePath) = query

        val hash = DigestUtils.sha256Hex(fileContents)

        val apiResponse = cache[filePath]?.second
        val cacheHit = cache.containsKey(filePath) && cache[filePath]?.first == hash

        return if (cacheHit) apiResponse else null
    }

    fun getCachedResponse(editor: Editor): ApiResponse? {
        val content = editor.document.text

        val hash = DigestUtils.sha256Hex(content)
        val path = editor.virtualFile.path

        val apiResponse = cache[path]?.second
        val cacheHit = cache.containsKey(path) && cache[path]?.first == hash

        return if (cacheHit) apiResponse else null
    }

    fun cacheResponse(entry: CacheEntry) {
        val (fileContents, filePath, response) = entry

        val contentHash = DigestUtils.sha256Hex(fileContents)

        cache[filePath] = Pair(contentHash, response)
    }
}