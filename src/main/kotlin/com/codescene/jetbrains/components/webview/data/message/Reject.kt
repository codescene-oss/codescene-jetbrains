package com.codescene.jetbrains.components.webview.data.message

import com.codescene.jetbrains.components.webview.data.shared.TelemetrySource
import kotlinx.serialization.Serializable

@Serializable
data class Reject(
    val source: TelemetrySource,
)