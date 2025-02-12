package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.data.review.Review
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.DocsSourceType
import com.codescene.jetbrains.services.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.GENERAL_CODE_HEALTH
import com.codescene.jetbrains.util.HealthDetails
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.getCodeHealth
import com.codescene.jetbrains.util.selectNode
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.JTree

const val HEALTH_SCORE = "Health Score"

class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    override val categoryToFilter = HEALTH_SCORE

    private fun getCodeVisionEntry(description: String): ClickableTextCodeVisionEntry {
        val codeHealth = CodeSmell().apply {
            category = "Code Health"
            highlightRange = Range().apply {
                startLine = 1
                startColumn = 1
                endLine = 1
                endColumn = 1
            }
            details = ""
        }

        return ClickableTextCodeVisionEntry(
            "Code Health: $description",
            id,
            { _, sourceEditor -> handleLensClick(sourceEditor, codeHealth) },
            CODE_HEALTH
        )
    }

    private fun getDescription(editor: Editor, result: Review?): String? {
        val cachedDelta = getCachedDelta(editor)
        val hasChanged = cachedDelta?.oldScore != cachedDelta?.newScore

        return when {
            cachedDelta != null && hasChanged -> getCodeHealth(
                HealthDetails(
                    cachedDelta.oldScore,
                    cachedDelta.newScore
                )
            ).change

            result != null -> if (result.score != null) result.score.toString() else "N/A"
            else -> null
        }
    }

    override fun getLenses(editor: Editor, result: Review?): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val description = getDescription(editor, result) ?: return arrayListOf()

        val entry = getCodeVisionEntry(description)

        return arrayListOf(TextRange(0, 0) to entry)
    }

    override fun handleLensClick(editor: Editor, codeSmell: CodeSmell) {
        val project = editor.project!!
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val service = CodeSceneDocumentationService.getInstance(project)

        val nodeSelected = CodeHealthMonitorPanel.getInstance(project).contentPanel.components
            .filterIsInstance<JTree>()
            .firstOrNull()
            ?.let { selectNode(it, editor.virtualFile.path) } ?: false

        if (!nodeSelected)
            service.openDocumentationPanel(
                DocumentationParams(
                    editor,
                    CodeSmell().apply {
                        details = codeSmell.details
                        highlightRange = codeSmell.highlightRange
                        category = GENERAL_CODE_HEALTH
                    },
                    DocsSourceType.NONE
                )
            )
        else toolWindowManager.getToolWindow(CODESCENE)?.show()
    }
}