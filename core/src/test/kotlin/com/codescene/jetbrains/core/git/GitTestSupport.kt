package com.codescene.jetbrains.core.git

import java.io.File

object GitTestSupport {
    fun exec(
        testRepoPath: File,
        vararg command: String,
    ): String {
        val process =
            ProcessBuilder(*command)
                .directory(testRepoPath)
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }

    fun initGitRepo(testRepoPath: File) {
        exec(testRepoPath, "git", "init")
        exec(testRepoPath, "git", "config", "user.email", "test@example.com")
        exec(testRepoPath, "git", "config", "user.name", "Test User")
        exec(testRepoPath, "git", "config", "advice.defaultBranchName", "false")
        File(testRepoPath, "README.md").writeText("# Test Repository")
        exec(testRepoPath, "git", "add", "README.md")
        exec(testRepoPath, "git", "commit", "-m", "Initial commit")
    }

    fun initGitRepoWithFeatureBranch(
        testRepoPath: File,
        branchName: String = "test-feature-branch",
    ) {
        initGitRepo(testRepoPath)
        exec(testRepoPath, "git", "checkout", "-b", branchName)
    }

    fun initGitRepoWithExtraFiles(testRepoPath: File) {
        exec(testRepoPath, "git", "init")
        exec(testRepoPath, "git", "config", "user.email", "test@example.com")
        exec(testRepoPath, "git", "config", "user.name", "Test User")
        exec(testRepoPath, "git", "config", "advice.defaultBranchName", "false")
        File(testRepoPath, "README.ts").writeText("// Test Repository")
        File(testRepoPath, "existing.ts").writeText("export const foo = 1;")
        File(testRepoPath, "to-rename.js").writeText("console.log(\"original\");")
        File(testRepoPath, "to-modify.py").writeText("print(\"original\")")
        exec(testRepoPath, "git", "add", ".")
        exec(testRepoPath, "git", "commit", "-m", "Initial commit")
    }
}
