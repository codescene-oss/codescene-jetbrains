package com.codescene.jetbrains.components.webview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefMessageRouterHandlerAdapter

@Service(Service.Level.PROJECT)
class WebViewMessageHandler(private val project: Project): CefMessageRouterHandlerAdapter() {

    companion object {
        fun getInstance(project: Project): WebViewMessageHandler = project.service<WebViewMessageHandler>()
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
    // @codescene(disable:"Excess Number of Function Arguments")
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

    private fun getFiles(
    ): List<String> = listOf(
        "/Users/scopra/Documents/Code/codescene-jetbrains/src/main/kotlin/com/codescene/jetbrains/components/window/CodeSceneToolWindowFactory.kt",
        "/Users/scopra/Documents/Code/codescene-jetbrains/src/main/kotlin/com/codescene/jetbrains/components/settings/tab/AboutTab.kt",
        "/Users/scopra/Documents/Code/codescene-jetbrains/build.gradle.kts"
    )

    override fun onQueryCanceled(browser: CefBrowser?, frame: CefFrame?, queryId: Long) {
        // TODO...
    }
}