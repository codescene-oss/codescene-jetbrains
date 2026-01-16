package com.codescene.jetbrains.services.htmlviewer

import com.codescene.data.ace.FnToRefactor
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.codehealth.DocumentationHtmlContentBuilder
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.ACE_ACKNOWLEDGEMENT
import com.codescene.jetbrains.util.Constants.ACE_ACKNOWLEDGEMENT_FILE
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
@Service(Service.Level.PROJECT)
class AceAcknowledgementViewer(private val project: Project) : HtmlViewer<FnToRefactor>(project) {
    var functionToRefactor: FnToRefactor? = null
        private set

    companion object {
        fun getInstance(project: Project) = project.service<AceAcknowledgementViewer>()
    }

    override fun prepareFile(params: FnToRefactor): LightVirtualFile {
        functionToRefactor = params

        val markdown = getContent()
        val title = markdown.split("\n\n", limit = 2)[0]
        val transformParams = TransformMarkdownParams(originalContent = markdown, generalDocumentation = true)

        val fileContent = DocumentationHtmlContentBuilder()
            .title(title, Constants.LOGO_PATH)
            .usingStyleSheet(Constants.STYLE_BASE_PATH + "code-smell.css")
            .usingStyleSheet(Constants.STYLE_BASE_PATH + "ace.css")
            .content(transformParams)
            .build()

        return createTempFile(CreateTempFileParams("$ACE_ACKNOWLEDGEMENT.md", fileContent, project))
    }

    override fun sendTelemetry(params: FnToRefactor) {
        TelemetryService.getInstance().logUsage(TelemetryEvents.ACE_INFO_PRESENTED)
    }

    private fun getContent() = this@AceAcknowledgementViewer.javaClass.classLoader
        .getResourceAsStream(ACE_ACKNOWLEDGEMENT_FILE) //This file has not been added to shared docs yet
        ?.bufferedReader()
        ?.readText()
        ?: ""

}