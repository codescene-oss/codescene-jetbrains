package com.codescene.jetbrains.codeInsight.annotator

import com.codescene.jetbrains.codeInsight.intentions.ShowProblemIntentionAction
import com.codescene.jetbrains.config.global.CodeSceneGlobalSettingsStore
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
        annotationContext: AnnotationContext,
        @NotNull holder: AnnotationHolder
    ) {
        val settings = CodeSceneGlobalSettingsStore.getInstance().state

        if (!isFileSupported(psiFile.project, psiFile.virtualFile, settings.excludeGitignoreFiles)) {
            Log.warn("File type not supported: ${psiFile.virtualFile.name}. Skipping code smell annotation.")
            return
        }

        annotateFile(psiFile, holder, annotationContext.cache)
    }

    private fun annotateFile(psiFile: PsiFile, holder: AnnotationHolder, reviewCache: ApiResponse?) {
        val document = FileDocumentManager.getInstance().getDocument(psiFile.virtualFile) ?: return

        if (reviewCache != null) {
            Log.info("Annotating code smells for file: ${psiFile.name}")

            reviewCache.fileLevelCodeSmells.forEach { annotateCodeSmell(it, document, holder) }
            reviewCache.functionLevelCodeSmells.flatMap { it.codeSmells }
                .forEach { annotateCodeSmell(it, document, holder) }
            reviewCache.expressionLevelCodeSmells.forEach { annotateCodeSmell(it, document, holder) }

            Log.info("Successfully annotated code smells for file: ${psiFile.name}")
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

    override fun collectInformation(@NotNull file: PsiFile): AnnotationContext? {
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)

        val content = document?.text ?: run {
            Log.warn("No document found for file: ${file.name}. Skipping annotation.")

            return null
        }

        val cache = fetchCache(file, content)

        return AnnotationContext(cache)
    }

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext? =
        collectedInfo.takeIf { it.cache != null }

    class AnnotationContext(val cache: ApiResponse?)
}
