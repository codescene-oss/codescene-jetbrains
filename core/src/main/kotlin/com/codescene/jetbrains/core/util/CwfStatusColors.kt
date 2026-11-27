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

private const val MIN_STATUS_CONTRAST = 3.0

private val SUCCESS_FALLBACK_LIGHT = RgbColor(0x38, 0x8A, 0x34)
private val SUCCESS_FALLBACK_DARK = RgbColor(0x89, 0xD1, 0x85)
private val WARNING_FALLBACK_LIGHT = RgbColor(0xBF, 0x88, 0x03)
private val WARNING_FALLBACK_DARK = RgbColor(0xCC, 0xA7, 0x00)
private val ISSUE_LIGHT = RgbColor(0xA1, 0x5C, 0x00)
private val ISSUE_DARK = RgbColor(0xEE, 0x9D, 0x28)

private fun readableOrFallback(
    candidate: RgbColor?,
    editorBackground: RgbColor,
    fallback: RgbColor,
): RgbColor = candidate?.takeIf { contrastRatio(it, editorBackground) >= MIN_STATUS_CONTRAST } ?: fallback

fun resolveCwfStatusColors(
    successCandidate: RgbColor?,
    warningCandidate: RgbColor?,
    editorBackground: RgbColor,
    isBright: Boolean,
): CwfStatusColors =
    CwfStatusColors(
        success =
            readableOrFallback(
                successCandidate,
                editorBackground,
                if (isBright) SUCCESS_FALLBACK_LIGHT else SUCCESS_FALLBACK_DARK,
            ),
        warning =
            readableOrFallback(
                warningCandidate,
                editorBackground,
                if (isBright) WARNING_FALLBACK_LIGHT else WARNING_FALLBACK_DARK,
            ),
        issue = if (isBright) ISSUE_LIGHT else ISSUE_DARK,
    )
