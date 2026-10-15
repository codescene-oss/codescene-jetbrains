package com.codescene.jetbrains.core.util

import java.time.Duration

fun isExpiredCliCacheEntry(
    lastModifiedMillis: Long,
    nowMillis: Long,
    maxAge: Duration,
): Boolean = nowMillis - lastModifiedMillis >= maxAge.toMillis()
