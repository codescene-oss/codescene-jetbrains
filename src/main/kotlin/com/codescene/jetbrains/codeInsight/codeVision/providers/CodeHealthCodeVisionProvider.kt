package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.data.review.Range
import com.codescene.data.review.Review
import com.codescene.jetbrains.CodeSceneIcons.CODE_HEALTH
import com.codescene.jetbrains.codeInsight.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.codehealth.monitor.CodeHealthMonitorPanel
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.featureflag.FeatureFlagManager
import com.codescene.jetbrains.services.htmlviewer.CodeSceneDocumentationViewer
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.services.htmlviewer.DocumentationParams
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.GENERAL_CODE_HEALTH
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.wm.ToolWindowManager
import javax.swing.JTree

const val HEALTH_SCORE = "Health Score"

internal class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
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
                val oldReviewScore = deltaResult.oldScore.orElse(null)
                val newReviewScore = deltaResult.newScore.orElse(null)

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
            if (FeatureFlagManager.isEnabled(Constants.CWF_FLAG))
                handleOpenCwfDocs(editor)
            else
                handleOpenNativeDocs(editor)
        }
    }

    private fun handleOpenNativeDocs(editor: Editor) {
        val project = editor.project!!
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val docViewer = CodeSceneDocumentationViewer.getInstance(project)

        val nodeSelected = CodeHealthMonitorPanel.getInstance(editor.project!!).contentPanel.components
            .filterIsInstance<JTree>()
            .firstOrNull()
            ?.let { selectNode(it, editor.virtualFile.path) } ?: false

        if (!nodeSelected) docViewer.open(editor, DocumentationParams(GENERAL_CODE_HEALTH))
        else toolWindowManager.getToolWindow(CODESCENE)?.show()
    }

    private fun handleOpenCwfDocs(editor: Editor) {
        val docsData = DocsData(
            docType = nameDocMap[GENERAL_CODE_HEALTH]!!,
            fileData = FileMetaType(fileName = editor.virtualFile.path)
        )

        openDocs(docsData, editor.project!!, DocsEntryPoint.CODE_VISION)
    }

}