package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class FileEventHandlerTest {
    private val deltaCache = mockk<IDeltaCacheService>(relaxed = true)
    private val reviewCache = mockk<IReviewCacheService>(relaxed = true)
    private val baselineReviewCache = mockk<IBaselineReviewCacheService>(relaxed = true)
    private val handler = FileEventHandler(deltaCache, reviewCache, baselineReviewCache)

    @Test
    fun `handleRename updates all caches`() {
        handler.handleRename("old.kt", "new.kt")
        verify(exactly = 1) { deltaCache.updateKey("old.kt", "new.kt") }
        verify(exactly = 1) { reviewCache.updateKey("old.kt", "new.kt") }
        verify(exactly = 1) { baselineReviewCache.updateKey("old.kt", "new.kt") }
    }

    @Test
    fun `handleDelete invalidates all caches`() {
        handler.handleDelete("a.kt")
        verify(exactly = 1) { deltaCache.invalidate("a.kt") }
        verify(exactly = 1) { reviewCache.invalidate("a.kt") }
        verify(exactly = 1) { baselineReviewCache.invalidate("a.kt") }
    }

    @Test
    fun `handleMove updates all caches`() {
        handler.handleMove("old.kt", "new.kt")
        verify(exactly = 1) { deltaCache.updateKey("old.kt", "new.kt") }
        verify(exactly = 1) { reviewCache.updateKey("old.kt", "new.kt") }
        verify(exactly = 1) { baselineReviewCache.updateKey("old.kt", "new.kt") }
    }
}
