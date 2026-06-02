package com.codescene.jetbrains.platform.webview

import com.codescene.jetbrains.core.flag.RuntimeFlags
import com.codescene.jetbrains.core.models.CwfData
import com.codescene.jetbrains.core.models.View
import com.codescene.jetbrains.core.models.message.EditorMessages
import com.codescene.jetbrains.core.models.view.AceAcknowledgeData
import com.codescene.jetbrains.core.models.view.AceData
import com.codescene.jetbrains.core.models.view.DocsData
import com.codescene.jetbrains.core.models.view.HomeData
import com.codescene.jetbrains.platform.webview.util.JsEmbedEscapes
import com.codescene.jetbrains.platform.webview.util.StyleHelper
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class WebViewInitializer(
    private val project: Project,
) : LafManagerListener {
    private val browsers = mutableMapOf<View, JBCefBrowser>()

    companion object {
        fun getInstance(project: Project): WebViewInitializer = project.service<WebViewInitializer>()
    }

    init {
        val bus = ApplicationManager.getApplication().messageBus.connect()
        bus.subscribe(LafManagerListener.TOPIC, this)
    }

    private fun registerBrowser(
        id: View,
        browser: JBCefBrowser,
    ) {
        CwfWebviewLifecycle.getInstance(project).resetForNewBrowser(id)
        browsers[id] = browser
    }

    fun unregisterBrowser(id: View) {
        browsers.remove(id)
        CwfWebviewLifecycle.getInstance(project).resetForNewBrowser(id)
    }

    fun getBrowser(view: View): JBCefBrowser? = browsers[view]

    fun loadInitialContent(
        view: View,
        browser: JBCefBrowser,
        initialData: Any? = null,
    ) {
        registerBrowser(view, browser)

        val css = getFileContent("cs-cwf/index.css")
        val js = getFileContent("cs-cwf/index.js")
        val bootstrap = buildBootstrapScript()
        val themeCss = StyleHelper.getInstance().generateCssVariablesFromTheme()
        val ideContextJson = getInitialContext(view, initialData)
        val contentSecurityPolicy = WebViewCsp.buildContentSecurityPolicy(bootstrap, js)
        val html = buildInitialHtml(css, themeCss, ideContextJson, bootstrap, js)
        val document = CwfWebViewDocument(html, contentSecurityPolicy)

        val loadUrl = CwfWebViewContentRegistry.register(browser.cefBrowser, document)
        browser.loadURL(loadUrl)
    }

    private fun buildInitialHtml(
        css: String,
        themeCss: String,
        ideContextJson: String,
        bootstrap: String,
        js: String,
    ): String {
        val escapedContext = JsEmbedEscapes.escapeJsonForHtmlScript(ideContextJson)
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("  <head>")
            appendLine("    <meta charset=\"UTF-8\" />")
            appendLine("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />")
            appendLine("    <title>JetBrains React Webview</title>")
            appendLine("    <style>$css</style>")
            appendLine("    <style id=\"${WebViewCsp.THEME_STYLE_ELEMENT_ID}\">$themeCss</style>")
            append("    <script type=\"application/json\" id=\"${WebViewCsp.IDE_CONTEXT_ELEMENT_ID}\">")
            append(escapedContext)
            appendLine("</script>")
            appendLine("  </head>")
            appendLine("  <body>")
            appendLine("    <div id=\"root\"></div>")
            appendLine("    <script>$bootstrap</script>")
            appendLine("    <script type=\"module\">$js</script>")
            appendLine("  </body>")
            appendLine("</html>")
        }
    }

    override fun lookAndFeelChanged(p0: LafManager) {
        val css = StyleHelper.getInstance().generateCssVariablesFromTheme()
        val cssLiteral = JsEmbedEscapes.toJsStringLiteral(css)

        val js =
            """
            (function() {
                let style = document.getElementById('${WebViewCsp.THEME_STYLE_ELEMENT_ID}');
                if (!style) {
                    style = document.createElement('style');
                    style.id = '${WebViewCsp.THEME_STYLE_ELEMENT_ID}';
                    document.head.appendChild(style);
                }
                style.textContent = $cssLiteral;
            })();
            """.trimIndent()

        browsers.values.forEach { it.cefBrowser.executeJavaScript(js, null, 0) }
    }

    private fun getInitialContext(
        view: View,
        initialData: Any? = null,
    ): String {
        val isPro = true // TODO: resolve with auth

        return when (view) {
            View.HOME -> encodeCwfDataOrEmpty<HomeData>(HomeData(), view, isPro)

            View.DOCS -> encodeCwfDataOrEmpty<DocsData>(initialData, view, isPro)

            View.ACE -> encodeCwfDataOrEmpty<AceData>(initialData, view, isPro)

            View.ACE_ACKNOWLEDGE -> encodeCwfDataOrEmpty<AceAcknowledgeData>(initialData, view, isPro)
        }
    }

    private inline fun <reified T : Any> encodeCwfDataOrEmpty(
        initialData: Any?,
        view: View,
        isPro: Boolean,
    ): String {
        val isDevMode = RuntimeFlags.isDevMode

        val json =
            Json {
                encodeDefaults = true
                prettyPrint = true
            }

        val typed = initialData as? T ?: return "{}"
        val data = buildCwfData(typed, view, isPro, isDevMode)

        return json.encodeToString<CwfData<T>>(data)
    }

    private fun <T> buildCwfData(
        data: T,
        view: View,
        isPro: Boolean,
        isDevMode: Boolean,
    ): CwfData<T> =
        CwfData(
            view = view.value,
            pro = isPro,
            devmode = isDevMode,
            data = data,
        )

    private fun buildBootstrapScript(): String =
        """
        (function() {
            const contextEl = document.getElementById("${WebViewCsp.IDE_CONTEXT_ELEMENT_ID}");
            if (contextEl) {
                window.ideContext = JSON.parse(contextEl.textContent);
            }
        })();
        ${getLinkClickHandler()}
        """.trimIndent()

    private fun getLinkClickHandler() =
        """
        document.addEventListener("click", (e) => {
            const link = e.target.closest("a");
            if (link && link.href) {
                e.preventDefault();
                window.cefQuery({
                    request: JSON.stringify({
                        messageType: "${EditorMessages.OPEN_LINK.value}",
                        payload: link.href
                    })
                });
            }
        });
        """.trimIndent()

    private fun getFileContent(resource: String) =
        this@WebViewInitializer.javaClass.classLoader.getResourceAsStream(resource)?.bufferedReader()?.readText() ?: ""
}
