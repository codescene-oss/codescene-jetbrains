package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.data.CodeReview
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.services.CodeSceneService
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.getTextRange
import com.codescene.jetbrains.util.isFileSupported
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.reflections.Reflections
import java.awt.event.MouseEvent

@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    companion object {
        @Volatile
        var activeReviewApiCalls = mutableSetOf<String>()

        @Volatile
        var activeDeltaApiCalls = mutableSetOf<String>()

        private var providers: List<String> = emptyList()

        fun markApiCallComplete(filePath: String, apiCalls: MutableSet<String>) {
            apiCalls.remove(filePath)
        }

        fun getProviders(): List<String> {
            if (providers.isEmpty()) {
                val reflections = Reflections("${this::class.java.packageName}.providers")
                val providerClasses = reflections.getSubTypesOf(CodeSceneCodeVisionProvider::class.java)

                providers = providerClasses.map { it.simpleName }
            }

            return providers
        }
    }

    abstract val categoryToFilter: String

    override val id: String = this::class.simpleName!!

    override val name: String = "${this::class.java.packageName}.providers.${this::class.simpleName}"

    abstract fun handleClick(editor: Editor, category: String, event: MouseEvent?)

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun precomputeOnUiThread(editor: Editor) {
        // Precomputations on the UI thread are unnecessary in this context, so this is left intentionally empty.
    }

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        editor.project ?: return CodeVisionState.READY_EMPTY

        val excludeGitignoreFiles = CodeSceneGlobalSettingsStore.getInstance().state.excludeGitignoreFiles
        val fileSupported = isFileSupported(editor.project!!, editor.virtualFile, excludeGitignoreFiles)

        if (!fileSupported) return CodeVisionState.READY_EMPTY

        return recomputeCodeVision(editor)
    }

    private fun triggerApiCall(
        editor: Editor,
        apiCalls: MutableSet<String>,
        apiCallAction: (CodeSceneService) -> Unit
    ) {
        val filePath = editor.virtualFile.path

        if (!isApiCallInProgressForFile(filePath, apiCalls)) {
            markApiCallInProgress(filePath, apiCalls)

            val codeSceneService = CodeSceneService.getInstance(editor.project!!)

            apiCallAction(codeSceneService)
        }
    }

    private fun recomputeCodeVision(editor: Editor): CodeVisionState {
        val codeVisionEnabled = CodeSceneGlobalSettingsStore.getInstance().state.enableCodeLenses

        val project = editor.project!!
        val document = editor.document
        val query = ReviewCacheQuery(document.text, editor.virtualFile.path)

        val reviewCache = ReviewCacheService.getInstance(project)
        val cachedReview = reviewCache.getCachedResponse(query)

        val cachedDelta = getCachedDelta(editor)

        if (cachedDelta == null) triggerApiCall(editor, activeDeltaApiCalls) { codeSceneService ->
            codeSceneService.codeDelta(editor)
        }

        if (cachedReview == null) {
            triggerApiCall(editor, activeReviewApiCalls) { codeSceneService ->
                codeSceneService.reviewCode(editor)
            }

            return CodeVisionState.NotReady
        }

        if (!codeVisionEnabled) return CodeVisionState.READY_EMPTY

        val lenses = getLenses(editor, cachedReview)

        return CodeVisionState.Ready(lenses)
    }

    open fun getLenses(
        editor: Editor,
        result: CodeReview?
    ): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        getCodeSmellsByCategory(result).forEach { smell ->
            val range = getTextRange(smell, editor.document)

            val entry = getCodeVisionEntry(smell)

            lenses.add(range to entry)
        }

        return lenses
    }

    private fun List<CodeSmell>.filterByCategory(categoryToFilter: String): List<CodeSmell> {
        return this.filter { it.category == categoryToFilter }
    }

    private fun getCodeSmellsByCategory(codeAnalysisResult: CodeReview?): List<CodeSmell> {
        val fileLevelSmells =
            codeAnalysisResult?.fileLevelCodeSmells?.filterByCategory(categoryToFilter) ?: emptyList()

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

    private fun markApiCallInProgress(filePath: String, apiCalls: MutableSet<String>) {
        apiCalls.add(filePath)
    }

    private fun isApiCallInProgressForFile(filePath: String, apiCalls: MutableSet<String>) = apiCalls.contains(filePath)

}