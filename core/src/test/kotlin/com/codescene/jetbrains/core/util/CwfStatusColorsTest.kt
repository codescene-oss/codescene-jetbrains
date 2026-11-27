package com.codescene.jetbrains.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CwfStatusColorsTest {
    @Test
    fun `contrastRatio of black on white is 21`() {
        val black = RgbColor(0, 0, 0)
        val white = RgbColor(255, 255, 255)
        val ratio = contrastRatio(black, white)
        assertEquals(21.0, ratio, 0.01)
    }

    @Test
    fun `contrastRatio is symmetric`() {
        val a = RgbColor(100, 150, 200)
        val b = RgbColor(50, 75, 100)
        assertEquals(contrastRatio(a, b), contrastRatio(b, a), 0.0001)
    }

    @Test
    fun `readable success candidate is used`() {
        val editorBg = RgbColor(255, 255, 255)
        val successCandidate = RgbColor(0, 128, 0)
        val result =
            resolveCwfStatusColors(
                successCandidate = successCandidate,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(successCandidate, result.success)
    }

    @Test
    fun `readable warning candidate is used`() {
        val editorBg = RgbColor(255, 255, 255)
        val warningCandidate = RgbColor(191, 136, 3)
        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = warningCandidate,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(warningCandidate, result.warning)
    }

    @Test
    fun `missing success candidate falls back to light green when bright`() {
        val editorBg = RgbColor(255, 255, 255)
        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(RgbColor(0x38, 0x8A, 0x34), result.success)
    }

    @Test
    fun `missing success candidate falls back to dark green when dark`() {
        val editorBg = RgbColor(0, 0, 0)
        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = false,
            )
        assertEquals(RgbColor(0x89, 0xD1, 0x85), result.success)
    }

    @Test
    fun `missing warning candidate falls back to light amber when bright`() {
        val editorBg = RgbColor(255, 255, 255)
        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(RgbColor(0xBF, 0x88, 0x03), result.warning)
    }

    @Test
    fun `missing warning candidate falls back to dark amber when dark`() {
        val editorBg = RgbColor(0, 0, 0)
        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = false,
            )
        assertEquals(RgbColor(0xCC, 0xA7, 0x00), result.warning)
    }

    @Test
    fun `success candidate below 3 to 1 falls back`() {
        val editorBg = RgbColor(255, 255, 255)
        val paleGreen = RgbColor(200, 255, 200)
        assertTrue(contrastRatio(paleGreen, editorBg) < 3.0)

        val result =
            resolveCwfStatusColors(
                successCandidate = paleGreen,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(RgbColor(0x38, 0x8A, 0x34), result.success)
    }

    @Test
    fun `warning candidate below 3 to 1 falls back`() {
        val editorBg = RgbColor(255, 255, 255)
        val paleAmber = RgbColor(255, 240, 200)
        assertTrue(contrastRatio(paleAmber, editorBg) < 3.0)

        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = paleAmber,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(RgbColor(0xBF, 0x88, 0x03), result.warning)
    }

    @Test
    fun `candidate pair just above and just below 3 to 1 verifies the greater than or equal comparison`() {
        val editorBg = RgbColor(255, 255, 255)
        val darkGray = RgbColor(100, 100, 100)
        val lightGray = RgbColor(150, 150, 150)

        val darkRatio = contrastRatio(darkGray, editorBg)
        val lightRatio = contrastRatio(lightGray, editorBg)

        assertTrue(darkRatio >= 3.0)
        assertTrue(lightRatio < 3.0)

        val resultDark =
            resolveCwfStatusColors(
                successCandidate = darkGray,
                warningCandidate = darkGray,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(darkGray, resultDark.success)
        assertEquals(darkGray, resultDark.warning)

        val resultLight =
            resolveCwfStatusColors(
                successCandidate = lightGray,
                warningCandidate = lightGray,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(RgbColor(0x38, 0x8A, 0x34), resultLight.success)
        assertEquals(RgbColor(0xBF, 0x88, 0x03), resultLight.warning)
    }

    @Test
    fun `issue is A15C00 when bright and EE9D28 when dark`() {
        val editorBg = RgbColor(255, 255, 255)

        val brightResult =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = true,
            )
        assertEquals(RgbColor(0xA1, 0x5C, 0x00), brightResult.issue)

        val darkResult =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = editorBg,
                isBright = false,
            )
        assertEquals(RgbColor(0xEE, 0x9D, 0x28), darkResult.issue)
    }

    @Test
    fun `all four fallbacks reach 3 to 1 on default Light editor background`() {
        val lightBg = RgbColor(0xFF, 0xFF, 0xFF)

        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = lightBg,
                isBright = true,
            )

        assertTrue(contrastRatio(result.success, lightBg) >= 3.0)
        assertTrue(contrastRatio(result.warning, lightBg) >= 3.0)
    }

    @Test
    fun `all four fallbacks reach 3 to 1 on default Dark editor background`() {
        val darkBg = RgbColor(0x1E, 0x1F, 0x22)

        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = darkBg,
                isBright = false,
            )

        assertTrue(contrastRatio(result.success, darkBg) >= 3.0)
        assertTrue(contrastRatio(result.warning, darkBg) >= 3.0)
    }

    @Test
    fun `all four fallbacks reach 3 to 1 on Darcula editor background`() {
        val darculaBg = RgbColor(0x2B, 0x2B, 0x2B)

        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = darculaBg,
                isBright = false,
            )

        assertTrue(contrastRatio(result.success, darculaBg) >= 3.0)
        assertTrue(contrastRatio(result.warning, darculaBg) >= 3.0)
    }

    @Test
    fun `High Contrast with black background not bright resolves to dark set`() {
        val blackBg = RgbColor(0x00, 0x00, 0x00)

        val result =
            resolveCwfStatusColors(
                successCandidate = null,
                warningCandidate = null,
                editorBackground = blackBg,
                isBright = false,
            )

        assertEquals(RgbColor(0x89, 0xD1, 0x85), result.success)
        assertEquals(RgbColor(0xCC, 0xA7, 0x00), result.warning)
        assertEquals(RgbColor(0xEE, 0x9D, 0x28), result.issue)

        assertTrue(contrastRatio(result.success, blackBg) >= 3.0)
        assertTrue(contrastRatio(result.warning, blackBg) >= 3.0)
        assertTrue(contrastRatio(result.issue, blackBg) >= 3.0)
    }
}
