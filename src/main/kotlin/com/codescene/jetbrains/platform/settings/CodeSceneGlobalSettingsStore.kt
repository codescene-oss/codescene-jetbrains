package com.codescene.jetbrains.platform.settings

import com.codescene.jetbrains.core.contracts.ISettingsChangeListener
import com.codescene.jetbrains.core.contracts.ISettingsProvider
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.settings.SettingsStateManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "CodeSceneGlobalSettings",
    storages = [Storage("codescene-settings.xml")],
)
@Service(Service.Level.APP)
class CodeSceneGlobalSettingsStore : PersistentStateComponent<CodeSceneGlobalSettingsPersisted>, ISettingsProvider {
    private val stateManager = SettingsStateManager()

    override fun getState(): CodeSceneGlobalSettingsPersisted {
        val persisted = stateManager.getState().toPersisted()
        persisted.aceTokenConfigured = AceAuthTokenStore.hasToken()
        persisted.aceAuthToken = ""
        return persisted
    }

    override fun loadState(state: CodeSceneGlobalSettingsPersisted) {
        val legacyToken = state.aceAuthToken.takeIf { it.isNotBlank() }
        if (legacyToken != null) {
            AceAuthTokenStore.setToken(legacyToken)
        }
        val tokenConfigured = state.aceTokenConfigured || AceAuthTokenStore.hasToken()
        stateManager.loadState(state.toCore().copy(aceTokenConfigured = tokenConfigured))
    }

    override fun currentState(): CodeSceneGlobalSettings = stateManager.currentState()

    override fun getAceAuthToken(): String = AceAuthTokenStore.getToken()

    override fun setAceAuthToken(token: String) {
        AceAuthTokenStore.setToken(token)
        stateManager.updateAceTokenConfigured(AceAuthTokenStore.hasToken())
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().saveSettings()
        }
    }

    override fun updateTelemetryConsent(hasAccepted: Boolean) {
        stateManager.updateTelemetryConsent(hasAccepted)
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().saveSettings()
        }
    }

    override fun updateTelemetryNoticeShown(shown: Boolean) {
        stateManager.updateTelemetryNoticeShown(shown)
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

    fun notifyIfStateChanged(
        oldState: CodeSceneGlobalSettings,
        previousAceAuthToken: String? = null,
    ) {
        val tokenChanged = previousAceAuthToken != null && previousAceAuthToken != getAceAuthToken()
        stateManager.notifyIfStateChanged(oldState, tokenChanged)
    }

    companion object {
        fun getInstance(): CodeSceneGlobalSettingsStore {
            return ApplicationManager.getApplication().getService(CodeSceneGlobalSettingsStore::class.java)
        }
    }
}
