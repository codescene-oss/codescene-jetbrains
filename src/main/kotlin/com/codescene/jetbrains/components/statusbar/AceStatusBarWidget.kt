package com.codescene.jetbrains.components.statusbar

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.flag.RuntimeFlags
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.util.Constants.ACE_STATUS
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.getAceStatusClickConsumer
import com.codescene.jetbrains.util.getStatusBarWidgetIcon
import com.codescene.jetbrains.util.getStatusBarWidgetTooltip
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import java.awt.event.MouseEvent

class AceStatusBarWidget : StatusBarWidget.IconPresentation, StatusBarWidget {
    private var statusBar: StatusBar? = null
    private var value: String = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus.value

    override fun ID(): String = this::class.simpleName!!

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText() = getStatusBarWidgetTooltip()

    override fun getIcon() = getStatusBarWidgetIcon()

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
                    Log.debug("Refreshing $ACE_STATUS in Status Bar...")
                    value = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus.value
                    statusBar?.updateWidget(ID())
                }
            })
    }

    /**
     * Handle actions based on ACE status.
     */
    override fun getClickConsumer(): Consumer<MouseEvent> {
        return getAceStatusClickConsumer()
    }
}

internal class AceStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = this::class.simpleName!!
    override fun getDisplayName(): String = ACE_STATUS
    override fun isAvailable(project: Project): Boolean = RuntimeFlags.aceFeature
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = AceStatusBarWidget()
    override fun isEnabledByDefault(): Boolean = true
}
