package com.codescene.jetbrains.core.review

import com.codescene.jetbrains.core.models.shared.RangeCamelCase
import com.codescene.jetbrains.core.util.replaceTextInRange

data class AceDiffContext(
    val refactoredFilePath: String,
    val range: RangeCamelCase,
    val refactoredCode: String,
)

fun resolveAceDiffContext(
    refactoredFilePath: String?,
    range: RangeCamelCase?,
    refactoredCode: String?,
): AceDiffContext? {
    if (refactoredFilePath == null || range == null || refactoredCode == null) return null

    return AceDiffContext(
        refactoredFilePath = refactoredFilePath,
        range = range,
        refactoredCode = refactoredCode,
    )
}

fun buildAceDiffText(
    originalText: String,
    offsetRange: IntRange,
    refactoredCode: String,
): String = replaceTextInRange(originalText, offsetRange, refactoredCode)
