package com.codescene.jetbrains.core.telemetry

private const val MAX_STACK_FRAMES = 25
private const val CODESCENE_STACK_PREFIX = "com.codescene."
private val TOKEN_PATTERN = Regex("[A-Za-z0-9_-]{20,}")

fun isOriginatingFromCodescene(throwable: Throwable): Boolean {
    val frame = throwable.stackTrace.firstOrNull() ?: return false
    return frame.className.startsWith(CODESCENE_STACK_PREFIX)
}

fun buildUnhandledErrorPayload(
    throwable: Throwable,
    pathPrefixesToRedact: List<String> = emptyList(),
    extraData: Map<String, Any> = emptyMap(),
): Map<String, Any> {
    val sanitizedPrefixes = pathPrefixesToRedact.filter { it.isNotBlank() }.distinct()
    return buildMap {
        put("name", throwable::class.java.name)
        put("message", redactSensitiveText(throwable.message ?: "", sanitizedPrefixes))
        put("stack", redactSensitiveText(truncateStackTrace(throwable), sanitizedPrefixes))
        if (extraData.isNotEmpty()) put("extraData", extraData)
    }
}

fun redactSensitiveText(
    text: String,
    pathPrefixesToRedact: List<String>,
): String {
    var result = text
    pathPrefixesToRedact
        .sortedByDescending { it.length }
        .forEach { prefix -> result = result.replace(prefix, "<redacted>") }
    val userHome = System.getProperty("user.home")
    if (!userHome.isNullOrEmpty()) {
        result = result.replace(userHome, "<redacted>")
    }
    return TOKEN_PATTERN.replace(result) { "<redacted>" }
}

fun truncateStackTrace(throwable: Throwable): String {
    val lines = throwable.stackTraceToString().lines()
    if (lines.size <= MAX_STACK_FRAMES) return lines.joinToString("\n")
    val omitted = lines.size - MAX_STACK_FRAMES
    return lines.take(MAX_STACK_FRAMES).joinToString("\n") + "\n\t... $omitted more"
}
