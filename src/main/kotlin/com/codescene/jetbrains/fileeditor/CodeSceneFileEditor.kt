package com.codescene.jetbrains.fileeditor

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.CodeNavigationService
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.AceAcknowledgementViewer
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.ACE_ACKNOWLEDGEMENT
import com.codescene.jetbrains.util.Constants.CODESCENE
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.json.JSONObject
import java.awt.Desktop
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.net.URI
import javax.swing.JComponent

class CodeSceneFileEditor(val project: Project, private val file: VirtualFile) : FileEditor {

    companion object {
        private const val JAVASCRIPT_SEND_MESSAGE = """
            window.sendMessage = function(data) {
                %s
            };
        """
        private const val FUNCTION_LOCATION = """
            document.getElementById('function-location').addEventListener('click', function() {
                 const scriptTag = document.getElementById('function-data');
                 if (scriptTag) {
                   const functionData = JSON.parse(scriptTag.textContent);
                   window.sendMessage(JSON.stringify({
                    action: 'goto-function-location',
                    focusLine: functionData.focusLine,
                    fileName: functionData.fileName,
                   }));
                 }
            });
            """
        private const val ACE_BUTTON = """
            document.getElementById('ace-button').addEventListener('click', function() {
                 window.sendMessage(JSON.stringify({
                    action: 'show-me-ace'
                 }));
            });
            """
        private val eventListeners = listOf(
            FUNCTION_LOCATION,
            ACE_BUTTON
        )
    }

    private val jcefBrowser: JBCefBrowser = JBCefBrowser()
    private val jsQuery = JBCefJSQuery.create(jcefBrowser as JBCefBrowserBase)
    private val panel: JBPanel<*> = JBPanel<JBPanel<*>>()
    private val propertyChangeSupport = PropertyChangeSupport(this)
    private val userData = mutableMapOf<Key<*>, Any?>()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        panel.layout = java.awt.BorderLayout()
        panel.add(jcefBrowser.component, java.awt.BorderLayout.CENTER)
        val markdownContent = String(file.contentsToByteArray(), file.charset)
        jcefBrowser.loadHTML(markdownContent)
        jsQuery.addHandler { data ->
            handleAction(data)
            null
        }
        initializeJavascriptCallback()
        setupBrowserBehaviour()
    }

    private fun setupBrowserBehaviour() {
        jcefBrowser.jbCefClient.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?,
                frame: CefFrame?,
                request: CefRequest?,
                userGesture: Boolean,
                isRedirect: Boolean
            ): Boolean {
                val url = request?.url
                if (url != null) {
                    try {
                        val uri = URI(url)
                        when (uri.scheme) {
                            "http", "https" -> {
                                // Open http/https links in the system's default browser
                                Desktop.getDesktop().browse(uri)
                                return true
                            }
                        }
                    } catch (e: Exception) {
                        Log.debug("Unable to open uri in external browser: ${e.message}")
                    }
                }
                return false
            }
        }, jcefBrowser.cefBrowser)
    }

    private fun initializeJavascriptCallback() {
        jcefBrowser.jbCefClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
                    setupJavascript(browser)
                }
            },
            jcefBrowser.cefBrowser
        )
    }

    private fun setupJavascript(browser: CefBrowser?) {
        browser?.executeJavaScript(
            JAVASCRIPT_SEND_MESSAGE.format(jsQuery.inject("data")),
            null, 0
        )

        eventListeners.forEach { browser?.executeJavaScript(it, null, 0) }
    }

    private fun handleAction(data: String) {
        val json = JSONObject(data)
        val action = json.get("action") ?: ""

        when (action) {
            "goto-function-location" -> {
                val fileName = json.get("fileName") as String
                val focusLine = json.get("focusLine") as Int
                scope.launch {
                    CodeNavigationService
                        .getInstance(project)
                        .focusOnLine(fileName, focusLine)

                }
            }

            "show-me-ace" -> {
                Log.info("ACE acknowledged", "${this::javaClass::name} - ${project.name}")
                TelemetryService.getInstance().logUsage(TelemetryEvents.ACE_INFO_ACKNOWLEDGED)

                CodeSceneGlobalSettingsStore.getInstance().state.aceAcknowledged = true
                val function = AceAcknowledgementViewer.getInstance(project).functionToRefactor

                scope.launch(Dispatchers.Main) {
                    val editor = getSelectedTextEditor(project, "")

                    handleAceEntryPoint(RefactoringParams(project, editor, function, AceEntryPoint.ACE_ACKNOWLEDGEMENT))
                    closeWindow(ACE_ACKNOWLEDGEMENT, project)
                }
            }
        }
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = panel
    override fun getName(): String = "$CODESCENE Html Viewer"
    override fun setState(state: FileEditorState) { /* implementation not needed */
    }

    override fun isModified(): Boolean = false
    override fun isValid(): Boolean = file.isValid
    override fun getFile(): VirtualFile = file

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propertyChangeSupport.removePropertyChangeListener(listener)
    }

    // Override of FileEditor interface's method
    override fun <T : Any?> getUserData(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return userData[key] as T?
    }

    // Override of FileEditor interface's method
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        if (value == null) {
            userData.remove(key)
        } else {
            userData[key] = value
        }
    }

    override fun dispose() {
        jsQuery.dispose()
        jcefBrowser.jbCefClient.dispose()
        jcefBrowser.dispose()
        panel.removeAll()
        scope.cancel()
    }
}