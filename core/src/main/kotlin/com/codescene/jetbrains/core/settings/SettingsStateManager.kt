package com.codescene.jetbrains.core.settings

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings

class SettingsStateManager {
    private var extensionSettingsState: CodeSceneGlobalSettings = CodeSceneGlobalSettings()
    private val listeners = mutableSetOf<ISettingsChangeListener>()

    fun getState(): CodeSceneGlobalSettings = extensionSettingsState

    fun loadState(state: CodeSceneGlobalSettings) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState = state
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    fun currentState(): CodeSceneGlobalSettings = extensionSettingsState

    fun updateTelemetryConsent(hasAccepted: Boolean) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState.telemetryConsentGiven = hasAccepted
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    fun updateTelemetryNoticeShown(shown: Boolean) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState.telemetryNoticeShown = shown
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    fun updateAceStatus(status: AceStatus) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState.aceStatus = status
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    fun updateAceAcknowledged(acknowledged: Boolean) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState.aceAcknowledged = acknowledged
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    fun addSettingsChangeListener(listener: ISettingsChangeListener) {
        listeners += listener
    }

    fun removeSettingsChangeListener(listener: ISettingsChangeListener) {
        listeners -= listener
    }

    fun notifyIfStateChanged(oldState: CodeSceneGlobalSettings) {
        val newState = extensionSettingsState.copy()
        if (oldState != newState) {
            notifyListeners(oldState, newState)
        }
    }

    private fun notifyListeners(
        oldState: CodeSceneGlobalSettings,
        newState: CodeSceneGlobalSettings,
    ) {
        listeners.forEach { it.onSettingsChanged(oldState, newState) }
    }
}
