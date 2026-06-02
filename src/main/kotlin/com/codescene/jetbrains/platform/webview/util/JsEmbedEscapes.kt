package com.codescene.jetbrains.platform.webview.util

import kotlinx.serialization.json.Json

object JsEmbedEscapes {
    private val json = Json

    fun toJsStringLiteral(value: String): String = json.encodeToString(value)

    fun escapeJsonForHtmlScript(json: String): String =
        json
            .replace("</", "<\\/")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
}
