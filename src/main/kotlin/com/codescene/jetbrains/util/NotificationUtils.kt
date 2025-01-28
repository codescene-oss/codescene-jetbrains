package com.codescene.jetbrains.util

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.codescene.jetbrains.util.Constants.TERMS_AND_CONDITIONS_URL
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import java.awt.Desktop
import java.net.URI

/**
 * The `showNotification` method handles the notification actions for the Terms and Conditions acceptance flow.
 * The notification provides three actions:
1. Accepting the Terms and Conditions: Updates user settings and expires the notification.
2. Ignoring the Terms and Conditions: Expires the notification without making any changes.
3. Viewing the Terms and Conditions: Opens the provided URL in the default browser.

 * The notification also listens for expiration events. If the notification is dismissed without accepting
the Terms and Conditions (e.g., closing the IDE/project, clicking "Ignore", or manually closing the pop-up),
a telemetry event for non-acceptance should be triggered if the user hasn't accepted the Terms and Conditions.
 */
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

    notification.addAction(object : AnAction("View Terms && Policies") {
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
        if (!CodeSceneGlobalSettingsStore.getInstance().state.termsAndConditionsAccepted) {
            //TODO: telemetry event for ignored TaC
        }
    }

    notification.notify(project)
}
