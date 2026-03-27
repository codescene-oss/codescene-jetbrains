package com.codescene.jetbrains.core.contracts

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.delta.DeltaCacheQuery

interface IDeltaCacheService {
    fun get(query: DeltaCacheQuery): Pair<Boolean, Delta?>

    fun put(entry: DeltaCacheEntry)

    fun setIncludeInCodeHealthMonitor(
        filePath: String,
        include: Boolean,
    )

    fun invalidate(filePath: String)

    fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    )

    fun getAll(): List<Pair<String, DeltaCacheItem>>
}
