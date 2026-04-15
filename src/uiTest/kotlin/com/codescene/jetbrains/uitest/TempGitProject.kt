package com.codescene.jetbrains.uitest

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object TempGitProject {
    fun createWithJavaFile(prefix: String = "codescene-ui"): Path {
        val dir = Files.createTempDirectory(prefix)
        dir.toFile().deleteOnExit()
        Files.writeString(
            dir.resolve("Sample.java"),
            """
            public class Sample {
              public static void main(String[] args) {}
            }

            """.trimIndent(),
        )
        runGit(dir, "init")
        runGit(dir, "config", "user.email", "ui-test@local")
        runGit(dir, "config", "user.name", "UI Test")
        runGit(dir, "add", ".")
        runGit(dir, "commit", "-m", "init")
        return dir
    }

    private fun runGit(
        dir: Path,
        vararg args: String,
    ) {
        val command = listOf("git", *args)
        val process =
            ProcessBuilder(command)
                .directory(dir.toFile())
                .redirectErrorStream(true)
                .start()
        val finished = process.waitFor(2, TimeUnit.MINUTES)
        require(finished && process.exitValue() == 0) {
            val out = process.inputStream.readAllBytes().decodeToString()
            "git ${args.joinToString(" ")} failed (exit=${process.exitValue()}): $out"
        }
    }
}
