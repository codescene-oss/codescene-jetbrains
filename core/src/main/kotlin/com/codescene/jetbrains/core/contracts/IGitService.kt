package com.codescene.jetbrains.core.contracts

interface IGitService {
    fun getBranchCreationCommitCode(filePath: String): String

    fun getBranchCreationCommitHash(filePath: String): String?

    fun getRepoRelativePath(filePath: String): String?

    fun isIgnored(filePath: String): Boolean
}
