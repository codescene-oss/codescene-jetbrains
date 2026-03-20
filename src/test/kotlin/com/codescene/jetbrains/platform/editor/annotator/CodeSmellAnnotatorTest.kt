package com.codescene.jetbrains.platform.editor.annotator

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeSmellAnnotatorTest {
    private val annotator = CodeSmellAnnotator()

    @Test
    fun `doAnnotate returns null when both review and ACE cache are empty`() {
        val context = CodeSmellAnnotator.AnnotationContext(reviewCache = null, aceCache = emptyList())

        val result = annotator.doAnnotate(context)

        assertNull(result)
    }

    @Test
    fun `doAnnotate returns context when review cache exists`() {
        val review = mockk<Review>()
        val context = CodeSmellAnnotator.AnnotationContext(reviewCache = review, aceCache = emptyList())

        val result = annotator.doAnnotate(context)

        assertEquals(context, result)
    }

    @Test
    fun `doAnnotate returns context when ACE cache exists`() {
        val context = CodeSmellAnnotator.AnnotationContext(reviewCache = null, aceCache = listOf(mockk<FnToRefactor>()))

        val result = annotator.doAnnotate(context)

        assertEquals(context, result)
    }
}
