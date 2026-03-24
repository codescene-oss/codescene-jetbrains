package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeSmellAnnotatorLogicTest {
    @Test
    fun `shouldAnnotateCodeSmells returns false when review and ace cache are empty`() {
        assertEquals(false, shouldAnnotateCodeSmells(review = null, aceCache = emptyList()))
    }

    @Test
    fun `shouldAnnotateCodeSmells returns true when review exists`() {
        assertEquals(true, shouldAnnotateCodeSmells(review = mockk<Review>(), aceCache = emptyList()))
    }

    @Test
    fun `shouldAnnotateCodeSmells returns true when ace cache exists`() {
        assertEquals(true, shouldAnnotateCodeSmells(review = null, aceCache = listOf(mockk<FnToRefactor>())))
    }
}
