package com.codescene.jetbrains.config.global

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "CodeSceneGlobalSettings",
    storages = [Storage("codescene-settings.xml")]
)
class CodeSceneGlobalSettingsStore : PersistentStateComponent<CodeSceneGlobalSettings> {
    private var extensionSettingsState: CodeSceneGlobalSettings = CodeSceneGlobalSettings()

    override fun getState(): CodeSceneGlobalSettings {
        return extensionSettingsState
    }

    override fun loadState(state: CodeSceneGlobalSettings) {
        extensionSettingsState = state
    }

    fun updateTermsAndConditionsAcceptance(hasAccepted: Boolean) {
        extensionSettingsState.termsAndConditionsAccepted = hasAccepted

        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().saveSettings()
        }
    }

    companion object {
        fun getInstance(): CodeSceneGlobalSettingsStore {
            return ApplicationManager.getApplication().getService(CodeSceneGlobalSettingsStore::class.java)
        }
    }
}