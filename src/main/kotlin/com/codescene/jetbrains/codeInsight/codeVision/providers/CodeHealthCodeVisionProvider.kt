package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.data.review.Range
import com.codescene.data.review.Review
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.Constants.GENERAL_CODE_HEALTH
import com.codescene.jetbrains.util.HealthDetails
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.getCodeSmell
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

const val HEALTH_SCORE = "Health Score"

class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = HEALTH_SCORE

    private fun getCodeVisionEntry(description: String): ClickableTextCodeVisionEntry {
        val codeHealth = CodeVisionCodeSmell(category = "Code Health", highlightRange = Range(1, 1, 1, 1), details = "")

        return ClickableTextCodeVisionEntry(
            "Code Health: $description",
            id,
            { _, sourceEditor -> handleLensClick(sourceEditor, codeHealth) },
            CODE_HEALTH
        )
    }

    private fun getDescription(editor: Editor, result: Review?): String? {
        val cachedDelta = getCachedDelta(editor)

        val deltaResult = cachedDelta.second
        val oldScore = deltaResult?.oldScore
        val newScore = deltaResult?.newScore
        val hasChanged = oldScore != newScore

        return when {
            deltaResult != null && hasChanged -> {
                val oldReviewScore = deltaResult.oldScore
                val newReviewScore = deltaResult.newScore

                getCodeHealth(HealthDetails(oldReviewScore, newReviewScore)).change
            }

            result?.score?.isPresent == true -> result.score.get().toString()

            else -> "N/A".takeIf { result != null }
        }
    }

    override fun getLenses(editor: Editor, result: Review?): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val description = getDescription(editor, result) ?: return arrayListOf()

        val entry = getCodeVisionEntry(description)

        return arrayListOf(TextRange(0, 0) to entry)
    }

    override fun handleLensClick(editor: Editor, codeSmell: CodeVisionCodeSmell) {
        editor.project?.let {
            val docsData = DocsData(
                docType = nameDocMap[GENERAL_CODE_HEALTH]!!,
                fileData = FileMetaType(fileName = editor.virtualFile.path)
            )

            val smell = getCodeSmell(codeSmell)
            openDocs(docsData, it, DocsEntryPoint.CODE_VISION, smell)
        }
    }
}