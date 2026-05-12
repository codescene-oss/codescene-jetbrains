package com.codescene.jetbrains.core.util

import java.time.Duration

fun resolveCliCacheFileName(
    filePath: String,
    repoRelativePath: String?,
): String {
    return filePath
}

fun resolveBaselineCliCacheFileName(
    filePath: String,
    repoRelativePath: String?,
    commitSha: String?,
): String {
    val currentFileName = resolveCliCacheFileName(filePath, repoRelativePath)
    val normalizedCommitSha = commitSha?.takeIf { it.isNotBlank() } ?: return currentFileName
    return "$normalizedCommitSha:$currentFileName"
}

fun isExpiredCliCacheEntry(
    lastModifiedMillis: Long,
    nowMillis: Long,
    maxAge: Duration,
): Boolean = nowMillis - lastModifiedMillis >= maxAge.toMillis()
