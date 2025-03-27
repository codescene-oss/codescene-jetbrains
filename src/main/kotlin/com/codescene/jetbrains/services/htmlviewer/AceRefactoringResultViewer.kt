package com.codescene.jetbrains.services.htmlviewer

import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.util.Constants.ACE_REFACTORING_SUGGESTION
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
        val (name, refactoringResult) = params
        val credits = refactoringResult.creditsInfo.get()

        return createTempFile(
            "$ACE_REFACTORING_SUGGESTION.md",
            """
                <html>
                  <p> 
                    This is a placeholder for the result of $name's refactoring.
                    <ul>
                        <li>Available credits: ${credits.limit - credits.used}</li>
                        <li>Confidence: ${refactoringResult.confidence.title}</li>
                        <li>Reasons summary: ${refactoringResult.reasons.map { it.summary }.joinToString(", ")}</li>
                        <li>Added code smells: ${refactoringResult.refactoringProperties.addedCodeSmells.joinToString(", ")}</li>
                        <li>Removed code smells: ${
                refactoringResult.refactoringProperties.removedCodeSmells.joinToString(
                    ", "
                )
            }</li>
                        <li>Code: ${refactoringResult.code} </li>
                    </ul>
                  </p>
                </html>
            """.trimIndent(),
            project
        )
    }
}