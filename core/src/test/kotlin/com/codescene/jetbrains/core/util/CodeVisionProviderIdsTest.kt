package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CodeVisionProviderIdsTest {
    @Test
    fun `defaultCodeVisionProviderIds returns providers in refresh order`() {
        assertEquals(
            listOf(
                "AggregatedSmellCodeVisionProvider",
                "CodeHealthCodeVisionProvider",
                "AceCodeVisionProvider",
            ),
            defaultCodeVisionProviderIds,
        )
    }
}
