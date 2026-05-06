package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IGitService

class StubGitService(
    private val contentByPath: Map<String, String> = emptyMap(),
    private val commitHashByPath: Map<String, String?> = emptyMap(),
    private val repoRelativePathByPath: Map<String, String?> = emptyMap(),
    private val repoRootByPath: Map<String, String?> = emptyMap(),
    private val ignoredByPath: Map<String, Boolean> = emptyMap(),
    private val defaultContent: String = "",
    private val defaultCommitHash: String? = null,
    private val defaultRepoRelativePath: String? = null,
    private val defaultRepoRoot: String? = null,
    private val defaultIgnored: Boolean = false,
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

    override fun getRepoRoot(filePath: String): String? {
        return repoRootByPath[filePath] ?: defaultRepoRoot
    }

    override fun isIgnored(filePath: String): Boolean {
        return ignoredByPath[filePath] ?: defaultIgnored
    }
}
