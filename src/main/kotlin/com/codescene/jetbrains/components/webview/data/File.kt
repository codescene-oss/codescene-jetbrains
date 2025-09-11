package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.Serializable

@Serializable
data class FileMetaType(
    val fn: Fn? = null,
    val fileName: String
)