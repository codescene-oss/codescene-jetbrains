package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.data.ApiResponse
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.awt.event.MouseEvent

class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = ""

    private fun getCodeVisionEntry(result: ApiResponse?): ClickableTextCodeVisionEntry {
        val text = "Code health score: ${result?.score}/10"

        return ClickableTextCodeVisionEntry(
            text,
            id,
            { event, sourceEditor -> handleClick(sourceEditor, "Health Score", event) },
            AllIcons.General.Balloon
        )
    }

    override fun getLenses(editor: Editor, result: ApiResponse?): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        if (result != null) {
            val range = TextRange(0, 0)
            val entry = getCodeVisionEntry(result)

            lenses.add(range to entry)
        }

        return lenses
    }

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}