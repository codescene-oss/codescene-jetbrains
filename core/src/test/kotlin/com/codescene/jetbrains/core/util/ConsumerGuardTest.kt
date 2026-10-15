package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.contracts.ILogger
import io.mockk.every
import io.mockk.mockk
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ConsumerGuardTest {
    private val warns = mutableListOf<Pair<String, String>>()
    private val logger =
        mockk<ILogger>(relaxed = true) {
            every { warn(any(), any()) } answers {
                warns += firstArg<String>() to (secondArg<String?>() ?: "")
            }
        }

    @Before
    fun clearWarns() {
        warns.clear()
    }

    @Test
    fun `a failing event is logged and the next event still runs`() {
        val guard = ConsumerGuard(logger, "svc")
        val handled = mutableListOf<String>()
        guard.handle("a") { throw IllegalStateException("boom") }
        guard.handle("b") { handled += it }
        assertEquals(listOf("b"), handled)
        assertEquals(listOf("String failed: boom" to "svc"), warns)
    }

    @Test
    fun `an exception without a message logs its class name`() {
        val guard = ConsumerGuard(logger, "svc")
        guard.handle(1) { throw IllegalStateException() }
        assertEquals(listOf("Int failed: java.lang.IllegalStateException" to "svc"), warns)
    }

    @Test
    fun `cancellation is rethrown and not logged`() {
        val guard = ConsumerGuard(logger, "svc")
        assertThrows(CancellationException::class.java) {
            guard.handle("x") { throw CancellationException() }
        }
        assertEquals(emptyList<Pair<String, String>>(), warns)
    }

    @Test
    fun `control-flow exceptions are rethrown and not logged`() {
        val guard = ConsumerGuard(logger, "svc") { it is UnsupportedOperationException }
        assertThrows(UnsupportedOperationException::class.java) {
            guard.handle("x") { throw UnsupportedOperationException() }
        }
        assertEquals(emptyList<Pair<String, String>>(), warns)
    }

    @Test
    fun `errors are not caught`() {
        val guard = ConsumerGuard(logger, "svc")
        assertThrows(AssertionError::class.java) {
            guard.handle("x") { throw AssertionError() }
        }
        assertEquals(emptyList<Pair<String, String>>(), warns)
    }

    @Test
    fun `a successful event logs nothing`() {
        val guard = ConsumerGuard(logger, "svc")
        var seen: String? = null
        guard.handle("ok") { seen = it }
        assertEquals("ok", seen)
        assertEquals(emptyList<Pair<String, String>>(), warns)
    }
}
