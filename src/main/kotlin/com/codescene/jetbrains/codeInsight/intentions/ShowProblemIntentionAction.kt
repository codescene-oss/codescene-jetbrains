package com.codescene.jetbrains.codeInsight.intentions

import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.webview.data.DocsData
import com.codescene.jetbrains.components.webview.data.FileMetaType
import com.codescene.jetbrains.components.webview.data.Fn
import com.codescene.jetbrains.components.webview.data.RangeCamelCase
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ShowProblemIntentionAction(private val codeSmell: CodeVisionCodeSmell) : IntentionAction, LowPriorityAction {
    private val name = "$CODESCENE: ${codeSmell.category}"

    override fun getText(): String = name

    override fun getFamilyName(): String = name

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        editor?.let {
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

            openDocs(docsData, project, DocsEntryPoint.INTENTION_ACTION)
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
}