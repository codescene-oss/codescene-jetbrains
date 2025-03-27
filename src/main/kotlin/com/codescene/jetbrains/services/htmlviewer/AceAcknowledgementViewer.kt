package com.codescene.jetbrains.services.htmlviewer

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.ACE_ACKNOWLEDGEMENT
import com.intellij.ide.ui.LafManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

data class AceAcknowledgementViewerParams(
    val editor: Editor?,
    val function: FnToRefactor
)

@Service(Service.Level.PROJECT)
class AceAcknowledgementViewer(private val project: Project) : HtmlViewer<AceAcknowledgementViewerParams>(project) {
    var functionToRefactor: FnToRefactor? = null

    companion object {
        fun getInstance(project: Project) = project.service<AceAcknowledgementViewer>()
    }

    override fun prepareFile(params: AceAcknowledgementViewerParams): LightVirtualFile {
        val (editor, function) = params
        functionToRefactor = function

        TelemetryService.getInstance().logUsage(TelemetryEvents.ACE_INFO_PRESENTED)

        val classLoader = this@AceAcknowledgementViewer.javaClass.classLoader

        val markdown = classLoader.getResourceAsStream("ace-info.md")?.bufferedReader()?.readText() ?: ""
        val transformParams = TransformMarkdownParams(markdown, "codeSmellName", true)
        val markdownContent = transformMarkdownToHtml(transformParams, true)

        val headingParams = HeadingParams(
            editor = editor,
            content = markdown,
            classLoader = classLoader,
            standaloneDocumentation = true
        )
        val header = prepareHeader(headingParams)

        return createTempFile("$ACE_ACKNOWLEDGEMENT.md", "$header$markdownContent", project)
    }

    override fun lookAndFeelChanged(p0: LafManager) {
        TODO("Not yet implemented")
    }
}