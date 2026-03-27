package com.codescene.jetbrains.core.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class AceStatusType(
    val status: String,
    val hasToken: Boolean,
)
