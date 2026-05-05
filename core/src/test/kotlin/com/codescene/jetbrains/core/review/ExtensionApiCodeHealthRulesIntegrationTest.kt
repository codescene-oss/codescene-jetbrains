package com.codescene.jetbrains.core.review

import com.codescene.ExtensionAPI
import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Test

class ExtensionApiCodeHealthRulesIntegrationTest {
    private lateinit var cacheDir: Path
    private val tempRoots = mutableListOf<Path>()

    @Before
    fun setUp() {
        cacheDir = Files.createTempDirectory("codescene-extension-api-cache")
    }

    @After
    fun tearDown() {
        cacheDir.toFile().deleteRecursively()
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun `review applies code health rules from repo root`() {
        val rulesRepo = prepareGitRepoWithRules("src/BumpyRoad.cs", bumpyRoadCSharpCode)
        val defaultRepo = prepareGitRepoWithoutRules("src/BumpyRoad.cs", bumpyRoadCSharpCode)

        val defaultReview = review("src/BumpyRoad.cs", bumpyRoadCSharpCode, defaultRepo)
        val rulesReview = review("src/BumpyRoad.cs", bumpyRoadCSharpCode, rulesRepo)

        assertTrue(
            "code-health-rules should improve Bumpy Road score",
            reviewScore(rulesReview) > reviewScore(defaultReview),
        )
        assertEquals(10.0, reviewScore(rulesReview), 0.01)
        assertEquals(0, totalSmellCount(rulesReview))
    }

    @Test
    fun `delta applies code health rules from repo root`() {
        val rulesRepo = prepareGitRepoWithRules("src/BumpyRoad.cs", bumpyRoadCSharpCode)
        val defaultRepo = prepareGitRepoWithoutRules("src/BumpyRoad.cs", bumpyRoadCSharpCode)

        val defaultDelta = delta("src/BumpyRoad.cs", simpleCSharpCode, bumpyRoadCSharpCode, defaultRepo)
        val rulesDelta = delta("src/BumpyRoad.cs", simpleCSharpCode, bumpyRoadCSharpCode, rulesRepo)
        assertNotNull("default delta should be present", defaultDelta)
        val defaultNewScore = optionalScore(defaultDelta!!.newScore)

        assertTrue("default rules should report Bumpy Road degradation", defaultNewScore < 10.0)
        assumeNotNull(rulesDelta)
        val rulesNewScore = optionalScore(rulesDelta!!.newScore)
        assertTrue(
            "code-health-rules should improve delta new score",
            rulesNewScore > defaultNewScore,
        )
        assertEquals(10.0, rulesNewScore, 0.01)
    }

    private fun review(
        fileName: String,
        code: String,
        repoRoot: Path,
    ): Review =
        ExtensionAPI.review(
            ReviewParams("./$fileName", code, repoRoot.toString()),
            cacheParams(),
        )

    private fun delta(
        fileName: String,
        oldCode: String,
        newCode: String,
        repoRoot: Path,
    ): Delta? =
        ExtensionAPI.delta(
            ReviewParams("./$fileName", oldCode, repoRoot.toString()),
            ReviewParams("./$fileName", newCode, repoRoot.toString()),
            cacheParams(),
        )

    private fun cacheParams(): CacheParams = CacheParams(cacheDir.toString())

    private fun prepareGitRepoWithRules(
        sourceRelativePath: String,
        sourceContent: String,
    ): Path =
        prepareGitRepo(sourceRelativePath, sourceContent).also { repoRoot ->
            writeRepoFile(repoRoot, ".codescene/code-health-rules.json", rulesBumpyRoadWeight0)
            runGit(repoRoot, "add", ".")
            runGit(repoRoot, "commit", "-m", "test")
        }

    private fun prepareGitRepoWithoutRules(
        sourceRelativePath: String,
        sourceContent: String,
    ): Path =
        prepareGitRepo(sourceRelativePath, sourceContent).also { repoRoot ->
            runGit(repoRoot, "add", ".")
            runGit(repoRoot, "commit", "-m", "test")
        }

    private fun prepareGitRepo(
        sourceRelativePath: String,
        sourceContent: String,
    ): Path {
        val repoRoot = Files.createTempDirectory("codescene-extension-api-rules")
        tempRoots.add(repoRoot)
        runGit(repoRoot, "init")
        runGit(repoRoot, "config", "user.email", "test@example.com")
        runGit(repoRoot, "config", "user.name", "Test User")
        writeRepoFile(repoRoot, sourceRelativePath, sourceContent)
        return repoRoot
    }

    private fun writeRepoFile(
        repoRoot: Path,
        relativePath: String,
        content: String,
    ) {
        val path = repoRoot.resolve(relativePath)
        Files.createDirectories(path.parent)
        Files.writeString(path, content)
    }

    private fun runGit(
        repoRoot: Path,
        vararg args: String,
    ) {
        val process =
            ProcessBuilder(listOf("git", *args))
                .directory(repoRoot.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        check(exitCode == 0) { "git ${args.joinToString(" ")} failed with exit code $exitCode: $output" }
    }

    private fun reviewScore(review: Review): Double = optionalScore(review.score)

    private fun optionalScore(score: Optional<Double>): Double {
        assertTrue("score should be present", score.isPresent)
        return score.get()
    }

    private fun totalSmellCount(review: Review): Int =
        review.fileLevelCodeSmells.size + review.functionLevelCodeSmells.sumOf { it.codeSmells.size }

    private val simpleCSharpCode =
        """
        namespace CodeScene.ExtensionApi.Tests;

        public static class Calculator
        {
            public static int Add(int a, int b) => a + b;
        }
        """.trimIndent()

    private val bumpyRoadCSharpCode =
        """
        using System.Collections.Generic;
        using System.IO;
        using System.Text;
        using System.Text.RegularExpressions;

        namespace CodeScene.ExtensionApi.Tests
        {
            class BumpyRoadExample
            {
                public void ProcessDirectory(string path)
                {
                    var files = new List<string>();
                    var directory = new DirectoryInfo(path);

                    foreach (FileInfo fileInfo in directory.GetFiles())
                    {
                        if (Regex.IsMatch(fileInfo.Name, @"^data\d+\.csv${'$'}"))
                        {
                            files.Add(fileInfo.FullName);
                        }
                    }

                    var sb = new StringBuilder();
                    foreach (string filePath in files)
                    {
                        using (var reader = new StreamReader(filePath))
                        {
                            string line;
                            while ((line = reader.ReadLine()) != null)
                            {
                                sb.Append(line);
                            }
                        }
                    }

                    using (var writer = new StreamWriter("data.csv"))
                    {
                        writer.Write(sb.ToString());
                    }
                }
            }
        }
        """.trimIndent()

    private val rulesBumpyRoadWeight0 =
        """
        {
          "rule_sets": [
            {
              "matching_content_path": "**/*",
              "rules": [
                {
                  "name": "Bumpy Road Ahead",
                  "weight": 0.0
                }
              ]
            }
          ]
        }
        """.trimIndent()
}
