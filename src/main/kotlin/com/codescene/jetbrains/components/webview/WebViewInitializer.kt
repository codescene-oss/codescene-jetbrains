package com.codescene.jetbrains.components.webview

import com.codescene.jetbrains.components.webview.data.*
import com.codescene.jetbrains.components.webview.util.StyleHelper
import com.codescene.jetbrains.util.Log
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
class WebViewInitializer : LafManagerListener {
    private val browsers = mutableMapOf<String, JBCefBrowser>()

    companion object {
        fun getInstance(project: Project): WebViewInitializer = project.service<WebViewInitializer>()
    }

    init {
        val bus = ApplicationManager.getApplication().messageBus.connect()
        bus.subscribe(LafManagerListener.TOPIC, this)
    }

    private fun registerBrowser(id: String, browser: JBCefBrowser) {
        browsers[id] = browser
    }

    fun unregisterBrowser(id: String) {
        browsers.remove(id)
    }

    fun getBrowser(view: String): JBCefBrowser? = browsers[view]

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
    fun getInitialScript(view: String, browser: JBCefBrowser, initialData: Any? = null): String {
        registerBrowser(view, browser)

        val css = getFileContent("cs-cwf/assets/index.css")
        val js = getFileContent("cs-cwf/assets/index.js")

        return """
            <!DOCTYPE html>
            <html lang="en">
              <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <meta http-equiv="Content-Security-Policy" content="
                default-src 'none';
                script-src 'self' 'unsafe-inline' 'unsafe-eval' https://*;
                style-src 'self' 'unsafe-inline' https://*;
                img-src 'self' data: 'unsafe-inline' https://*;
                font-src 'self';
                connect-src https://*;
                ">
                <title>JetBrains React Webview</title>
                <style>$css</style>
                <script type="module">
                  ${getLinkClickHandler()}
                  function setContext() {
                    window.ideContext = ${getInitialContext(view, initialData)}
                    const css = `${StyleHelper.getInstance().generateCssVariablesFromTheme()}`;
                    const style = document.createElement('style');
                    style.id = '{STYLE_ELEMENT_ID}';
                    style.textContent = css;
                    document.head.appendChild(style);
                  }
                  setContext();
                </script>
              </head>
              <body>
                <div id="root"></div>
                <script type="module">$js</script>
              </body>
            </html>
        """.trimIndent()
    }

    /**
     * Callback invoked when the IDE look-and-feel (theme) changes.
     *
     * This method regenerates a set of CSS variables based on the current
     * IDE theme (via [StyleHelper.generateCssVariablesFromTheme]) and
     * injects them into the WebView (CWF) content.
     *
     * The CSS variables are applied by dynamically creating or updating
     * a `<style>` element with a fixed ID (`{STYLE_ELEMENT_ID}`) inside
     * the WebView's DOM. This ensures that the WebView's styling always
     * matches the active IDE theme.
     */
    override fun lookAndFeelChanged(p0: LafManager) {
        val css = StyleHelper.getInstance().generateCssVariablesFromTheme()

        val js = """
        (function() {
            let style = document.getElementById('{STYLE_ELEMENT_ID}');
            if (!style) {
                style = document.createElement('style');
                style.id = '{STYLE_ELEMENT_ID}';
                document.head.appendChild(style);
            }
            style.textContent = `$css`;
        })();
        """.trimIndent()

        browsers.values.forEach { it.cefBrowser.executeJavaScript(js, null, 0) }
    }

    /**
     * Builds the initial JSON context payload for a given WebView.
     *
     * The context contains serialized data objects that are passed to
     * the client-side WebView to initialize its initial state. The structure of
     * the payload depends on the requested [view].
     *
     * Currently supported views:
     * - [View.HOME]: Creates and serializes a [HomeData] instance wrapped in [CwfData].
     * - [View.DOCS]: Not yet implemented, returns an empty string.
     * - [View.ACE]: Not yet implemented, returns an empty string.
     * - Any other value: Logs a warning and returns an empty JSON object (`{}`).
     */
    private fun getInitialContext(view: String, initialData: Any? = null): String {
        val isDevMode = System.getProperty("cwfIsDevMode")?.toBoolean() ?: false
        val isPro = true // TODO: resolve with auth

        val json = Json {
            encodeDefaults = true
            prettyPrint = true
        }

        when (view) {
            View.HOME.value -> {
                val data = buildCwfData(HomeData(), view, isPro, isDevMode)
                val dataJson = json.encodeToString<CwfData<HomeData>>(data)

                return dataJson
            }

            View.DOCS.value -> {
                if (initialData !is DocsData) return "{}"

                val data = buildCwfData(initialData, view, isPro, isDevMode)
                val dataJson = json.encodeToString<CwfData<DocsData>>(data)

                return dataJson
            }

            View.ACE.value -> {
                return "" // Not implemented
            }

            else -> {
                Log.warn(
                    "Unknown view. Unable to get initial context for CWF.",
                    WebViewInitializer::class::simpleName.toString()
                )
                return "{}";
            }
        }
    }

    private fun <T> buildCwfData(
        data: T,
        view: String,
        isPro: Boolean,
        isDevMode: Boolean
    ): CwfData<T> = CwfData(
        view = view,
        pro = isPro,
        devmode = isDevMode,
        data = data
    )

    /**
     * Provides a JavaScript snippet that intercepts all link clicks inside the WebView.
     *
     * By default, clicking a link (`<a href="...">`) in a WebView would try to
     * navigate the embedded browser instance. Since this is undesirable, the script
     * overrides the default behavior and instead sends the link URL back to the
     * host application using `window.cefQuery`.
     *
     * The host can then handle the request and open the link in the user's external
     * browser.
     */
    private fun getLinkClickHandler() = """
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