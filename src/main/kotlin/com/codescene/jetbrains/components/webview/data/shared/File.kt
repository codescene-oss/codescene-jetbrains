package com.codescene.jetbrains.components.webview.data.shared

import kotlinx.serialization.Serializable

@Serializable
data class FileMetaType(
    val fn: Fn? = null,
    val fileName: String
)