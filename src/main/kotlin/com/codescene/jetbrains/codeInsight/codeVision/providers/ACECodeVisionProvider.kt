package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.data.delta.FunctionFinding
import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.util.getCachedDelta
import com.codescene.jetbrains.util.getTextRange
import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

@Suppress("UnstableApiUsage")
class ACECodeVisionProvider : CodeVisionProvider<Unit> {
    override val id: String = this::class.simpleName!!

    override val name: String = "CodeScene ACE"

    override val defaultAnchor = CodeVisionAnchorKind.Right

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun isAvailableFor(project: Project) = CodeSceneGlobalSettingsStore.getInstance().state.enableAutoRefactor

    override fun precomputeOnUiThread(editor: Editor) {
        // Precomputations on the UI thread are unnecessary in this context, so this is left intentionally empty.
    }

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        editor.project ?: return CodeVisionState.READY_EMPTY

        val cachedDelta = getCachedDelta(editor)

        // TODO: Preflight to check if supported?

        // Dummy implementation to trigger ACE lens:
        val refactorableFunctions = cachedDelta.second?.functionLevelFindings?.filter {
            (it?.function?.name?.startsWith("a", true) == true || (it?.function?.name?.startsWith(
                "t",
                true
            ) == true) && it.function.range != null)
        }

        val lenses = getLens(editor, refactorableFunctions)

        return CodeVisionState.Ready(lenses)
    }

    private fun getLens(
        editor: Editor,
        refactorableFunctions: List<FunctionFinding>?
    ): List<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        refactorableFunctions?.forEach {
            val range = getTextRange(it.function.range.startLine to it.function.range.endLine, editor.document)
            val entry = ClickableTextCodeVisionEntry(
                text = name,
                providerId = id,
                onClick = { _, sourceEditor -> handleLensClick(sourceEditor) },
                icon = CODESCENE_ACE
            )

            lenses.add(range to entry)
        }

        return lenses
    }

    private fun handleLensClick(editor: Editor) {
        val project = editor.project ?: return
        val codeSceneDocumentationService = CodeSceneDocumentationService.getInstance(project)

        codeSceneDocumentationService.openAcePanel(editor)
    }
}