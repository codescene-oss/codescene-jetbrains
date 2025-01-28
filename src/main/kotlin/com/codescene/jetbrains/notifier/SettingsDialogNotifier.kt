package com.codescene.jetbrains.notifier

import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.util.messages.Topic

const val SETTINGS_DIALOG_NOTIFIER = "Open $CODESCENE Settings Dialog"

interface SettingsDialogNotifier {
    fun dialogOpened()

    companion object {
        val TOPIC: Topic<SettingsDialogNotifier> =
            Topic.create(SETTINGS_DIALOG_NOTIFIER, SettingsDialogNotifier::class.java)
    }
}