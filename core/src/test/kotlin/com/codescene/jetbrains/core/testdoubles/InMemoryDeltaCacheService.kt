package com.codescene.jetbrains.core.testdoubles

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.delta.DeltaCacheQuery
import com.codescene.jetbrains.core.delta.DeltaCacheService

class InMemoryDeltaCacheService : IDeltaCacheService {
    private val delegate = DeltaCacheService(TestLogger)

    override fun get(query: DeltaCacheQuery): Pair<Boolean, Delta?> = delegate.get(query)

    override fun put(entry: DeltaCacheEntry) {
        delegate.put(entry)
    }

    override fun setIncludeInCodeHealthMonitor(
        filePath: String,
        include: Boolean,
    ) {
        delegate.setIncludeInCodeHealthMonitor(filePath, include)
    }

    override fun invalidate(filePath: String) {
        delegate.invalidate(filePath)
    }

    override fun updateKey(
        oldFilePath: String,
        newFilePath: String,
    ) {
        delegate.updateKey(oldFilePath, newFilePath)
    }

    override fun getAll(): List<Pair<String, DeltaCacheItem>> = delegate.getAll()
}
