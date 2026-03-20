package com.codescene.jetbrains.platform.editor.codeVision.providers

import com.codescene.data.review.Range
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.delta.getCachedDelta
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.util.Constants.GENERAL_CODE_HEALTH
import com.codescene.jetbrains.core.util.HealthDetails
import com.codescene.jetbrains.core.util.getCodeHealth
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.codeVision.CodeSceneCodeVisionProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODE_HEALTH
import com.codescene.jetbrains.platform.util.handleOpenGeneralDocs
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange

internal class CodeHealthCodeVisionProvider : CodeSceneCodeVisionProvider() {
    private fun getCodeVisionEntry(description: String): ClickableTextCodeVisionEntry {
        val codeHealth = CodeVisionCodeSmell(category = "Code Health", highlightRange = Range(1, 1, 1, 1), details = "")

        return ClickableTextCodeVisionEntry(
            "Code Health: $description",
            id,
            { _, sourceEditor -> handleLensClick(sourceEditor, codeHealth) },
            CODE_HEALTH,
        )
    }

    private fun getDescription(
        editor: Editor,
        result: Review?,
    ): String? {
        val project = editor.project ?: return null
        val services = CodeSceneProjectServiceProvider.getInstance(project)
        val cachedDelta =
            getCachedDelta(
                filePath = editor.virtualFile.path,
                fileContent = editor.document.text,
                gitService = services.gitService,
                deltaCacheService = services.deltaCacheService,
            )

        val deltaResult = cachedDelta.second
        val hasChanged = deltaResult?.oldScore?.orElse(null) != deltaResult?.newScore?.orElse(null)

        return when {
            deltaResult != null && hasChanged -> {
                val oldReviewScore = deltaResult.oldScore.orElse(null)
                val newReviewScore = deltaResult.newScore.orElse(null)

                getCodeHealth(HealthDetails(oldReviewScore, newReviewScore)).change
            }

            result?.score?.isPresent == true -> {
                result.score.get().toString()
            }

            else -> {
                "N/A".takeIf { result != null }
            }
        }
    }

    override fun getLenses(
        editor: Editor,
        result: Review?,
    ): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val description = getDescription(editor, result) ?: return arrayListOf()

        val entry = getCodeVisionEntry(description)

        return arrayListOf(TextRange(0, 0) to entry)
    }

    override fun handleLensClick(
        editor: Editor,
        codeSmell: CodeVisionCodeSmell,
    ) {
        editor.project?.let {
            handleOpenGeneralDocs(it, GENERAL_CODE_HEALTH, DocsEntryPoint.CODE_VISION)
        }
    }
}
