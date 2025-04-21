package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.codehealth.HtmlContentBuilder
import com.codescene.jetbrains.util.Constants.STYLE_BASE_PATH
import com.codescene.jetbrains.util.CreateTempFileParams
import com.codescene.jetbrains.util.HtmlPart
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.createTempFile
import com.codescene.jetbrains.util.getLanguageByExtension
import com.codescene.jetbrains.util.refactoringSummary
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtil.getFileExtension

@Service(Service.Level.PROJECT)
class AceRefactoringResultViewer(private val project: Project) : HtmlViewer<RefactoredFunction>(project) {
    companion object {
        fun getInstance(project: Project) = project.service<AceRefactoringResultViewer>()
    }

    override fun prepareFile(params: RefactoredFunction): LightVirtualFile {
        val (name, refactoringResult) = params
        val credits = refactoringResult.creditsInfo.get()

        val title = refactoringResult.confidence.title

        var content = StringBuilder()
        refactoringSummary(content, refactoringResult.confidence)

        val heading = refactoringResult.confidence.reviewHeader
        val builder = HtmlContentBuilder()

        val reasons: String
        val codePartTitle: String
        if (refactoringResult.confidence.level.value() == 0) {
            reasons = "The LLMs couldn't provide an ideal refactoring due to the specific complexities of the code. Though not an endorsed solution, it is displayed as a guide to help refine your approach."
            codePartTitle = "Refactored code (unverified)"
        } else {
            reasons = if (refactoringResult.reasons.size > 0) {
                "<ul>${refactoringResult.reasons.map { it.summary }.joinToString("\n")}</ul>"
            } else {
                ""
            }
            codePartTitle = "Refactored code"
        }

        val fileExtension = getFileExtension(params.fileName)

        val contentParams = when {
            reasons.isNotEmpty() -> HtmlPart(heading.get(), reasons)
            else -> null
        }
        val codeContentParams = HtmlPart(codePartTitle, refactoringResult.code, true, getLanguageByExtension(fileExtension.toString()))

        CodeSceneGlobalSettingsStore.getInstance().state.lastFunctionLocation = FunctionLocation(params.focusLine ?: 1, params.fileName)


        val fileContent = builder
            .title(title)
            .functionLocation(params.fileName, params.focusLine ?: 1)
            .usingStyleSheet(STYLE_BASE_PATH + "ace-results.css")
            .usingStyleSheet(STYLE_BASE_PATH + "code-smell.css")
            .content(content, contentParams)
            .content(content, codeContentParams)
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