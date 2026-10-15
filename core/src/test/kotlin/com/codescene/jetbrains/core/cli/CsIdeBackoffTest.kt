package com.codescene.jetbrains.core.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class CsIdeBackoffTest {
    @Test
    fun `backoff doubles to the cap`() {
        val backoff = CsIdeBackoff(initialMs = 1_000, maxMs = 4_000)
        var now = 0L

        backoff.recordFailure(now)
        assertEquals(1_000, backoff.remainingMs(now))
        assertEquals(500, backoff.remainingMs(now + 500))

        now += 1_000
        assertEquals(0, backoff.remainingMs(now))

        backoff.recordFailure(now)
        assertEquals(2_000, backoff.remainingMs(now))

        now += 2_000
        backoff.recordFailure(now)
        assertEquals(4_000, backoff.remainingMs(now))

        now += 4_000
        backoff.recordFailure(now)
        assertEquals(4_000, backoff.remainingMs(now))
    }

    @Test
    fun `reset clears cooldown`() {
        val backoff = CsIdeBackoff(initialMs = 1_000, maxMs = 30_000)
        backoff.recordFailure(0)
        assertEquals(1_000, backoff.remainingMs(0))
        backoff.reset()
        assertEquals(0, backoff.remainingMs(0))
    }
}
