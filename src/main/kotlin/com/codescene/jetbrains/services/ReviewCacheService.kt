package com.codescene.jetbrains.services

import com.codescene.jetbrains.data.ApiResponse
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class ReviewCacheService {
    private val cache = ConcurrentHashMap<String, Pair<String, ApiResponse>>()

    companion object {
        fun getInstance(project: Project): ReviewCacheService = project.service<ReviewCacheService>()
    }

    fun getCachedResponse(editor: Editor): ApiResponse? {
        val content = editor.document.text
        val hash = DigestUtils.sha256Hex(content)
        val path = editor.virtualFile.path

        val apiResponse = cache[path]?.second
        val cacheHit = cache.containsKey(path) && cache[path]?.first == hash

        return if (cacheHit) apiResponse else null
    }

    fun cacheResponse(filePath: String, code: String, response: ApiResponse) {
        val contentHash = DigestUtils.sha256Hex(code)

        cache[filePath] = Pair(contentHash, response)
    }
}