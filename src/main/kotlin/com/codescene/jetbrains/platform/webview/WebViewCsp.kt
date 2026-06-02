package com.codescene.jetbrains.platform.webview

import java.security.MessageDigest
import java.util.Base64

internal object WebViewCsp {
    const val IDE_CONTEXT_ELEMENT_ID = "codescene-ide-context"
    const val THEME_STYLE_ELEMENT_ID = "{STYLE_ELEMENT_ID}"

    fun buildContentSecurityPolicy(
        bootstrapScript: String,
        moduleScript: String,
    ): String {
        val bootstrapHash = sha256Base64(bootstrapScript)
        val moduleHash = sha256Base64(moduleScript)
        return listOf(
            "default-src 'none'",
            "script-src 'self' 'sha256-$bootstrapHash' 'sha256-$moduleHash'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https://*.codescene.io https://*.codescene.com",
            "font-src 'self'",
            "connect-src 'self' https://*.codescene.io https://*.codescene.com",
            "frame-ancestors 'none'",
            "base-uri 'none'",
        ).joinToString("; ")
    }

    private fun sha256Base64(source: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }
}
