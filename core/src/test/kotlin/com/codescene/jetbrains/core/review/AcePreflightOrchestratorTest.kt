package com.codescene.jetbrains.core.review

import com.codescene.data.ace.PreflightResponse
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.models.settings.CodeSceneGlobalSettings
import com.codescene.jetbrains.core.testdoubles.InMemorySettingsProvider
import io.mockk.mockk
import java.net.ConnectException
import java.net.http.HttpTimeoutException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AcePreflightOrchestratorTest {
    private lateinit var settingsProvider: InMemorySettingsProvider
    private lateinit var statusChanges: MutableList<AceStatus>
    private var preflightResponse: PreflightResponse? = null
    private var fetchBypassCache: Boolean? = null

    @Before
    fun setUp() {
        statusChanges = mutableListOf()
        preflightResponse = mockk(relaxed = true)
        fetchBypassCache = null
    }

    private fun createOrchestrator(
        settings: CodeSceneGlobalSettings =
            CodeSceneGlobalSettings(
                enableAutoRefactor = true,
                aceAuthToken = "token",
            ),
        fetchResult: suspend (Boolean) -> TimedResult<PreflightResponse?> = { bypassCache ->
            fetchBypassCache = bypassCache
            TimedResult(preflightResponse, 100)
        },
    ) = AcePreflightOrchestrator(
        settingsProvider = InMemorySettingsProvider(settings).also { settingsProvider = it },
        logger = TestLogger,
        serviceName = "Test",
        fetchPreflight = fetchResult,
        onStatusChange = { statusChanges.add(it) },
    )

    @Test
    fun `runPreflight returns response when ace and auto-refactor enabled`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            val result = orchestrator.runPreflight(aceFeatureEnabled = true)
            assertNotNull(result)
        }

    @Test
    fun `runPreflight returns null when ace feature disabled`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            val result = orchestrator.runPreflight(aceFeatureEnabled = false)
            assertNull(result)
        }

    @Test
    fun `runPreflight returns null when auto-refactor disabled`() =
        runBlocking {
            val settings = CodeSceneGlobalSettings(enableAutoRefactor = false, aceAuthToken = "token")
            val orchestrator = createOrchestrator(settings = settings)
            val result = orchestrator.runPreflight(aceFeatureEnabled = true)
            assertNull(result)
        }

    @Test
    fun `runPreflight sets DEACTIVATED status when skipped`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(aceFeatureEnabled = false)
            assertEquals(AceStatus.DEACTIVATED, settingsProvider.currentState().aceStatus)
        }

    @Test
    fun `runPreflight does not bypass cache on normal run`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(aceFeatureEnabled = true, force = false)
            assertEquals(false, fetchBypassCache)
        }

    @Test
    fun `runPreflight bypasses cache on forced run`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(aceFeatureEnabled = true, force = true)
            assertEquals(true, fetchBypassCache)
        }

    @Test
    fun `runPreflight calls onStatusChange with SIGNED_IN on forced success with token`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(aceFeatureEnabled = true, force = true)
            assertTrue(statusChanges.contains(AceStatus.SIGNED_IN))
        }

    @Test
    fun `runPreflight calls onStatusChange with SIGNED_OUT on forced success without token`() =
        runBlocking {
            val settings = CodeSceneGlobalSettings(enableAutoRefactor = true, aceAuthToken = "")
            val orchestrator = createOrchestrator(settings = settings)
            orchestrator.runPreflight(aceFeatureEnabled = true, force = true)
            assertTrue(statusChanges.contains(AceStatus.SIGNED_OUT))
        }

    @Test
    fun `runPreflight returns null and sets OFFLINE on timeout`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    fetchResult = { throw HttpTimeoutException("timeout") },
                )
            val result = orchestrator.runPreflight(aceFeatureEnabled = true)
            assertNull(result)
            assertTrue(statusChanges.contains(AceStatus.OFFLINE))
        }

    @Test
    fun `runPreflight returns null and sets ERROR on generic exception`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    fetchResult = { throw RuntimeException("fail") },
                )
            val result = orchestrator.runPreflight(aceFeatureEnabled = true)
            assertNull(result)
            assertTrue(statusChanges.contains(AceStatus.ERROR))
        }

    @Test
    fun `runPreflight returns null and sets OFFLINE on ConnectException`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    fetchResult = { throw ConnectException("refused") },
                )
            val result = orchestrator.runPreflight(aceFeatureEnabled = true)
            assertNull(result)
            assertTrue(statusChanges.contains(AceStatus.OFFLINE))
        }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
