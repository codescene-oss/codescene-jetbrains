package com.codescene.jetbrains.services.htmlviewer.codehealth

import com.codescene.data.ace.Confidence
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.services.htmlviewer.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.util.TransformMarkdownParams
import com.codescene.jetbrains.util.getLanguageByExtension
import com.intellij.util.PathUtil.getFileExtension

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class AceRefactoringHtmlContentBuilder : HtmlContentBuilder() {
    private var aceButtons = """
                |<div class="ace-buttons-container">
                |   <button id="accept-refactor-button" class="ace-button-style ace-button-blue">Accept Auto-Refactor</button>
                |   <button id="reject-refactor-button" class="ace-button-style ace-button-gray">Reject</button>
                |</div>
            """

    override fun summary(confidence: Confidence) = apply {
        val level = confidence.level.value()
        val details = confidence.recommendedAction.details
        val description = confidence.recommendedAction.description

        val levelClass = "level-$level"
        if (level == 0) {
            this.summary = """
                |<div class="refactoring-summary $levelClass">
                |<div class="refactoring-summary-header $levelClass">$description</div>
                |<span>$details</span>
                |${retryButton()}
                |</div>
            """.trimMargin().trim()
        } else {
            this.summary =
                """
                |<div class="refactoring-summary $levelClass">
                |<div class="refactoring-summary-header $levelClass">$description</div>
                |<span>$details</span>
                |</div>
                |$aceButtons
                """.trimMargin().trim()
        }
    }

    private fun retryButton(): String {
        return """
            |<div>
            |<button id="retry-refactor-button" class="ace-button-style ace-button-blue">Retry Auto-Refactor</button>
            |</div>
        """.trimMargin()
    }

    override fun reasons(refactoringResult: RefactorResponse) = apply {
        val heading = refactoringResult.confidence.reviewHeader.get()
        val content = if (refactoringResult.confidence.level.value() == 0) {
            "The LLMs couldn't provide an ideal refactoring due to the specific complexities of the code. " +
                    "Though not an endorsed solution, it is displayed as a guide to help refine your approach."
        } else {
            if (refactoringResult.reasons.size > 0) {
                "<ul>${refactoringResult.reasons.map { it.summary }.joinToString("\n")}</ul>"
            } else {
                ""
            }
        }

        // WARNING: empty line after summary is mandatory, otherwise whole html gets broken!!!
        if (content.isNotEmpty()) {
            this.reasons =
                """
                |<div>
                |<details open>
                |<summary>$heading</summary>
                |
                |<p>$content</p>
                |</details>
                |</div>
                """.trimMargin().trim()
        }
    }

    override fun code(refactoredFunction: RefactoredFunction) = apply {
        val confidenceLevel = refactoredFunction.refactoringResult.confidence.level.value()
        val code = refactoredFunction.refactoringResult.code
        val fileExtension = getFileExtension(refactoredFunction.fileName)
        val languageString = getLanguageByExtension(fileExtension.toString())
        val highlightedCode: String = generateHighlightedHtml(
            code,
            languageString,
            MarkdownCodeDelimiter.MULTI_LINE
        )

        val codeTitle = if (confidenceLevel == 0) {
            "Refactored code (unverified)"
        } else {
            "Refactored code"
        }

        // WARNING: empty line after summary is mandatory, otherwise whole html gets broken!!!
        if (code.isNotEmpty()) {
            this.code =
                """
                |<div>
                |<details open>
                |<summary>$codeTitle</summary>
                |
                |$highlightedCode
                |</details>
                |</div>
                """.trimIndent().trimMargin()
        }
    }

    override fun content(params: TransformMarkdownParams?): HtmlContentBuilder {
        return this
    }

    // language=HTML
    override fun build() =
        """
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |<meta charset="UTF-8">
            |<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline';">
            |<style>
            |$customStyle
            |</style>
            |</head>
            |<body>
            |<br>
            |$title
            |$focusLine
            |<hr>
            |$summary
            |$reasons
            |$code
            |$webViewData
            |</body>
            |</html>
        """.trimMargin()
}
