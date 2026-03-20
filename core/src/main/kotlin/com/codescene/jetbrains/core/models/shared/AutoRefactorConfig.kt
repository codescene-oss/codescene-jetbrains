package com.codescene.jetbrains.core.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class AutoRefactorConfig(
    // Indicate whether the user has approved the use of ACE yet
    val activated: Boolean = false,
    // Show any type of ACE functionality
    val visible: Boolean = false,
    // Disable the visible button if visible: true
    val disabled: Boolean = true,
)
