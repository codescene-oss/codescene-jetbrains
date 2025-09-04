package com.codescene.jetbrains.components.webview.data

import kotlinx.serialization.Serializable

enum class View(val value: String) {
    HOME("home"),
    ACE("ace")
}

@Serializable
data class CWFMessage(
    val messageType: String,
    val payload: String? = null
)

@Serializable
data class CWFData<T>(
    val data: T
)

@Serializable
data class FileDeltaData(
    val files: List<String>,
    val commitBaseline: String, // "default" | "HEAD" | "branchCreate"
    val showOnboarding: Boolean,
    val featureFlags: List<String>,
    // add more...
)