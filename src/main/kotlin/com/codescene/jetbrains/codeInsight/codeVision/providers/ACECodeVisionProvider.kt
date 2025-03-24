package com.codescene.jetbrains.codeInsight.codeVision.providers

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.util.fetchAceCache
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
        val settings = CodeSceneGlobalSettingsStore.getInstance().state
        if (editor.project == null || !settings.enableAutoRefactor) return CodeVisionState.READY_EMPTY

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

    //TODO: Common entry point logic
    private fun handleLensClick(editor: Editor, function: FnToRefactor) {
        val project = editor.project ?: return
        val codeSceneDocumentationService = CodeSceneDocumentationService.getInstance(project)

        //Open window after refactoring result is ready? No loader solution?
        AceService.getInstance().refactor(function)
        codeSceneDocumentationService.openAcePanel(editor, function)
    }
}