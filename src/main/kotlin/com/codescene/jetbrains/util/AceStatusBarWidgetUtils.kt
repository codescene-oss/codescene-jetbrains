package com.codescene.jetbrains.util

import com.codescene.jetbrains.CodeSceneIcons.CODESCENE_TW
import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.actions.ShowSettingsAction
import com.codescene.jetbrains.config.global.AceStatus
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.AceService
import com.codescene.jetbrains.util.Constants.ACE_STATUS
import com.codescene.jetbrains.util.Constants.CONTACT_US_ABOUT_ACE_URL
import com.codescene.jetbrains.util.Constants.DEACTIVATED
import com.codescene.jetbrains.util.Constants.GREEN
import com.codescene.jetbrains.util.Constants.OUT_OF_CREDITS
import com.codescene.jetbrains.util.Constants.RED
import com.codescene.jetbrains.util.Constants.RETRY
import com.codescene.jetbrains.util.Constants.SIGNED_IN
import com.codescene.jetbrains.util.Constants.SIGNED_OUT
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.ide.BrowserUtil
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.util.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent

fun getStatusBarWidgetTooltip() = when (CodeSceneGlobalSettingsStore.getInstance().state.aceStatus) {
    AceStatus.SIGNED_IN -> SIGNED_IN
    AceStatus.SIGNED_OUT -> SIGNED_OUT
    AceStatus.DEACTIVATED -> DEACTIVATED
    AceStatus.OUT_OF_CREDITS -> OUT_OF_CREDITS
    AceStatus.ERROR, AceStatus.OFFLINE -> RETRY
}

fun getStatusBarWidgetIcon(): Icon {
    val originalIcon = CODESCENE_TW
    val wh = 10
    val aceStatus = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus

    val iconWithBadge = when (aceStatus) {
        AceStatus.ERROR -> ExecutionUtil.getIndicator(originalIcon, wh, wh, RED)
        AceStatus.SIGNED_IN -> ExecutionUtil.getIndicator(originalIcon, wh, wh, GREEN)
        AceStatus.SIGNED_OUT -> ExecutionUtil.getIndicator(originalIcon, wh, wh, JBColor.ORANGE)
        AceStatus.DEACTIVATED -> ExecutionUtil.getIndicator(originalIcon, wh, wh, JBColor.GRAY)
        AceStatus.OFFLINE -> ExecutionUtil.getIndicator(originalIcon, wh, wh, JBColor.LIGHT_GRAY)
        AceStatus.OUT_OF_CREDITS -> ExecutionUtil.getIndicator(originalIcon, wh, wh, JBColor.YELLOW)
    }

    return iconWithBadge
}

fun getAceStatusClickConsumer() = Consumer { e: MouseEvent ->
    if (e.button == MouseEvent.BUTTON1) {
        val aceStatus = CodeSceneGlobalSettingsStore.getInstance().state.aceStatus
        val actionGroup = DefaultActionGroup()
        getActionsBasedOnStatus(aceStatus, actionGroup)

        // get the specific component (widget)
        val widgetComponent: JComponent? = e.source as? JComponent

        widgetComponent?.let { component ->
            val popup = JBPopupFactory.getInstance()
                .createActionGroupPopup(
                    "$ACE_STATUS: ${aceStatus.value}",
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

private fun getActionsBasedOnStatus(aceStatus: AceStatus, actionGroup: DefaultActionGroup) {
    when (aceStatus) {
        AceStatus.ERROR, AceStatus.OFFLINE -> {
            actionGroup.add(object : AnAction(UiLabelsBundle.message("widgetRetryAce")) {
                override fun actionPerformed(e: AnActionEvent) {
                    Log.info("Connection to ACE retried from status bar widget for '${aceStatus.value}' status.")
                    CoroutineScope(Dispatchers.IO).launch {
                        AceService.getInstance().runPreflight(true)
                    }
                }
            })
            actionGroup.addSeparator()
        }

        AceStatus.OUT_OF_CREDITS -> {
            actionGroup.add(object : AnAction(UiLabelsBundle.message("widgetLearnMore")) {
                override fun actionPerformed(e: AnActionEvent) {
                    BrowserUtil.browse(CONTACT_US_ABOUT_ACE_URL)
                }
            })
        }

        AceStatus.SIGNED_OUT, AceStatus.DEACTIVATED -> {
            actionGroup.add(ShowSettingsAction())
        }

        AceStatus.SIGNED_IN -> {
            actionGroup.add(object : AnAction(UiLabelsBundle.message("widgetAceActive")) {
                override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

                override fun update(e: AnActionEvent) {
                    e.presentation.isEnabled = false
                }

                override fun actionPerformed(p0: AnActionEvent) {
                    // No action needed
                }
            })
        }
    }
}