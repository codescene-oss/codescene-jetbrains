package com.codescene.jetbrains.services.htmlviewer.codehealth

import com.codescene.data.ace.Confidence
import com.codescene.data.ace.RefactorResponse
import com.codescene.jetbrains.services.api.RefactoredFunction
import com.codescene.jetbrains.util.TransformMarkdownParams
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import java.util.stream.Collectors

abstract class HtmlContentBuilder {
    protected var title: String = ""
    protected var focusLine: String = ""
    protected var content: String = ""
    protected var summary: String = ""
    protected var reasons: String = ""
    protected var code: String = ""
    protected var webViewData: String = ""
    protected var customStyle = StringBuilder(
        """
        |${JBCefScrollbarsHelper.buildScrollbarsStyle().trim()}
        |${PreviewThemeStyles.createStylesheet().trim()}
    """.trimMargin().trim()
    )

    fun withWebViewData(html: String) = apply {
        this.webViewData += "\n$html"
    }

    fun usingStyleSheet(stylePath: String) = apply {
        val classLoader = this@HtmlContentBuilder.javaClass.classLoader
        val style = classLoader
            .getResourceAsStream(stylePath)
            ?.bufferedReader()
            ?.lines()
            ?.filter { line -> line.trim().isNotEmpty() }
            ?.collect(Collectors.joining("\n"))

        this.customStyle.append(style)
    }

    fun title(title: String, logoPath: String = "") = apply {
        val logo = if (logoPath.isNotEmpty())
            this@HtmlContentBuilder.javaClass.classLoader
                .getResourceAsStream(logoPath)
                ?.bufferedReader()
                ?.readText()
                ?: ""
        else ""

        if (logo.isEmpty()) {
            this.title =
                """
                |<h2 class="icon-header">
                |$title
                |</h2>
                """.trimMargin().trim()
        } else {
            this.title =
                """
                |<h2 class="icon-header">
                |$logo 
                |$title
                |</h2>
                """.trimMargin().trim()
        }
    }

    fun functionLocation(fileName: String, focusLine: Int) = apply {
        this.focusLine =
            """
        |<div class="documentation-header">
        |<p>
        |<span id="function-location">$fileName&nbsp;&nbsp;
        |<span id="line-number">[Ln&nbsp;$focusLine]</span>
        |</span>
        |</p>
        |</div>
        """.trimMargin().trim()
    }

    abstract fun build(): String
    abstract fun summary(confidence: Confidence): HtmlContentBuilder
    abstract fun reasons(refactoringResult: RefactorResponse): HtmlContentBuilder
    abstract fun code(refactoredFunction: RefactoredFunction): HtmlContentBuilder
    abstract fun content(params: TransformMarkdownParams?): HtmlContentBuilder
}


