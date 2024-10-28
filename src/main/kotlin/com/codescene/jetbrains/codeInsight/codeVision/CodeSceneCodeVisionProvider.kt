package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.data.ApiResponse
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.services.CacheQuery
import com.codescene.jetbrains.services.CodeSceneService
import com.codescene.jetbrains.services.ReviewCacheService
import com.codescene.jetbrains.util.getTextRange
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.reflections.Reflections
import java.awt.event.MouseEvent

@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    companion object {
        @Volatile
        var isApiCallInProgress: Boolean = false
        private var providers: List<String> = emptyList()

        fun getProviders(): List<String> {
            if (providers.isEmpty()) {
                val reflections = Reflections("${this::class.java.packageName}.providers")
                val providerClasses = reflections.getSubTypesOf(CodeSceneCodeVisionProvider::class.java)

                providers = providerClasses.map { it.simpleName }
            }

            return providers
        }
    }

    private val settings = CodeSceneGlobalSettingsStore.getInstance().state

    abstract val categoryToFilter: String

    override val id: String = this::class.simpleName!!

    override val name: String = "${this::class.java.packageName}.providers.${this::class.simpleName}"

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

        return recomputeCodeVision(editor)
    }

    override fun isAvailableFor(project: Project): Boolean = settings.enableCodeLenses

    private fun triggerCodeReview(editor: Editor, project: Project) {
        if (!isApiCallInProgress) {
            isApiCallInProgress = true

            CodeSceneService.getInstance(project).reviewCode(editor)
        }
    }

    private fun recomputeCodeVision(editor: Editor): CodeVisionState {
        val project = editor.project!!
        val document = editor.document
        val query = CacheQuery(document.text, editor.virtualFile.path)

        val cacheService = ReviewCacheService.getInstance(project)
        val cachedResponse = cacheService.getCachedResponse(query)

        if (cachedResponse == null) {
            triggerCodeReview(editor, project)

            return CodeVisionState.READY_EMPTY
        }

        val lenses = getLenses(document, cachedResponse)

        return CodeVisionState.Ready(lenses)
    }

    open fun getLenses(
        document: Document,
        result: ApiResponse?
    ): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        getCodeSmellsByCategory(result).forEach { smell ->
            val range = getTextRange(smell, document)

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