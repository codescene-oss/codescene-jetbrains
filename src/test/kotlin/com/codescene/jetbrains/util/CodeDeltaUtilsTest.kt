package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Function
import com.codescene.data.delta.Range
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeDeltaUtilsTest {
    private val function = Function("exampleFunction", Range(1, 10, 1, 15))

    private val details = listOf(
        ChangeDetail(
            ChangeDetail.ChangeType.DEGRADED,
            "Code Smell",
            "Duplicate Code",
            com.codescene.data.delta.Position(5, 40)
        ),
        ChangeDetail(
            ChangeDetail.ChangeType.INTRODUCED,
            "Code Smell",
            "Large Method",
            com.codescene.data.delta.Position(20, 40)
        ),
        ChangeDetail(
            ChangeDetail.ChangeType.FIXED,
            "Code Smell",
            "Bumpy Road Ahead",
            com.codescene.data.delta.Position(5, 40)
        ),
    )

    @Test
    fun `getFunctionDeltaTooltip resolved correctly when degrading issues and fixes exist`() {
        val result = getFunctionDeltaTooltip(function, details)

        assertEquals(
            "Function \"exampleFunction\" • 1 issue fixed • 2 issues degrading code health",
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