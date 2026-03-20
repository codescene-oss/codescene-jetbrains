package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AceLensDataTest {
    private fun createFn(
        name: String = "fn",
        startLine: Int = 1,
        endLine: Int = 10,
    ): FnToRefactor {
        val range = mockk<com.codescene.data.ace.Range>(relaxed = true)
        every { range.startLine } returns startLine
        every { range.endLine } returns endLine
        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns name
        every { fn.range } returns range
        return fn
    }

    @Test
    fun `computeAceLenses returns empty list for null input`() {
        assertTrue(computeAceLenses(null).isEmpty())
    }

    @Test
    fun `computeAceLenses returns empty list for empty input`() {
        assertTrue(computeAceLenses(emptyList()).isEmpty())
    }

    @Test
    fun `computeAceLenses maps function names and ranges`() {
        val fns =
            listOf(
                createFn("fn1", 1, 10),
                createFn("fn2", 20, 30),
            )
        val result = computeAceLenses(fns)
        assertEquals(2, result.size)
        assertEquals("fn1", result[0].functionName)
        assertEquals(1, result[0].startLine)
        assertEquals(10, result[0].endLine)
        assertEquals("fn2", result[1].functionName)
        assertEquals(20, result[1].startLine)
        assertEquals(30, result[1].endLine)
    }
}
