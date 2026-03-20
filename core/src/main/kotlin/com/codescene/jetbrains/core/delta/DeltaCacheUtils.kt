package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.contracts.IDeltaCacheService
import com.codescene.jetbrains.core.contracts.IGitService

fun getCachedDelta(
    filePath: String,
    fileContent: String,
    gitService: IGitService,
    deltaCacheService: IDeltaCacheService,
): Pair<Boolean, Delta?> {
    val oldCode = gitService.getBranchCreationCommitCode(filePath)
    val cacheQuery = DeltaCacheQuery(filePath, oldCode, fileContent)

    return deltaCacheService.get(cacheQuery)
}
