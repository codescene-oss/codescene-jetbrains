package com.codescene.jetbrains.codeInsight.annotator

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import com.codescene.jetbrains.codeInsight.codeVision.CodeVisionCodeSmell
import com.codescene.jetbrains.codeInsight.intentions.AceRefactorAction
import com.codescene.jetbrains.codeInsight.intentions.ShowProblemIntentionAction
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
import com.codescene.jetbrains.services.cache.ReviewCacheQuery
import com.codescene.jetbrains.services.cache.ReviewCacheService
import com.codescene.jetbrains.util.*
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

            review.fileLevelCodeSmells.forEach {
                annotateCodeSmell(
                    CodeVisionCodeSmell(
                        details = it.details,
                        highlightRange = it.highlightRange,
                        category = it.category
                    ), document, holder, ace
                )
            }
            review.functionLevelCodeSmells
                .flatMap { functionSmell ->
                    functionSmell.codeSmells
                        .map { codeSmell ->
                            CodeVisionCodeSmell(
                                details = codeSmell.details,
                                category = codeSmell.category,
                                highlightRange = codeSmell.highlightRange,
                                functionName = functionSmell.function
                            )
                        }
                }
                .forEach { annotateCodeSmell(it, document, holder, ace) }

            Log.info("Successfully annotated code smells for file: ${psiFile.name}")
        }
    }

    private fun annotateCodeSmell(
        codeSmell: CodeVisionCodeSmell,
        document: Document,
        holder: AnnotationHolder,
        refactorableFunctions: List<FnToRefactor> = emptyList()
    ) {
        val settings = CodeSceneGlobalSettingsStore.getInstance().state
        val range = getTextRange(codeSmell.highlightRange.startLine to codeSmell.highlightRange.endLine, document)
        val message = formatCodeSmellMessage(codeSmell.category, codeSmell.details)

        Log.debug("Creating annotation for code smell '${codeSmell.category}' at range: $range")

        val function =
            if (settings.aceEnabled && settings.enableAutoRefactor)
                getRefactorableFunction(codeSmell, refactorableFunctions)
            else null

        val annotationBuilder = holder.newAnnotation(HighlightSeverity.WARNING, message)
            .range(range)
            .highlightType(ProblemHighlightType.WARNING)
            .withFix(ShowProblemIntentionAction(codeSmell))

        function?.let { annotationBuilder.withFix(AceRefactorAction(function)) }

        annotationBuilder.create()
    }

    private fun fetchCache(psiFile: PsiFile, content: String): Review? {
        val path = psiFile.virtualFile.path
        val query = ReviewCacheQuery(content, path)

        return ReviewCacheService.getInstance(psiFile.project).get(query).also {
            if (it == null) Log.info("No cache available for ${path}. Skipping annotation.")
        }
    }

    override fun collectInformation(@NotNull file: PsiFile): AnnotationContext? {
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)

        val content = document?.text ?: run {
            Log.warn("No document found for file: ${file.name}. Skipping annotation.")

            return null
        }

        val cache = fetchCache(file, content)
        val aceCache = fetchAceCache(file.virtualFile.path, content, file.project)

        return AnnotationContext(cache, aceCache)
    }

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext? =
        collectedInfo.takeIf { it.reviewCache != null || it.aceCache.isNotEmpty() }

    class AnnotationContext(val reviewCache: Review?, val aceCache: List<FnToRefactor>)
}