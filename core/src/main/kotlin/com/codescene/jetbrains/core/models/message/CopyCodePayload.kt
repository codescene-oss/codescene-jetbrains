package com.codescene.jetbrains.core.models.message

import kotlinx.serialization.Serializable

@Serializable
data class CopyCodePayload(
    val code: String? = null,
)
