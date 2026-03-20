package com.codescene.jetbrains.platform.editor.intentions

import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.platform.util.handleOpenDocs
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

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        file: PsiFile,
    ): Boolean = true

    override fun invoke(
        project: Project,
        editor: Editor?,
        file: PsiFile?,
    ) {
        handleOpenDocs(editor, codeSmell, DocsEntryPoint.INTENTION_ACTION)
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.LOW
}
