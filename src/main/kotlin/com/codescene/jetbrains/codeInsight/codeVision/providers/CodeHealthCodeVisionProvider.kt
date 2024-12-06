package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.util.HealthDetails
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.getCodeHealth
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.awt.event.MouseEvent

const val HEALTH_SCORE = "Health Score"

class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = HEALTH_SCORE

    private fun getCodeVisionEntry(description: String) = ClickableTextCodeVisionEntry(
        "Code Health: $description",
        id,
        { event, sourceEditor -> handleClick(sourceEditor, categoryToFilter, event) },
        CODE_HEALTH
    )

    private fun getDescription(editor: Editor, result: CodeReview?): String? {
        val cachedDelta = getCachedDelta(editor)
        val hasChanged = cachedDelta?.oldScore != cachedDelta?.newScore

        return when {
            cachedDelta != null && hasChanged -> getCodeHealth(
                HealthDetails(
                    cachedDelta.oldScore,
                    cachedDelta.newScore
                )
            ).change
            result != null -> result.score.toString()
            else -> null
        }
    }

    override fun getLenses(editor: Editor, result: CodeReview?): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val description = getDescription(editor, result) ?: return arrayListOf()

        val entry = getCodeVisionEntry(description)

        return arrayListOf(TextRange(0, 0) to entry)
    }

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}