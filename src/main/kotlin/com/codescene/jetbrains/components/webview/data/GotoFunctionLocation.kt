package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.Serializable

@Serializable
data class GotoFunctionLocation(
    val fileName: String?,
    val fn: Fn? = null
)

@Serializable
data class Fn(
    val name: String?,
    val range: RangeCamelCase?
)

@Serializable
data class RangeCamelCase(
    val endLine: Int,
    val endColumn: Int,
    val startLine: Int,
    val startColumn: Int,
)
