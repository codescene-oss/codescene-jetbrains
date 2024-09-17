package com.codescene.jetbrains.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import org.jetbrains.annotations.NonNls

//WORK IN PROGRESS - TODO

@State(
    name = "com.codescene.jetbrains.services",
    storages = [Storage("SdkSettingsPlugin.xml")]
)
class ExtensionSettings : PersistentStateComponent<ExtensionSettings.State> {
    data class State(
        @NonNls var cloudUrl: String = "",
        @NonNls var cloudApiUrl: String = "",
        var enableCodeLenses: Boolean = true,
        var enableAutoRefactor: Boolean = false,
        var previewCodeHealthGate: Boolean = false,
        var excludeGitignoreFiles: Boolean = false
    )

    private var extensionSettingsState: State = State()

    override fun getState(): State {
        return extensionSettingsState
    }

    override fun loadState(state: State) {
        extensionSettingsState = state
    }

    companion object {
        fun getInstance(): ExtensionSettings {
            return ApplicationManager.getApplication().getService(ExtensionSettings::class.java)
        }
    }
}