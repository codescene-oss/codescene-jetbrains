package com.codescene.jetbrains.services.cache

import com.codescene.data.ace.FnToRefactor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.apache.commons.codec.digest.DigestUtils

data class AceRefactorableFunctionCacheItem(
    val content: String,
    val result: List<FnToRefactor>
)

data class AceRefactorableFunctionCacheEntry(
    val filePath: String,
    val content: String,
    val result: List<FnToRefactor>
)

data class AceRefactorableFunctionCacheQuery(
    val filePath: String,
    val content: String,
)

@Service(Service.Level.PROJECT)
class AceRefactorableFunctionsCacheService : CacheService<
        AceRefactorableFunctionCacheQuery,
        AceRefactorableFunctionCacheEntry,
        AceRefactorableFunctionCacheItem,
        List<FnToRefactor>
        >() {
    companion object {
        fun getInstance(project: Project): AceRefactorableFunctionsCacheService =
            project.service<AceRefactorableFunctionsCacheService>()
    }

    override fun get(query: AceRefactorableFunctionCacheQuery): List<FnToRefactor> {
        val (filePath, content) = query
        val code = DigestUtils.sha256Hex(content)

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
}