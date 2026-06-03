package com.codescene.jetbrains.core.git

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val configJson = Json { ignoreUnknownKeys = true }

fun parseBaselineBranchFromConfig(jsonText: String?): String? {
    if (jsonText.isNullOrBlank()) return null
    return runCatching {
        configJson
            .parseToJsonElement(jsonText.trim())
            .jsonObject["baseline_branch"]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }.getOrNull()
}
