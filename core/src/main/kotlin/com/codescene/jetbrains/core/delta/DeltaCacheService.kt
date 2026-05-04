package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.git.pathCacheKey
import com.codescene.jetbrains.core.git.pathFileName
import com.codescene.jetbrains.core.review.CacheService
import org.apache.commons.codec.digest.DigestUtils

data class DeltaCacheItem(
    val headHash: String,
    val currentHash: String,
    val deltaApiResponse: Delta?,
    val includeInCodeHealthMonitor: Boolean = true,
    val filePath: String = "",
)

data class DeltaCacheEntry(
    val filePath: String,
    val headContent: String,
    val currentFileContent: String,
    val deltaApiResponse: Delta?,
    val includeInCodeHealthMonitor: Boolean = true,
)

data class DeltaCacheQuery(
    val filePath: String,
    val headCommitContent: String,
    val currentFileContent: String,
)

private const val DELTA_CACHE_LOG = "CodeSceneDeltaCache"

open class DeltaCacheService(
    log: ILogger,
) : CacheService<DeltaCacheQuery, DeltaCacheEntry, DeltaCacheItem, Pair<Boolean, Delta?>>(log) {
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

        val cacheKey = key(filePath)
        val entry = cache[cacheKey]
        val apiResponse = entry?.deltaApiResponse

        val headMatches = entry?.headHash == oldHash
        val currentMatches = entry?.currentHash == newHash
        val contentsMatch = headMatches && currentMatches
        val isCacheHitOrNotStale = cache.containsKey(cacheKey) && contentsMatch

        if (!isCacheHitOrNotStale) {
            val shortPath = pathFileName(filePath)
            val reason =
                when {
                    !cache.containsKey(cacheKey) -> "no_entry"
                    !headMatches && !currentMatches -> "head_and_current_mismatch"
                    !headMatches -> "head_mismatch"
                    else -> "current_mismatch"
                }
            log.debug(
                "delta cache miss file=$shortPath reason=$reason " +
                    "qHead=${oldHash.take(8)} qCur=${newHash.take(8)} " +
                    "lenBaseline=${headCommitContent.length} lenCurrent=${currentFileContent.length} " +
                    "sHead=${entry?.headHash?.take(8)} sCur=${entry?.currentHash?.take(8)}",
                DELTA_CACHE_LOG,
            )
        }

        return isCacheHitOrNotStale to apiResponse
    }

    open override fun put(entry: DeltaCacheEntry) {
        val headHash = DigestUtils.sha256Hex(entry.headContent)
        val currentContentHash = DigestUtils.sha256Hex(entry.currentFileContent)

        cache[key(entry.filePath)] =
            DeltaCacheItem(
                headHash,
                currentContentHash,
                entry.deltaApiResponse,
                entry.includeInCodeHealthMonitor,
                entry.filePath,
            )
        val shortPath = pathFileName(entry.filePath)
        log.debug(
            "delta cache put file=$shortPath head=${headHash.take(8)} cur=${currentContentHash.take(8)} " +
                "lenBaseline=${entry.headContent.length} lenCurrent=${entry.currentFileContent.length} " +
                "deltaNull=${entry.deltaApiResponse == null}",
            DELTA_CACHE_LOG,
        )
    }

    fun setIncludeInCodeHealthMonitor(
        filePath: String,
        include: Boolean,
    ) {
        val cacheKey = key(filePath)
        val existing = cache[cacheKey] ?: return
        cache[cacheKey] = existing.copy(includeInCodeHealthMonitor = include)
    }

    override fun getAll(): List<Pair<String, DeltaCacheItem>> {
        return cache.entries
            .map { (key, item) -> (item.filePath.ifEmpty { key }) to item }
            .filter { (_, item) ->
                if (!item.includeInCodeHealthMonitor) return@filter false
                val delta = item.deltaApiResponse ?: return@filter false
                val scoreChanged = (delta.scoreChange ?: 0.0) != 0.0
                val codeChanged = item.headHash != item.currentHash
                scoreChanged || codeChanged
            }
    }

    override fun key(filePath: String): String = pathCacheKey(filePath)
}
