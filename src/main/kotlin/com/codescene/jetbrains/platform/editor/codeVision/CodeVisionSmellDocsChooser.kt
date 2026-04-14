package com.codescene.jetbrains.platform.editor.codeVision

import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.models.DocsEntryPoint
import com.codescene.jetbrains.core.util.formatCodeSmellMessage
import com.codescene.jetbrains.platform.UiLabelsBundle
import com.codescene.jetbrains.platform.util.handleOpenDocs
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent

internal fun showSmellDocumentationChooser(
    mouseEvent: MouseEvent?,
    editor: Editor,
    smells: List<CodeVisionCodeSmell>,
) {
    val group = DefaultActionGroup()
    for (smell in smells) {
        group.add(
            object : AnAction(formatCodeSmellMessage(smell.category, smell.details)) {
                override fun getActionUpdateThread() = ActionUpdateThread.BGT

                override fun actionPerformed(e: AnActionEvent) {
                    handleOpenDocs(editor, smell, DocsEntryPoint.CODE_VISION)
                }
            },
        )
    }
    val title = UiLabelsBundle.message("codeVisionChooseCodeSmell")
    val context = DataManager.getInstance().getDataContext(editor.contentComponent)
    val popup =
        JBPopupFactory.getInstance().createActionGroupPopup(
            title,
            group,
            context,
            JBPopupFactory.ActionSelectionAid.NUMBERING,
            true,
        )
    ApplicationManager.getApplication().invokeLater {
        if (mouseEvent != null) {
            val component = mouseEvent.component ?: editor.contentComponent
            popup.show(RelativePoint(component, mouseEvent.point))
        } else {
            popup.showInBestPositionFor(editor)
        }
    }
}
