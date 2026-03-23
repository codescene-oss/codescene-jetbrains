package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.contracts.ILogger
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseServiceTest {
    @Test
    fun `runWithClassLoaderChange returns result and elapsed time`() {
        val logger = mockk<ILogger>(relaxed = true)
        val service = TestBaseService(logger)

        val result = service.run { "ok" }

        assertEquals("ok", result.result)
        assertTrue(result.elapsedMs >= 0)
        verify { logger.info(match { it.contains("Received response from CodeScene API in") }, "TestBaseService") }
        verify { logger.debug("Reverted to original ClassLoader", "TestBaseService") }
    }

    @Test
    fun `runWithClassLoaderChange rethrows exceptions`() {
        val logger = mockk<ILogger>(relaxed = true)
        val service = TestBaseService(logger)

        try {
            service.run { throw IllegalStateException("boom") }
            throw AssertionError("Expected exception to be thrown")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        verify {
            logger.debug(
                match { it.contains("Exception during CodeScene API operation. Error message: boom") },
                "TestBaseService",
            )
        }
        verify { logger.debug("Reverted to original ClassLoader", "TestBaseService") }
    }

    private class TestBaseService(
        log: ILogger,
    ) : BaseService(log) {
        fun <T> run(action: () -> T): TimedResult<T> = runWithClassLoaderChange(action)
    }
}
