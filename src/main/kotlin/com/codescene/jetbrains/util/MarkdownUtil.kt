package com.codescene.jetbrains.util

import com.codescene.data.review.CodeSmell
import com.codescene.jetbrains.services.htmlviewer.DocumentationParams
import com.codescene.jetbrains.services.htmlviewer.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.services.htmlviewer.codehealth.MarkdownCodeDelimiter
import com.codescene.jetbrains.services.htmlviewer.codehealth.PreviewThemeStyles
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.codescene.jetbrains.util.Constants.GENERAL_CODE_HEALTH
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import java.util.stream.Collectors

val threeBackticks = MarkdownCodeDelimiter.MULTI_LINE.value
val oneBacktick = MarkdownCodeDelimiter.SINGLE_LINE.value

data class HeadingParams(
    val codeSmell: CodeSmell? = null,
    val standaloneDocumentation: Boolean,
    val content: String,
    val classLoader: ClassLoader,
    val file: VirtualFile? = null
)

data class TransformMarkdownParams(
    val originalContent: String,
    val codeSmellName: String,
    val standaloneDocumentation: Boolean = false
)

data class HtmlPart(
    val title: String,
    val body: String
)

/**
 * Reading raw documentation md files and creating HTML content out of it,
 * which is then added to separately created header as base for all styling.
 */
fun prepareMarkdownContent(params: DocumentationParams, classLoader: ClassLoader): String {
    val (file, codeSmell) = params

    val codeSmellName = codeSmell.category
    val codeSmellFileName = categoryToFileName(codeSmellName) + ".md"

    val standaloneDocumentation =
        codeSmellName.contains(GENERAL_CODE_HEALTH) || codeSmellName.contains(CODE_HEALTH_MONITOR)

    //TODO: revert old logic for getting standalone docs path once monitor doc is updated
    val path = if (codeSmellName.contains(GENERAL_CODE_HEALTH))
        Constants.DOCUMENTATION_BASE_PATH
    else if (codeSmellName.contains(CODE_HEALTH_MONITOR))
        ""
    else Constants.ISSUES_PATH

    Log.debug("Preparing content for file $codeSmellName.md")
    val inputStream = classLoader.getResourceAsStream(path + codeSmellFileName)

    val content = inputStream?.bufferedReader()?.readText() ?: ""
    val markdownContent =
        transformMarkdownToHtml(TransformMarkdownParams(content, codeSmellName, standaloneDocumentation))

    val header = prepareHeader(HeadingParams(codeSmell, standaloneDocumentation, content, classLoader, file))

    return "$header$markdownContent"
}

/**
 * Transforming Markdown content into HTML content by reorganizing it and inserting HTML tags.
 */
fun transformMarkdownToHtml(
    params: TransformMarkdownParams,
    ace: Boolean = false
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
        if (ace) newContent.append(
            """
                <div id="ace-button-container">
                  <button id="ace-button">Show me CodeScene ACE</button>
                  <span id="ace-span">You can disable CodeScene ACE anytime in settings.</span>
                </div>
            """.trimIndent()
        )
    }
    return newContent.append("\n</body></html>").toString()
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
 * Method to generate header part of documentation file.
 * It is fetching three different styles, static, theme dependent and scrollbar and applying them.
 * Also, it generates header HTML content.
 */
fun prepareHeader(params: HeadingParams): String {
    val (codeSmell, standaloneDocumentation, _, classLoader, file) = params

    // styling
    val style = getStyling(classLoader)

    // components
    val heading = getHeading(params)
    val documentationSubHeader = if (!standaloneDocumentation) getDocumentationSubHeader(file, codeSmell) else ""

    // language=HTML
    return """
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |    <meta charset="UTF-8">
            |    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline';">
            |    <style>
            |        $style
            |    </style>
            |</head>
            |<body>
            |    <br>
            |    $heading
            |    $documentationSubHeader
            |    <hr>
            """.trimMargin()
}

/**
 * Generates an HTML heading for the documentation, optionally including a logo for standalone documentation.
 */
fun getHeading(params: HeadingParams): String {
    val (codeSmell, standaloneDocumentation, content, classLoader) = params

    val logoSvg = classLoader.getResourceAsStream(Constants.LOGO_PATH)?.bufferedReader()?.readText()

    return if (!standaloneDocumentation) "<h2>${codeSmell?.category}</h2>" else {
        val heading = content.split("\n\n", limit = 2)[0]

        """
        |    <h1 class="icon-header">
        |        $logoSvg
        |        $heading
        |    </h1>
        """.trimMargin()
    }
}

/**
 * Generates a complete CSS stylesheet by combining a base style, scrollbar styling, and theme-specific styles.
 */
fun getStyling(classLoader: ClassLoader): String {
    val styleStream = classLoader.getResourceAsStream(Constants.STYLE_BASE_PATH + "code-smell.css")
    val aceStyleStream = classLoader.getResourceAsStream(Constants.STYLE_BASE_PATH + "ace.css")
    val staticStyleBuilder = StringBuilder()

    val scrollbarStyle = JBCefScrollbarsHelper.buildScrollbarsStyle()
    val themeStyle = PreviewThemeStyles.createStylesheet()

    staticStyleBuilder.append(
        styleStream?.bufferedReader()?.lines()
            ?.filter { line -> line.trim().isNotEmpty() }
            ?.collect(Collectors.joining("\n")))
    staticStyleBuilder.append(
        aceStyleStream?.bufferedReader()?.lines()
            ?.filter { line -> line.trim().isNotEmpty() }
            ?.collect(Collectors.joining("\n")))
    staticStyleBuilder.append(scrollbarStyle)
    staticStyleBuilder.append(themeStyle)

    return staticStyleBuilder.toString()
}

/**
 * Generates an HTML sub-header for code smell files, displaying the file name and line number.
 * If `standaloneDocumentation` is true (e.g., general code health or code health monitor info),
 * no sub-header is returned.
 *
 */
fun getDocumentationSubHeader(file: VirtualFile?, codeSmell: CodeSmell?): String {
    val fileName = file?.name ?: ""
    val lineNumber = codeSmell?.highlightRange?.startLine ?: 1

    return """
        |    <div class="documentation-header">
        |        <p>
        |            <span id="function-location">$fileName&nbsp;&nbsp;
        |                <span id="line-number">[Ln&nbsp;$lineNumber]</span>
        |            </span>
        |        </p>
        |    </div>
        """.trimMargin()
}

/**
 * Method to create virtual file for our generated documentation content.
 */
fun createTempFile(name: String, content: String, project: Project): LightVirtualFile {
    val file = LightVirtualFile(name, content)
    val psiFile = runReadAction { PsiManager.getInstance(project).findFile(file) } ?: return file
    file.isWritable = false

    WriteCommandAction.runWriteCommandAction(project) {
        psiFile.virtualFile.refresh(false, false)
    }
    return file
}