package com.codescene.jetbrains.platform.api

import com.codescene.jetbrains.core.delta.DeltaCacheEntry
import com.codescene.jetbrains.core.delta.DeltaCacheQuery
import com.codescene.jetbrains.core.review.BaselineReviewCacheEntry
import com.codescene.jetbrains.core.review.BaselineReviewCacheQuery
import com.codescene.jetbrains.core.review.ReviewCacheQuery
import com.codescene.jetbrains.core.review.resolveDeltaExecutionPlan
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.charset.Charset

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
        val fileData =
            ReadAction.compute<Pair<ByteArray, Charset>?, Exception> {
                if (!file.isValid) {
                    return@compute null
                }
                Pair(file.contentsToByteArray(), file.charset)
            }
        if (fileData == null) {
            Log.info("reviewByPath file became invalid path=$filePath", "CodeSceneCachedReview")
            return
        }
        val currentCode = String(fileData.first, fileData.second)
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
            Log.info(
                "reviewByPath cache hit path=$filePath totalTime=${System.currentTimeMillis() - startTime}ms",
                "CodeSceneCachedReview",
            )
            handleDeltaByPath(filePath, fileName, currentCode, cachedReview.score.orElse(null))
            return
        }

        val reviewStart = System.currentTimeMillis()
        val review = reviewService.performCodeReviewByPath(filePath, fileName, currentCode)
        Log.info(
            "reviewByPath review took ${System.currentTimeMillis() - reviewStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        if (review == null) {
            Log.info("reviewByPath no result path=$filePath", "CodeSceneCachedReview")
            return
        }

        Log.info(
            "reviewByPath completed path=$filePath totalTime=${System.currentTimeMillis() - startTime}ms",
            "CodeSceneCachedReview",
        )
        handleDeltaByPath(filePath, fileName, currentCode, review.score.orElse(null))
    }

    private suspend fun handleDeltaByPath(
        filePath: String,
        fileName: String,
        currentCode: String,
        currentScore: Double?,
    ) {
        val startTime = System.currentTimeMillis()
        val gitStart = System.currentTimeMillis()
        val baselineCode = serviceProvider.gitService.getBranchCreationCommitCode(filePath)
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
                    filePath = filePath,
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

        if (!plan.shouldRunDelta) {
            Log.info(
                "handleDeltaByPath skipDelta totalTime=${System.currentTimeMillis() - startTime}ms file=$fileName",
                "CodeSceneCachedReview",
            )
            return
        }

        val deltaCacheStart = System.currentTimeMillis()
        val query = DeltaCacheQuery(filePath, baselineCode, currentCode)
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
            serviceProvider.deltaCacheService.setIncludeInCodeHealthMonitor(filePath, false)
            return
        }
        Log.info("handleDeltaByPath cache miss file=$fileName", "CodeSceneCachedReview")

        val deltaStart = System.currentTimeMillis()
        deltaService.performDeltaAnalysisByPath(filePath, fileName, currentCode)
        Log.info(
            "handleDeltaByPath deltaAnalysis took ${System.currentTimeMillis() - deltaStart}ms file=$fileName",
            "CodeSceneCachedReview",
        )
        serviceProvider.deltaCacheService.setIncludeInCodeHealthMonitor(filePath, true)
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
        val baselineScore = reviewService.reviewBaseline(path, fileName, baselineCode)
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
