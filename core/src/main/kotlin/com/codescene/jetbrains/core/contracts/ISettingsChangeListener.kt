package com.codescene.jetbrains.core.contracts

import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings

fun interface ISettingsChangeListener {
    fun onSettingsChanged(
        oldState: CodeSceneGlobalSettings,
        newState: CodeSceneGlobalSettings,
    )
}
