package com.codescene.jetbrains.services

import com.codescene.jetbrains.codeInsight.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.codeInsight.codehealth.MarkdownCodeDelimiter
import com.codescene.jetbrains.codeInsight.codehealth.PreviewThemeStyles
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.util.Constants
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.categoryToFileName
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
        val threeTicks = MarkdownCodeDelimiter.MULTI_LINE.value
        val oneTick = MarkdownCodeDelimiter.SINGLE_LINE.value
    }


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

    private fun transformMarkdownToHtml(originalContent: String, codeSmellName: String): String {
        val content = "$codeSmellName\n\n$originalContent"
        val parts = content.split("## ")
        val newContent = StringBuilder("")
        val toHtmlConverter = MarkdownToHtmlConverter(MarkdownDefaultFlavour()) //)
        parts.forEach { part ->
            val title = part.substring(0, part.indexOf("\n"))
            var body = part.substring(part.indexOf("\n") + 1)

            body = updateOneLineCodeParts(body)

            if (body.contains(threeTicks)) {
                val codePart = body.substring(body.indexOf(threeTicks), body.lastIndexOf(threeTicks) + 4)
                val languageString = codePart.substring(3, codePart.indexOf("\n"))
                var highlightedBody: String = generateHighlightedHtml(
                    codePart.substring(codePart.indexOf("\n") + 1, codePart.lastIndexOf(threeTicks)),
                    languageString.uppercase(),
                    MarkdownCodeDelimiter.MULTI_LINE
                )
                val newBody = body.replace(codePart, highlightedBody)
                appendSubpart(newContent, title, toHtmlConverter.convertMarkdownToHtml(newBody))
            } else {
                appendSubpart(newContent, title, toHtmlConverter.convertMarkdownToHtml(body))
            }
        }
        return newContent.append("\n</body></html>").toString()
    }

    private fun updateOneLineCodeParts(body: String): String {
        var singleLineCodeResolved = body
        if (containsSingleBacktick(body)) {
            while (containsSingleBacktick(singleLineCodeResolved)) {
                val firstIndex = singleLineCodeResolved.indexOf(oneTick)
                val codePart = singleLineCodeResolved.substring(
                    firstIndex,
                    singleLineCodeResolved.indexOf(oneTick, firstIndex + 1) + 1
                )

                var highlightedBody: String = generateHighlightedHtml(
                    codePart.substring(codePart.indexOf(oneTick) + 1, codePart.lastIndexOf(oneTick)),
                    "",
                    MarkdownCodeDelimiter.SINGLE_LINE
                )
                singleLineCodeResolved = singleLineCodeResolved.replace(codePart, highlightedBody)
            }
        }
        return singleLineCodeResolved
    }

    private fun containsSingleBacktick(string: String): Boolean {
        if (string.contains(oneTick)) {
            for (i in string.indices) {
                // checking if there is only single ` without other thick before or after
                if (string[i] == '`' && (i + 1 >= string.length || (string[i + 1] != '`' && (i > 0 && string[i - 1] != '`')))) {
                    return true
                }
            }
        }
        return false
    }

    private fun appendSubpart(content: StringBuilder, title: String, body: String) {
        content.append(
            """
            <div>
                <details open>
                    <summary>$title</summary>
            """.trimIndent()
        )
            .append(body)
            .append("\n</details></div>\n\n")
    }


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

    private fun createTempFile(project: Project, name: String, content: String): VirtualFile {
        val file = LightVirtualFile(name, content)
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return file
        file.isWritable = false

        WriteCommandAction.runWriteCommandAction(project) {
            psiFile.virtualFile.refresh(false, false)
        }
        return file
    }

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