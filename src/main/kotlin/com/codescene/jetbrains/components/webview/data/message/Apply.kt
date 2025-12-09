package com.codescene.jetbrains.components.webview.data.message

import com.codescene.jetbrains.components.webview.data.shared.Fn
import com.codescene.jetbrains.components.webview.data.shared.RangeCamelCase
import com.codescene.jetbrains.components.webview.data.shared.TelemetrySource
import kotlinx.serialization.Serializable

@Serializable
data class Apply(
    val fn: Fn,
    val code: String,
    val filePath: String,
    val range: RangeCamelCase,
    val source: TelemetrySource
)