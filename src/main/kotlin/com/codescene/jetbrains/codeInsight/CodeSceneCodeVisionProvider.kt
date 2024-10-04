package com.codescene.jetbrains.codeInsight

import com.intellij.codeInsight.codeVision.*
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.Nls
import java.awt.event.MouseEvent

//TODO: refactor
@Suppress("UnstableApiUsage")
abstract class CodeSceneCodeVisionProvider : CodeVisionProvider<Unit> {
    abstract val categoryToFilter: String

    abstract override val id: String

    abstract fun handleClick(editor: Editor, element: CodeSmell, event: MouseEvent?)

    override val defaultAnchor = CodeVisionAnchorKind.Top

    override val relativeOrderings: List<CodeVisionRelativeOrdering> = emptyList()

    override fun precomputeOnUiThread(editor: Editor) {}

    override fun computeCodeVision(editor: Editor, uiData: Unit): CodeVisionState {
        val project = editor.project ?: return CodeVisionState.READY_EMPTY

        return recomputeLenses(editor, project)
    }

    open fun logClickToFUS(element: PsiElement, hint: @Nls String) {}

    private fun getTextRange(codeSmell: CodeSmell, editor: Editor): TextRange {
        val start = editor.document.getLineStartOffset(codeSmell.range.startLine) - 1
        val end = editor.document.getLineStartOffset(codeSmell.range.endLine) - 1

        return TextRange(start, end)
    }

    private fun List<CodeSmell>.filterByCategory(): List<CodeSmell> {
        return this.filter { it.category == categoryToFilter }
    }

    private fun getCodeSmellsByCategory(): List<CodeSmell> {
        val fileLevelSmells = codeAnalysisResult.fileLevelCodeSmells.filterByCategory()

        val functionLevelSmells = codeAnalysisResult.functionLevelCodeSmells.flatMap { functionCodeSmell ->
            functionCodeSmell.codeSmells.filterByCategory()
        }

        val expressionLevelSmells = codeAnalysisResult.expressionLevelCodeSmells.filterByCategory()

        return fileLevelSmells + functionLevelSmells + expressionLevelSmells
    }


    private fun getCodeVisionEntry(codeSmell: CodeSmell): ClickableTextCodeVisionEntry {
        val message = codeSmell.details.takeIf { it.isNotEmpty() }
            ?.let { "${codeSmell.category} ($it)" } ?: codeSmell.category

        return ClickableTextCodeVisionEntry(
            message,
            id,
            { event, sourceEditor -> handleClick(sourceEditor, codeSmell, event) },
            AllIcons.General.InspectionsWarningEmpty
        )
    }

    private fun handleCodeSmells(editor: Editor): ArrayList<Pair<TextRange, CodeVisionEntry>> {
        val lenses = ArrayList<Pair<TextRange, CodeVisionEntry>>()

        getCodeSmellsByCategory().forEach { smell ->
            val range = getTextRange(smell, editor)
            val entry = getCodeVisionEntry(smell)

            lenses.add(range to entry)
        }

        return lenses
    }

    private fun recomputeLenses(editor: Editor, project: Project): CodeVisionState {
        if (DumbService.isDumb(project)) return CodeVisionState.READY_EMPTY

        return ReadAction.compute<CodeVisionState, RuntimeException> {
            val file = FileDocumentManager.getInstance().getFile(editor.document)?.findPsiFile(project)
                ?: return@compute CodeVisionState.READY_EMPTY

            if (file.project.isDefault) return@compute CodeVisionState.READY_EMPTY

            val lenses = handleCodeSmells(editor)
            CodeVisionState.Ready(lenses)
        }
    }
}