package com.codescene.jetbrains.services.cache

import com.codescene.jetbrains.util.Log
import java.util.concurrent.ConcurrentHashMap
import org.apache.commons.codec.digest.DigestUtils

abstract class CacheService<Q, E, V, R> {
    protected val cache = ConcurrentHashMap<String, V>()
    private val cacheImplementation = this::class.java.simpleName

    protected fun hash(content: String): String {
        return DigestUtils.sha256Hex(content)
    }

    abstract fun get(query: Q): R?

    abstract fun put(entry: E)

    fun invalidate(key: String) {
        cache[key]?.let {
            cache.remove(key)
            Log.debug("[$cacheImplementation] entry for key $key has been invalidated.")
        }
    }

    fun updateKey(oldKey: String, newKey: String) {
        val entry = cache[oldKey]

        if (entry != null) {
            cache[newKey] = entry

            invalidate(oldKey)
            Log.debug("[$cacheImplementation] $oldKey to $newKey.")
        }
    }

    open fun getAll(): List<Pair<String, V>> = cache.entries.map { it.key to it.value }
}