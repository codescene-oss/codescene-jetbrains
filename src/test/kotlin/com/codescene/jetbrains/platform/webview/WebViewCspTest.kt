package com.codescene.jetbrains.platform.webview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebViewCspTest {
    @Test
    fun buildContentSecurityPolicyOmitsPermissiveScriptAndNetworkSources() {
        val csp = WebViewCsp.buildContentSecurityPolicy("bootstrap();", "import x;")

        val scriptSrc = csp.substringAfter("script-src ").substringBefore(";")
        assertTrue(scriptSrc.startsWith("'self'"))
        assertTrue(scriptSrc.contains("'sha256-"))
        assertFalse(scriptSrc.contains("unsafe-inline"))
        assertFalse(scriptSrc.contains("unsafe-eval"))
        assertFalse(Regex("""\shttps://\*(\s|;|$)""").containsMatchIn(csp))
        assertTrue(csp.contains("connect-src 'self' https://*.codescene.io https://*.codescene.com"))
        assertTrue(csp.contains("frame-ancestors 'none'"))
        assertTrue(csp.contains("base-uri 'none'"))
    }

    @Test
    fun buildContentSecurityPolicyIncludesFrameAncestorsForResponseHeader() {
        val csp = WebViewCsp.buildContentSecurityPolicy("bootstrap();", "import x;")
        assertTrue(csp.contains("frame-ancestors 'none'"))
    }
}
