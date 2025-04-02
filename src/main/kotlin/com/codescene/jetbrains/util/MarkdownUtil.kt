package com.codescene.jetbrains.util

import com.codescene.jetbrains.services.htmlviewer.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.services.htmlviewer.codehealth.MarkdownCodeDelimiter
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node

val threeBackticks = MarkdownCodeDelimiter.MULTI_LINE.value
val oneBacktick = MarkdownCodeDelimiter.SINGLE_LINE.value

data class TransformMarkdownParams(
    val originalContent: String,
    val codeSmellName: String = "",
    val standaloneDocumentation: Boolean = false
)

data class HtmlPart(
    val title: String,
    val body: String
)

data class CreateTempFileParams(
    val name: String,
    val content: String,
    val project: Project
)

/**
 * Transforming Markdown content into HTML content by reorganizing it and inserting HTML tags.
 */
fun transformMarkdownToHtml(
    params: TransformMarkdownParams
): String {
    val (originalContent, codeSmellName, standaloneDocumentation) = params

    val content = if (!standaloneDocumentation)
        "$codeSmellName\n\n$originalContent"
    else originalContent.split("\n\n", limit = 2)[1]

    val parts = content.split("## ")
    val newContent = StringBuilder("")

    parts.forEach { part ->
        val title = part.substring(0, part.indexOf("\n"))
        var body = if (!standaloneDocumentation) part.substring(part.indexOf("\n") + 1) else part

        body = updateOneLineCodeParts(body)

        newContent.adaptBody(title, body, standaloneDocumentation)
    }

    return newContent.toString()
}

/**
 * Adapts and appends the provided content to the current `StringBuilder` in an HTML format,
 * handling markdown conversion, code highlighting, and specific cases for standalone documentation.
 */
fun StringBuilder.adaptBody(title: String, body: String, standaloneDocumentation: Boolean) {
    if (body.contains(threeBackticks)) {
        val codePart = body.substring(body.indexOf(threeBackticks), body.lastIndexOf(threeBackticks) + 4)
        val languageString = codePart.substring(3, codePart.indexOf("\n"))
        val highlightedBody: String = generateHighlightedHtml(
            codePart.substring(codePart.indexOf("\n") + 1, codePart.lastIndexOf(threeBackticks)),
            languageString,
            MarkdownCodeDelimiter.MULTI_LINE
        )

        val newBody = convertMarkdownToHtml(body.replace(codePart, highlightedBody))
        appendSubpart(this, HtmlPart(title, newBody))
    } else if (standaloneDocumentation) {
        append(convertMarkdownToHtml(body))
    } else {
        appendSubpart(this, HtmlPart(title, convertMarkdownToHtml(body)))
    }
}

/**
 * Method to transform single line Markdown code into single line HTML code by
 * highlighting it and replacing original code in the string with highlighted one.
 */
fun updateOneLineCodeParts(body: String): String {
    var singleLineCodeResolved = body
    if (containsSingleBacktick(body)) {
        while (containsSingleBacktick(singleLineCodeResolved)) {
            val firstIndex = singleLineCodeResolved.indexOf(oneBacktick)
            val codePart = singleLineCodeResolved.substring(
                firstIndex,
                singleLineCodeResolved.indexOf(oneBacktick, firstIndex + 1) + 1
            )

            val highlightedBody: String = generateHighlightedHtml(
                codePart.substring(codePart.indexOf(oneBacktick) + 1, codePart.lastIndexOf(oneBacktick)),
                "",
                MarkdownCodeDelimiter.SINGLE_LINE
            )
            singleLineCodeResolved = singleLineCodeResolved.replace(codePart, highlightedBody)
        }
    }
    return singleLineCodeResolved
}

/**
 * Checking if there is only single backtick (`) character in the string.
 * Multiple backticks should result false.
 */
fun containsSingleBacktick(string: String): Boolean {
    if (string.contains(oneBacktick)) {
        for (i in string.indices) {
            // checking if there is only single ` without other thick before or after
            if (string[i] == '`' && surroundingCharactersNotBackticks(string, i)) {
                return true
            }
        }
    }
    return false
}

fun convertMarkdownToHtml(markdown: String): String {
    val parser = Parser.builder().build()
    val renderer = HtmlRenderer.builder().build()
    val document: Node = parser.parse(markdown)

    return "<body>${renderer.render(document)}</body>"
}

/**
 * Method to append single collapsable part to the current HTML content.
 */
fun appendSubpart(content: StringBuilder, htmlPart: HtmlPart) {
    content.append(
        """
            <div>
                <details open>
                    <summary>${htmlPart.title}</summary>
            """.trimIndent()
    )
        .append(htmlPart.body)
        .append("\n</details></div>\n\n")
}

/**
 * Method to create virtual file for our generated documentation content.
 */
fun createTempFile(params: CreateTempFileParams): LightVirtualFile {
    val (name, content, project) = params
    val file = LightVirtualFile(name, content)
    val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) } ?: return file
    file.isWritable = false

    WriteCommandAction.runWriteCommandAction(project) {
        psiFile.virtualFile.refresh(false, false)
    }
    return file
}