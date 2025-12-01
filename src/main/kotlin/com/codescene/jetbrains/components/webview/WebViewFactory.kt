package com.codescene.jetbrains.components.webview

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.codescene.jetbrains.flag.RuntimeFlags
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefMessageRouter
import javax.swing.JLabel

object WebViewFactory {
    /**
     * Creates a generic JCEF WebView component for a given project, view type, and optional initial data.
     * This encapsulates:
     * - Browser initialization,
     * - Loading the HTML content,
     * - Message router setup,
     * - Fallback if JCEF is not supported.
     *
     * @param project The current project.
     * @param view The view type (HOME, ACE, DOCS, etc.)
     * @return Content containing the initialized WebView.
     */
    fun createWebViewComponent(
        project: Project,
        view: View,
        initialData: Any? = null
    ): Content {
        val contentFactory = ContentFactory.getInstance()
        val isDevMode = RuntimeFlags.isDevMode

        if (!JBCefApp.isSupported()) {
            return contentFactory.createContent(
                JBScrollPane().apply { add(JLabel(UiLabelsBundle.message("jcefNotSupported"))) },
                null,
                false
            )
        }

        val jcefBrowser = JBCefBrowser.createBuilder()
            .setEnableOpenDevToolsMenuItem(isDevMode)
            .build()

        val webViewInitializer = WebViewInitializer.getInstance(project)
        val html = webViewInitializer.getInitialScript(view, jcefBrowser, initialData)

        // Setup message router for IDE ↔ CWF communication
        val messageHandler = CwfMessageHandler.getInstance(project)
        val messageRouter = CefMessageRouter.create()
        messageRouter.addHandler(messageHandler, true)

        jcefBrowser.apply {
            jbCefClient.cefClient.addMessageRouter(messageRouter)
            loadHTML(html)
        }

        return contentFactory.createContent(jcefBrowser.component, null, false)
    }
}