package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.AceEntryPoint
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.RefactoringParams
import com.codescene.jetbrains.util.fetchAceCache
import com.codescene.jetbrains.util.getTextRange
import com.codescene.jetbrains.util.handleAceEntryPoint
import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionProvider
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.CodeVisionState
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange

@Suppress("UnstableApiUsage")
class AceCodeVisionProvider : CodeVisionProvider<Unit> {
    override val id: String = this::class.simpleName!!

    override val name: String = "CodeScene ACE"

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun isAvailableFor(project: Project) = true

    override fun precomputeOnUiThread(editor: Editor) {
        // Precomputations on the UI thread are unnecessary in this context, so this is left intentionally empty.
    }

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        val settings = CodeSceneGlobalSettingsStore.getInstance().state

        val disabled = editor.project == null || !settings.enableAutoRefactor ||
                settings.aceAuthToken.trim().isEmpty() || settings.aceStatus == AceStatus.DEACTIVATED
        if (disabled) {
            Log.info("Rendering empty code vision providers for file '${editor.virtualFile?.name}'.")
            return CodeVisionState.READY_EMPTY
        }

        val aceResults = fetchAceCache(editor.virtualFile.path, editor.document.text, editor.project!!)
        val lenses = getLens(editor, aceResults)

        return CodeVisionState.Ready(lenses)
    }

    private fun getLens(
        editor: Editor,
        refactorableFunctions: List<FnToRefactor>?
    ): List<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        refactorableFunctions?.forEach {
            val range = getTextRange(it.range.startLine to it.range.endLine, editor.document)
            val entry = ClickableTextCodeVisionEntry(
                text = name,
                providerId = id,
                onClick = { _, sourceEditor -> handleLensClick(sourceEditor, it) },
                icon = CODESCENE_ACE
            )

            lenses.add(range to entry)
        }

        return lenses
    }

    private fun handleLensClick(editor: Editor, function: FnToRefactor) {
        handleAceEntryPoint(RefactoringParams(editor.project!!, editor, function, AceEntryPoint.CODE_VISION))
    }
}