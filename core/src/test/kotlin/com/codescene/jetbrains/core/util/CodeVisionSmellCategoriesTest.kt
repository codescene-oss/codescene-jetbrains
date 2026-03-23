package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeVisionSmellCategoriesTest {
    @Test
    fun `orderedForDisplay is not empty`() {
        assertTrue(CodeVisionSmellCategories.orderedForDisplay.isNotEmpty())
    }

    @Test
    fun `orderedForDisplay is sorted alphabetically`() {
        val list = CodeVisionSmellCategories.orderedForDisplay
        val sorted = list.sortedBy { it.lowercase() }
        assertEquals(sorted, list)
    }
}
