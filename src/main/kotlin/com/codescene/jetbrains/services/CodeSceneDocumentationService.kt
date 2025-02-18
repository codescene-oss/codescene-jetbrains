package com.codescene.jetbrains.services

import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Range
import com.codescene.jetbrains.codeInsight.codehealth.CodeHighlighter.generateHighlightedHtml
import com.codescene.jetbrains.codeInsight.codehealth.CodeSceneHtmlViewer
import com.codescene.jetbrains.codeInsight.codehealth.MarkdownCodeDelimiter
import com.codescene.jetbrains.codeInsight.codehealth.PreviewThemeStyles
import com.codescene.jetbrains.services.telemetry.TelemetryService
import com.codescene.jetbrains.util.*
import com.codescene.jetbrains.util.Constants.CODE_HEALTH_MONITOR
import com.codescene.jetbrains.util.Constants.GENERAL_CODE_HEALTH
import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.jcef.JBCefScrollbarsHelper
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.util.ast.Node
import java.util.stream.Collectors

@Service(Service.Level.PROJECT)
class CodeSceneDocumentationService(private val project: Project) : LafManagerListener {
    private val fileEditorManager = FileEditorManager.getInstance(project)
    private lateinit var lastDocsSourceType: DocsSourceType
    private val threeBackticks = MarkdownCodeDelimiter.MULTI_LINE.value
    private val oneBacktick = MarkdownCodeDelimiter.SINGLE_LINE.value

    var functionLocation: FunctionLocation? = null
    lateinit var sourceEditor: Editor

    init {
        // Subscribe to theme updates when the service is initialized
        val bus = ApplicationManager.getApplication().messageBus.connect()
        bus.subscribe(LafManagerListener.TOPIC, this)
    }

    companion object {
        fun getInstance(project: Project): CodeSceneDocumentationService =
            project.service<CodeSceneDocumentationService>()
    }

    //TODO: refactor & adapt to ACE markdown preview
    fun openAcePanel(editor: Editor?) {
        val classLoader = this@CodeSceneDocumentationService.javaClass.classLoader
        val inputStream = classLoader.getResourceAsStream("ace-intro.md")
        val markdown = inputStream?.bufferedReader()?.readText() ?: ""
        val markdownContent =
            transformMarkdownToHtml(TransformMarkdownParams(markdown, "codeSmellName", true))
        val header =
            prepareHeader(HeadingParams(CodeSmell("", Range(1, 1, 1, 1), ""), true, markdown, classLoader, editor))

        val documentationFile = createTempFile("CodeScene ACE Auto-Refactoring.md", "$header$markdownContent")

        if (editor != null)
            splitWindow(documentationFile)
        else
            openDocumentationWithoutActiveEditor(documentationFile)
    }

    /**
     * Method will be called when code lens or annotation related to specific
     * [CodeSmell] is clicked. It will open virtual file in right split of the original file
     * where lens or annotation is shown.
     * Documentation file opened will be showing HTML content for that CodeSmell,
     * using custom [CodeSceneHtmlViewer]
     */
    fun openDocumentationPanel(params: DocumentationParams) {
        val (editor, codeSmell, docsSourceType) = params
        if (editor != null) {
            sourceEditor = editor
            functionLocation = FunctionLocation(editor.virtualFile.path, codeSmell)
        }

        lastDocsSourceType = docsSourceType

        val codeSmellFileName = codeSmell.category + ".md"
        val markdownContent = prepareMarkdownContent(params)

        Log.info("Opening documentation file $codeSmellFileName")
        val documentationFile = createTempFile(codeSmellFileName, markdownContent)

        if (editor != null && !fileEditorManager.selectedFiles.contains(documentationFile)) {
            splitWindow(documentationFile)
            logTelemetryEvent(codeSmell)
        } else {
            openDocumentationWithoutActiveEditor(documentationFile)
        }
    }

    private fun logTelemetryEvent(codeSmell: CodeSmell) {
        if (lastDocsSourceType != DocsSourceType.NONE) {
            TelemetryService.getInstance().logUsage(
                TelemetryEvents.OPEN_DOCS_PANEL,
                mutableMapOf<String, Any>(
                    Pair("source", lastDocsSourceType.value),
                    Pair("category", codeSmell.category)
                )
            )
        }
    }

    /**
     * Opens the given documentation file in a right-split editor.
     * Closes any other currently opened document files that match names in `codeSmellNames` before opening the new file.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    private fun splitWindow(file: VirtualFile) {
        val editorManagerEx = FileEditorManagerEx.getInstanceEx(project)
        val docWindow = editorManagerEx.windows
            .firstOrNull { editorWindow ->
                editorWindow.fileList.any { codeSmellNames.contains(it.nameWithoutExtension) }
            }

        editorManagerEx.splitters.openInRightSplit(file, false)

        fileEditorManager.openFiles
            .filterIsInstance<LightVirtualFile>()
            .filter { it != file && codeSmellNames.contains(it.nameWithoutExtension) }
            .forEach { docWindow?.closeFile(it) }
    }

    /**
     * Opens a standalone documentation file (e.g., Code Health Monitor docs)
     * if there are no other open files and the documentation file is not already open.
     *
     * @param file The [VirtualFile] to be opened in a right-split editor.
     */
    private fun openDocumentationWithoutActiveEditor(file: VirtualFile) {
        val docNotOpen = fileEditorManager.openFiles.none { it.name == file.name }
        val shouldOpenFile = fileEditorManager.openFiles.isEmpty() && docNotOpen
        if (shouldOpenFile) fileEditorManager.openFile(file, false)
    }

