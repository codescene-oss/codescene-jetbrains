package com.codescene.jetbrains.codeInsight.intentions

import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class ShowProblemIntentionAction(private val codeSmell: String) : IntentionAction, PriorityAction {
    override fun getText(): String = "CodeScene: $codeSmell"

    override fun getFamilyName(): String = "$CODESCENE: $codeSmell"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        println("Showing problem details: $codeSmell")
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
}