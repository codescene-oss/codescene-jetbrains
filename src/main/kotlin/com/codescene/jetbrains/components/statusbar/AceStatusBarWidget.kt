package com.codescene.jetbrains.components.statusbar

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_ACE
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.services.AceService
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JOptionPane

class AceStatusBarWidget : StatusBarWidget.MultipleTextValuesPresentation, StatusBarWidget {
    private var value: String? = null

    override fun ID(): String = "AceStatusBarWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String = "CodeScene ACE Status: ${getSelectedValue()}"

    override fun getClickConsumer(): Consumer<MouseEvent>? = Consumer<MouseEvent> {
        JOptionPane.showMessageDialog(null, "You clicked the custom status bar widget!")
    }

    override fun getSelectedValue(): @NlsContexts.StatusBarText String? = value

    override fun getIcon(): Icon? = CODESCENE_ACE

    override fun install(statusBar: StatusBar) {

        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AceStatusRefreshNotifier.TOPIC,
            object : AceStatusRefreshNotifier {
                override fun refresh() {
                    Log.warn("Refreshing ACE status in Status Bar...")

                    val newStatus = AceService.getInstance().getPreflightInfo()
                    value = newStatus.name
                }
            })
    }

    override fun dispose() {
    }

}

class AceStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = "AceStatusBarWidgetFactory"

    override fun getDisplayName(): String = "CodeScene ACE Status"

    override fun isAvailable(project: Project): Boolean = true

    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = AceStatusBarWidget()

    override fun isEnabledByDefault(): Boolean = true
}
