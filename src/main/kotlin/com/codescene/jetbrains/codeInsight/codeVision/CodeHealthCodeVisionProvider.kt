package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.codeInsight.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.codeAnalysisResult
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.awt.event.MouseEvent

class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = ""
    override val id = "codeVision.codescene.codeHealth"
    override val name = "com.codescene.codeVision.codeHealth"

    private fun getCodeVisionEntry(): ClickableTextCodeVisionEntry {
        val text = "Code health score: ${codeAnalysisResult.score}/10"

        return ClickableTextCodeVisionEntry(
            text,
            id,
            { event, sourceEditor -> handleClick(sourceEditor, "Health Score", event) },
            AllIcons.General.Balloon
        )
    }

    override fun handleCodeSmells(editor: Editor): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        val range = TextRange(0, 0)
        val entry = getCodeVisionEntry()

        lenses.add(range to entry)

        return lenses
    }

    override fun handleClick(editor: Editor, category: String, event: MouseEvent?) {
        //TODO
    }
}