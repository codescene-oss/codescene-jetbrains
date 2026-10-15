package com.codescene.jetbrains.core.contracts

interface IGitService {
    fun getRepoRelativePath(filePath: String): String?

    fun getRepoRoot(filePath: String): String?

    fun isIgnored(filePath: String): Boolean
}
