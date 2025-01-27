package com.codescene.jetbrains.services.cache

import com.codescene.data.delta.Delta
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils

data class DeltaCacheItem(
    val headHash: String,
    val currentHash: String,
    val deltaApiResponse: Delta,
)

data class DeltaCacheEntry(
    val filePath: String,
    val headContent: String,
    val currentFileContent: String,
    val deltaApiResponse: Delta,
)

data class DeltaCacheQuery(
    val filePath: String,
    val headCommitContent: String,
    val currentFileContent: String
)

@Service(Service.Level.PROJECT)
class DeltaCacheService : CacheService<DeltaCacheQuery, DeltaCacheEntry, DeltaCacheItem, Delta>() {
    companion object {
        fun getInstance(project: Project): DeltaCacheService = project.service<DeltaCacheService>()
    }

    override fun get(query: DeltaCacheQuery): Delta? {
        val (filePath, headCommitContent, currentFileContent) = query

        val oldHash = DigestUtils.sha256Hex(headCommitContent)
        val newHash = DigestUtils.sha256Hex(currentFileContent)

        val entry = cache[filePath]
        val apiResponse = entry?.deltaApiResponse

        val contentsMatch = entry?.headHash == oldHash && entry?.currentHash == newHash
        val cacheHit = cache.containsKey(filePath) && contentsMatch

        return if (cacheHit) apiResponse else null
    }

    override fun put(entry: DeltaCacheEntry) {
        val (filePath, headContent, currentFileContent, deltaApiResponse) = entry

        val headHash = DigestUtils.sha256Hex(headContent)
        val currentContentHash = DigestUtils.sha256Hex(currentFileContent)

        cache[filePath] = DeltaCacheItem(headHash, currentContentHash, deltaApiResponse)
    }
}