    /**
     * Reading raw documentation md files and creating HTML content out of it,
     * which is then added to separately created header as base for all styling.
     */
    private fun prepareMarkdownContent(params: DocumentationParams): String {
        val (editor, codeSmell) = params

        val codeSmellName = codeSmell.category
        val codeSmellFileName = categoryToFileName(codeSmellName) + ".md"

        val standaloneDocumentation =
            codeSmellName.contains(GENERAL_CODE_HEALTH) || codeSmellName.contains(CODE_HEALTH_MONITOR)
        val path = if (standaloneDocumentation) Constants.DOCUMENTATION_BASE_PATH else Constants.ISSUES_PATH

        Log.info("Preparing content for file $codeSmellName.md")
        val classLoader = this@CodeSceneDocumentationService.javaClass.classLoader
        val inputStream = classLoader.getResourceAsStream(path + codeSmellFileName)

        val content = inputStream?.bufferedReader()?.readText() ?: ""
        val markdownContent =
            transformMarkdownToHtml(TransformMarkdownParams(content, codeSmellName, standaloneDocumentation))

        val header = prepareHeader(HeadingParams(codeSmell, standaloneDocumentation, content, classLoader, editor))

        return "$header$markdownContent"
    }

    /**
     * Transforming Markdown content into HTML content by reorganizing it and inserting HTML tags.
     */
    private fun transformMarkdownToHtml(
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
        return newContent.append("\n</body></html>").toString()
    }

    /**
     * Adapts and appends the provided content to the current `StringBuilder` in an HTML format,
     * handling markdown conversion, code highlighting, and specific cases for standalone documentation.
     */
    private fun StringBuilder.adaptBody(title: String, body: String, standaloneDocumentation: Boolean) {
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
    private fun updateOneLineCodeParts(body: String): String {
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

    private fun convertMarkdownToHtml(markdown: String): String {
        val parser = Parser.builder().build()
        val renderer = HtmlRenderer.builder().build()
        val document: Node = parser.parse(markdown)

        return "<body>${renderer.render(document)}</body>"
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
    private fun prepareHeader(params: HeadingParams): String {
        val (codeSmell, standaloneDocumentation, _, classLoader, editor) = params

        val fileName = editor?.virtualFile?.name ?: ""
        val lineNumber = codeSmell.highlightRange?.startLine ?: 1

        // styling
        val style = getStyling(classLoader)

        // components
        val heading = getHeading(params)
        val documentationSubHeader = getDocumentationSubHeader(fileName, lineNumber, standaloneDocumentation)

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
    private fun getHeading(params: HeadingParams): String {
        val (codeSmell, standaloneDocumentation, content, classLoader) = params

        val logoSvg = classLoader.getResourceAsStream(Constants.LOGO_PATH)?.bufferedReader()?.readText()

        return if (!standaloneDocumentation) "<h2>${codeSmell.category}</h2>" else {
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
    private fun getStyling(classLoader: ClassLoader): String {
        val styleStream = classLoader.getResourceAsStream(Constants.STYLE_BASE_PATH + "code-smell.css")
        val staticStyleBuilder = StringBuilder()

        val scrollbarStyle = JBCefScrollbarsHelper.buildScrollbarsStyle()
        val themeStyle = PreviewThemeStyles.createStylesheet()

        staticStyleBuilder.append(
            styleStream?.bufferedReader()?.lines()
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
    private fun getDocumentationSubHeader(fileName: String, lineNumber: Int, standaloneDocumentation: Boolean) =
        if (!standaloneDocumentation) {
            """
        |    <div class="documentation-header">
        |        <p>
        |            <span id="function-location">$fileName&nbsp;&nbsp;
        |                <span id="line-number">[Ln&nbsp;$lineNumber]</span>
        |            </span>
        |        </p>
        |    </div>
        """.trimMargin()
        } else ""

    /**
     * Method to create virtual file for our generated documentation content.
     */
    private fun createTempFile(name: String, content: String): VirtualFile {
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
        if (functionLocation?.codeSmell != null) {
            this.openDocumentationPanel(
                DocumentationParams(
                    sourceEditor,
                    functionLocation!!.codeSmell,
                    lastDocsSourceType
                )
            )
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

data class DocumentationParams(
    val editor: Editor?,
    val codeSmell: CodeSmell,
    val docsSourceType: DocsSourceType
)

data class HeadingParams(
    val codeSmell: CodeSmell,
    val standaloneDocumentation: Boolean,
    val content: String,
    val classLoader: ClassLoader,
    val editor: Editor? = null
)

data class TransformMarkdownParams(
    val originalContent: String,
    val codeSmellName: String,
    val standaloneDocumentation: Boolean = false
)

enum class DocsSourceType(val value: String) {
    INTENTION_ACTION("diagnostic-item"),
    CODE_HEALTH_DETAILS("code-health-details"),
    CODE_VISION("codelens (review)"),
    NONE("none")
}