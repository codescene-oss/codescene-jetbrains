package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.data.ApiResponse
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.services.CodeSceneService
import com.codescene.jetbrains.services.ReviewCacheService
import com.codescene.jetbrains.util.getTextRange
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import kotlinx.coroutines.runBlocking
import java.awt.event.MouseEvent

@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    abstract val categoryToFilter: String

    abstract override val id: String
    abstract fun handleClick(editor: Editor, category: String, event: MouseEvent?)

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun precomputeOnUiThread(editor: Editor) {}

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        editor.project ?: return CodeVisionState.READY_EMPTY

        val settings = CodeSceneGlobalSettingsStore.getInstance().state

        if (!settings.enableCodeLenses) {
            return CodeVisionState.READY_EMPTY
        }

        return recomputeLenses(editor)
    }

    private fun recomputeLenses(editor: Editor): CodeVisionState {
        val cacheService = ReviewCacheService.getInstance(editor.project!!)
        val cachedResponse = cacheService.getCachedResponse(editor)

        if (cachedResponse == null) {
            //TODO: Explore non-blocking options
            val review = runBlocking {
                CodeSceneService.getInstance(editor.project!!).reviewCode(editor)
            }

            val lenses = getLenses(editor, review)

            return CodeVisionState.Ready(lenses)
        }

        val lenses = getLenses(editor, cachedResponse)

        return CodeVisionState.Ready(lenses)
    }

    open fun getLenses(
        editor: Editor,
        result: ApiResponse?
    ): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        getCodeSmellsByCategory(result).forEach { smell ->
            val range = getTextRange(smell, editor)

            val entry = getCodeVisionEntry(smell)

            lenses.add(range to entry)
        }

        return lenses
    }

    private fun List<CodeSmell>.filterByCategory(categoryToFilter: String): List<CodeSmell> {
        return this.filter { it.category == categoryToFilter }
    }

    private fun getCodeSmellsByCategory(codeAnalysisResult: ApiResponse?): List<CodeSmell> {
        val fileLevelSmells = codeAnalysisResult?.fileLevelCodeSmells?.filterByCategory(categoryToFilter) ?: emptyList()

        val functionLevelSmells = codeAnalysisResult?.functionLevelCodeSmells?.flatMap { functionCodeSmell ->
            functionCodeSmell.codeSmells.filterByCategory(categoryToFilter)
        } ?: emptyList()

        val expressionLevelSmells =
            codeAnalysisResult?.expressionLevelCodeSmells?.filterByCategory(categoryToFilter) ?: emptyList()

        return fileLevelSmells + functionLevelSmells + expressionLevelSmells
    }

    private fun getCodeVisionEntry(codeSmell: CodeSmell): ClickableTextCodeVisionEntry =
        ClickableTextCodeVisionEntry(
            codeSmell.category,
            id,
            { event, sourceEditor -> handleClick(sourceEditor, codeSmell.category, event) },
            AllIcons.General.InspectionsWarningEmpty
        )

}