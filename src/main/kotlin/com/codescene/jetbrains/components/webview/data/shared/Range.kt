package com.codescene.jetbrains.components.webview.data.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Range(
    @SerialName("end-line") val endLine: Int,
    @SerialName("end-column") val endColumn: Int,
    @SerialName("start-line") val startLine: Int,
    @SerialName("start-column") val startColumn: Int,
)