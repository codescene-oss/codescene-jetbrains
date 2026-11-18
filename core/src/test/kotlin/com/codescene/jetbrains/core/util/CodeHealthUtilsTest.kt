package com.codescene.jetbrains.core.util

import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeHealthUtilsTest {
    private val noApplicationCode = "No application code detected for scoring"

    @Test
    fun `a changed delta shows the old and the new score`() {
        val delta = mockDelta(oldScore = 6.5, newScore = 8.25)
        assertEquals("6.5 → 8.25", resolveCodeHealthDescription(delta, mockReview(score = 7.0)))
    }

    @Test
    fun `whole scores are shown without a decimal`() {
        val delta = mockDelta(oldScore = 10.0, newScore = 9.0)
        assertEquals("10 → 9", resolveCodeHealthDescription(delta, mockReview(score = 9.0)))
    }

    @Test
    fun `a changed delta without a new score shows n_a for it`() {
        val delta = mockDelta(oldScore = 9.5, newScore = null)
        assertEquals("9.5 → n/a", resolveCodeHealthDescription(delta, mockReview(score = 9.5)))
    }

    @Test
    fun `an unchanged delta shows its score out of ten`() {
        val delta = mockDelta(oldScore = 8.0, newScore = 8.0)
        assertEquals("8/10", resolveCodeHealthDescription(delta, mockReview(score = 9.0)))
    }

    @Test
    fun `a file without a baseline shows only the current score out of ten`() {
        val delta = mockDelta(oldScore = null, newScore = 9.38)
        assertEquals("9.38/10", resolveCodeHealthDescription(delta, mockReview(score = 9.38)))
    }

    @Test
    fun `a delta without any score reports no application code`() {
        val delta = mockDelta(oldScore = null, newScore = null)
        assertEquals(noApplicationCode, resolveCodeHealthDescription(delta, mockReview(score = 8.5)))
    }

    @Test
    fun `without a delta the review score is shown out of ten`() {
        assertEquals("8.5/10", resolveCodeHealthDescription(null, mockReview(score = 8.5)))
    }

    @Test
    fun `without a delta a review without a score reports no application code`() {
        assertEquals(noApplicationCode, resolveCodeHealthDescription(null, mockReview(score = null)))
    }

    @Test
    fun `without a delta or a review there is no description`() {
        assertNull(resolveCodeHealthDescription(null, null))
    }

    private fun mockReview(score: Double?): Review =
        mockk {
            every { this@mockk.score } returns Optional.ofNullable(score)
        }

    private fun mockDelta(
        oldScore: Double?,
        newScore: Double?,
    ): Delta =
        mockk {
            every { this@mockk.oldScore } returns Optional.ofNullable(oldScore)
            every { this@mockk.newScore } returns Optional.ofNullable(newScore)
        }
}
