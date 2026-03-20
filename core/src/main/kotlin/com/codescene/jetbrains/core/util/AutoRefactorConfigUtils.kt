package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig

fun toAutoRefactorConfig(settings: CodeSceneGlobalSettings): AutoRefactorConfig =
    AutoRefactorConfig(
        activated = settings.aceAcknowledged,
        visible = RuntimeFlags.aceFeature && settings.enableAutoRefactor,
        disabled = settings.aceAuthToken.trim().isEmpty(),
    )
