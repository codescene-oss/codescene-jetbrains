package com.codescene.jetbrains.components.webview.data.shared

import kotlinx.serialization.Serializable

@Serializable
data class AutoRefactorConfig(
    val visible: Boolean = false, // Show any type of ACE functionality
    val disabled: Boolean = true, // Disable the visible button if visible: true
    val activated: Boolean = false // Indicate that the user has not approved the use of ACE yet
)