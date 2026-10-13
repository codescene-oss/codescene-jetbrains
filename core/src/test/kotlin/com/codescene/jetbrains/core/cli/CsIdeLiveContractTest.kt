package com.codescene.jetbrains.core.cli

import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.ace.FnToRefactor
import com.codescene.data.delta.Delta
import com.codescene.data.review.Review
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.bumpyRoadCSharpCode
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.complexKotlinCode
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.prepareGitRepoWithRules
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.prepareGitRepoWithoutRules
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.simpleCSharpCode
import com.codescene.jetbrains.core.cli.CsIdeLiveContractFixtures.simpleKotlinCode
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class CsIdeLiveContractTest {
    private val tempRoots = mutableListOf<Path>()

    @After
    fun tearDown() {
        tempRoots.forEach { it.toFile().deleteRecursively() }
        tempRoots.clear()
    }

    @Test
    fun `deviceId returns non-blank value`() {
        assertTrue(client.deviceId().isNotBlank())
    }

    @Test
    fun `review returns valid response for Kotlin code`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            assertReview(review("ReviewSubject.kt", simpleKotlinCode, repoRoot, cacheDir))
        }
    }

    @Test
    fun `delta returns valid response for changed Kotlin code`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val previous = review("DeltaSubject.kt", simpleKotlinCode, repoRoot, cacheDir)
            val current = review("DeltaSubject.kt", complexKotlinCode, repoRoot, cacheDir)
            assertDelta(client.delta(previous.rawScore, current.rawScore))
        }
    }

    @Test
    fun `fnsToRefactor accepts review findings`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val review = review("AceReviewSubject.kt", complexKotlinCode, repoRoot, cacheDir)
            val codeSmells = review.fileLevelCodeSmells + review.functionLevelCodeSmells.flatMap { it.codeSmells }
            assertTrue("review produced no code smells", codeSmells.isNotEmpty())
            val functions =
                retry {
                    client.fnsToRefactor(
                        CodeParams(complexKotlinCode, "./AceReviewSubject.kt"),
                        CacheParams(cacheDir.toString()),
                        codeSmells,
                    )
                }
            assertFunctions(functions)
        }
    }

    @Test
    fun `fnsToRefactor accepts delta response`() {
        withIsolatedWorkspace { cacheDir, repoRoot ->
            val delta =
                retry {
                    client.reviewDelta(
                        ReviewParams("./AceDeltaSubject.kt", simpleKotlinCode, repoRoot.toString()),
                        ReviewParams("./AceDeltaSubject.kt", complexKotlinCode, repoRoot.toString()),
                        CacheParams(cacheDir.toString()),
                    )
                }
            assertDelta(delta)
            val functions =
                retry {
                    client.fnsToRefactor(
                        CodeParams(complexKotlinCode, "./AceDeltaSubject.kt"),
                        CacheParams(cacheDir.toString()),
                        delta!!,
                    )
                }
            assertFunctions(functions)
        }
    }

    @Test
    fun `review applies code health rules from repo root`() {
        val rulesRepo = prepareGitRepoWithRules(tempRoots, "src/BumpyRoad.cs", bumpyRoadCSharpCode)
        val defaultRepo = prepareGitRepoWithoutRules(tempRoots, "src/BumpyRoad.cs", bumpyRoadCSharpCode)
        val cacheDir = tempDir("codescene-cs-ide-rules-cache")
        val defaultReview = review("src/BumpyRoad.cs", bumpyRoadCSharpCode, defaultRepo, cacheDir)
        val rulesReview = review("src/BumpyRoad.cs", bumpyRoadCSharpCode, rulesRepo, cacheDir)
        assertTrue(
            "code-health-rules should improve Bumpy Road score",
            reviewScore(rulesReview) > reviewScore(defaultReview),
        )
        assertEquals(10.0, reviewScore(rulesReview), 0.01)
        assertEquals(0, totalSmellCount(rulesReview))
    }

    @Test
    fun `delta applies code health rules without repo path on delta call`() {
        val rulesRepo = prepareGitRepoWithRules(tempRoots, "src/BumpyRoad.cs", bumpyRoadCSharpCode)
        val defaultRepo = prepareGitRepoWithoutRules(tempRoots, "src/BumpyRoad.cs", bumpyRoadCSharpCode)
        val cacheDir = tempDir("codescene-cs-ide-rules-cache")
        val defaultDelta =
            client.reviewDelta(
                ReviewParams("./src/BumpyRoad.cs", simpleCSharpCode, defaultRepo.toString()),
                ReviewParams("./src/BumpyRoad.cs", bumpyRoadCSharpCode, defaultRepo.toString()),
                CacheParams(cacheDir.toString()),
            )
        val rulesDelta =
            client.reviewDelta(
                ReviewParams("./src/BumpyRoad.cs", simpleCSharpCode, rulesRepo.toString()),
                ReviewParams("./src/BumpyRoad.cs", bumpyRoadCSharpCode, rulesRepo.toString()),
                CacheParams(cacheDir.toString()),
            )
        assertNotNull("default delta should be present", defaultDelta)
        val defaultNewScore = optionalScore(defaultDelta!!.newScore)
        assertTrue("default rules should report Bumpy Road degradation", defaultNewScore < 10.0)
        val rulesNewScore = rulesDelta?.let { optionalScore(it.newScore) } ?: 10.0
        assertEquals(10.0, rulesNewScore, 0.01)
    }

    private fun tempDir(prefix: String): Path = Files.createTempDirectory(prefix).also { tempRoots.add(it) }

    private inline fun withIsolatedWorkspace(block: (cacheDir: Path, repoRoot: Path) -> Unit) {
        block(tempDir("codescene-cs-ide-cache"), tempDir("codescene-cs-ide-repo"))
    }

    private fun review(
        fileName: String,
        code: String,
        repoRoot: Path,
        cacheDir: Path,
    ): Review =
        retry {
            val result =
                client.review(
                    ReviewParams("./$fileName", code, repoRoot.toString()),
                    CacheParams(cacheDir.toString()),
                )
            assertNotNull(result)
            result!!
        }

    private inline fun <T> retry(
        attempts: Int = 3,
        block: () -> T,
    ): T {
        var last: Throwable? = null
        repeat(attempts) { attempt ->
            try {
                return block()
            } catch (e: Throwable) {
                last = e
                if (attempt < attempts - 1) Thread.sleep(250)
            }
        }
        throw last!!
    }

    private fun assertReview(review: Review) {
        assertScoreInRange(review.score)
        assertNotNull(review.fileLevelCodeSmells)
        assertNotNull(review.functionLevelCodeSmells)
    }

    private fun assertDelta(delta: Delta?) {
        assertNotNull(delta)
        delta!!.oldScore?.let(::assertOptionalScoreInRange)
        delta.newScore?.let(::assertOptionalScoreInRange)
        assertNotNull(delta.fileLevelFindings)
        assertNotNull(delta.functionLevelFindings)
    }

    private fun assertFunctions(functions: List<FnToRefactor>) {
        functions.forEach { function ->
            assertTrue(function.name.isNotBlank())
            assertNotNull(function.body)
            assertNotNull(function.fileType)
            assertNotNull(function.refactoringTargets)
        }
    }

    private fun reviewScore(review: Review): Double = optionalScore(review.score)

    private fun optionalScore(score: Optional<Double>): Double {
        assertTrue("score should be present", score.isPresent)
        return score.get()
    }

    private fun totalSmellCount(review: Review): Int =
        review.fileLevelCodeSmells.size + review.functionLevelCodeSmells.sumOf { it.codeSmells.size }

    private fun assertScoreInRange(score: Optional<Double>) {
        assertTrue("review score should be present", score.isPresent)
        assertTrue("score should be between 0 and 10", score.get() in 0.0..10.0)
    }

    private fun assertOptionalScoreInRange(score: Optional<Double>) {
        score.ifPresent { value ->
            assertTrue("score should be between 0 and 10", value in 0.0..10.0)
        }
    }

    companion object {
        private lateinit var server: CsIdeServer
        private lateinit var client: CsIdeClient
        private var childPid: Long = 0

        @BeforeClass
        @JvmStatic
        fun startServer() {
            val javaHome = javaHomeForServer()
            val launchSpec = {
                CsIdeLaunch.spec(
                    CsIdeLaunchEnvironment(),
                    javaHome,
                    CsIdeLaunch.extensionApiJar(),
                    CsIdeLaunch.defaultThreads(Runtime.getRuntime().availableProcessors()),
                    System.getProperty("os.name"),
                    System.getenv(),
                )
            }
            val delegate = SystemCsIdeProcessFactory(launchSpec)
            val factory =
                CsIdeProcessFactory {
                    delegate.start().also { childPid = it.pid }
                }
            server = CsIdeServer(factory, TestLogger)
            client = CsIdeClient(CsIdeTransport(server::request))
            assertTrue(client.deviceId().isNotBlank())
        }

        @AfterClass
        @JvmStatic
        fun stopServer() {
            val pid = childPid
            server.close()
            if (pid != 0L) {
                awaitProcessDead(pid)
            }
        }

        private fun javaHomeForServer(): Path {
            val override = System.getProperty("csIde.java")?.trim()?.takeIf { it.isNotEmpty() }
            if (override != null) return Path.of(override)
            val home = Path.of(System.getProperty("java.home"))
            return if (home.fileName.toString().equals("jre", ignoreCase = true)) home.parent else home
        }

        private fun awaitProcessDead(pid: Long) {
            val deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(15)
            while (System.currentTimeMillis() < deadline) {
                val alive = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)
                if (!alive) return
                Thread.sleep(100)
            }
            assertFalse(
                "cs-ide process $pid survived server close",
                ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false),
            )
        }
    }
}
