package com.codescene.jetbrains.codeInsight.annotator

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.CodeSmell
import com.codescene.data.review.Review
import com.codescene.jetbrains.codeInsight.intentions.ACERefactorAction
import com.codescene.jetbrains.codeInsight.intentions.ShowProblemIntentionAction
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionCacheQuery
import com.codescene.jetbrains.services.cache.AceRefactorableFunctionsCacheService
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
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
        annotationContext: AnnotationContext,
        @NotNull holder: AnnotationHolder
    ) {
        if (!isFileSupported(psiFile.project, psiFile.virtualFile)) {
            Log.warn("File type not supported: ${psiFile.virtualFile.name}. Skipping code smell annotation.")
            return
        }

        annotateFile(psiFile, holder, annotationContext)
    }

    private fun annotateFile(
        psiFile: PsiFile, holder: AnnotationHolder, annotationContext: AnnotationContext
    ) {
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile) ?: return
        val review = annotationContext.reviewCache
        val ace = annotationContext.aceCache
        if (review != null) {
            Log.info("Annotating code smells for file: ${psiFile.name}")

            review.fileLevelCodeSmells.forEach { annotateCodeSmell(it, document, holder) }
            review.functionLevelCodeSmells.flatMap { it.codeSmells }
                .forEach { annotateCodeSmell(it, document, holder) }

            Log.info("Successfully annotated code smells for file: ${psiFile.name}")
        }

        ace.forEach { annotateAce(it, document, holder) }
    }

    //TODO: refactor
    private fun annotateAce(function: FnToRefactor, document: Document, holder: AnnotationHolder) {
        getTextRange(function.range.startLine to function.range.startLine+1, document).let { range ->
            val message = "CodeScene ACE"

            Log.debug("Creating annotation for ACE on function '${function.name}' at range: $range")

            holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(range)
                .highlightType(ProblemHighlightType.WARNING)
                .withFix(ACERefactorAction())
                .create()
        }
    }

    private fun annotateCodeSmell(codeSmell: CodeSmell, document: Document, holder: AnnotationHolder) {
        getTextRange(codeSmell.highlightRange.startLine to codeSmell.highlightRange.endLine, document).let { range ->
            val message = formatCodeSmellMessage(codeSmell.category, codeSmell.details)

            Log.debug("Creating annotation for code smell '${codeSmell.category}' at range: $range")

            holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(range)
                .highlightType(ProblemHighlightType.WARNING)
                .withFix(ShowProblemIntentionAction(codeSmell))
                .create()
        }
    }

    private fun fetchCache(psiFile: PsiFile, content: String): Review? {
        val path = psiFile.virtualFile.path
        val query = ReviewCacheQuery(content, path)

        return ReviewCacheService.getInstance(psiFile.project).get(query).also {
            if (it == null) Log.info("No cache available for ${path}. Skipping annotation.")
        }
    }

    //TODO: refactor
    private fun fetchAceCache(psiFile: PsiFile, content: String): List<FnToRefactor> {
        val path = psiFile.virtualFile.path
        val query = AceRefactorableFunctionCacheQuery(path, content)

        return AceRefactorableFunctionsCacheService.getInstance(psiFile.project).get(query).also {
            if (it.isEmpty()) Log.info("No cache available for ${path}. Skipping annotation.")
        }
    }

    override fun collectInformation(@NotNull file: PsiFile): AnnotationContext? {
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)

        val content = document?.text ?: run {
            Log.warn("No document found for file: ${file.name}. Skipping annotation.")

            return null
        }

        val cache = fetchCache(file, content)
        val aceCache = fetchAceCache(file, content)

        return AnnotationContext(cache, aceCache)
    }

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext? =
        collectedInfo.takeIf { it.reviewCache != null || it.aceCache.isNotEmpty() }

    class AnnotationContext(val reviewCache: Review?, val aceCache: List<FnToRefactor>)
}