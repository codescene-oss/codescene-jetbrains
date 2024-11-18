package com.codescene.jetbrains.services

import com.codescene.jetbrains.data.CodeDelta
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

data class DeltaCacheEntry(
    val oldFileContent: String,
    val currentFileContent: String,
    val deltaApiResponse: CodeDelta,
)

data class DeltaCacheQuery(
    val filePath: String,
    val headCommitContent: String,
    val currentFileContent: String
)

//TODO: standardize cache
@Service(Service.Level.PROJECT)
class DeltaCacheService {
    private val cache = ConcurrentHashMap<String, DeltaCacheEntry>()

    companion object {
        fun getInstance(project: Project): DeltaCacheService = project.service<DeltaCacheService>()
    }

    fun getCachedResponse(query: DeltaCacheQuery): CodeDelta? {
        val (filePath, headCommitContent, currentFileContent) = query

        val oldHash = DigestUtils.sha256Hex(headCommitContent)
        val newHash = DigestUtils.sha256Hex(currentFileContent)

        val entry = cache[filePath]
        val apiResponse = entry?.deltaApiResponse

        val contentsMatch = entry?.oldFileContent == oldHash && entry?.currentFileContent == newHash
        val cacheHit = cache.containsKey(filePath) && contentsMatch

        return if (cacheHit) apiResponse else null
    }

    fun cacheResponse(filePath: String, entry: DeltaCacheEntry) {
        val (headCommitHash, currentFileContentHash, deltaApiResponse) = entry

        val headContentHash = DigestUtils.sha256Hex(headCommitHash)
        val currentContentHash = DigestUtils.sha256Hex(currentFileContentHash)

        cache[filePath] = DeltaCacheEntry(headContentHash, currentContentHash, deltaApiResponse)
    }
}