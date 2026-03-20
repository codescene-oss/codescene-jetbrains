package com.codescene.jetbrains.platform.editor.intentions

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.util.AceEntryPoint
import com.codescene.jetbrains.core.util.Constants.CODESCENE
import com.codescene.jetbrains.platform.icons.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.RefactoringParams
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiFile

class AceRefactorAction(private val function: FnToRefactor) : IntentionAction, HighPriorityAction, Iconable {
    private val name = "Refactor using $CODESCENE ACE"

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
        if (editor == null) return
        AceEntryOrchestrator.getInstance(project).handleAceEntryPoint(
            RefactoringParams(
                project = project,
                editor = editor,
                request =
                    RefactoringRequest(
                        filePath = editor.virtualFile.path,
                        language = editor.virtualFile.extension,
                        function = function,
                        source = AceEntryPoint.INTENTION_ACTION,
                    ),
            ),
        )
    }

    override fun startInWriteAction(): Boolean = false

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.HIGH

    override fun getIcon(p0: Int) = CODESCENE_ACE
}
