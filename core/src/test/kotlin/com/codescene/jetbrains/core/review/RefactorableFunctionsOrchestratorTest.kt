package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.testdoubles.InMemoryAceRefactorableFunctionsCache
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RefactorableFunctionsOrchestratorTest {
    @Test
    fun `fetchAndCache stores returned functions in cache and returns metadata`() {
        val cache = InMemoryAceRefactorableFunctionsCache()
        val orchestrator = RefactorableFunctionsOrchestrator(TestLogger, cache)
        val fn = mockk<FnToRefactor>(relaxed = true)

        val result =
            orchestrator.fetchAndCache(
                filePath = "a.kt",
                content = "content",
                serviceName = "svc",
                getFunctions = { TimedResult(listOf(fn), 120) },
            )

        assertEquals("a.kt", result.filePath)
        assertEquals("content", result.content)
        assertEquals(120L, result.elapsedMs)
        assertEquals(1, result.functions.size)
        assertSame(fn, result.functions.first())
        assertEquals(1, cache.get("a.kt", "content").size)
    }

    @Test
    fun `fetchAndCache stores empty results`() {
        val cache = InMemoryAceRefactorableFunctionsCache()
        val orchestrator = RefactorableFunctionsOrchestrator(TestLogger, cache)

        val result =
            orchestrator.fetchAndCache(
                filePath = "a.kt",
                content = "content",
                serviceName = "svc",
                getFunctions = { TimedResult(emptyList(), 10) },
            )

        assertEquals(0, result.functions.size)
        assertEquals(0, cache.get("a.kt", "content").size)
    }
}
