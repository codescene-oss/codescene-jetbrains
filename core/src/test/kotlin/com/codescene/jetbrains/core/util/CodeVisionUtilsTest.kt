package com.codescene.jetbrains.core.util

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Function
import com.codescene.data.review.Range
import com.codescene.data.review.Review
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeVisionUtilsTest {
    private fun createCodeSmell(
        category: String = "Complex Method",
        details: String = "details",
    ): CodeSmell {
        val smell = mockk<CodeSmell>(relaxed = true)
        every { smell.category } returns category
        every { smell.details } returns details
        every { smell.highlightRange } returns mockk(relaxed = true)
        return smell
    }

    private fun createFunctionLevelSmell(
        functionName: String,
        range: Range,
        codeSmells: List<CodeSmell>,
    ): Function {
        val function = mockk<Function>(relaxed = true)
        every { function.function } returns functionName
        every { function.range } returns range
        every { function.codeSmells } returns codeSmells
        return function
    }

    @Test
    fun `returns empty list when review is null`() {
        val result = getCodeSmellsByCategory(null, "Complex Method")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns file level smells matching category`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns
            listOf(
                createCodeSmell("Complex Method"),
                createCodeSmell("Brain Method"),
            )
        every { review.functionLevelCodeSmells } returns emptyList()

        val result = getCodeSmellsByCategory(review, "Complex Method")
        assertEquals(1, result.size)
        assertEquals("Complex Method", result[0].category)
    }

    @Test
    fun `returns empty list when no smells match category`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns listOf(createCodeSmell("Brain Method"))
        every { review.functionLevelCodeSmells } returns emptyList()

        val result = getCodeSmellsByCategory(review, "Complex Method")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `file level smell has null functionName`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns listOf(createCodeSmell("Cat"))
        every { review.functionLevelCodeSmells } returns emptyList()

        val result = getCodeSmellsByCategory(review, "Cat")
        assertEquals(1, result.size)
        assertEquals(null, result[0].functionName)
        assertEquals(null, result[0].functionRange)
    }

    @Test
    fun `handles null fileLevelCodeSmells`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns null
        every { review.functionLevelCodeSmells } returns emptyList()

        val result = getCodeSmellsByCategory(review, "Complex Method")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `handles null functionLevelCodeSmells`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns emptyList()
        every { review.functionLevelCodeSmells } returns null

        val result = getCodeSmellsByCategory(review, "Complex Method")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `returns empty for empty review`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns emptyList()
        every { review.functionLevelCodeSmells } returns emptyList()

        val result = getCodeSmellsByCategory(review, "Any")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `preserves details from code smell`() {
        val review = mockk<Review>(relaxed = true)
        every { review.fileLevelCodeSmells } returns listOf(createCodeSmell("Cat", "important detail"))
        every { review.functionLevelCodeSmells } returns emptyList()

        val result = getCodeSmellsByCategory(review, "Cat")
        assertEquals("important detail", result[0].details)
    }

    @Test
    fun `sets function range for function level smells`() {
        val review = mockk<Review>(relaxed = true)
        val functionRange = Range(3, 1, 30, 1)
        every { review.fileLevelCodeSmells } returns emptyList()
        every { review.functionLevelCodeSmells } returns
            listOf(
                createFunctionLevelSmell(
                    functionName = "myFn",
                    range = functionRange,
                    codeSmells = listOf(createCodeSmell("Complex Method")),
                ),
            )

        val result = getCodeSmellsByCategory(review, "Complex Method")
        assertEquals(1, result.size)
        assertEquals(functionRange, result[0].functionRange)
    }
}
