package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.services.api.telemetry.TelemetryService
import com.codescene.jetbrains.services.htmlviewer.codehealth.HtmlContentBuilder
import com.codescene.jetbrains.util.Constants.ACE_REFACTORING_SUGGESTION
import com.codescene.jetbrains.util.Constants.STYLE_BASE_PATH
import com.codescene.jetbrains.util.CreateTempFileParams
import com.codescene.jetbrains.util.HtmlPart
import com.codescene.jetbrains.util.TelemetryEvents
import com.codescene.jetbrains.util.createTempFile
import com.codescene.jetbrains.util.refactoringSummary
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
        val (name, refactoringResult) = params
        val credits = refactoringResult.creditsInfo.get()

        var content = StringBuilder()
        refactoringSummary(content, refactoringResult.confidence)

        content.append(
            """
            <p> 
                This is a placeholder for the result of $name's refactoring.
                <ul>
                    <li>Available credits: ${credits.limit - credits.used}</li>
                    <li>Confidence: ${refactoringResult.confidence.title}</li>
                    <li>Reasons summary: ${refactoringResult.reasons.map { it.summary }.joinToString(", ")}</li>
                    <li>Added code smells: ${refactoringResult.refactoringProperties.addedCodeSmells.joinToString(", ")}</li>
                    <li>Removed code smells: ${refactoringResult.refactoringProperties.removedCodeSmells.joinToString(", ")}</li>
                    <li>Code: ${"\n" + refactoringResult.code} </li>
                </ul>
            </p>
        """.trimIndent())

        val heading = refactoringResult.confidence.reviewHeader
        val builder = HtmlContentBuilder()
        val contentParams = HtmlPart(heading.get(), refactoringResult.reasons.map { it.summary }.joinToString(", "))

        builder.functionLocation(params.fileName, params.focusLine ?: 1)

        val fileContent = builder
            .title(refactoringResult.confidence.title)
            .usingStyleSheet(STYLE_BASE_PATH + "ace-results.css")
            .usingStyleSheet(STYLE_BASE_PATH + "code-smell.css")
            .content(content, contentParams)
            .build()

        return createTempFile(CreateTempFileParams("$ACE_REFACTORING_SUGGESTION.md", fileContent, project))

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