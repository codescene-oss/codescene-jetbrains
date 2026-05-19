package com.codescene.jetbrains.platform.util

import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.review.shouldCheckRefactorableFunctions
import com.codescene.jetbrains.core.util.extractExtension
import com.codescene.jetbrains.core.util.normalizeAbsolutePath
import com.codescene.jetbrains.core.util.resolveCliCacheFileName
import com.codescene.jetbrains.platform.api.AceService
import com.codescene.jetbrains.platform.api.RefactorableFunctionsSource
import com.codescene.jetbrains.platform.di.CodeSceneApplicationServiceProvider
import com.codescene.jetbrains.platform.di.CodeSceneProjectServiceProvider
import com.intellij.openapi.project.Project

suspend fun refreshAceFromReview(
    project: Project,
    filePath: String,
    fileName: String,
    currentCode: String,
    review: Review,
): Boolean {
    val fileExtension = extractExtension(fileName)
    val appServices = CodeSceneApplicationServiceProvider.getInstance()
    if (!shouldCheckRefactorableFunctions(appServices.settingsProvider, appServices.aceService, fileExtension)) {
        return false
    }
    val services = CodeSceneProjectServiceProvider.getInstance(project)
    val normalizedPath = normalizeAbsolutePath(filePath)
    val cliFileName =
        resolveCliCacheFileName(normalizedPath, services.gitService.getRepoRelativePath(normalizedPath))
    val params = CodeParams(currentCode, cliFileName)
    val cacheParams = CacheParams(services.cliCacheService.getCachePath())
    return AceService.getInstance().getRefactorableFunctions(
        project,
        normalizedPath,
        currentCode,
        params,
        cacheParams,
        RefactorableFunctionsSource.FromReview(review),
    )
}

suspend fun refreshAceFromDelta(
    project: Project,
    filePath: String,
    fileName: String,
    currentCode: String,
    delta: Delta,
): Boolean {
    val fileExtension = extractExtension(fileName)
    val appServices = CodeSceneApplicationServiceProvider.getInstance()
    if (!shouldCheckRefactorableFunctions(appServices.settingsProvider, appServices.aceService, fileExtension)) {
        return false
    }
    val services = CodeSceneProjectServiceProvider.getInstance(project)
    val normalizedPath = normalizeAbsolutePath(filePath)
    val cliFileName =
        resolveCliCacheFileName(normalizedPath, services.gitService.getRepoRelativePath(normalizedPath))
    val params = CodeParams(currentCode, cliFileName)
    val cacheParams = CacheParams(services.cliCacheService.getCachePath())
    return AceService.getInstance().getRefactorableFunctions(
        project,
        normalizedPath,
        currentCode,
        params,
        cacheParams,
        RefactorableFunctionsSource.FromDelta(delta),
    )
}
