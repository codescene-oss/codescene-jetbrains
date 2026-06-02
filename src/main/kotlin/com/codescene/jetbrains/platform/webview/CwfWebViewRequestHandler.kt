package com.codescene.jetbrains.platform.webview

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest

internal class CwfWebViewRequestHandler : CefRequestHandlerAdapter() {
    override fun getResourceRequestHandler(
        browser: CefBrowser,
        frame: CefFrame?,
        request: CefRequest,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?,
    ): CefResourceRequestHandler? {
        if (CwfWebViewContentRegistry.find(browser, request.url) == null) {
            return null
        }
        return object : CefResourceRequestHandlerAdapter() {
            override fun getResourceHandler(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?,
            ): CefResourceHandler? {
                val currentBrowser = browser ?: return null
                val currentRequest = request ?: return null
                val document = CwfWebViewContentRegistry.find(currentBrowser, currentRequest.url) ?: return null
                return CwfHtmlResourceHandler(document.html, document.contentSecurityPolicy)
            }
        }
    }
}
