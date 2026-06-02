package com.codescene.jetbrains.platform.webview

import java.util.Collections
import java.util.UUID
import java.util.WeakHashMap
import org.cef.browser.CefBrowser

internal object CwfWebViewContentRegistry {
    private const val URL_PREFIX = "file:///jbcefbrowser/codescene-cwf/"

    private val documentsByBrowser = WeakHashMap<CefBrowser, MutableMap<String, CwfWebViewDocument>>()

    val requestHandler = CwfWebViewRequestHandler()

    fun register(
        browser: CefBrowser,
        document: CwfWebViewDocument,
    ): String {
        val url = URL_PREFIX + UUID.randomUUID()
        documentsFor(browser)[normalizeUrl(url)] = document
        return url
    }

    fun find(
        browser: CefBrowser,
        url: String,
    ): CwfWebViewDocument? = documentsByBrowser[browser]?.get(normalizeUrl(url))

    private fun documentsFor(browser: CefBrowser): MutableMap<String, CwfWebViewDocument> =
        documentsByBrowser.getOrPut(browser) { Collections.synchronizedMap(mutableMapOf()) }

    private fun normalizeUrl(url: String): String = url.replace(Regex("/$"), "")
}
