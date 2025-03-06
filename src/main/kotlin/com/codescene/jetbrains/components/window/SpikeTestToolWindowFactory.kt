package com.codescene.jetbrains.components.window

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter
import javax.swing.JLabel

class SpikeTestToolWindowFactory : ToolWindowFactory {
    private lateinit var project: Project

    private val jcefBrowser: JBCefBrowser = JBCefBrowser.createBuilder().setEnableOpenDevToolsMenuItem(true).build()

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
        this.project = project
        val content: Content

        if (JBCefApp.isSupported()) {
            val html = prepareHTML()
            val messageRouter = CefMessageRouter.create()
            messageRouter.addHandler(getMessageRouter(), true)

            jcefBrowser.apply {
                jbCefClient.cefClient.addMessageRouter(messageRouter)
                loadHTML(html)
            }

            content = ContentFactory.getInstance()
                .createContent(JBScrollPane(jcefBrowser.component).apply {
                    verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                    horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                }, null, false)
        } else {
            content = ContentFactory.getInstance()
                .createContent(
                    JBScrollPane().apply { add(JLabel("JCEF is not supported. Render some placeholder here...")) },
                    null,
                    false
                )
        }

        toolWindow.contentManager.addContent(content)
    }

    /**
     * Prepares and modifies the HTML document by embedding the CSS and JavaScript directly into the HTML.
     * This eliminates the need to pull them from external resources at runtime.
     * While it is possible to pull the files from resources dynamically, for the sake of this demo,
     * it was chosen to embed the script and style in the HTML document.
     *
     * The function:
     * - Retrieves the content of the HTML, CSS, and JavaScript files.
     * - Replaces specific placeholders in the HTML with the embedded CSS and JavaScript code.
     *
     * @return The modified HTML document as a string.
     */
    private fun getFileContent(resource: String) =
        this@SpikeTestToolWindowFactory.javaClass.classLoader
            .getResourceAsStream(resource)
            ?.bufferedReader()
            ?.readText()
            ?: ""

    private fun prepareHTML(): String {
        val html = getFileContent("build/index.html")
        val css = getFileContent("build/assets/index.css")
        val js = getFileContent("build/assets/index.js")

        var modifiedHtml = html

        val targetCss = "<title>CS Spike</title>"
        modifiedHtml = modifiedHtml.replace(targetCss.toRegex(), "${targetCss}\n<style>${css}</style>")

        val targetJs = "<div id=\"root\"></div>"
        modifiedHtml = modifiedHtml.replace(targetJs, "${targetJs}\n<script>${js}</script>")

        modifiedHtml = modifiedHtml
            .replace("<script type=\"module\" crossorigin src=\"/assets/index.js\"></script>", "")
            .replace("<link rel=\"stylesheet\" crossorigin href=\"/assets/index.css\">", "")

        return modifiedHtml
    }

    /**
     * Returns a basic implementation of a `CefMessageRouterHandlerAdapter` for handling
     * messages sent from the *webview*. This serves as a base for native message handling in *both* directions.
     *
     * In this example:
     * - If the incoming message (request) is "init", the handler executes JavaScript in the browser
     *   to trace the event and send a "file-tree" `postMessage` back to the webview. This message includes
     *   an array of file paths that can be opened.
     * - If the request is "open-file", it logs the event and then opens a specific file in the IDE using
     *   the native approach.
     *
     * For sending messages from the native application (JetBrains IDE) to the webview, the JavaScript
     * `window.postMessage` is used. The `sendToJetBrains` function (on the webview side) triggers native events
     * to send data and invoke a callback for success or failure.
     *
     * Note: For a production-level implementation, consider implementing a `JSON` mapper to properly parse the
     *       incoming `JSON` data and extract the payload. This basic example uses simple string checks.
     *
     * @return A `CefMessageRouterHandlerAdapter` that processes incoming queries from the webview.
     */
    private fun getMessageRouter() = object : CefMessageRouterHandlerAdapter() {
        override fun onQuery(
            browser: CefBrowser?,
            frame: CefFrame?,
            queryId: Long,
            request: String?,
            persistent: Boolean,
            callback: CefQueryCallback?
        ): Boolean {
            if (request == null) return false
            val files = getFiles()
            val filesToOpen = files.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }

            if (request.contains("init")) {
                browser?.executeJavaScript(
                    """
                    console.log("JetBrains has received the 'init' message! Sending 3 files...", $filesToOpen);
                    window.postMessage({
                        messageType: "file-tree",
                        payload: $filesToOpen,
                    });
                """.trimIndent(),
                    null, 0
                )
            } else if (request.contains("open-file")) {
                val fileToOpen = files.random()
                browser?.executeJavaScript(
                    "console.log(\"JetBrains has received the open-file event: $fileToOpen.\")",
                    null,
                    0
                )
                ApplicationManager.getApplication().invokeLaterOnWriteThread {
                    LocalFileSystem
                        .getInstance()
                        .findFileByPath(fileToOpen)
                        ?.let { FileEditorManager.getInstance(project).openFile(it, true) }
                }
            }

            callback?.success("Message processed")

            return true
        }
    }

    //Dummy implementation:
    private fun getFiles(
    ): List<String> = listOf(
        "/Users/scopra/Documents/Code/codescene-jetbrains/src/main/kotlin/com/codescene/jetbrains/components/window/CodeSceneToolWindowFactory.kt",
        "/Users/scopra/Documents/Code/codescene-jetbrains/src/main/kotlin/com/codescene/jetbrains/components/settings/tab/AboutTab.kt",
        "/Users/scopra/Documents/Code/codescene-jetbrains/build.gradle.kts"
    )
}