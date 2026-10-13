package com.codescene.jetbrains.core.cli

import java.nio.file.Files
import java.nio.file.Path

object CsIdeLiveContractFixtures {
    val simpleKotlinCode =
        """
        class Calculator {
            fun add(a: Int, b: Int): Int = a + b
        }
        """.trimIndent()

    val complexKotlinCode =
        """
        class ComplexProcessor {
            fun process(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int): Int {
                if (a > 0) {
                    if (b > 0) {
                        if (c > 0) {
                            if (d > 0) {
                                return a + b + c + d + e + f + g + h
                            }
                        }
                    }
                }
                return 0
            }
        }
        """.trimIndent()

    val simpleCSharpCode =
        """
        namespace CodeScene.ExtensionApi.Tests;

        public static class Calculator
        {
            public static int Add(int a, int b) => a + b;
        }
        """.trimIndent()

    val bumpyRoadCSharpCode =
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

    val rulesBumpyRoadWeight0 =
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

    fun prepareGitRepoWithRules(
        tempRoots: MutableList<Path>,
        sourceRelativePath: String,
        sourceContent: String,
    ): Path =
        prepareGitRepo(tempRoots, sourceRelativePath, sourceContent).also { repoRoot ->
            writeRepoFile(repoRoot, ".codescene/code-health-rules.json", rulesBumpyRoadWeight0)
            runGit(repoRoot, "add", ".")
            runGit(repoRoot, "commit", "-m", "test")
        }

    fun prepareGitRepoWithoutRules(
        tempRoots: MutableList<Path>,
        sourceRelativePath: String,
        sourceContent: String,
    ): Path =
        prepareGitRepo(tempRoots, sourceRelativePath, sourceContent).also { repoRoot ->
            runGit(repoRoot, "add", ".")
            runGit(repoRoot, "commit", "-m", "test")
        }

    private fun prepareGitRepo(
        tempRoots: MutableList<Path>,
        sourceRelativePath: String,
        sourceContent: String,
    ): Path {
        val repoRoot = Files.createTempDirectory("codescene-cs-ide-rules")
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
}
