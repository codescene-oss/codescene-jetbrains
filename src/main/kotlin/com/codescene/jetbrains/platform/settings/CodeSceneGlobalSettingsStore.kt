package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "CodeSceneGlobalSettings",
    storages = [Storage("codescene-settings.xml")],
)
class CodeSceneGlobalSettingsStore : PersistentStateComponent<CodeSceneGlobalSettings>, ISettingsProvider {
    private var extensionSettingsState: CodeSceneGlobalSettings = CodeSceneGlobalSettings()
    private val listeners = mutableSetOf<ISettingsChangeListener>()

    override fun getState(): CodeSceneGlobalSettings {
        return extensionSettingsState
    }

    override fun loadState(state: CodeSceneGlobalSettings) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState = state
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    override fun currentState(): CodeSceneGlobalSettings = extensionSettingsState

    override fun updateTelemetryConsent(hasAccepted: Boolean) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState.telemetryConsentGiven = hasAccepted
        notifyListeners(oldState, extensionSettingsState.copy())

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().saveSettings()
        }
    }

    override fun updateAceStatus(status: AceStatus) {
        val oldState = extensionSettingsState.copy()
        extensionSettingsState.aceStatus = status
        notifyListeners(oldState, extensionSettingsState.copy())
    }

    override fun updateAceAcknowledged(acknowledged: Boolean) {
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

    companion object {
        fun getInstance(): CodeSceneGlobalSettingsStore {
            return ApplicationManager.getApplication().getService(CodeSceneGlobalSettingsStore::class.java)
        }
    }
}
