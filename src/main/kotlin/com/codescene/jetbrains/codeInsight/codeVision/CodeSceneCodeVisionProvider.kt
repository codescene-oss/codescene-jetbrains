package com.codescene.jetbrains.codeInsight.codeVision

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.data.review.Review
import com.codescene.jetbrains.CodeSceneIcons.CODE_SMELL
import com.codescene.jetbrains.components.webview.data.DocsData
import com.codescene.jetbrains.components.webview.data.FileMetaType
import com.codescene.jetbrains.components.webview.data.Fn
import com.codescene.jetbrains.components.webview.data.RangeCamelCase
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.CodeDeltaService
import com.codescene.jetbrains.services.api.CodeReviewService
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.getTextRange
import com.codescene.jetbrains.util.isFileSupported
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import org.reflections.Reflections
import java.util.concurrent.ConcurrentHashMap

data class CodeVisionCodeSmell(
    val details: String,
    val category: String,
    val highlightRange: Range,
    val functionName: String? = null
)

@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    companion object {
        val activeReviewApiCalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

        val activeDeltaApiCalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

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

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun precomputeOnUiThread(editor: Editor) {
        // Precomputations on the UI thread are unnecessary in this context, so this is left intentionally empty.
    }

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        editor.project ?: return CodeVisionState.READY_EMPTY

        val fileSupported = isFileSupported(editor.project!!, editor.virtualFile)

        if (!fileSupported) return CodeVisionState.READY_EMPTY

        return recomputeCodeVision(editor)
    }

    private fun triggerApiCall(
        editor: Editor,
        apiCalls: MutableSet<String>,
        action: () -> Unit
    ) {
        val filePath = editor.virtualFile.path

        if (!isApiCallInProgressForFile(filePath, apiCalls)) {
            markApiCallInProgress(filePath, apiCalls)

            action()
        }
    }

    private fun recomputeCodeVision(editor: Editor): CodeVisionState {
        val codeVisionEnabled = CodeSceneGlobalSettingsStore.getInstance().state.enableCodeLenses
        val monitorEnabled = CodeSceneGlobalSettingsStore.getInstance().state.codeHealthMonitorEnabled

        val project = editor.project!!
        val document = editor.document
        val query = ReviewCacheQuery(document.text, editor.virtualFile.path)

        val reviewCache = ReviewCacheService.getInstance(project)
        val cachedReview = reviewCache.get(query)

        val cachedDelta = getCachedDelta(editor)

        if (!cachedDelta.first && monitorEnabled) triggerApiCall(editor, activeDeltaApiCalls) {
            CodeDeltaService.getInstance(project).review(editor)
        }

        if (cachedReview == null) {
            triggerApiCall(editor, activeReviewApiCalls) {
                CodeReviewService.getInstance(project).review(editor)
            }

            return CodeVisionState.NotReady
        }

        if (!codeVisionEnabled) return CodeVisionState.READY_EMPTY

        val lenses = getLenses(editor, cachedReview)

        return CodeVisionState.Ready(lenses)
    }

    open fun getLenses(
        editor: Editor,
        result: Review?
    ): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        getCodeSmellsByCategory(result).forEach { smell ->
            val range = getTextRange(smell.highlightRange.startLine to smell.highlightRange.endLine, editor.document)

            val entry = getCodeVisionEntry(smell)

            lenses.add(range to entry)
        }

        return lenses
    }

    private fun List<CodeSmell>.filterByCategory(categoryToFilter: String): List<CodeSmell> {
        return this.filter { it.category == categoryToFilter }
    }

    private fun getCodeSmellsByCategory(codeAnalysisResult: Review?): List<CodeVisionCodeSmell> {
        val fileLevelSmells =
            codeAnalysisResult?.fileLevelCodeSmells?.filterByCategory(categoryToFilter)?.map { smell ->
                CodeVisionCodeSmell(
                    details = smell.details,
                    category = smell.category,
                    highlightRange = smell.highlightRange
                )
            } ?: emptyList()

        val functionLevelSmells = codeAnalysisResult?.functionLevelCodeSmells
            ?.flatMap { function ->
                function.codeSmells
                    .filterByCategory(categoryToFilter)
                    .map { smell ->
                        CodeVisionCodeSmell(
                            functionName = function.function,
                            details = smell.details,
                            category = smell.category,
                            highlightRange = smell.highlightRange
                        )
                    }
            } ?: emptyList()

        return fileLevelSmells + functionLevelSmells
    }

    private fun getCodeVisionEntry(codeSmell: CodeVisionCodeSmell): ClickableTextCodeVisionEntry =
        ClickableTextCodeVisionEntry(
            codeSmell.category,
            id,
            { _, sourceEditor -> handleLensClick(sourceEditor, codeSmell) },
            CODE_SMELL
        )

    open fun handleLensClick(editor: Editor, codeSmell: CodeVisionCodeSmell) {
        val project = editor.project ?: return

        val docsData = DocsData(
            docType = nameDocMap[codeSmell.category] ?: "",
            fileData = FileMetaType(
                fileName = editor.virtualFile.name,
                fn = Fn(
                    name = codeSmell.functionName ?: "",
                    range = RangeCamelCase(
                        endLine = codeSmell.highlightRange.endLine,
                        startLine = codeSmell.highlightRange.startLine,
                        endColumn = codeSmell.highlightRange.endColumn,
                        startColumn = codeSmell.highlightRange.startColumn
                    )
                )
            )
        )

        openDocs(docsData, project, DocsEntryPoint.CODE_VISION)
    }

    private fun markApiCallInProgress(filePath: String, apiCalls: MutableSet<String>) {
        apiCalls.add(filePath)
    }

    private fun isApiCallInProgressForFile(filePath: String, apiCalls: MutableSet<String>) = apiCalls.contains(filePath)

}