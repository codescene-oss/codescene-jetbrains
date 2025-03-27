package com.codescene.jetbrains.services.htmlviewer

import com.codescene.data.review.CodeSmell
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.prepareMarkdownContent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
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
    val editor: Editor?,
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
        val (editor, codeSmell) = params

        val name = codeSmell.category + ".md"
        val classLoader = this@CodeSceneDocumentationViewer.javaClass.classLoader
        val content = prepareMarkdownContent(params, classLoader)
        val file = LightVirtualFile(name, content)

        editor?.let { functionLocation = FunctionLocation(it.virtualFile.path, codeSmell) }

        val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) } ?: return file
        file.isWritable = false

        WriteCommandAction.runWriteCommandAction(project) {
            psiFile.virtualFile.refresh(false, false)
        }

        return file
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
}