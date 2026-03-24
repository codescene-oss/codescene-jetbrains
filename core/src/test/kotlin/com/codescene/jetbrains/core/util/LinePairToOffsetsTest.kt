package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LinePairToOffsetsTest {
    @Test
    fun `linePairToOffsets maps 1-based line pair through document line callbacks`() {
        val (start, end) =
            linePairToOffsets(
                startLineOneBased = 1,
                endLineOneBased = 3,
                lineStartOffset = { line -> if (line == 0) 10 else 0 },
                lineEndOffset = { line -> if (line == 2) 50 else 0 },
            )
        assertEquals(10, start)
        assertEquals(50, end)
    }
}
