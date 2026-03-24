package com.codescene.jetbrains.platform.editor.codeVision.providers

import com.codescene.jetbrains.core.util.CodeVisionSmellCategories
import org.junit.Assert.assertEquals
import org.junit.Test

class AggregatedSmellCodeVisionProviderTest {
    @Test
    fun `categoriesForLenses matches ordered display list`() {
        val method =
            AggregatedSmellCodeVisionProvider::class.java.getDeclaredMethod("categoriesForLenses").apply {
                isAccessible = true
            }

        @Suppress("UNCHECKED_CAST")
        val categories = method.invoke(AggregatedSmellCodeVisionProvider()) as List<String>
        assertEquals(CodeVisionSmellCategories.orderedForDisplay, categories)
    }
}
