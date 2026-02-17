package com.codescene.jetbrains.components.webview.data.shared

import kotlinx.serialization.Serializable

@Serializable
data class Fn(
    val name: String?,
    val range: RangeCamelCase?,
)
