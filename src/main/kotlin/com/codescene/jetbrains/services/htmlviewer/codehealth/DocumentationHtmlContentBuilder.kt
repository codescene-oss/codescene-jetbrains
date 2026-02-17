package com.codescene.jetbrains.services.htmlviewer.codehealth

import com.codescene.data.ace.Confidence
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.util.TransformMarkdownParams
import com.codescene.jetbrains.util.transformMarkdownToHtml

// TODO[CWF-DELETE]: Remove once CWF is fully rolled out
class DocumentationHtmlContentBuilder : HtmlContentBuilder() {
    override fun content(params: TransformMarkdownParams?) =
        apply {
            params?.let { this.content = transformMarkdownToHtml(params) }
        }

    // language=HTML
    override fun build() =
        """
            |<!DOCTYPE html>
            |<html lang="en">
            |    <head>
            |        <meta charset="UTF-8">
            |        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline';">
            |        <style>
            |            $customStyle
            |        </style>
            |    </head>
            |    <body>
            |        <br>
            |        $title
            |        $focusLine
            |        <hr>
            |        $webViewData
            |        $content
            |    </body>
            |</html>
        """.trimMargin()

    override fun summary(confidence: Confidence): HtmlContentBuilder {
        return this
    }

    override fun reasons(refactoringResult: RefactorResponse): HtmlContentBuilder {
        return this
    }

    override fun code(refactoredFunction: RefactoredFunction): HtmlContentBuilder {
        return this
    }
}
