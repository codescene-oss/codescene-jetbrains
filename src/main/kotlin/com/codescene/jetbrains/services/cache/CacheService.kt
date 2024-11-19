package com.codescene.jetbrains.services.cache

import org.apache.commons.codec.digest.DigestUtils
import java.util.concurrent.ConcurrentHashMap

abstract class CacheService<Q, E, V, R> {
    protected val cache = ConcurrentHashMap<String, V>()

    protected fun hash(content: String): String {
        return DigestUtils.sha256Hex(content)
    }

    abstract fun getCachedResponse(query: Q): R?

    abstract fun cacheResponse(entry: E)
}