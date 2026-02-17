package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.codehealth.DocumentationHtmlContentBuilder
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.codescene.jetbrains.util.Constants.DOCUMENTATION_BASE_PATH
import com.codescene.jetbrains.util.Constants.GENERAL_CODE_HEALTH
import com.codescene.jetbrains.util.Constants.ISSUES_PATH
import com.codescene.jetbrains.util.CreateTempFileParams
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.TransformMarkdownParams
import com.codescene.jetbrains.util.categoryToFileName
import com.codescene.jetbrains.util.createTempFile
import com.codescene.jetbrains.util.generalDocs
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

enum class DocsEntryPoint(val value: String) {
    ACTION("action"),
    CODE_VISION("codelens (review)"),
    INTENTION_ACTION("diagnostic-item"),
    CODE_HEALTH_DETAILS("code-health-details")
}

data class DocumentationParams(
    val heading: String,
    val fileName: String = "",
    val filePath: String = "",
    val focusLine: Int? = null,
    val docsEntryPoint: DocsEntryPoint? = null,
)

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
@Service(Service.Level.PROJECT)
class CodeSceneDocumentationViewer(private val project: Project) : HtmlViewer<DocumentationParams>(project) {
    companion object {
        fun getInstance(project: Project) = project.service<CodeSceneDocumentationViewer>()
    }

    override fun prepareFile(params: DocumentationParams): LightVirtualFile {
        val (heading, fileName, filePath, focusLine) = params
        val isGeneralDocumentation = generalDocs.contains(heading)

        val builder = DocumentationHtmlContentBuilder()
        val contentParams = TransformMarkdownParams(getContent(heading), heading, isGeneralDocumentation)

        val scriptTag = """
            <script id="function-data" type="application/json">
              {
                 "fileName": "$filePath",
                 "focusLine": $focusLine
              }
            </script>
        """.trimIndent()

        if (!isGeneralDocumentation) builder
            .title(heading)
            .functionLocation(fileName, focusLine ?: 1)
            .withWebViewData(scriptTag)
        else builder.title(heading, Constants.LOGO_PATH)

        val fileContent = builder
            .usingStyleSheet(Constants.STYLE_BASE_PATH + "code-smell.css")
            .content(contentParams)
            .build()

        return createTempFile(CreateTempFileParams("$heading.md", fileContent, project))
    }

    override fun sendTelemetry(params: DocumentationParams) {
        params.docsEntryPoint?.let {
            TelemetryService.getInstance().logUsage(
                TelemetryEvents.OPEN_DOCS_PANEL,
                mutableMapOf<String, Any>(
                    Pair("source", params.docsEntryPoint),
                    Pair("category", params.heading)
                )
            )
        }
    }

    private fun getContent(heading: String): String {
        /**
         * TODO: revert monitor to have same logic as GENERAL_CODE_HEALTH when VSCode has new monitor
         *  baseline and new docs version is released,
         *  then, delete local code-health-monitor doc file.
         */
        val prefix = when (heading) {
            CODE_HEALTH_MONITOR -> ""
            GENERAL_CODE_HEALTH -> DOCUMENTATION_BASE_PATH
            else -> ISSUES_PATH
        }
        val filePath = "${prefix}${categoryToFileName(heading)}.md"

        return this@CodeSceneDocumentationViewer.javaClass.classLoader
            .getResourceAsStream(filePath)
            ?.bufferedReader()
            ?.readText()
            ?: ""
    }
}