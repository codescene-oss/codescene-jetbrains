package com.codescene.jetbrains.config.global

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "CodeSceneGlobalSettings",
    storages = [Storage("codescene-settings.xml")],
    externalStorageOnly = true
)
class CodeSceneGlobalSettingsStore : PersistentStateComponent<CodeSceneGlobalSettings> {
    private var extensionSettingsState: CodeSceneGlobalSettings = CodeSceneGlobalSettings()

    override fun getState(): CodeSceneGlobalSettings {
        return extensionSettingsState
    }

    override fun loadState(state: CodeSceneGlobalSettings) {
        extensionSettingsState = state
    }

    companion object {
        fun getInstance(): CodeSceneGlobalSettingsStore {
            return ApplicationManager.getApplication().getService(CodeSceneGlobalSettingsStore::class.java)
        }
    }
}