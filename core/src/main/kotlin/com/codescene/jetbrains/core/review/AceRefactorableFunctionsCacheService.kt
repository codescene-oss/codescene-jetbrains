package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.contracts.IAceRefactorableFunctionsCache
import com.codescene.jetbrains.core.contracts.ILogger
import com.codescene.jetbrains.core.util.isSha256Hex
import org.apache.commons.codec.digest.DigestUtils

data class AceRefactorableFunctionCacheItem(
    val content: String,
    val result: List<FnToRefactor>,
)

data class AceRefactorableFunctionCacheEntry(
    val filePath: String,
    val content: String,
    val result: List<FnToRefactor>,
)

data class AceRefactorableFunctionCacheQuery(
    val filePath: String,
    val content: String,
)

open class AceRefactorableFunctionsCacheService(
    log: ILogger,
) : CacheService<
        AceRefactorableFunctionCacheQuery,
        AceRefactorableFunctionCacheEntry,
        AceRefactorableFunctionCacheItem,
        List<FnToRefactor>,
    >(log),
    IAceRefactorableFunctionsCache {
    override fun get(query: AceRefactorableFunctionCacheQuery): List<FnToRefactor> {
        val (filePath, content) = query
        val code = if (isSha256Hex(content)) content else DigestUtils.sha256Hex(content)

        cache[filePath]?.let {
            if (it.content == code) return it.result
        }

        return emptyList()
    }

    override fun put(entry: AceRefactorableFunctionCacheEntry) {
        val (filePath, content, result) = entry
        val code = DigestUtils.sha256Hex(content)

        cache[filePath] = AceRefactorableFunctionCacheItem(code, result)
    }

    override fun get(
        filePath: String,
        content: String,
    ): List<FnToRefactor> = get(AceRefactorableFunctionCacheQuery(filePath, content))

    override fun put(
        filePath: String,
        content: String,
        result: List<FnToRefactor>,
    ) = put(AceRefactorableFunctionCacheEntry(filePath, content, result))
}
