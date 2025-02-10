package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Function
import com.codescene.data.delta.Range
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeDeltaUtilsTest {
    private val function = Function("exampleFunction", Range(1, 10, 1, 15))

    private val details = listOf(
        ChangeDetail("degraded", "Code Smell", "Duplicate Code", com.codescene.data.delta.Position(5, 40)),
        ChangeDetail("introduced", "Code Smell", "Large Method", com.codescene.data.delta.Position(20, 40)),
        ChangeDetail("fixed", "Code Smell", "Bumpy Road Ahead", com.codescene.data.delta.Position(5, 40)),
    )

    @Test
    fun `getFunctionDeltaTooltip resolved correctly when degrading issues exist`() {
        val result = getFunctionDeltaTooltip(function, details)

        assertEquals(
            "Function \"exampleFunction\" • Contains 2 issues degrading code health",
            result
        )
    }

    @Test
    fun `getFunctionDeltaTooltip resolved correctly when degrading issues do not exist`() {
        val details = emptyList<ChangeDetail>()

        val result = getFunctionDeltaTooltip(function, details)

        assertEquals(
            "Function \"exampleFunction\"",
            result
        )
    }
}