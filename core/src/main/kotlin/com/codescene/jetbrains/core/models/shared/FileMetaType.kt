package com.codescene.jetbrains.core.models.shared

import kotlinx.serialization.Serializable

@Serializable
data class FileMetaType(
    val fn: Fn? = null,
    val fileName: String,
)
