package com.codescene.jetbrains.core.util

fun replaceTextInRange(
    fullText: String,
    range: IntRange,
    replacement: String,
): String =
    StringBuilder(fullText).apply {
        replace(range.first, range.last, replacement)
    }.toString()
