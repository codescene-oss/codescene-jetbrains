package com.codescene.jetbrains.core.delta

import com.codescene.data.delta.Delta
import com.codescene.jetbrains.core.contracts.IDeltaCacheService

fun getCachedDelta(
    filePath: String,
    fileContent: String,
    deltaCacheService: IDeltaCacheService,
): Pair<Boolean, Delta?> = deltaCacheService.getByCurrentContent(filePath, fileContent)
