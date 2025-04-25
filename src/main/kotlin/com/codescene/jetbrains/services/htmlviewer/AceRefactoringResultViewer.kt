package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.codehealth.AceRefactoringHtmlContentBuilder
import com.codescene.jetbrains.util.Constants.STYLE_BASE_PATH
import com.codescene.jetbrains.util.CreateTempFileParams
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.createTempFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import java.io.File

@Service(Service.Level.PROJECT)
class AceRefactoringResultViewer(private val project: Project) : HtmlViewer<RefactoredFunction>(project) {
    var refactoredFunction: RefactoredFunction? = null
        private set

    companion object {
        fun getInstance(project: Project) = project.service<AceRefactoringResultViewer>()
    }

    override fun prepareFile(params: RefactoredFunction): LightVirtualFile {
        refactoredFunction = params
        val (_, refactoringResult, fileName, focusLine) = params
        val title = refactoringResult.confidence.title
        val builder = AceRefactoringHtmlContentBuilder()

        val scriptTag = """
            <script id="function-data" type="application/json">
              {
                 "fileName": "${fileName}",
                 "focusLine": ${focusLine}
              }
            </script>
        """.trimIndent()

        val fileContentBuilder = builder
            .title(title)
            .functionLocation(params.fileName, params.focusLine ?: 1)
            .usingStyleSheet(STYLE_BASE_PATH + "ace-results.css")
            .usingStyleSheet(STYLE_BASE_PATH + "code-smell.css")
            .summary(refactoringResult.confidence)
            .reasons(refactoringResult)
            .code(params)

        if (fileName.isNotEmpty()) {
            fileContentBuilder.functionLocation(File(fileName).name, focusLine ?: 1)
                .withWebViewData(scriptTag)
        }

        val fileContent = fileContentBuilder.build()

        return createTempFile(CreateTempFileParams("$title.md", fileContent, project))

    }

    override fun sendTelemetry(params: RefactoredFunction) {
        TelemetryService.getInstance().logUsage(
            TelemetryEvents.ACE_REFACTOR_PRESENTED, mutableMapOf(
                Pair("confidence", params.refactoringResult.confidence.level),
                Pair("isCached", params.refactoringResult.metadata.cached)
            )
        )
    }
}