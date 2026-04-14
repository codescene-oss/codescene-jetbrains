package com.codescene.jetbrains.platform.git

import git4idea.repo.GitRepository

interface GitCommandExecutor {
    fun runDiff(
        repository: GitRepository,
        baseCommit: String,
    ): List<String>

    fun runRevParse(repository: GitRepository): String?

    fun runMergeBase(
        repository: GitRepository,
        rev1: String,
        rev2: String,
    ): String?
}
