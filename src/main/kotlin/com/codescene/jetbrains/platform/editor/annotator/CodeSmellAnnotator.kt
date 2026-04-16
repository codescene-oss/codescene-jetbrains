package com.codescene.jetbrains.platform.editor.annotator

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.models.CodeVisionCodeSmell
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.util.formatCodeSmellMessage
import com.codescene.jetbrains.core.util.getRefactorableFunction
import com.codescene.jetbrains.core.util.shouldAnnotateCodeSmells
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.editor.intentions.AceRefactorAction
import com.codescene.jetbrains.platform.editor.intentions.ShowProblemIntentionAction
import com.codescene.jetbrains.platform.util.AceEntryOrchestrator
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.getTextRange
import com.codescene.jetbrains.platform.util.isFileSupported
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NotNull

class CodeSmellAnnotator : ExternalAnnotator<
    CodeSmellAnnotator.AnnotationContext,
    CodeSmellAnnotator.AnnotationContext,
>() {
    override fun apply(
        @NotNull psiFile: PsiFile,
        annotationContext: AnnotationContext,
        @NotNull holder: AnnotationHolder,
    ) {
        if (!isRuntimeSafe(psiFile)) {
            return
        }

        if (!isFileSupported(psiFile.project, psiFile.virtualFile)) {
            Log.warn("File type not supported: ${psiFile.virtualFile.name}. Skipping code smell annotation.")
            return
        }

        annotateFile(psiFile, holder, annotationContext)
    }

    private fun annotateFile(
        psiFile: PsiFile,
        holder: AnnotationHolder,
        annotationContext: AnnotationContext,
    ) {
        val document =
            runSafeAction<Document?>(
                psiFile = psiFile,
                action = "read document for annotation",
                fallback = null,
            ) {
                FileDocumentManager.getInstance().getDocument(psiFile.virtualFile)
            } ?: return
        val review = annotationContext.reviewCache
        val ace = annotationContext.aceCache

        if (review != null) {
            val serviceProvider =
                runSafeAction<CodeSceneProjectServiceProvider?>(
                    psiFile = psiFile,
                    action = "resolve project services for annotation",
                    fallback = null,
                ) {
                    CodeSceneProjectServiceProvider.getInstance(psiFile.project)
                } ?: return
            Log.info("Annotating code smells for file: ${psiFile.name}")

            review.fileLevelCodeSmells.forEach {
                annotateCodeSmell(
                    CodeVisionCodeSmell(
                        details = it.details,
                        highlightRange = it.highlightRange,
                        category = it.category,
                    ),
                    document,
                    holder,
                    serviceProvider,
                    ace,
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
                                functionName = functionSmell.function,
                                functionRange = functionSmell.range,
                            )
                        }
                }
                .forEach { annotateCodeSmell(it, document, holder, serviceProvider, ace) }

            Log.info("Successfully annotated code smells for file: ${psiFile.name}")
        }
    }

    private fun annotateCodeSmell(
        codeSmell: CodeVisionCodeSmell,
        document: Document,
        holder: AnnotationHolder,
        serviceProvider: CodeSceneProjectServiceProvider,
        refactorableFunctions: List<FnToRefactor> = emptyList(),
    ) {
        val settings = serviceProvider.settingsProvider.currentState()
        val range = getTextRange(codeSmell.highlightRange.startLine to codeSmell.highlightRange.endLine, document)
        val message = formatCodeSmellMessage(codeSmell.category, codeSmell.details)

        Log.debug("Creating annotation for code smell '${codeSmell.category}' at range: $range")

        val aceAvailable =
            settings.enableAutoRefactor && settings.aceAuthToken.trim().isNotEmpty()
        val function =
            if (aceAvailable) {
                getRefactorableFunction(codeSmell.category, codeSmell.highlightRange.startLine, refactorableFunctions)
            } else {
                null
            }

        val annotationBuilder =
            holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(range)
                .highlightType(ProblemHighlightType.WARNING)
                .withFix(ShowProblemIntentionAction(codeSmell))

        function?.let { annotationBuilder.withFix(AceRefactorAction(function)) }

        annotationBuilder.create()
    }

    private fun fetchCache(
        psiFile: PsiFile,
        content: String,
    ): Review? {
        return runSafeAction<Review?>(
            psiFile = psiFile,
            action = "fetch review cache",
            fallback = null,
        ) {
            val path = psiFile.virtualFile.path
            val query = ReviewCacheQuery(content, path)
            CodeSceneProjectServiceProvider.getInstance(psiFile.project).reviewCacheService.get(query)
        }.also {
            if (it == null) {
                Log.info("No cache available for ${resolvePathForLogging(psiFile)}. Skipping annotation.")
            }
        }
    }

    private fun fetchAceCache(
        psiFile: PsiFile,
        content: String,
    ): List<FnToRefactor> {
        return runSafeAction<List<FnToRefactor>>(
            psiFile = psiFile,
            action = "fetch ACE cache",
            fallback = emptyList(),
        ) {
            val path = psiFile.virtualFile.path
            AceEntryOrchestrator.getInstance(psiFile.project).fetchAceCache(path, content)
        }
    }

    override fun collectInformation(
        @NotNull file: PsiFile,
    ): AnnotationContext? {
        val document =
            runSafeAction<Document?>(
                psiFile = file,
                action = "read document for collection",
                fallback = null,
            ) {
                FileDocumentManager.getInstance().getDocument(file.virtualFile)
            }

        val content =
            document?.text ?: run {
                Log.warn("No document found for file: ${file.name}. Skipping annotation.")

                return null
            }

        val cache = fetchCache(file, content)
        val aceCache = fetchAceCache(file, content)

        return AnnotationContext(cache, aceCache)
    }

    override fun doAnnotate(collectedInfo: AnnotationContext): AnnotationContext? =
        collectedInfo.takeIf { shouldAnnotateCodeSmells(it.reviewCache, it.aceCache) }

    private fun isRuntimeSafe(psiFile: PsiFile): Boolean {
        val application = ApplicationManager.getApplication()
        val project = psiFile.project
        if (application.isDisposed) return false
        if (project.isDisposed) return false
        if (!psiFile.isValid) return false

        val virtualFile = psiFile.virtualFile ?: return false

        return virtualFile.isValid
    }

    private fun resolvePathForLogging(psiFile: PsiFile): String {
        return if (isRuntimeSafe(psiFile)) {
            psiFile.virtualFile.path
        } else {
            psiFile.name
        }
    }

    private inline fun <T> runSafeAction(
        psiFile: PsiFile,
        action: String,
        fallback: T,
        block: () -> T,
    ): T {
        if (!isRuntimeSafe(psiFile)) {
            Log.warn("Skipping $action for ${psiFile.name}: project or file is no longer valid.")
            return fallback
        }

        return try {
            block()
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: LinkageError) {
            Log.warn("Skipping $action for ${psiFile.name}: ${e.message ?: e::class.java.simpleName}")
            fallback
        } catch (e: Exception) {
            Log.warn("Skipping $action for ${psiFile.name}: ${e.message ?: e::class.java.simpleName}")
            fallback
        }
    }

    class AnnotationContext(val reviewCache: Review?, val aceCache: List<FnToRefactor>)
}
