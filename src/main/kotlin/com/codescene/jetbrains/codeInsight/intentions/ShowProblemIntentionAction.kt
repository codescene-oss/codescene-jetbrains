package com.codescene.jetbrains.codeInsight.intentions

import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.services.DocsSourceType
import com.codescene.jetbrains.services.DocumentationParams
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ShowProblemIntentionAction(private val codeSmell: CodeSmell) : IntentionAction, PriorityAction {
    private val name = "$CODESCENE: ${codeSmell.category}"

    override fun getText(): String = name

    override fun getFamilyName(): String = name

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val codeSceneDocumentationService = CodeSceneDocumentationService.getInstance(project)
        if (editor != null) {
            codeSceneDocumentationService.openDocumentationPanel(DocumentationParams(editor, codeSmell, DocsSourceType.INTENTION_ACTION))
        }
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
}