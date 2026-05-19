package com.codescene.jetbrains.core.testdoubles

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.git.pathCacheKey
import com.codescene.jetbrains.core.util.isSha256Hex
import org.apache.commons.codec.digest.DigestUtils

class InMemoryAceRefactorableFunctionsCache : IAceRefactorableFunctionsCache {
    private val cache = mutableMapOf<String, CacheItem>()

    override fun get(
        filePath: String,
        content: String,
    ): List<FnToRefactor> {
        val contentHash = if (isSha256Hex(content)) content else DigestUtils.sha256Hex(content)
        return cache[pathCacheKey(filePath)]?.takeIf { it.contentHash == contentHash }?.result.orEmpty()
    }

    override fun getLastKnown(filePath: String): List<FnToRefactor> = cache[pathCacheKey(filePath)]?.result.orEmpty()

    override fun put(
        filePath: String,
        content: String,
        result: List<FnToRefactor>,
    ) {
        cache[pathCacheKey(filePath)] = CacheItem(DigestUtils.sha256Hex(content), result)
    }

    override fun invalidate(filePath: String) {
        cache.remove(pathCacheKey(filePath))
    }

    private data class CacheItem(
        val contentHash: String,
        val result: List<FnToRefactor>,
    )
}
