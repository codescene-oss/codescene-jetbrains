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
        val key = key(filePath)
        cache[key]?.let {
            cache.remove(key)
            log.debug("[$cacheImplementation] entry for key $key has been invalidated.")
        }
    }

    fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    ) {
        val oldKey = key(oldFilePath)
        val newKey = key(newFilePath)
        val entry = cache[oldKey]

        if (entry != null) {
            cache[newKey] = entry

            invalidate(oldFilePath)
            log.debug("[$cacheImplementation] $oldKey to $newKey.")
        }
    }

    open fun getAll(): List<Pair<String, V>> = cache.entries.map { it.key to it.value }

    protected open fun key(filePath: String): String = filePath
}
