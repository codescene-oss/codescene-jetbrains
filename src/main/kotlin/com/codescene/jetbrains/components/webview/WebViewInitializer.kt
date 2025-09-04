package com.codescene.jetbrains.components.webview

import com.codescene.jetbrains.components.webview.data.CwfData
import com.codescene.jetbrains.components.webview.data.EditorMessages
import com.codescene.jetbrains.components.webview.data.HomeData
import com.codescene.jetbrains.components.webview.data.View
import com.codescene.jetbrains.util.Log
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Service(Service.Level.PROJECT)
class WebViewInitializer {
    companion object {
        fun getInstance(project: Project): WebViewInitializer = project.service<WebViewInitializer>()
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
    fun getInitialScript(view: String): String { //TODO: ADD data: WebViewData
        val html = getFileContent("build/index.html")
        val css = getFileContent("build/assets/index.css")
        val js = getFileContent("build/assets/index.js")

        val targetInitialData = "{{initialDataContext}}"
        val targetJs = "{{jsScript}}"
        val targetCss = "{{css}}"

        val initialData = """
            <script type="module">
              ${getLinkClickHandler()}
              function setContext() {
                window.ideContext = ${getInitialContext(view, isPro = false, isDevMode = true)}
              }
              setContext();
            </script>
        """.trimIndent()

        var modifiedHtml = html
        modifiedHtml = modifiedHtml.replace(targetCss, "<style>$css</style>")
        modifiedHtml = modifiedHtml.replace(targetJs, """<script type="module">$js</script>""")
        modifiedHtml = modifiedHtml.replace(targetInitialData, initialData)

        return modifiedHtml
    }

    private fun getInitialContext(view: String, isPro: Boolean, isDevMode: Boolean): String {
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
        isDevMode: Boolean,
        featureFlags: List<String> = emptyList()
    ): CwfData<T> = CwfData(
        view = view,
        pro = isPro,
        devmode = isDevMode,
        featureFlags = featureFlags,
        data = data
    )

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