package com.codescene.jetbrains.components.statusbar

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.notifier.AceStatusRefreshNotifier
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.util.Constants.ACE_STATUS
import com.codescene.jetbrains.util.Log
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent

class AceStatusBarWidget : StatusBarWidget.IconPresentation, StatusBarWidget {
    private var statusBar: StatusBar? = null
    private var value: String = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus.value
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun ID(): String = this::class.simpleName!!

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String = "$ACE_STATUS: $value"

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
                    Log.debug("Refreshing $ACE_STATUS in Status Bar...")
                    value = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus.value
                    statusBar?.updateWidget(ID())
                }
            })
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? {
        return Consumer { e: MouseEvent ->
            if (e.button == MouseEvent.BUTTON1) {
                val actionGroup = DefaultActionGroup().apply {
                    add(object : AnAction("Refresh") {
                        override fun actionPerformed(e: AnActionEvent) {
                            scope.launch {
                                AceService.getInstance().runPreflight(true)
                            }
                        }

                        override fun update(e: AnActionEvent) {
                            if (value != AceStatus.ERROR.value && value != AceStatus.OFFLINE.value) {
                                e.presentation.isEnabled = false
                            }
                        }
                    })
                }

                // get the specific component (widget)
                val widgetComponent: JComponent? = e.source as? JComponent

                widgetComponent?.let { component ->
                    val popup = JBPopupFactory.getInstance()
                        .createActionGroupPopup(
                            ACE_STATUS,
                            actionGroup,
                            DataManager.getInstance().getDataContext(component),
                            JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                            true
                        )

                    val popupSize = popup.content.preferredSize
                    val locationOnScreen = component.locationOnScreen
                    val popupX = locationOnScreen.x + e.x
                    val popupY = locationOnScreen.y - popupSize.height

                    ApplicationManager.getApplication().invokeLater {
                        popup.showInScreenCoordinates(component, Point(popupX, popupY))
                    }
                }
            }
        }
    }
}

class AceStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = this::class.simpleName!!
    override fun getDisplayName(): String = ACE_STATUS
    override fun isAvailable(project: Project): Boolean = CodeSceneGlobalSettingsStore.getInstance().state.aceEnabled
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = AceStatusBarWidget()
    override fun isEnabledByDefault(): Boolean = true
}

