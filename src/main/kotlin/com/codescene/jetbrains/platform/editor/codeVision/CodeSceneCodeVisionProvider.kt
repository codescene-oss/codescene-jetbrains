package com.codescene.jetbrains.platform.editor.codeVision

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.delta.getCachedDelta
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.util.CodeVisionAction
import com.codescene.jetbrains.core.util.CodeVisionDecisionInput
import com.codescene.jetbrains.core.util.getCodeSmellsByCategory
import com.codescene.jetbrains.core.util.resolveCodeVisionDecision
import com.codescene.jetbrains.platform.api.CodeDeltaService
import com.codescene.jetbrains.platform.api.CodeReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.AceCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.AggregatedSmellCodeVisionProvider
import com.codescene.jetbrains.platform.editor.codeVision.providers.CodeHealthCodeVisionProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODE_SMELL
import com.codescene.jetbrains.platform.util.getTextRange
import com.codescene.jetbrains.platform.util.handleOpenDocs
import com.codescene.jetbrains.platform.util.isFileSupported
import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionProvider
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.CodeVisionState
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import java.util.concurrent.ConcurrentHashMap

@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    companion object {
        val activeReviewApiCalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

        val activeDeltaApiCalls: MutableSet<String> = ConcurrentHashMap.newKeySet()

        fun markApiCallComplete(
            filePath: String,
            apiCalls: MutableSet<String>,
        ) {
            apiCalls.remove(filePath)
        }

        fun getProviders(): List<String> =
            listOf(
                AggregatedSmellCodeVisionProvider::class.simpleName!!,
                CodeHealthCodeVisionProvider::class.simpleName!!,
                AceCodeVisionProvider::class.simpleName!!,
            )
    }

    protected open fun categoriesForLenses(): List<String> = emptyList()

    override val id: String = this::class.simpleName!!

    override val name: String = "${this::class.java.packageName}.providers.${this::class.simpleName}"

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun precomputeOnUiThread(editor: Editor) {
        // Precomputations on the UI thread are unnecessary in this context, so this is left intentionally empty.
    }

    override fun computeCodeVision(
        editor: Editor,
        uiData: Unit,
    ): CodeVisionState {
        editor.project ?: return CodeVisionState.READY_EMPTY

        val fileSupported = isFileSupported(editor.project!!, editor.virtualFile)

        if (!fileSupported) return CodeVisionState.READY_EMPTY

        return recomputeCodeVision(editor)
    }

    private fun triggerApiCall(
        editor: Editor,
        apiCalls: MutableSet<String>,
        action: () -> Unit,
    ) {
        val filePath = editor.virtualFile.path

        if (!isApiCallInProgressForFile(filePath, apiCalls)) {
            markApiCallInProgress(filePath, apiCalls)

            action()
        }
    }

    private fun recomputeCodeVision(editor: Editor): CodeVisionState {
        val project = editor.project!!
        val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
        val settings = serviceProvider.settingsProvider.currentState()
        val document = editor.document
        val query = ReviewCacheQuery(document.text, editor.virtualFile.path)

        val cachedReview = serviceProvider.reviewCacheService.get(query)

        val cachedDelta =
            getCachedDelta(
                filePath = editor.virtualFile.path,
                fileContent = document.text,
                gitService = serviceProvider.gitService,
                deltaCacheService = serviceProvider.deltaCacheService,
            )

        val decision =
            resolveCodeVisionDecision(
                CodeVisionDecisionInput(
                    codeVisionEnabled = settings.enableCodeLenses,
                    monitorEnabled = settings.codeHealthMonitorEnabled,
                    hasCachedReview = cachedReview != null,
                    hasCachedDelta = cachedDelta.first,
                ),
            )

        if (decision.needsDeltaApiCall) {
            triggerApiCall(editor, activeDeltaApiCalls) {
                project.service<CodeDeltaService>().review(editor)
            }
        }

        if (decision.needsReviewApiCall) {
            triggerApiCall(editor, activeReviewApiCalls) {
                project.service<CodeReviewService>().review(editor)
            }
        }

        return when (decision.action) {
            CodeVisionAction.NOT_READY -> CodeVisionState.NotReady
            CodeVisionAction.READY_EMPTY -> CodeVisionState.READY_EMPTY
            CodeVisionAction.READY -> CodeVisionState.Ready(getLenses(editor, cachedReview))
        }
    }

    open fun getLenses(
        editor: Editor,
        result: Review?,
    ): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val categories = categoriesForLenses()
        if (categories.isEmpty()) {
            return arrayListOf()
        }
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()
        for (category in categories) {
            getCodeSmellsByCategory(result, category).forEach { smell ->
                val range =
                    getTextRange(smell.highlightRange.startLine to smell.highlightRange.endLine, editor.document)
                val entry = getCodeVisionEntry(smell)
                lenses.add(range to entry)
            }
        }
        return lenses
    }

    private fun getCodeVisionEntry(codeSmell: CodeVisionCodeSmell): ClickableTextCodeVisionEntry =
        ClickableTextCodeVisionEntry(
            codeSmell.category,
            id,
            { _, sourceEditor -> handleLensClick(sourceEditor, codeSmell) },
            CODE_SMELL,
        )

    open fun handleLensClick(
        editor: Editor,
        codeSmell: CodeVisionCodeSmell,
    ) {
        handleOpenDocs(editor, codeSmell, DocsEntryPoint.CODE_VISION)
    }

    private fun markApiCallInProgress(
        filePath: String,
        apiCalls: MutableSet<String>,
    ) {
        apiCalls.add(filePath)
    }

    private fun isApiCallInProgressForFile(
        filePath: String,
        apiCalls: MutableSet<String>,
    ) = apiCalls.contains(filePath)
}
