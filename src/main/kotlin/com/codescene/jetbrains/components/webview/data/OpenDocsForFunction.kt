package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.Serializable

@Serializable
data class OpenDocsForFunction(
    val docType: String,
    val fileName: String,
    val fn: Fn? = null
)