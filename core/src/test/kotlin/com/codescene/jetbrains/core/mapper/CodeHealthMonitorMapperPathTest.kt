package com.codescene.jetbrains.core.mapper

import com.codescene.data.delta.Delta
import com.codescene.data.delta.FunctionFinding as DeltaFunctionFinding
import com.codescene.jetbrains.core.delta.DeltaCacheItem
import com.codescene.jetbrains.core.models.shared.AutoRefactorConfig
import com.codescene.jetbrains.core.models.view.FunctionToRefactor
import io.mockk.every
import io.mockk.mockk
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Test

class CodeHealthMonitorMapperPathTest {
    private val autoRefactorConfig = AutoRefactorConfig()
    private val defaultFeatureFlags = listOf("jobs", "ace-status-indicator")
    private val noRefactorResolver: (String, String, DeltaFunctionFinding) -> FunctionToRefactor? = { _, _, _ -> null }
    private val idleAnalysis = MonitorAnalysis(emptyList(), "idle", null, 0)

    private fun createDelta(): Delta {
        val delta = mockk<Delta>(relaxed = true)
        every { delta.scoreChange } returns 0.0
        every { delta.newScore } returns Optional.empty()
        every { delta.oldScore } returns Optional.empty()
        every { delta.fileLevelFindings } returns null
        every { delta.functionLevelFindings } returns null
        return delta
    }

    private fun createCacheItem(delta: Delta) =
        DeltaCacheItem(headHash = "head", currentHash = "current", deltaApiResponse = delta)

    @Test
    fun `toCwfData normalizes mixed path to backslashes for monitor display`() {
        val mixedPath = "C:/repo\\src/File.kt"
        val fileDelta =
            CodeHealthMonitorMapper(pathSeparator = '\\')
                .toCwfData(
                    deltaResults = listOf(mixedPath to createCacheItem(createDelta())),
                    monitorAnalysis = idleAnalysis,
                    functionToRefactorResolver = noRefactorResolver,
                    autoRefactorConfig = autoRefactorConfig,
                    featureFlags = defaultFeatureFlags,
                    devmode = false,
                )
                .data!!
                .fileDeltaData[0]

        assertEquals("C:\\repo\\src\\File.kt", fileDelta.file.fileName)
    }

    @Test
    fun `toCwfData normalizes mixed path to forward slashes for monitor display`() {
        val mixedPath = "/repo\\src/File.kt"
        val fileDelta =
            CodeHealthMonitorMapper(pathSeparator = '/')
                .toCwfData(
                    deltaResults = listOf(mixedPath to createCacheItem(createDelta())),
                    monitorAnalysis = idleAnalysis,
                    functionToRefactorResolver = noRefactorResolver,
                    autoRefactorConfig = autoRefactorConfig,
                    featureFlags = defaultFeatureFlags,
                    devmode = false,
                )
                .data!!
                .fileDeltaData[0]

        assertEquals("/repo/src/File.kt", fileDelta.file.fileName)
    }

    @Test
    fun `toCwfData passes original path to function to refactor resolver`() {
        val range = mockk<com.codescene.data.delta.Range>(relaxed = true)
        every { range.startLine } returns 1
        every { range.endLine } returns 10

        val function = mockk<com.codescene.data.delta.Function>(relaxed = true)
        every { function.name } returns "myFn"
        every { function.range } returns Optional.of(range)

        val fnFinding = mockk<DeltaFunctionFinding>(relaxed = true)
        every { fnFinding.function } returns function
        every { fnFinding.changeDetails } returns emptyList()

        val delta = mockk<Delta>(relaxed = true)
        every { delta.scoreChange } returns 0.0
        every { delta.newScore } returns Optional.empty()
        every { delta.oldScore } returns Optional.empty()
        every { delta.fileLevelFindings } returns null
        every { delta.functionLevelFindings } returns listOf(fnFinding)

        val mixedPath = "C:/repo\\src/File.kt"
        var resolverFilePath: String? = null
        val resolver: (String, String, DeltaFunctionFinding) -> FunctionToRefactor? = { filePath, _, _ ->
            resolverFilePath = filePath
            null
        }

        CodeHealthMonitorMapper(pathSeparator = '\\')
            .toCwfData(
                deltaResults = listOf(mixedPath to createCacheItem(delta)),
                monitorAnalysis = idleAnalysis,
                functionToRefactorResolver = resolver,
                autoRefactorConfig = autoRefactorConfig,
                featureFlags = defaultFeatureFlags,
                devmode = false,
            )

        assertEquals(mixedPath, resolverFilePath)
    }
}
