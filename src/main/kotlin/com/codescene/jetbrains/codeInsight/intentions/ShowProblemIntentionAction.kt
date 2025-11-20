package com.codescene.jetbrains.codeInsight.intentions

import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.components.webview.data.shared.FileMetaType
import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.view.DocsData
import com.codescene.jetbrains.components.webview.util.nameDocMap
import com.codescene.jetbrains.components.webview.util.openDocs
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.getCodeSmell
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
                    fileName = editor.virtualFile.path,
                    fn = Fn(
                        name = codeSmell.functionInfo?.name ?: "",
                        range = RangeCamelCase(
                            endLine = codeSmell.functionInfo?.range?.endLine ?: 0,
                            startLine = codeSmell.functionInfo?.range?.startLine ?: 0,
                            endColumn = codeSmell.functionInfo?.range?.endColumn ?: 0,
                            startColumn = codeSmell.functionInfo?.range?.startColumn ?: 0,
                        )
                    )
                )
            )

            val smell = getCodeSmell(codeSmell)
            openDocs(docsData, project, DocsEntryPoint.INTENTION_ACTION, smell)
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
}