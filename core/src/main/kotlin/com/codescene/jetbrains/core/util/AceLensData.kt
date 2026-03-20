package com.codescene.jetbrains.core.util

import com.codescene.data.ace.FnToRefactor

data class AceLensData(
    val startLine: Int,
    val endLine: Int,
    val functionName: String,
)

fun computeAceLenses(refactorableFunctions: List<FnToRefactor>?): List<AceLensData> =
    refactorableFunctions?.map { fn ->
        AceLensData(
            startLine = fn.range.startLine,
            endLine = fn.range.endLine,
            functionName = fn.name,
        )
    } ?: emptyList()
