package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.config.global.FunctionLocation
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

@Service(Service.Level.PROJECT)
class AceRefactoringResultViewer(private val project: Project) : HtmlViewer<RefactoredFunction>(project) {
    companion object {
        fun getInstance(project: Project) = project.service<AceRefactoringResultViewer>()
    }

    override fun prepareFile(params: RefactoredFunction): LightVirtualFile {
        CodeSceneGlobalSettingsStore.getInstance().state.lastFunctionLocation = FunctionLocation(params.focusLine ?: 1, params.fileName)
        val refactoringResult = params.refactoringResult
        val title = refactoringResult.confidence.title
        val builder = AceRefactoringHtmlContentBuilder()

        val fileContent = builder
            .title(title)
            .functionLocation(params.fileName, params.focusLine ?: 1)
            .usingStyleSheet(STYLE_BASE_PATH + "ace-results.css")
            .usingStyleSheet(STYLE_BASE_PATH + "code-smell.css")
            .summary(refactoringResult.confidence)
            .reasons(refactoringResult)
            .code(params)
            .build()

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