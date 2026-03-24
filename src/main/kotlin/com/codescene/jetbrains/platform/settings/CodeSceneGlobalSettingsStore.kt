package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.settings.SettingsStateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "CodeSceneGlobalSettings",
    storages = [Storage("codescene-settings.xml")],
)
class CodeSceneGlobalSettingsStore : PersistentStateComponent<CodeSceneGlobalSettings>, ISettingsProvider {
    private val stateManager = SettingsStateManager()

    override fun getState(): CodeSceneGlobalSettings = stateManager.getState()

    override fun loadState(state: CodeSceneGlobalSettings) {
        stateManager.loadState(state)
    }

    override fun currentState(): CodeSceneGlobalSettings = stateManager.currentState()

    override fun updateTelemetryConsent(hasAccepted: Boolean) {
        stateManager.updateTelemetryConsent(hasAccepted)
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().saveSettings()
        }
    }

    override fun updateAceStatus(status: AceStatus) {
        stateManager.updateAceStatus(status)
    }

    override fun updateAceAcknowledged(acknowledged: Boolean) {
        stateManager.updateAceAcknowledged(acknowledged)
    }

    fun addSettingsChangeListener(listener: ISettingsChangeListener) {
        stateManager.addSettingsChangeListener(listener)
    }

    fun removeSettingsChangeListener(listener: ISettingsChangeListener) {
        stateManager.removeSettingsChangeListener(listener)
    }

    fun notifyIfStateChanged(oldState: CodeSceneGlobalSettings) {
        stateManager.notifyIfStateChanged(oldState)
    }

    companion object {
        fun getInstance(): CodeSceneGlobalSettingsStore {
            return ApplicationManager.getApplication().getService(CodeSceneGlobalSettingsStore::class.java)
        }
    }
}
