package com.codescene.jetbrains.platform.webview

import com.intellij.openapi.diagnostic.logger
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse

internal class CwfHtmlResourceHandler(
    private val html: String,
    private val contentSecurityPolicy: String,
) : CefResourceHandlerAdapter() {
    private val inputStream: InputStream = ByteArrayInputStream(html.toByteArray(StandardCharsets.UTF_8))

    override fun processRequest(
        request: CefRequest,
        callback: CefCallback,
    ): Boolean {
        callback.Continue()
        return true
    }

    override fun getResponseHeaders(
        response: CefResponse,
        responseLength: IntRef,
        redirectUrl: StringRef,
    ) {
        response.mimeType = "text/html"
        response.status = 200
        response.setHeaderByName("Content-Security-Policy", contentSecurityPolicy, true)
    }

    override fun readResponse(
        dataOut: ByteArray,
        bytesToRead: Int,
        bytesRead: IntRef,
        callback: CefCallback,
    ): Boolean {
        try {
            val availableSize = inputStream.available()
            if (availableSize > 0) {
                val read = inputStream.read(dataOut, 0, minOf(bytesToRead, availableSize))
                bytesRead.set(read)
                return true
            }
        } catch (e: IOException) {
            LOG.error(e)
        }
        bytesRead.set(0)
        try {
            inputStream.close()
        } catch (e: IOException) {
            LOG.error(e)
        }
        return false
    }

    private companion object {
        private val LOG = logger<CwfHtmlResourceHandler>()
    }
}
