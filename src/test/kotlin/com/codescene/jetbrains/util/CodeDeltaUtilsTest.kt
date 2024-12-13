package com.codescene.jetbrains.util

import com.codescene.jetbrains.data.*
import com.codescene.jetbrains.data.Function
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeDeltaUtilsTest {
    private val function = Function(
        name = "exampleFunction",
        range = HighlightRange(1, 10, 1, 15)
    )

    @Test
    fun `getFunctionDeltaTooltip resolved correctly when degrading issues exist`() {
        val details = listOf(
            ChangeDetails("Code Smell", "Duplicate Code", ChangeType.DEGRADED, Position(5, 15)),
            ChangeDetails("Code Smell", "Large Method", ChangeType.INTRODUCED, Position(20, 40)),
            ChangeDetails("Code Smell", "Bumpy Road Ahead", ChangeType.FIXED, Position(20, 40))
        )

        val result = getFunctionDeltaTooltip(function, details)

        assertEquals(
            "Function \"exampleFunction\" • Contains 2 issues degrading code health",
            result
        )
    }

    @Test
    fun `getFunctionDeltaTooltip resolved correctly when degrading issues do not exist`() {
        val details = emptyList<ChangeDetails>()

        val result = getFunctionDeltaTooltip(function, details)

        assertEquals(
            "Function \"exampleFunction\"",
            result
        )
    }
}