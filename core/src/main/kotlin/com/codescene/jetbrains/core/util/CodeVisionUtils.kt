package com.codescene.jetbrains.core.util

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell

fun getCodeSmellsByCategory(
    codeAnalysisResult: Review?,
    categoryToFilter: String,
): List<CodeVisionCodeSmell> {
    val fileLevelSmells =
        codeAnalysisResult?.fileLevelCodeSmells
            ?.filterByCategory(categoryToFilter)
            ?.map { smell ->
                CodeVisionCodeSmell(
                    details = smell.details,
                    category = smell.category,
                    highlightRange = smell.highlightRange,
                )
            } ?: emptyList()

    val functionLevelSmells =
        codeAnalysisResult?.functionLevelCodeSmells
            ?.flatMap { function ->
                function.codeSmells
                    .filterByCategory(categoryToFilter)
                    .map { smell ->
                        CodeVisionCodeSmell(
                            functionName = function.function,
                            functionRange = function.range,
                            details = smell.details,
                            category = smell.category,
                            highlightRange = smell.highlightRange,
                        )
                    }
            } ?: emptyList()

    return fileLevelSmells + functionLevelSmells
}

private fun List<CodeSmell>.filterByCategory(categoryToFilter: String): List<CodeSmell> {
    return this.filter { it.category == categoryToFilter }
}
