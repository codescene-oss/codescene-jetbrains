package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.delta.DeltaCacheQuery
import com.codescene.jetbrains.core.review.BaselineReviewCacheEntry
import com.codescene.jetbrains.core.review.BaselineReviewCacheQuery
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.review.resolveDeltaExecutionPlan
import com.codescene.jetbrains.core.review.resolveProgressMessage
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.Log
import com.codescene.jetbrains.platform.util.refreshAceFromDelta
import com.codescene.jetbrains.platform.util.refreshAceFromReview
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem

@Service(Service.Level.PROJECT)
class PathBasedReviewHandler(private val project: Project) {
    private val serviceProvider = CodeSceneProjectServiceProvider.getInstance(project)
    private val reviewService = project.service<CodeReviewService>()
    private val deltaService = project.service<CodeDeltaService>()

    companion object {
        fun getInstance(project: Project): PathBasedReviewHandler = project.service<PathBasedReviewHandler>()
    }

    suspend fun performCachedReviewByPath(
        filePath: String,
        fileName: String,
    ) {
        val startTime = System.currentTimeMillis()
        val file = LocalFileSystem.getInstance().findFileByPath(filePath)
        if (file == null) {
            Log.info("reviewByPath file not found path=$filePath", "CodeSceneCachedReview")
            return
        }

        val fileReadStart = System.currentTimeMillis()
        // Prefer document content (editor buffer) over VFS content (disk) when available.
        // This prevents Code Health Monitor flickering when a file has unsaved changes:
        // editor-triggered reviews use document.text while periodic polling would use disk content,
        // causing the cache to flip-flop between fixed/unfixed states until the file is saved.
        val currentCode =
            ReadAction.compute<String?, Exception> {
                if (!file.isValid) {
                    return@compute null
                }
                val document: Document? = FileDocumentManager.getInstance().getDocument(file)
                if (document != null) {
                    Log.info("reviewByPath using document content file=$fileName", "CodeSceneCachedReview")
                    document.text
                } else {
                    Log.info("reviewByPath falling back to VFS content file=$fileName", "CodeSceneCachedReview")
                    String(file.contentsToByteArray(), file.charset)
                }
            }
        if (currentCode == null) {
            Log.info("reviewByPath file became invalid path=$filePath", "CodeSceneCachedReview")
            return
        }
        Log.info(
            "reviewByPath fileRead took ${System.currentTimeMillis() - fileReadStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )

        val cacheCheckStart = System.currentTimeMillis()
        val cachedReview = serviceProvider.reviewCacheService.get(ReviewCacheQuery(currentCode, filePath))
        Log.info(
            "reviewByPath cacheCheck took ${System.currentTimeMillis() - cacheCheckStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        if (cachedReview != null) {
            val baselineCode = serviceProvider.gitService.getBranchCreationCommitCode(filePath)
            val deltaQuery = DeltaCacheQuery(filePath, baselineCode, currentCode)
            val (deltaHit, _) = serviceProvider.deltaCacheService.get(deltaQuery)

            if (deltaHit) {
                Log.info(
                    "reviewByPath full cache hit path=$filePath totalTime=${System.currentTimeMillis() - startTime}ms",
                    "CodeSceneCachedReview",
                )
                return
            }

            Log.info(
                "reviewByPath review cache hit, delta miss path=$filePath",
                "CodeSceneCachedReview",
            )
            handleDeltaByPath(filePath, fileName, currentCode, cachedReview.score.orElse(null), reviewMiss = false)
            return
        }

        val reviewStart = System.currentTimeMillis()
        val progressMessage = resolveProgressMessage(fileName, true)
        val review =
            serviceProvider.progressService.runWithProgress(progressMessage) {
                reviewService.performCodeReviewByPath(filePath, fileName, currentCode)
            }
        Log.info(
            "reviewByPath review took ${System.currentTimeMillis() - reviewStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        if (review == null) {
            Log.info("reviewByPath no result path=$filePath", "CodeSceneCachedReview")
            return
        }

        refreshAceFromReview(project, filePath, fileName, currentCode, review)

        Log.info(
            "reviewByPath completed path=$filePath totalTime=${System.currentTimeMillis() - startTime}ms",
            "CodeSceneCachedReview",
        )
        handleDeltaByPath(filePath, fileName, currentCode, review.score.orElse(null), reviewMiss = true)
    }

    private suspend fun handleDeltaByPath(
        filePath: String,
        fileName: String,
        currentCode: String,
        currentScore: Double?,
        reviewMiss: Boolean,
    ) {
        val normalizedPath = normalizeAbsolutePath(filePath)
        val startTime = System.currentTimeMillis()
        val gitStart = System.currentTimeMillis()
        val baselineCode = serviceProvider.gitService.getBranchCreationCommitCode(normalizedPath)
        val gitElapsed = System.currentTimeMillis() - gitStart
        Log.info(
            "handleDeltaByPath gitBaseline took ${gitElapsed}ms file=$fileName baselineLen=${baselineCode.length}",
            "CodeSceneCachedReview",
        )

        val baselineScoreStart = System.currentTimeMillis()
        val baselineScore = getBaselineScore(filePath, fileName, baselineCode)
        Log.info(
            "handleDeltaByPath baselineScore took ${System.currentTimeMillis() - baselineScoreStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )

        val plan = resolveDeltaExecutionPlan(baselineCode, currentCode, currentScore, baselineScore)

        if (plan.shouldCacheEmptyDelta) {
            serviceProvider.deltaCacheService.put(
                DeltaCacheEntry(
                    filePath = normalizedPath,
                    headContent = baselineCode,
                    currentFileContent = currentCode,
                    deltaApiResponse = null,
                ),
            )
            Log.info(
                "handleDeltaByPath cachedEmpty totalTime=${System.currentTimeMillis() - startTime}ms file=$fileName",
                "CodeSceneCachedReview",
            )
            return
        }

        val deltaCacheStart = System.currentTimeMillis()
        val query = DeltaCacheQuery(normalizedPath, baselineCode, currentCode)
        val (deltaHit, _) = serviceProvider.deltaCacheService.get(query)
        Log.info(
            "handleDeltaByPath deltaCacheCheck took ${System.currentTimeMillis() - deltaCacheStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        if (deltaHit) {
            Log.info(
                "handleDeltaByPath cache hit file=$fileName totalTime=${System.currentTimeMillis() - startTime}ms",
                "CodeSceneCachedReview",
            )
            return
        }
        Log.info("handleDeltaByPath cache miss file=$fileName", "CodeSceneCachedReview")

        val deltaStart = System.currentTimeMillis()
        val deltaProgressMessage = resolveProgressMessage(fileName, false)
        serviceProvider.progressService.runWithProgress(deltaProgressMessage) {
            deltaService.performDeltaAnalysisByPath(normalizedPath, fileName, currentCode)
        }
        Log.info(
            "handleDeltaByPath deltaAnalysis took ${System.currentTimeMillis() - deltaStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        if (!reviewMiss) {
            val query = DeltaCacheQuery(normalizedPath, baselineCode, currentCode)
            val (_, delta) = serviceProvider.deltaCacheService.get(query)
            if (delta != null) {
                refreshAceFromDelta(project, filePath, fileName, currentCode, delta)
            }
        }
        Log.info(
            "handleDeltaByPath done file=$fileName totalTime=${System.currentTimeMillis() - startTime}ms",
            "CodeSceneCachedReview",
        )
    }

    private suspend fun getBaselineScore(
        path: String,
        fileName: String,
        baselineCode: String,
    ): Double? {
        if (baselineCode.isBlank()) {
            Log.info("getBaselineScore skipped (blank baseline) file=$fileName", "CodeSceneCachedReview")
            return null
        }

        val cacheStart = System.currentTimeMillis()
        val query = BaselineReviewCacheQuery(fileContents = baselineCode, filePath = path)
        val (found, score) = serviceProvider.baselineReviewCacheService.get(query)
        Log.info(
            "getBaselineScore cacheCheck took ${System.currentTimeMillis() - cacheStart}ms hit=$found file=$fileName",
            "CodeSceneCachedReview",
        )
        if (found) {
            return score
        }

        val reviewStart = System.currentTimeMillis()
        val baselineProgressMessage = resolveProgressMessage(fileName, true)
        val baselineScore =
            serviceProvider.progressService.runWithProgress(baselineProgressMessage) {
                reviewService.reviewBaseline(path, fileName, baselineCode)
            }
        Log.info(
            "getBaselineScore reviewBaseline took ${System.currentTimeMillis() - reviewStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        serviceProvider.baselineReviewCacheService.put(
            BaselineReviewCacheEntry(
                fileContents = baselineCode,
                filePath = path,
                score = baselineScore,
            ),
        )
        return baselineScore
    }
}
