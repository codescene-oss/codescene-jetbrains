package com.codescene.jetbrains.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.newEditor.SettingsDialogListener
import com.intellij.openapi.project.Project

object SettingsDialogListener {
    fun subscribeToSettingsDialog(project: Project) {
        project.messageBus.connect(project as Disposable).subscribe(SettingsDialogListener.TOPIC, object : SettingsDialogListener {
            fun beforeConfigurableDisplayed(configurable: Configurable) {
                if (configurable.displayName == "My Specific Configurable") {
                    println("Opening My Specific Configurable")
                    // Perform your specific action here
                }
            }

            fun afterConfigurableDisplayed(configurable: Configurable) {
                if (configurable.displayName == "My Specific Configurable") {
                    println("Displayed My Specific Configurable")
                    // Additional actions after the configurable is displayed
                }
            }
        })
    }
}