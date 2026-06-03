package com.codescene.jetbrains.core.git

import java.io.File

fun mainLineContextFromTestRepo(testRepoPath: File): MainLineBranchContext {
    val configFile = File(testRepoPath, ".codescene/config.json")
    val configured =
        if (configFile.isFile) {
            parseBaselineBranchFromConfig(configFile.readText())
        } else {
            null
        }
    val originDefault = readOriginHeadBranch(testRepoPath)
    val localBranches = readLocalBranchNames(testRepoPath)
    return MainLineBranchContext(
        configuredBaseline = configured,
        defaultBranchFromOriginHead = originDefault,
        localBranchNames = localBranches,
    )
}

private fun readOriginHeadBranch(testRepoPath: File): String? {
    val output =
        runGit(testRepoPath, "git", "symbolic-ref", "refs/remotes/origin/HEAD").trim()
    if (output.isEmpty() || output.startsWith("fatal")) return null
    val prefix = "refs/remotes/origin/"
    return if (output.startsWith(prefix)) {
        output.removePrefix(prefix)
    } else {
        null
    }
}

private fun readLocalBranchNames(testRepoPath: File): Set<String> {
    val output = runGit(testRepoPath, "git", "for-each-ref", "--format=%(refname:short)", "refs/heads")
    return output
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .toSet()
}

private fun runGit(
    testRepoPath: File,
    vararg command: String,
): String {
    val process =
        ProcessBuilder(*command)
            .directory(testRepoPath)
            .redirectErrorStream(true)
            .start()
    val result = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return result
}
