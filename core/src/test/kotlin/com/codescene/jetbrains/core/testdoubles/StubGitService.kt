package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IGitService

class StubGitService(
    private val contentByPath: Map<String, String> = emptyMap(),
    private val commitHashByPath: Map<String, String?> = emptyMap(),
    private val repoRelativePathByPath: Map<String, String?> = emptyMap(),
    private val defaultContent: String = "",
    private val defaultCommitHash: String? = null,
    private val defaultRepoRelativePath: String? = null,
) : IGitService {
    override fun getBranchCreationCommitCode(filePath: String): String {
        return contentByPath[filePath] ?: defaultContent
    }

    override fun getBranchCreationCommitHash(filePath: String): String? {
        return commitHashByPath[filePath] ?: defaultCommitHash
    }

    override fun getRepoRelativePath(filePath: String): String? {
        return repoRelativePathByPath[filePath] ?: defaultRepoRelativePath
    }
}
