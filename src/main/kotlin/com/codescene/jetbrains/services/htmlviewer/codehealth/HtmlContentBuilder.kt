package com.codescene.jetbrains.services.htmlviewer.codehealth

import com.codescene.jetbrains.services.htmlviewer.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.util.HtmlPart
import com.codescene.jetbrains.util.TransformMarkdownParams
import com.codescene.jetbrains.util.appendSubpart
import com.codescene.jetbrains.util.transformMarkdownToHtml
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import java.util.stream.Collectors

class HtmlContentBuilder {
    private var title: String = ""
    private var content: String = ""
    private var focusLine: String = ""
    private var customStyle = StringBuilder(
        """
        ${JBCefScrollbarsHelper.buildScrollbarsStyle()}
        ${PreviewThemeStyles.createStylesheet()}
    """.trimIndent()
    )

    fun content(params: TransformMarkdownParams) = apply {
        this.content = transformMarkdownToHtml(params)
    }

    fun content(contentBuilder: StringBuilder, params: HtmlPart?) = apply {
        params?.let {
            if (params.isCode) {
                val highlightedBody: String = generateHighlightedHtml(
                    params.body,
                    params.languageString,
                    MarkdownCodeDelimiter.MULTI_LINE
                )

                val newBody = params.body.replace(params.body, highlightedBody)
                appendSubpart(contentBuilder, HtmlPart(params.title, newBody, params.isCode, params.languageString))
                this.content = contentBuilder.toString()
            } else {
                appendSubpart(contentBuilder, params)
                this.content = contentBuilder.toString()
            }
        }
        this.content = contentBuilder.toString()
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

        this.title =
            """
        |    <h2 class="icon-header">
        |        $logo
        |        $title
        |    </h2>
        """.trimMargin()
    }

    fun functionLocation(fileName: String, focusLine: Int) = apply {
        this.focusLine =
            """
        |    <div class="documentation-header">
        |        <p>
        |            <span id="function-location">$fileName&nbsp;&nbsp;
        |                <span id="line-number">[Ln&nbsp;$focusLine]</span>
        |            </span>
        |        </p>
        |    </div>
        """.trimMargin()
    }

    // language=HTML
    fun build() = """
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
            |        $content
            |    </body>
            |</html>
        """.trimMargin()

}
