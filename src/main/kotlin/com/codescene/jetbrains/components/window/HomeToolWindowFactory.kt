package com.codescene.jetbrains.components.window

import com.codescene.jetbrains.UiLabelsBundle
import com.codescene.jetbrains.components.webview.WebViewInitializer
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.components.webview.handler.CwfMessageHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefMessageRouter
import javax.swing.JLabel

internal class HomeToolWindowFactory : ToolWindowFactory {
    /**
     * Creates and sets up the content for the ToolWindow in the JetBrains IDE.
     * This method configures the JCEF browser to render a React app if JCEF is supported.
     *
     * In this example, it assumes the user has provided a **React build** in the resources folder,
     * which will be loaded into the JCEF browser. If JCEF is supported, a message router is set up
     * to handle communication between the native IDE and the webview (React app).
     *
     * The setup consists of the following:
     * - The HTML content is prepared using the `prepareHTML` method (which should return the React app HTML).
     * - A message router is created and added to the JCEF browser client.
     * - The React app is loaded into the JCEF browser.
     * - If JCEF is not supported, a placeholder message is shown instead.
     *
     * @param project The current project within the IDE.
     * @param toolWindow The tool window where the content will be displayed.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val content: Content
        val jcefBrowser = JBCefBrowser.createBuilder()
            .setEnableOpenDevToolsMenuItem(true)
            .build()

        if (JBCefApp.isSupported()) {
            val webViewInitializer = WebViewInitializer.getInstance(project);
            val html = webViewInitializer.getInitialScript(View.HOME.value, jcefBrowser)

            val messageHandler = CwfMessageHandler.getInstance(project)
            val messageRouter = CefMessageRouter.create()

            messageRouter.addHandler(messageHandler, true)

            jcefBrowser.apply {
                jbCefClient.cefClient.addMessageRouter(messageRouter)
                loadHTML(html)
            }

            content = ContentFactory.getInstance()
                .createContent(jcefBrowser.component, null, false)
        } else {
            content = ContentFactory.getInstance()
                .createContent(
                    JBScrollPane().apply { add(JLabel(UiLabelsBundle.message("jcefNotSupported"))) },
                    null,
                    false
                )
        }

        toolWindow.contentManager.addContent(content)
    }
}