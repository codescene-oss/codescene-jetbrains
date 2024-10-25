package com.codescene.jetbrains.codeInsight.annotator

import com.codescene.jetbrains.codeInsight.intentions.ShowProblemIntentionAction
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.services.CacheQuery
import com.codescene.jetbrains.services.ReviewCacheService
import com.codescene.jetbrains.util.formatCodeSmellMessage
import com.codescene.jetbrains.util.getTextRange
import com.codescene.jetbrains.util.isFileSupported
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull

class CodeSmellAnnotator : ExternalAnnotator<
        CodeSmellAnnotator.AnnotationContext, CodeSmellAnnotator.AnnotationContext
        >() {
    //TODO: refactor
    override fun apply(
        @NotNull psiFile: PsiFile,
        annotationResult: AnnotationContext,
        @NotNull holder: AnnotationHolder
    ) {
        val project = psiFile.project
        val fileName = psiFile.name
        val extension = psiFile.virtualFile.extension ?: return

        println("Triggering apply for $fileName")

        if (isFileSupported(extension)) {
            val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)

            if (document != null) {
                println("Found document for $fileName")

                val query = CacheQuery(document.text, psiFile.virtualFile.path)
                val cache =
                    ReviewCacheService.getInstance(project).getCachedResponse(query)

                if (cache != null) {
                    annotateCodeSmells(cache.fileLevelCodeSmells, document, holder)

                    cache.functionLevelCodeSmells.forEach { functionSmell ->
                        annotateCodeSmells(functionSmell.codeSmells, document, holder)
                    }

                    annotateCodeSmells(cache.expressionLevelCodeSmells, document, holder)

                    println("Annotated file $fileName successfully")
                }
            }
        }
    }

    override fun collectInformation(@NotNull file: PsiFile): AnnotationContext = AnnotationContext()

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext = collectedInfo

    private fun annotateCodeSmells(
        codeSmells: List<CodeSmell>,
        document: Document,
        holder: AnnotationHolder
    ) {
        codeSmells.forEach { codeSmell ->
            val validTextRange = getTextRange(codeSmell, document)

            getAnnotation(codeSmell, validTextRange, holder)
        }
    }

    private fun getAnnotation(
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