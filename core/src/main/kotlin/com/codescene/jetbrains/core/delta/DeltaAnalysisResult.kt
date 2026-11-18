package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta

data class DeltaAnalysisResult(
    val delta: Delta?,
    val shouldRefreshUi: Boolean,
)
