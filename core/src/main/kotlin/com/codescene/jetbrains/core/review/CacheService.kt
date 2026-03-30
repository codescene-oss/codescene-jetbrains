package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.ILogger
import java.util.concurrent.ConcurrentHashMap
import org.apache.commons.codec.digest.DigestUtils

abstract class CacheService<Q, E, V, R>(
    protected val log: ILogger,
) {
    protected val cache = ConcurrentHashMap<String, V>()
    private val cacheImplementation = this::class.java.simpleName

    protected fun hash(content: String): String {
        return DigestUtils.sha256Hex(content)
    }

    abstract fun get(query: Q): R?

    abstract fun put(entry: E)

    open fun invalidate(filePath: String) {
        cache[filePath]?.let {
            cache.remove(filePath)
            log.debug("[$cacheImplementation] entry for key $filePath has been invalidated.")
        }
    }

    fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    ) {
        val entry = cache[oldFilePath]

        if (entry != null) {
            cache[newFilePath] = entry

            invalidate(oldFilePath)
            log.debug("[$cacheImplementation] $oldFilePath to $newFilePath.")
        }
    }

    open fun getAll(): List<Pair<String, V>> = cache.entries.map { it.key to it.value }
}
