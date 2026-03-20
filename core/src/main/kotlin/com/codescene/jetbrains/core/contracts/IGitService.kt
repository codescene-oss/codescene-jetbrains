package com.codescene.jetbrains.core.contracts

interface IGitService {
    fun getBranchCreationCommitCode(filePath: String): String
}
