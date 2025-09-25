package com.codescene.jetbrains.components.webview.data.message

import com.codescene.jetbrains.components.webview.data.shared.Fn
import kotlinx.serialization.Serializable

@Serializable
data class RequestAndPresentRefactoring(
    val fileName: String,
    val fn: Fn
)