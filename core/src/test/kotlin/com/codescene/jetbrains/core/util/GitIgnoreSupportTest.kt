package com.codescene.jetbrains.core.util

import com.codescene.jetbrains.core.contracts.IGitService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitIgnoreSupportTest {
    @Test
    fun `isFileSupportedForAnalysis honors directory ignore patterns`() =
        withGitRepo(
            mapOf(
                ".gitignore" to "generated/\n",
                "generated/Main.kt" to "class Main\n",
            ),
        ) { repoRoot, gitService ->
            assertFalse(
                isFileSupportedForAnalysis(
                    extension = "kt",
                    inProjectContent = true,
                    filePath = repoRoot.resolve("generated/Main.kt").toString(),
                    gitService = gitService,
                ),
            )
        }

    @Test
    fun `isFileSupportedForAnalysis honors nested gitignore files`() =
        withGitRepo(
            mapOf(
                "src/generated/.gitignore" to "*.kt\n",
                "src/generated/Hidden.kt" to "class Hidden\n",
            ),
        ) { repoRoot, gitService ->
            assertFalse(
                isFileSupportedForAnalysis(
                    extension = "kt",
                    inProjectContent = true,
                    filePath = repoRoot.resolve("src/generated/Hidden.kt").toString(),
                    gitService = gitService,
                ),
            )
        }

    @Test
    fun `isFileSupportedForAnalysis honors glob patterns`() =
        withGitRepo(
            mapOf(
                ".gitignore" to "*.min.js\n",
                "assets/app.min.js" to "alert('minified')\n",
                "assets/app.js" to "alert('source')\n",
            ),
        ) { repoRoot, gitService ->
            assertFalse(
                isFileSupportedForAnalysis(
                    extension = "js",
                    inProjectContent = true,
                    filePath = repoRoot.resolve("assets/app.min.js").toString(),
                    gitService = gitService,
                ),
            )
            assertTrue(
                isFileSupportedForAnalysis(
                    extension = "js",
                    inProjectContent = true,
                    filePath = repoRoot.resolve("assets/app.js").toString(),
                    gitService = gitService,
                ),
            )
        }

    @Test
    fun `isFileSupportedForAnalysis honors negation patterns`() =
        withGitRepo(
            mapOf(
                ".gitignore" to "*.kt\n!keep.kt\n",
                "drop.kt" to "class Drop\n",
                "keep.kt" to "class Keep\n",
            ),
        ) { repoRoot, gitService ->
            assertFalse(
                isFileSupportedForAnalysis(
                    extension = "kt",
                    inProjectContent = true,
                    filePath = repoRoot.resolve("drop.kt").toString(),
                    gitService = gitService,
                ),
            )
            assertTrue(
                isFileSupportedForAnalysis(
                    extension = "kt",
                    inProjectContent = true,
                    filePath = repoRoot.resolve("keep.kt").toString(),
                    gitService = gitService,
                ),
            )
        }

    private fun withGitRepo(
        files: Map<String, String>,
        assertion: (repoRoot: Path, gitService: IGitService) -> Unit,
    ) {
        val repoRoot = Files.createTempDirectory("codescene-gitignore")
        try {
            runGit(repoRoot, "init")
            files.forEach { (relativePath, content) ->
                val file = repoRoot.resolve(relativePath)
                Files.createDirectories(file.parent ?: repoRoot)
                Files.writeString(file, content)
            }
            assertion(repoRoot, CliGitService(repoRoot))
        } finally {
            repoRoot.toFile().deleteRecursively()
        }
    }

    private fun runGit(
        workingDirectory: Path,
        vararg args: String,
    ): GitCommandResult {
        val process =
            ProcessBuilder(listOf("git", *args))
                .directory(workingDirectory.toFile())
                .start()
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw IllegalStateException("git ${args.joinToString(" ")} timed out")
        }

        return GitCommandResult(
            exitCode = process.exitValue(),
            stdout = process.inputStream.bufferedReader().readText(),
            stderr = process.errorStream.bufferedReader().readText(),
        )
    }

    private data class GitCommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
    )

    private class CliGitService(
        private val repoRoot: Path,
    ) : IGitService {
        override fun getBranchCreationCommitCode(filePath: String): String = ""

        override fun getBranchCreationCommitHash(filePath: String): String? = null

        override fun getRepoRelativePath(filePath: String): String? = null

        override fun isIgnored(filePath: String): Boolean {
            val relativePath = repoRoot.relativize(Paths.get(filePath)).normalize().toGitPath()
            val result =
                ProcessBuilder(
                    listOf(
                        "git",
                        "check-ignore",
                        "--no-index",
                        "--",
                        relativePath,
                    ),
                ).directory(repoRoot.toFile())
                    .start()

            if (!result.waitFor(10, TimeUnit.SECONDS)) {
                result.destroyForcibly()
                throw IllegalStateException("git check-ignore timed out for $relativePath")
            }

            return result.exitValue() == 0
        }
    }
}

private fun Path.toGitPath(): String = toString().replace('\\', '/')
