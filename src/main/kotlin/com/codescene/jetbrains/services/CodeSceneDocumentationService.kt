package com.codescene.jetbrains.services

import com.codescene.jetbrains.codeInsight.codehealth.CodeSceneHtmlViewer
import com.codescene.jetbrains.codeInsight.codehealth.MarkdownCodeDelimiter
import com.codescene.jetbrains.codeInsight.codehealth.PreviewThemeStyles
import com.codescene.jetbrains.codeInsight.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.categoryToFileName
import com.codescene.jetbrains.util.surroundingCharactersNotBackticks
import com.intellij.ide.actions.OpenInRightSplitAction.Companion.openInRightSplit
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.markdown.utils.MarkdownToHtmlConverter
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import org.intellij.plugins.markdown.lang.parser.MarkdownDefaultFlavour
import java.util.stream.Collectors

@Service(Service.Level.PROJECT)
class CodeSceneDocumentationService(project: Project) : LafManagerListener {
    private val fileEditorManager = FileEditorManager.getInstance(project)

    init {
        // Subscribe to theme updates when the service is initialized
        val bus = ApplicationManager.getApplication().messageBus.connect()
        bus.subscribe(LafManagerListener.TOPIC, this)
    }

    companion object {
        fun getInstance(project: Project): CodeSceneDocumentationService =
            project.service<CodeSceneDocumentationService>()

        var functionLocation: FunctionLocation? = null
        var editor: Editor? = null
        val threeBackticks = MarkdownCodeDelimiter.MULTI_LINE.value
        val oneBacktick = MarkdownCodeDelimiter.SINGLE_LINE.value
    }

    /**
     * Method will be called when code lens or annotation related to specific
     * [CodeSmell] is clicked. It will open virtual file in right split of the original file
     * where lens or annotation is shown.
     * Documentation file opened will be showing HTML content for that CodeSmell,
     * using custom [CodeSceneHtmlViewer]
     */
    fun openDocumentationPanel(editor: Editor, codeSmell: CodeSmell) {
        Companion.editor = editor
        val project = editor.project!!

        functionLocation = FunctionLocation(editor.virtualFile.name, codeSmell)
        val codeSmellFileName = codeSmell.category + ".md"

        val markdownContent = prepareMarkdownContent(editor, codeSmell)

        Log.info("Opening documentation file $codeSmellFileName")
        val documentationFile = createTempFile(project, codeSmellFileName, markdownContent)
        if (!fileEditorManager.selectedFiles.contains(documentationFile)) {
            // return focus to original file
            fileEditorManager.openFile(editor.virtualFile, true, true)

            openInRightSplit(project, documentationFile, null, false)?.closeAllExcept(documentationFile)
        }
    }

    /**
     * Reading raw documentation md files and creating HTML content out of it,
     * which is then added to separately created header as base for all styling.
     */
    private fun prepareMarkdownContent(editor: Editor, codeSmell: CodeSmell): String {
        val codeSmellName = codeSmell.category
        val codeSmellFileName = categoryToFileName(codeSmellName) + ".md"

        Log.info("Preparing content for file $codeSmellName.md")
        val classLoader = this@CodeSceneDocumentationService.javaClass.classLoader
        val inputStream = classLoader.getResourceAsStream(Constants.DOCUMENTATION_BASE_PATH + codeSmellFileName)
        val markdownContent =
            inputStream?.bufferedReader()?.readText()?.let { transformMarkdownToHtml(it, codeSmellName) }
        val header = prepareHeader(editor, codeSmell)

        return "$header$markdownContent"
    }

