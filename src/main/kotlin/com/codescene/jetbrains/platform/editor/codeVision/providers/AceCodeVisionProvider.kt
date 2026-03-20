package com.codescene.jetbrains.platform.editor.codeVision.providers

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.computeAceLenses
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.RefactoringParams
import com.codescene.jetbrains.platform.util.getTextRange
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

    override fun computeCodeVision(
        editor: Editor,
        uiData: Unit,
    ): CodeVisionState {
        val project = editor.project ?: return CodeVisionState.READY_EMPTY
        val settings = CodeSceneProjectServiceProvider.getInstance(project).settingsProvider.currentState()

        val disabled =
            !settings.enableAutoRefactor ||
                settings.aceAuthToken.trim().isEmpty() || settings.aceStatus == AceStatus.DEACTIVATED
        if (disabled) {
            Log.info("Rendering empty code vision providers for file '${editor.virtualFile?.name}'.")
            return CodeVisionState.READY_EMPTY
        }

        val aceResults =
            AceEntryOrchestrator.getInstance(
                project,
            ).fetchAceCache(editor.virtualFile.path, editor.document.text)
        val lenses = getLens(editor, aceResults)

        return CodeVisionState.Ready(lenses)
    }

    private fun getLens(
        editor: Editor,
        refactorableFunctions: List<FnToRefactor>?,
    ): List<Pair<TextRange, CodeVisionEntry>> {
        val lensData = computeAceLenses(refactorableFunctions)
        val functionsMap = refactorableFunctions?.associateBy { it.name } ?: emptyMap()

        return lensData.mapNotNull { data ->
            val function = functionsMap[data.functionName] ?: return@mapNotNull null
            val range = getTextRange(data.startLine to data.endLine, editor.document)
            val entry =
                ClickableTextCodeVisionEntry(
                    text = name,
                    providerId = id,
                    onClick = { _, sourceEditor -> handleLensClick(sourceEditor, function) },
                    icon = CODESCENE_ACE,
                )
            range to entry
        }
    }

    private fun handleLensClick(
        editor: Editor,
        function: FnToRefactor,
    ) {
        AceEntryOrchestrator.getInstance(editor.project!!).handleAceEntryPoint(
            RefactoringParams(
                project = editor.project!!,
                editor = editor,
                request =
                    RefactoringRequest(
                        filePath = editor.virtualFile.path,
                        language = editor.virtualFile.extension,
                        function = function,
                        source = AceEntryPoint.CODE_VISION,
                    ),
            ),
        )
    }
}
