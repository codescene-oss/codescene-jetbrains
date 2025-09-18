package com.codescene.jetbrains.components.webview.data.shared

import kotlinx.serialization.Serializable

@Serializable
data class AutoRefactorConfig(
    val visible: Boolean = false,
    val disabled: Boolean = true,
    val activated: Boolean = false
)