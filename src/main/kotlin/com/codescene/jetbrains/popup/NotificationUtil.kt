package com.codescene.jetbrains.popup

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.TERMS_AND_CONDITIONS_URL
import com.codescene.jetbrains.util.Log
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.awt.Desktop
import java.net.URI

//TODO: refactor
class NotificationUtil {
    fun showNotification(project: Project?, title: String, content: String, type: NotificationType) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(CODESCENE)
            .createNotification(title, content, type)

        notification.addAction(object : AnAction(UiLabelsBundle.message("acceptButton")) {
            override fun actionPerformed(e: AnActionEvent) {
                val state = CodeSceneGlobalSettingsStore.getInstance()
                state.updateTermsAndConditionsAcceptance(true)
                notification.expire()

                //TODO: telemetry event for accepted TaC
            }
        })

        notification.addAction(object : AnAction(UiLabelsBundle.message("ignoreButton")) {
            override fun actionPerformed(e: AnActionEvent) {
                notification.expire()

                //TODO: telemetry event for ignored TaC
            }
        })

        notification.addAction(object : AnAction(UiLabelsBundle.message("termsAndConditionsButton")) {
            override fun actionPerformed(e: AnActionEvent) {
                try {
                    val uri = URI(TERMS_AND_CONDITIONS_URL)
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(uri)
                } catch (e: Exception) {
                    Log.warn("Unable to open link: ${e.message}")
                }
            }
        })

        notification.whenExpired {
            if (CodeSceneGlobalSettingsStore.getInstance().state.termsAndConditionsAccepted) {
                println("Closed but accepted")
            } else {
                println("Closed and not accepted!")
            }
        }

        notification.notify(project)
    }
}