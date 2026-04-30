package com.codescene.jetbrains.platform.editor.codeVision

import com.codescene.data.review.Review
import com.codescene.jetbrains.core.delta.getCachedDelta
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.util.CodeVisionAction
import com.codescene.jetbrains.core.util.CodeVisionDecision
import com.codescene.jetbrains.core.util.CodeVisionDecisionInput
import com.codescene.jetbrains.core.util.defaultCodeVisionProviderIds
import com.codescene.jetbrains.core.util.formatCodeSmellMessage
import com.codescene.jetbrains.core.util.resolveCodeVisionDecision
import com.codescene.jetbrains.platform.api.CachedReviewService
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODE_SMELL
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.handleOpenDocs
import com.codescene.jetbrains.platform.util.isFileSupported
import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionEntryExtraActionModel
import com.intellij.codeInsight.codeVision.CodeVisionProvider
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.CodeVisionState
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    companion object {
        private const val SMELL_EXTRA_ACTION_MARKER = ".smell."

        fun getProviders(): List<String> = defaultCodeVisionProviderIds
    }

    protected open fun categoriesForLenses(): List<String> = emptyList()

    override val id: String = this::class.simpleName!!

    override val name: String = "${this::class.java.packageName}.providers.${this::class.simpleName}"

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun precomputeOnUiThread(editor: Editor) {
        // Precomputations on the UI thread are unnecessary in this context, so this is left intentionally empty.
    }

    override fun handleExtraAction(
        editor: Editor,
        textRange: TextRange,
        actionId: String,
    ) {
        val index = parseSmellExtraActionIndex(actionId) ?: return
        val smells = smellsAtTextRange(editor, textRange) ?: return
        val smell = smells.getOrNull(index) ?: return
        handleLensClick(editor, smell)
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

    private fun scheduleReviewIfNeeded(
        project: Project,
        editor: Editor,
        decision: CodeVisionDecision,
        shortPath: String,
    ) {
        if (!decision.needsDeltaApiCall && !decision.needsReviewApiCall) {
            return
        }
        val filePath = editor.virtualFile.path
        val cachedReviewService = project.service<CachedReviewService>()
        if (cachedReviewService.activeReviewCalls.contains(filePath)) {
            Log.debug(
                "code vision skip schedule, review in flight file=$shortPath",
                "CodeSceneCodeVision",
            )
            return
        }
        val docStamp = editor.document.modificationStamp
        val unchangedSinceForeground =
            CodeVisionReviewScheduleHint.isDocumentUnchangedSinceForegrounded(filePath, docStamp)
        val debounceMs =
            when {
                !decision.needsReviewApiCall -> 0L
                unchangedSinceForeground -> 0L
                else -> null
            }
        Log.debug(
            "code vision scheduling review file=$shortPath debounceOverride=$debounceMs " +
                "unchangedSinceForeground=$unchangedSinceForeground",
            "CodeSceneCodeVision",
        )
        cachedReviewService.reviewFromCodeVision(editor, debounceMs)
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

        val shortPath = com.codescene.jetbrains.core.git.pathFileName(editor.virtualFile.path)
        Log.debug(
            "code vision file=$shortPath hasReview=${cachedReview != null} hasDelta=${cachedDelta.first} " +
                "monitor=${settings.codeHealthMonitorEnabled} needsReview=${decision.needsReviewApiCall} " +
                "needsDelta=${decision.needsDeltaApiCall} action=${decision.action} " +
                "lenDoc=${document.text.length}",
            "CodeSceneCodeVision",
        )

        scheduleReviewIfNeeded(project, editor, decision, shortPath)

        return when (decision.action) {
            CodeVisionAction.NOT_READY -> resolveNotReadyState(editor, serviceProvider, shortPath)
            CodeVisionAction.READY_EMPTY -> CodeVisionState.READY_EMPTY
            CodeVisionAction.READY -> CodeVisionState.Ready(getLenses(editor, cachedReview))
        }
    }

    private fun resolveNotReadyState(
        editor: Editor,
        serviceProvider: CodeSceneProjectServiceProvider,
        shortPath: String,
    ): CodeVisionState {
        val filePath = editor.virtualFile.path
        val staleReview =
            serviceProvider.reviewCacheService.getLastKnown(filePath)
                ?: return CodeVisionState.NotReady
        Log.debug("code vision reusing stale lenses file=$shortPath", "CodeSceneCodeVision")
        return CodeVisionState.Ready(getLenses(editor, staleReview))
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
        val pairs = collectSmellsWithHighlightRangesForVision(editor, result, categories)
        val grouped = groupReviewSmellsByHighlightRange(pairs)
        for ((range, smells) in grouped) {
            lenses.add(range to codeVisionEntryForSmells(smells))
        }
        return lenses
    }

    private fun smellsAtTextRange(
        editor: Editor,
        textRange: TextRange,
    ): List<CodeVisionCodeSmell>? {
        val categories = categoriesForLenses()
        if (categories.isEmpty()) {
            return null
        }
        val project = editor.project ?: return null
        val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
        val query = ReviewCacheQuery(editor.document.text, editor.virtualFile.path)
        val review = serviceProvider.reviewCacheService.get(query) ?: return null
        val pairs = collectSmellsWithHighlightRangesForVision(editor, review, categories)
        return groupReviewSmellsByHighlightRange(pairs)
            .firstOrNull { (r, _) -> r == textRange }
            ?.second
    }

    private fun parseSmellExtraActionIndex(actionId: String): Int? {
        val marker = "${id}$SMELL_EXTRA_ACTION_MARKER"
        if (!actionId.startsWith(marker)) {
            return null
        }
        return actionId.removePrefix(marker).toIntOrNull()
    }

    private fun smellExtraActionId(index: Int): String = "${id}$SMELL_EXTRA_ACTION_MARKER$index"

    private fun codeVisionEntryForSmells(smells: List<CodeVisionCodeSmell>): ClickableTextCodeVisionEntry {
        require(smells.isNotEmpty())
        if (smells.size == 1) {
            return singleSmellCodeVisionEntry(smells.first())
        }
        val text = smells.joinToString(" | ") { it.category }
        val tooltip = smells.joinToString("\n") { formatCodeSmellMessage(it.category, it.details) }
        val extraActions =
            smells.mapIndexed { index, smell ->
                CodeVisionEntryExtraActionModel(smell.category, smellExtraActionId(index))
            }
        return ClickableTextCodeVisionEntry(
            text,
            id,
            { mouseEvent, sourceEditor -> showSmellDocumentationChooser(mouseEvent, sourceEditor, smells) },
            CODE_SMELL,
            text,
            tooltip,
            extraActions,
        )
    }

    private fun singleSmellCodeVisionEntry(codeSmell: CodeVisionCodeSmell): ClickableTextCodeVisionEntry =
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
}
