package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Function
import com.codescene.data.delta.Range
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeDeltaUtilsTest {
    private val function = Function().apply {
        name = "exampleFunction"
        range = Range().apply {
            startLine = 1
            startColumn = 10
            endLine = 1
            endColumn = 15
        }
    }

    private val details = listOf(
        ChangeDetail().apply {
            category = "Code Smell"
            description = "Duplicate Code"
            changeType = "degraded"
            position = com.codescene.data.delta.Position().apply {
                line = 5
                column = 40
            }
        },
        ChangeDetail().apply {
            category = "Code Smell"
            description = "Large Method"
            changeType = "introduced"
            position = com.codescene.data.delta.Position().apply {
                line = 20
                column = 40
            }
        },
        ChangeDetail().apply {
            category = "Code Smell"
            description = "Bumpy Road Ahead"
            changeType = "fixed"
            position = com.codescene.data.delta.Position().apply {
                line = 5
                column = 40
            }
        },
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