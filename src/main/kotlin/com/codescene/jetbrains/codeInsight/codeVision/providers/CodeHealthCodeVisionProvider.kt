package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.round
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.awt.event.MouseEvent

class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = ""

    private fun getCodeVisionEntry(description: String) = ClickableTextCodeVisionEntry(
        "Code Health: $description",
        id,
        { event, sourceEditor -> handleClick(sourceEditor, "Health Score", event) },
        CODE_HEALTH
    )

    override fun getLenses(editor: Editor, result: CodeReview?): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val cachedDelta = getCachedDelta(editor)

        val description = when {
            cachedDelta != null -> "${round(cachedDelta.oldScore)} → ${round(cachedDelta.newScore)}"
            result != null -> result.score.toString()
            else -> return arrayListOf()
        }

        val entry = getCodeVisionEntry(description)

        return arrayListOf(TextRange(0, 0) to entry)
    }

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}