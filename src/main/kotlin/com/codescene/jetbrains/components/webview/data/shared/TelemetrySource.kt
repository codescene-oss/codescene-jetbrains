package com.codescene.jetbrains.components.webview.data.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TelemetrySource {

    @SerialName("ace-acknowledge")
    AceAcknowledge,

    @SerialName("ace")
    Ace,

    @SerialName("docs")
    Docs,

    @SerialName("home")
    Home,

    @SerialName("login")
    Login,
}