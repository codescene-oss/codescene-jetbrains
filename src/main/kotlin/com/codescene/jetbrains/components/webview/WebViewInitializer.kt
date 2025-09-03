package com.codescene.jetbrains.components.webview

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

const val IDE_TYPE = "JetBrains"

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

        var modifiedHtml = html
        modifiedHtml = modifiedHtml.replace(targetCss, "<style>$css</style>")
        modifiedHtml = modifiedHtml.replace(targetJs, """<script type="module">$js</script>""")

        val initialData = """
            <script type="module">
              function setContext() {
                window.ideContext = {
                  "ideType": "$IDE_TYPE",
                  "view": "$view",
                  "pro": false, 
                  "devmode": true,
                  "data": {
                    "fileDeltaData": [],
                    "jobs": []
                  } 
                }
              }
              setContext();
            </script>
        """.trimIndent()

        modifiedHtml = modifiedHtml.replace(targetInitialData, initialData)

        return modifiedHtml
    }

    private fun getFileContent(resource: String) =
        this@WebViewInitializer.javaClass.classLoader
            .getResourceAsStream(resource)
            ?.bufferedReader()
            ?.readText()
            ?: ""
}