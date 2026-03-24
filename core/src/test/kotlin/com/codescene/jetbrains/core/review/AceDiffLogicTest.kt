package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AceDiffLogicTest {
    @Test
    fun `resolveAceDiffContext returns null when file path is missing`() {
        val result = resolveAceDiffContext(null, RangeCamelCase(4, 1, 2, 1), "updated")

        assertNull(result)
    }

    @Test
    fun `resolveAceDiffContext returns null when range is missing`() {
        val result = resolveAceDiffContext("src/Main.kt", null, "updated")

        assertNull(result)
    }

    @Test
    fun `resolveAceDiffContext returns null when refactored code is missing`() {
        val result = resolveAceDiffContext("src/Main.kt", RangeCamelCase(4, 1, 2, 1), null)

        assertNull(result)
    }

    @Test
    fun `resolveAceDiffContext returns data when all inputs are present`() {
        val range = RangeCamelCase(4, 1, 2, 1)

        val result = resolveAceDiffContext("src/Main.kt", range, "updated")

        assertEquals(AceDiffContext("src/Main.kt", range, "updated"), result)
    }

    @Test
    fun `buildAceDiffText replaces the selected range`() {
        val result = buildAceDiffText("fun a() {\nold\n}\n", 10..13, "new")

        assertEquals("fun a() {\nnew\n}\n", result)
    }
}
