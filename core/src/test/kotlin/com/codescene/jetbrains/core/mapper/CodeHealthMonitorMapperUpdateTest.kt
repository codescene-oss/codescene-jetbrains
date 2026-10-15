package com.codescene.jetbrains.core.mapper

import com.codescene.data.delta.Delta
import com.codescene.data.delta.FunctionFinding as DeltaFunctionFinding
import com.codescene.jetbrains.core.cli.ReviewProgressNotice
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.models.CwfMessage
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHealthMonitorMapperUpdateTest {
    private val mapper = CodeHealthMonitorMapper()
    private val autoRefactorConfig = AutoRefactorConfig()
    private val defaultFeatureFlags = listOf("jobs", "ace-status-indicator")
    private val noRefactorResolver: (String, String, DeltaFunctionFinding) -> FunctionToRefactor? = { _, _, _ -> null }
    private val repoRoot = "/repo"

    private fun createDelta(scoreChange: Double = 0.0): Delta {
        val delta = mockk<Delta>(relaxed = true)
        every { delta.scoreChange } returns scoreChange
        every { delta.newScore } returns Optional.empty()
        every { delta.oldScore } returns Optional.empty()
        every { delta.fileLevelFindings } returns null
        every { delta.functionLevelFindings } returns null
        return delta
    }

    private fun createCacheItem(delta: Delta) =
        DeltaCacheItem(headHash = "head", currentHash = "current", deltaApiResponse = delta)

    @Test
    fun `toMessage encodes home payload as CWF message`() {
        val mainPath = "$repoRoot/src/Main.kt"
        val monitorAnalysis =
            buildMonitorAnalysis(
                runningPaths = listOf(mainPath),
                progress = ReviewProgressNotice(count = 0, done = 0, files = emptyList()),
                repoRoots = listOf(repoRoot),
                candidatePaths = listOf(mainPath),
                windows = false,
            )
        val result =
            mapper.toMessage(
                deltaResults = emptyList(),
                monitorAnalysis = monitorAnalysis,
                functionToRefactorResolver = noRefactorResolver,
                autoRefactorConfig = autoRefactorConfig,
                featureFlags = defaultFeatureFlags,
                devmode = true,
            )

        val parsed = Json.decodeFromString(CwfMessage.serializer(), result)
        assertEquals("update-renderer", parsed.messageType)
        assertTrue(result.contains("\"view\": \"home\""))
        assertTrue(result.contains(mainPath))
    }

    @Test
    fun `buildUpdate returns message and notification state`() {
        val delta = createDelta(scoreChange = 1.0)
        val mainPath = "$repoRoot/src/Main.kt"
        val result =
            mapper.buildUpdate(
                deltaResults = listOf("file.kt" to createCacheItem(delta)),
                runningPaths = listOf(mainPath),
                progress = ReviewProgressNotice(count = 0, done = 0, files = emptyList()),
                repoRoots = listOf(repoRoot),
                candidatePaths = listOf(mainPath),
                windows = false,
                functionToRefactorResolver = noRefactorResolver,
                autoRefactorConfig = autoRefactorConfig,
                featureFlags = defaultFeatureFlags,
                devmode = true,
            )

        assertTrue(result.message.contains("\"view\": \"home\""))
        assertTrue(result.message.contains(mainPath))
        assertEquals(true, result.hasNotification)
    }
}
