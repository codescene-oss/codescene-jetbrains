package com.codescene.jetbrains.services.cache

import com.codescene.data.delta.Delta
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils

data class DeltaCacheItem(
    val headHash: String,
    val currentHash: String,
    val deltaApiResponse: Delta?,
)

data class DeltaCacheEntry(
    val filePath: String,
    val headContent: String,
    val currentFileContent: String,
    val deltaApiResponse: Delta?,
)

data class DeltaCacheQuery(
    val filePath: String,
    val headCommitContent: String,
    val currentFileContent: String
)

@Service(Service.Level.PROJECT)
class DeltaCacheService : CacheService<DeltaCacheQuery, DeltaCacheEntry, DeltaCacheItem, Pair<Boolean, Delta?>>() {
    companion object {
        fun getInstance(project: Project): DeltaCacheService = project.service<DeltaCacheService>()
    }

    /**
     * Retrieves a cached delta response for the given query.
     *
     * This method distinguishes between:
     * - A cache **hit**, where the query matches a stored entry. In this case, it returns the cached `Delta?`,
     *   which can be `null` if the API originally returned `null`.
     * - A cache **miss**, where no matching entry exists. This is indicated by the first value in the returned pair (`false`).
     *
     * @param query The query containing file path and content hashes.
     * @return A `Pair` where the first value (`Boolean`) indicates whether the result was found in cache,
     *         and the second value (`Delta?`) is the cached API response (which may be `null`).
     */
    override fun get(query: DeltaCacheQuery): Pair<Boolean, Delta?> {
        val (filePath, headCommitContent, currentFileContent) = query

        val oldHash = DigestUtils.sha256Hex(headCommitContent)
        val newHash = DigestUtils.sha256Hex(currentFileContent)

        val entry = cache[filePath]
        val apiResponse = entry?.deltaApiResponse

        val contentsMatch = entry?.headHash == oldHash && entry?.currentHash == newHash
        val isCacheHitOrNotStale = cache.containsKey(filePath) && contentsMatch

        return isCacheHitOrNotStale to apiResponse
    }

    override fun put(entry: DeltaCacheEntry) {
        val (filePath, headContent, currentFileContent, deltaApiResponse) = entry

        val headHash = DigestUtils.sha256Hex(headContent)
        val currentContentHash = DigestUtils.sha256Hex(currentFileContent)

        cache[filePath] = DeltaCacheItem(headHash, currentContentHash, deltaApiResponse)
    }
}