package com.codescene.jetbrains.services.cache

import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

abstract class CacheService<Q, E, V, R> {
    protected val cache = ConcurrentHashMap<String, V>()

    protected fun hash(content: String): String {
        return DigestUtils.sha256Hex(content)
    }

    abstract fun get(query: Q): R?

    abstract fun put(entry: E)

    fun invalidate(key: String) {
        cache.remove(key)

        val cacheName = this::class.java.simpleName

        println("$cacheName: entry for key $key has been invalidated.") //todo: debug log
    }

    fun updateKey(oldKey: String, newKey: String) {
        val entry = cache[oldKey]

        if (entry != null) {
            cache[newKey] = entry

            invalidate(oldKey)
        }
    }
}