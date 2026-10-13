package com.codescene.jetbrains.core.cli

import java.nio.file.Files
import java.nio.file.Path

class WatchGitFixture(
    val root: Path,
) {
    fun init() {
        runGit("init")
        runGit("config", "user.email", "test@example.com")
        runGit("config", "user.name", "Test User")
        runGit("symbolic-ref", "HEAD", "refs/heads/main")
    }

    fun write(
        rel: String,
        content: String,
    ) {
        val path = root.resolve(rel)
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }

    fun delete(rel: String) {
        root.resolve(rel).toFile().delete()
    }

    fun commitAll(message: String) {
        runGit("add", "-A")
        runGit("commit", "-m", message)
    }

    fun checkoutNew(branch: String) {
        runGit("checkout", "-b", branch)
    }

    fun setOriginHead(branch: String) {
        runGit("update-ref", "refs/remotes/origin/$branch", "HEAD")
        runGit("symbolic-ref", "refs/remotes/origin/HEAD", "refs/remotes/origin/$branch")
    }

    private fun runGit(vararg args: String) {
        val process =
            ProcessBuilder(listOf("git", *args))
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "git ${args.joinToString(" ")} failed with exit code $exitCode: $output" }
    }
}
