package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig

fun toAutoRefactorConfig(settings: CodeSceneGlobalSettings): AutoRefactorConfig =
    AutoRefactorConfig(
        activated = settings.aceAcknowledged,
        visible = settings.enableAutoRefactor,
        disabled = settings.aceAuthToken.trim().isEmpty(),
    )
