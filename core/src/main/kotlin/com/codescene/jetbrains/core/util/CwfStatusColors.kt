package com.codescene.jetbrains.core.util

import kotlin.math.pow

data class CwfStatusColors(
    val success: RgbColor,
    val warning: RgbColor,
    val issue: RgbColor,
)

fun contrastRatio(
    a: RgbColor,
    b: RgbColor,
): Double {
    val l1 = relativeLuminance(a)
    val l2 = relativeLuminance(b)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05) / (darker + 0.05)
}

private fun relativeLuminance(color: RgbColor): Double {
    fun channel(c: Int): Double {
        val v = c / 255.0
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

fun resolveCwfStatusColors(
    successCandidate: RgbColor?,
    warningCandidate: RgbColor?,
    editorBackground: RgbColor,
    isBright: Boolean,
): CwfStatusColors {
    val successFallbackLight = RgbColor(0x38, 0x8A, 0x34)
    val successFallbackDark = RgbColor(0x89, 0xD1, 0x85)
    val warningFallbackLight = RgbColor(0xBF, 0x88, 0x03)
    val warningFallbackDark = RgbColor(0xCC, 0xA7, 0x00)
    val issueLight = RgbColor(0xA1, 0x5C, 0x00)
    val issueDark = RgbColor(0xEE, 0x9D, 0x28)

    val success =
        if (successCandidate != null && contrastRatio(successCandidate, editorBackground) >= 3.0) {
            successCandidate
        } else {
            if (isBright) successFallbackLight else successFallbackDark
        }

    val warning =
        if (warningCandidate != null && contrastRatio(warningCandidate, editorBackground) >= 3.0) {
            warningCandidate
        } else {
            if (isBright) warningFallbackLight else warningFallbackDark
        }

    val issue = if (isBright) issueLight else issueDark

    return CwfStatusColors(success, warning, issue)
}
