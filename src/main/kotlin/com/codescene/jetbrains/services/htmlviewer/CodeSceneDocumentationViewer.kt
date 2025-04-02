package com.codescene.jetbrains.services.htmlviewer

import com.codescene.data.review.CodeSmell
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.codehealth.HtmlContentBuilder
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.ISSUES_PATH
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile

data class FunctionLocation(
    val fileName: String,
    val codeSmell: CodeSmell
)

enum class DocsSourceType(val value: String) {
    NONE("none"),
    CODE_VISION("codelens (review)"),
    INTENTION_ACTION("diagnostic-item"),
    CODE_HEALTH_DETAILS("code-health-details")
}

data class DocumentationParams(
    val file: VirtualFile?,
    val codeSmell: CodeSmell,
    val docsSourceType: DocsSourceType
)

@Service(Service.Level.PROJECT)
class CodeSceneDocumentationViewer(private val project: Project) : HtmlViewer<DocumentationParams>(project) {
    var functionLocation: FunctionLocation? = null
        private set

    companion object {
        fun getInstance(project: Project) = project.service<CodeSceneDocumentationViewer>()
    }

    override fun prepareFile(params: DocumentationParams): LightVirtualFile {
        val (file, codeSmell) = params
        file?.let { functionLocation = FunctionLocation(file.path, codeSmell) }

        val fileName = codeSmell.category + ".md"
        val contentParams = TransformMarkdownParams(getContent(codeSmell), codeSmell.category)

        val fileContent = HtmlContentBuilder()
            .title(codeSmell.category)
            .usingStyleSheet(Constants.STYLE_BASE_PATH + "code-smell.css")
            .functionLocation(file?.name ?: "", codeSmell.highlightRange.startLine)
            .content(contentParams)
            .build()

        return createTempFile(CreateTempFileParams(fileName, fileContent, project))
    }

    override fun sendTelemetry(params: DocumentationParams) {
        if (params.docsSourceType != DocsSourceType.NONE)
            TelemetryService.getInstance().logUsage(
                TelemetryEvents.OPEN_DOCS_PANEL,
                mutableMapOf<String, Any>(
                    Pair("source", params.docsSourceType),
                    Pair("category", params.codeSmell.category)
                )
            )
    }

    private fun getContent(codeSmell: CodeSmell): String {
        val filePath = "${ISSUES_PATH}${categoryToFileName(codeSmell.category)}.md"

        return this@CodeSceneDocumentationViewer.javaClass.classLoader
            .getResourceAsStream(filePath)
            ?.bufferedReader()
            ?.readText()
            ?: ""
    }
}