package com.codescene.jetbrains.util

import com.codescene.data.delta.ChangeDetail
import com.codescene.data.delta.Function
import com.codescene.data.delta.Range
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CodeDeltaUtilsTest(
    private val expected: String,
    private val excludedTypes: List<ChangeDetail.ChangeType>
) : BasePlatformTestCase() {
    private val function = Function("exampleFunction", Range(1, 10, 1, 15))

    private val details = listOf(
        ChangeDetail(
            ChangeDetail.ChangeType.DEGRADED,
            "Code Smell",
            "Duplicate Code",
            5
        ),
        ChangeDetail(
            ChangeDetail.ChangeType.INTRODUCED,
            "Code Smell",
            "Large Method",
            20
        ),
        ChangeDetail(
            ChangeDetail.ChangeType.FIXED,
            "Code Smell",
            "Bumpy Road Ahead",
            5
        ),
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun provideTestCases() = listOf(
            arrayOf(
                "Function \"exampleFunction\" • 1 issue fixed",
                listOf(ChangeDetail.ChangeType.INTRODUCED, ChangeDetail.ChangeType.DEGRADED)
            ),
            arrayOf(
                "Function \"exampleFunction\" • 2 issues degrading code health",
                listOf(ChangeDetail.ChangeType.FIXED, ChangeDetail.ChangeType.IMPROVED)
            )
        )
    }

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

    @Test
    fun `extract file name from HTML formatted string`() {
        val input = "<html>exampleFile.js<span style='color:gray;'>-6.32%</span></html>"

        val result = extractFileName(input)

        assertEquals("exampleFile.js", result)
    }

    @Test
    fun `return null when input does not match`() {
        val input = "exampleFile.txt"
        val result = extractFileName(input)

        assertNull(result)
    }

    @Test
    fun `return null when input is empty`() {
        val input = ""
        val result = extractFileName(input)

        assertNull(result)
    }
}