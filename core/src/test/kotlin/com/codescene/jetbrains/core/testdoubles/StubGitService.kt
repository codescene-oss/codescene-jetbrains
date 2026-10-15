package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IGitService

class StubGitService(
    private val repoRelativePathByPath: Map<String, String?> = emptyMap(),
    private val repoRootByPath: Map<String, String?> = emptyMap(),
    private val ignoredByPath: Map<String, Boolean> = emptyMap(),
    private val defaultRepoRelativePath: String? = null,
    private val defaultRepoRoot: String? = null,
    private val defaultIgnored: Boolean = false,
) : IGitService {
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
