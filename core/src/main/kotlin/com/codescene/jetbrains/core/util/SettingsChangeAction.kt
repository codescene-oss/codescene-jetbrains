package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings

sealed class SettingsChangeAction {
    object RefreshCodeVision : SettingsChangeAction()

    data class RefreshAceUI(val enabled: Boolean) : SettingsChangeAction()

    object PublishAceStatusChange : SettingsChangeAction()

    object UpdateStatusBarWidget : SettingsChangeAction()
}

fun resolveSettingsChangeActions(
    oldState: CodeSceneGlobalSettings,
    newState: CodeSceneGlobalSettings,
    aceAuthTokenChanged: Boolean = false,
): List<SettingsChangeAction> {
    val actions = mutableListOf<SettingsChangeAction>()

    if (oldState.enableCodeLenses != newState.enableCodeLenses) {
        actions.add(SettingsChangeAction.RefreshCodeVision)
    }

    if (oldState.aceStatus != newState.aceStatus) {
        actions.add(SettingsChangeAction.RefreshAceUI(newState.aceStatus != AceStatus.DEACTIVATED))
        actions.add(SettingsChangeAction.PublishAceStatusChange)
    }

    if (oldState.aceTokenConfigured != newState.aceTokenConfigured || aceAuthTokenChanged) {
        actions.add(SettingsChangeAction.RefreshAceUI(newState.aceTokenConfigured))
        actions.add(SettingsChangeAction.UpdateStatusBarWidget)
    }

    return actions
}
