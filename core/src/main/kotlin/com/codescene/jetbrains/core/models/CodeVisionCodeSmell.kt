package com.codescene.jetbrains.core.models

import com.codescene.data.review.Range

data class CodeVisionCodeSmell(
    val details: String,
    val category: String,
    val highlightRange: Range,
    val functionName: String? = null,
    val functionRange: Range? = null,
)
