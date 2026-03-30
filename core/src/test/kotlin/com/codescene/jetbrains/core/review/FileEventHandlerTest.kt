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

    @Test
    fun `handleFileCacheUpdate routes Rename to updateKey on all caches`() {
        handler.handleFileCacheUpdate(FileCacheUpdate.Rename("a.kt", "b.kt"))
        verify(exactly = 1) { deltaCache.updateKey("a.kt", "b.kt") }
        verify(exactly = 1) { reviewCache.updateKey("a.kt", "b.kt") }
        verify(exactly = 1) { baselineReviewCache.updateKey("a.kt", "b.kt") }
    }

    @Test
    fun `handleFileCacheUpdate routes Move to updateKey on all caches`() {
        handler.handleFileCacheUpdate(FileCacheUpdate.Move("x.kt", "y.kt"))
        verify(exactly = 1) { deltaCache.updateKey("x.kt", "y.kt") }
        verify(exactly = 1) { reviewCache.updateKey("x.kt", "y.kt") }
        verify(exactly = 1) { baselineReviewCache.updateKey("x.kt", "y.kt") }
    }

    @Test
    fun `handleFileCacheUpdate routes Delete to invalidate on all caches`() {
        handler.handleFileCacheUpdate(FileCacheUpdate.Delete("z.kt"))
        verify(exactly = 1) { deltaCache.invalidate("z.kt") }
        verify(exactly = 1) { reviewCache.invalidate("z.kt") }
        verify(exactly = 1) { baselineReviewCache.invalidate("z.kt") }
    }
}
