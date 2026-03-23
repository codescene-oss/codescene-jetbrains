package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CodeHealthUtilsTest {
    @Test
    fun `getChangePercentage returns null when old score is null`() {
        val result = getChangePercentage(HealthDetails(oldScore = null, newScore = 9.0))
        assertNull(result)
    }

    @Test
    fun `getChangePercentage returns null when new score is null`() {
        val result = getChangePercentage(HealthDetails(oldScore = 9.0, newScore = null))
        assertNull(result)
    }

    @Test
    fun `getChangePercentage returns 0 when both old and new are zero`() {
        val result = getChangePercentage(HealthDetails(oldScore = 0.0, newScore = 0.0))
        assertEquals(0.0, result)
    }

    @Test
    fun `getChangePercentage returns 100 when old is zero and new is non zero`() {
        val result = getChangePercentage(HealthDetails(oldScore = 0.0, newScore = 7.0))
        assertEquals(100.0, result)
    }

    @Test
    fun `getChangePercentage returns absolute percentage with two decimals`() {
        val result = getChangePercentage(HealthDetails(oldScore = 6.0, newScore = 7.0))
        assertEquals(16.66, result)
    }

    @Test
    fun `getChangePercentage handles decline with absolute value`() {
        val result = getChangePercentage(HealthDetails(oldScore = 10.0, newScore = 8.0))
        assertEquals(20.0, result)
    }

    @Test
    fun `getCodeHealth returns unchanged state when values are equal`() {
        val result = getCodeHealth(HealthDetails(oldScore = 7.0, newScore = 7.0))
        assertEquals(HealthInformation(change = "7.0", percentage = ""), result)
    }

    @Test
    fun `getCodeHealth returns change and positive percentage for improvement`() {
        val result = getCodeHealth(HealthDetails(oldScore = 6.0, newScore = 7.0))
        assertEquals(HealthInformation(change = "6.0 -> 7.0", percentage = "+16.66%"), result)
    }

    @Test
    fun `getCodeHealth returns change and negative percentage for decline`() {
        val result = getCodeHealth(HealthDetails(oldScore = 10.0, newScore = 9.0))
        assertEquals(HealthInformation(change = "10.0 -> 9.0", percentage = "-10.0%"), result)
    }

    @Test
    fun `getCodeHealth uses NA when old score is missing`() {
        val result = getCodeHealth(HealthDetails(oldScore = null, newScore = 9.0))
        assertEquals(HealthInformation(change = "N/A -> 9.0", percentage = ""), result)
    }

    @Test
    fun `getCodeHealth uses NA when new score is missing`() {
        val result = getCodeHealth(HealthDetails(oldScore = 9.0, newScore = null))
        assertEquals(HealthInformation(change = "9.0 -> N/A", percentage = ""), result)
    }
}
