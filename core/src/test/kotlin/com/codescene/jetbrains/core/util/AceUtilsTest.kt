package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.settings.AceStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AceUtilsTest {
    @Test
    fun `getRefactorableFunction returns matching function by category and line`() {
        val fn = mockFn("Complex Method", 10)
        val result = getRefactorableFunction("Complex Method", 10, listOf(fn))
        assertSame(fn, result)
    }

    @Test
    fun `getRefactorableFunction returns null when no target matches`() {
        val fn = mockFn("Brain Method", 4)
        val result = getRefactorableFunction("Complex Method", 10, listOf(fn))
        assertNull(result)
    }

    @Test
    fun `resolveActivatedAceStatus returns signed out for blank token`() {
        assertEquals(AceStatus.SIGNED_OUT, resolveActivatedAceStatus("  "))
    }

    @Test
    fun `resolveActivatedAceStatus returns signed in for non blank token`() {
        assertEquals(AceStatus.SIGNED_IN, resolveActivatedAceStatus("token"))
    }

    private fun mockFn(
        category: String,
        line: Int,
    ): FnToRefactor {
        val target = mockk<com.codescene.data.ace.RefactoringTarget>()
        every { target.category } returns category
        every { target.line } returns line

        val fn = mockk<FnToRefactor>()
        every { fn.refactoringTargets } returns listOf(target)
        return fn
    }
}