    /**
     * Transforming Markdown content into HTML content by reorganizing it and inserting HTML tags.
     */
    private fun transformMarkdownToHtml(originalContent: String, codeSmellName: String): String {
        val content = "$codeSmellName\n\n$originalContent"
        val parts = content.split("## ")
        val newContent = StringBuilder("")
        val toHtmlConverter = MarkdownToHtmlConverter(MarkdownDefaultFlavour()) //)
        parts.forEach { part ->
            val title = part.substring(0, part.indexOf("\n"))
            var body = part.substring(part.indexOf("\n") + 1)

            body = updateOneLineCodeParts(body)

            if (body.contains(threeBackticks)) {
                val codePart = body.substring(body.indexOf(threeBackticks), body.lastIndexOf(threeBackticks) + 4)
                val languageString = codePart.substring(3, codePart.indexOf("\n"))
                var highlightedBody: String = generateHighlightedHtml(
                    codePart.substring(codePart.indexOf("\n") + 1, codePart.lastIndexOf(threeBackticks)),
                    languageString,
                    MarkdownCodeDelimiter.MULTI_LINE
                )
                val newBody = toHtmlConverter.convertMarkdownToHtml(body.replace(codePart, highlightedBody))
                appendSubpart(newContent, HtmlPart(title, newBody))
            } else {
                appendSubpart(newContent, HtmlPart(title, toHtmlConverter.convertMarkdownToHtml(body)))
            }
        }
        return newContent.append("\n</body></html>").toString()
    }

    /**
     * Method to transform single line Markdown code into single line HTML code by
     * highlighting it and replacing original code in the string with highlighted one.
     */
    private fun updateOneLineCodeParts(body: String): String {
        var singleLineCodeResolved = body
        if (containsSingleBacktick(body)) {
            while (containsSingleBacktick(singleLineCodeResolved)) {
                val firstIndex = singleLineCodeResolved.indexOf(oneBacktick)
                val codePart = singleLineCodeResolved.substring(
                    firstIndex,
                    singleLineCodeResolved.indexOf(oneBacktick, firstIndex + 1) + 1
                )

                var highlightedBody: String = generateHighlightedHtml(
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
    private fun containsSingleBacktick(string: String): Boolean {
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

    /**
     * Method to append single collapsable part to the current HTML content.
     */
    private fun appendSubpart(content: StringBuilder, htmlPart: HtmlPart) {
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
    private fun prepareHeader(editor: Editor, codeSmell: CodeSmell): String {
        val codeSmellName = codeSmell.category
        val fileName = editor.virtualFile.name
        val lineNumber = codeSmell.highlightRange.startLine

        // styling
        val classLoader = this@CodeSceneDocumentationService.javaClass.classLoader
        val styleStream = classLoader.getResourceAsStream(Constants.STYLE_BASE_PATH + "code-smell.css")
        val staticStyleBuilder = StringBuilder()
        staticStyleBuilder.append(styleStream?.bufferedReader()?.lines()
            ?.filter { line -> line.trim().isNotEmpty() }
            ?.collect(Collectors.joining("\n")))

        val staticStyle = staticStyleBuilder.toString()
        val scrollbarStyle = JBCefScrollbarsHelper.buildScrollbarsStyle()
        val themeStyle = PreviewThemeStyles.createStylesheet()

        // language=HTML
        val content = """
            |<!DOCTYPE html>
            |<html lang="en">
            |<head>
            |    <meta charset="UTF-8">
            |    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline';">
            |    <style>
            |        $themeStyle
            |        $staticStyle
            |        $scrollbarStyle
            |    </style>
            |</head>
            |<body>
            |    <br>
            |    <h2>$codeSmellName</h2>
            |    <div class="documentation-header">
            |        <p>
            |            <span id="function-location">$fileName&nbsp;&nbsp;
            |                <span id="line-number">[Ln&nbsp;$lineNumber]</span>
            |            </span>
            |        </p>
            |    </div>
            |    <hr>
            """.trimMargin()
        return content
    }

    /**
     * Method to create virtual file for our generated documentation content.
     */
    private fun createTempFile(project: Project, name: String, content: String): VirtualFile {
        val file = LightVirtualFile(name, content)
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return file
        file.isWritable = false

        WriteCommandAction.runWriteCommandAction(project) {
            psiFile.virtualFile.refresh(false, false)
        }
        return file
    }

    /**
     * Listener for look and feel changes (theme change).
     * In case of theme change we refresh currently opened documentation file to fetch new look and feel.
     */
    override fun lookAndFeelChanged(p0: LafManager) {
        if (functionLocation?.codeSmell != null && editor != null) {
            this.openDocumentationPanel(editor!!, functionLocation!!.codeSmell)
        }
    }
}

data class FunctionLocation(
    val fileName: String,
    val codeSmell: CodeSmell
)

data class HtmlPart(
    val title: String,
    val body: String
)