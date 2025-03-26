package com.codescene.jetbrains.components.statusbar

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.services.AceService
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import javax.swing.Icon

class AceStatusBarWidget : StatusBarWidget.IconPresentation, StatusBarWidget {
    private var statusBar: StatusBar? = null
    private var value: String = AceService.getInstance().getStatus().value

    override fun ID(): String = "AceStatusBarWidget"

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String = "CodeScene ACE Status: $value"

    override fun getIcon(): Icon = CODESCENE_TW

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        subscribeToAceStatusRefreshEvent()
    }

    override fun dispose() {
        statusBar = null
    }

    private fun subscribeToAceStatusRefreshEvent() {
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            AceStatusRefreshNotifier.TOPIC,
            object : AceStatusRefreshNotifier {
                override fun refresh() {
                    Log.warn("Refreshing ACE status in Status Bar...")
                    value = AceService.getInstance().getStatus().value
                    statusBar?.updateWidget(ID())
                }
            })
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { _: MouseEvent ->
                Log.warn("Button clicked")
            if (value == AceStatus.ERROR.value) {
                AceService.getInstance().runPreflight(true)
            }
        }
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

