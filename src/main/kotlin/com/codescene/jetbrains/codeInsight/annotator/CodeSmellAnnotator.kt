package com.codescene.jetbrains.codeInsight.annotator

import com.codescene.jetbrains.codeInsight.CodeSmell
import com.codescene.jetbrains.codeInsight.codeAnalysisResult
import com.codescene.jetbrains.codeInsight.intentions.ShowProblemIntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull

val extToLanguageId = mapOf(
    "js" to "javascript",
    "mjs" to "javascript",
    "sj" to "javascript",
    "jsx" to "javascriptreact",
    "ts" to "typescript",
    "tsx" to "typescriptreact",
    "brs" to "brightscript",
    "bs" to "brighterscript",
    "cls" to "apex",
    "tgr" to "apex",
    "trigger" to "apex",
    "c" to "c",
    "clj" to "clojure",
    "cljc" to "clojure",
    "cljs" to "clojure",
    "cc" to "cpp",
    "cpp" to "cpp",
    "cxx" to "cpp",
    "h" to "cpp",
    "hh" to "cpp",
    "hpp" to "cpp",
    "hxx" to "cpp",
    "ipp" to "cpp",
    "m" to "objective-c",
    "mm" to listOf("objective-c", "objective-cpp"),
    "cs" to "csharp",
    "erl" to "erlang",
    "go" to "go",
    "groovy" to "groovy",
    "java" to "java",
    "kt" to "kotlin",
    "php" to "php",
    "pm" to listOf("perl", "perl6"),
    "pl" to listOf("perl", "perl6"),
    "ps1" to "powershell",
    "psd1" to "powershell",
    "psm1" to "powershell",
    "py" to "python",
    "rb" to "ruby",
    "rs" to "rust",
    "swift" to "swift",
    "vb" to "vb",
    "vue" to "vue",
    "dart" to "dart",
    "scala" to "scala"
)

class CodeSmellAnnotator : ExternalAnnotator<
        CodeSmellAnnotator.AnnotationContext, CodeSmellAnnotator.AnnotationContext
        >() {
    override fun apply(
        @NotNull psiFile: PsiFile,
        annotationResult: AnnotationContext,
        @NotNull holder: AnnotationHolder
    ) {
        val fileExtension = psiFile.virtualFile.extension ?: return

        if (extToLanguageId.containsKey(fileExtension)) {
            val editor = FileEditorManager.getInstance(psiFile.project).selectedTextEditor ?: return

            annotateCodeSmells(codeAnalysisResult.fileLevelCodeSmells, editor, holder)

            codeAnalysisResult.functionLevelCodeSmells.forEach { functionSmell ->
                annotateCodeSmells(functionSmell.codeSmells, editor, holder)
            }

            annotateCodeSmells(codeAnalysisResult.expressionLevelCodeSmells, editor, holder)
        }
    }

    override fun collectInformation(@NotNull file: PsiFile): AnnotationContext = AnnotationContext()

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext = collectedInfo

    private fun formatCodeSmellMessage(category: String, details: String): String =
        if (details.isNotEmpty()) "$category ($details)" else category

    private fun annotateCodeSmells(
        codeSmells: List<CodeSmell>,
        editor: Editor,
        holder: AnnotationHolder
    ) {
        codeSmells.forEach { codeSmell ->
            val validTextRange = getTextRange(codeSmell, editor)

            addAnnotation(codeSmell, validTextRange, holder)
        }
    }

    private fun getTextRange(codeSmell: CodeSmell, editor: Editor): TextRange {
        val start = editor.document.getLineStartOffset(codeSmell.range.startLine - 1)
        val end = editor.document.getLineEndOffset(codeSmell.range.endLine - 1)

        return TextRange(start, end)
    }

    private fun addAnnotation(
        codeSmell: CodeSmell,
        validTextRange: TextRange,
        annotationHolder: AnnotationHolder
    ) {
        val message = formatCodeSmellMessage(codeSmell.category, codeSmell.details)

        return annotationHolder
            .newAnnotation(HighlightSeverity.WARNING, message)
            .range(validTextRange)
            .highlightType(ProblemHighlightType.GENERIC_ERROR_OR_WARNING)
            .withFix(ShowProblemIntentionAction(codeSmell.category))
            .create()
    }

    class AnnotationContext
}