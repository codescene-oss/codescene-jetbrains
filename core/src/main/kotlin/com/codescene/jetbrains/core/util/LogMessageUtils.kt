package com.codescene.jetbrains.core.util

fun formatLogMessage(
    message: String,
    service: String?,
): String {
    val servicePart = if (!service.isNullOrEmpty()) " [$service]" else ""
    return "${Constants.CODESCENE}$servicePart - $message"
}
