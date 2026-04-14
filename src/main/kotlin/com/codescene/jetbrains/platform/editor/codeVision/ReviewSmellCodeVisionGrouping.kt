package com.codescene.jetbrains.platform.editor.codeVision

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.util.CodeVisionSmellCategories
import com.codescene.jetbrains.core.util.getCodeSmellsByCategory
import com.codescene.jetbrains.platform.util.getTextRange
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

internal fun collectSmellsWithHighlightRangesForVision(
    editor: Editor,
    result: Review?,
    categories: List<String>,
): List<Pair<TextRange, CodeVisionCodeSmell>> {
    val out = ArrayList<Pair<TextRange, CodeVisionCodeSmell>>()
    for (category in categories) {
        for (smell in getCodeSmellsByCategory(result, category)) {
            val range =
                getTextRange(smell.highlightRange.startLine to smell.highlightRange.endLine, editor.document)
            out.add(range to smell)
        }
    }
    return out
}

internal fun groupReviewSmellsByHighlightRange(
    pairs: List<Pair<TextRange, CodeVisionCodeSmell>>,
): List<Pair<TextRange, List<CodeVisionCodeSmell>>> {
    val byRange = LinkedHashMap<TextRange, LinkedHashSet<CodeVisionCodeSmell>>()
    for ((range, smell) in pairs) {
        byRange.getOrPut(range) { linkedSetOf() }.add(smell)
    }
    val order = CodeVisionSmellCategories.orderedForDisplay
    return byRange.map { (range, set) ->
        val comparator =
            compareBy<CodeVisionCodeSmell> { smell ->
                smellCategoryOrderIndexForVision(order, smell)
            }.thenBy { it.category }
        range to set.sortedWith(comparator)
    }
}

private fun smellCategoryOrderIndexForVision(
    order: List<String>,
    smell: CodeVisionCodeSmell,
): Int {
    val index = order.indexOf(smell.category)
    return if (index < 0) Int.MAX_VALUE else index
}
