package com.codescene.jetbrains.codeInsight.intentions

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.services.CodeSceneDocumentationService
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile

class ACERefactorAction : IntentionAction, HighPriorityAction, Iconable {
    private val name = "Refactor using $CODESCENE ACE"

    override fun getText(): String = name

    override fun getFamilyName(): String = name

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val codeSceneDocumentationService = CodeSceneDocumentationService.getInstance(project)

        if (editor != null) codeSceneDocumentationService.openAcePanel(editor)
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.HIGH

    override fun getIcon(p0: Int) = CODESCENE_ACE
}