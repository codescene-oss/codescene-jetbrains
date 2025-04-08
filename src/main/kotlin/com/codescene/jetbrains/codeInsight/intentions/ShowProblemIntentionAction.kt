package com.codescene.jetbrains.codeInsight.intentions

import com.codescene.data.review.CodeSmell
import com.codescene.jetbrains.services.htmlviewer.CodeSceneDocumentationViewer
import com.codescene.jetbrains.services.htmlviewer.DocsEntryPoint
import com.codescene.jetbrains.services.htmlviewer.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ShowProblemIntentionAction(private val codeSmell: CodeSmell) : IntentionAction, LowPriorityAction {
    private val name = "$CODESCENE: ${codeSmell.category}"

    override fun getText(): String = name

    override fun getFamilyName(): String = name

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val docViewer = CodeSceneDocumentationViewer.getInstance(project)
        if (editor != null) {
            docViewer.open(
                editor,
                DocumentationParams(
                    codeSmell.category,
                    editor.virtualFile.name,
                    editor.virtualFile.path,
                    codeSmell.highlightRange.startLine,
                    DocsEntryPoint.INTENTION_ACTION
                )
            )
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
}