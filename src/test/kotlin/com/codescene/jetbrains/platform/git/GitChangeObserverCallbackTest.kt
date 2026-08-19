package com.codescene.jetbrains.platform.git

import com.codescene.jetbrains.core.contracts.IBaselineReviewCacheService
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IReviewCacheService
import com.codescene.jetbrains.core.review.FileEventHandler
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class GitChangeObserverCallbackTest {
    private lateinit var deltaCache: IDeltaCacheService
    private lateinit var reviewCache: IReviewCacheService
    private lateinit var baselineReviewCache: IBaselineReviewCacheService
    private lateinit var fileEventHandler: FileEventHandler

    @Before
    fun setup() {
        deltaCache = mockk(relaxed = true)
        reviewCache = mockk(relaxed = true)
        baselineReviewCache = mockk(relaxed = true)

        fileEventHandler = FileEventHandler(deltaCache, reviewCache, baselineReviewCache)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `handleDelete hides file from Code Health Monitor but preserves all caches`() {
        fileEventHandler.handleDelete("/test/path/file.kt")

        verify(exactly = 1) { deltaCache.setIncludeInCodeHealthMonitor("/test/path/file.kt", false) }
        verify(exactly = 0) { deltaCache.invalidate("/test/path/file.kt") }
        verify(exactly = 0) { reviewCache.invalidate("/test/path/file.kt") }
        verify(exactly = 0) { baselineReviewCache.invalidate("/test/path/file.kt") }
    }
}
