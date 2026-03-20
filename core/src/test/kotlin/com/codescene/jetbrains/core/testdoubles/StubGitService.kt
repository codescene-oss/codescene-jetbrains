package com.codescene.jetbrains.core.testdoubles

import com.codescene.jetbrains.core.contracts.IGitService

class StubGitService(
    private val contentByPath: Map<String, String> = emptyMap(),
    private val defaultContent: String = "",
) : IGitService {
    override fun getBranchCreationCommitCode(filePath: String): String {
        return contentByPath[filePath] ?: defaultContent
    }
}
