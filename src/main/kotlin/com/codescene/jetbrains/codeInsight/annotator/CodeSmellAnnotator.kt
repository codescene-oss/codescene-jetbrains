package com.codescene.jetbrains.codeInsight.annotator

import com.codescene.jetbrains.codeInsight.intentions.ShowProblemIntentionAction
import com.codescene.jetbrains.data.ApiResponse
import com.codescene.jetbrains.data.CodeSmell
import com.codescene.jetbrains.services.CacheQuery
import com.codescene.jetbrains.services.ReviewCacheService
import com.codescene.jetbrains.util.Log
import com.codescene.jetbrains.util.formatCodeSmellMessage
import com.codescene.jetbrains.util.getTextRange
import com.codescene.jetbrains.util.isFileSupported
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull

class CodeSmellAnnotator : ExternalAnnotator<
        CodeSmellAnnotator.AnnotationContext, CodeSmellAnnotator.AnnotationContext
        >() {
    override fun apply(
        @NotNull psiFile: PsiFile,
        annotationResult: AnnotationContext,
        @NotNull holder: AnnotationHolder
    ) {
        val extension = psiFile.virtualFile.extension ?: return

        if (!isFileSupported(extension)) {
            Log.warn("File type not supported for annotation: $psiFile.name. Skipping annotation.")

            return
        }

        annotateFile(psiFile, holder)
    }

    private fun annotateFile(psiFile: PsiFile, holder: AnnotationHolder) {
        val fileName = psiFile.name

        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile) ?: run {
            Log.warn("No document found for file: $fileName. Skipping annotation.")
            return
        }

        Log.debug("Document found for file: $fileName")

        fetchCache(psiFile, document.text)?.let { cache ->
            Log.info("Annotating code smells for file: ${fileName}")

            cache.fileLevelCodeSmells.forEach { annotateCodeSmell(it, document, holder) }
            cache.functionLevelCodeSmells.flatMap { it.codeSmells }.forEach { annotateCodeSmell(it, document, holder) }
            cache.expressionLevelCodeSmells.forEach { annotateCodeSmell(it, document, holder) }

            Log.info("Successfully annotated code smells for file: $fileName")
        }
    }

    private fun annotateCodeSmell(codeSmell: CodeSmell, document: Document, holder: AnnotationHolder) {
        getTextRange(codeSmell, document).let { range ->
            val message = formatCodeSmellMessage(codeSmell.category, codeSmell.details)

            Log.debug("Creating annotation for code smell '${codeSmell.category}' at range: $range")

            holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(range)
                .highlightType(ProblemHighlightType.WARNING)
                .withFix(ShowProblemIntentionAction(codeSmell.category))
                .create()
        }
    }

    private fun fetchCache(psiFile: PsiFile, content: String): ApiResponse? {
        val path = psiFile.virtualFile.path
        val query = CacheQuery(content, path)

        return ReviewCacheService.getInstance(psiFile.project).getCachedResponse(query).also {
            if (it == null) Log.info("No cache available for ${path}. Skipping annotation.")
        }
    }

    override fun collectInformation(@NotNull file: PsiFile): AnnotationContext = AnnotationContext()

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext = collectedInfo

    class AnnotationContext
}