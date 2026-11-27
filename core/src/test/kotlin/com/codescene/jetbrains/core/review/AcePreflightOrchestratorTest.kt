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
                aceTokenConfigured = true,
            ),
        aceAuthToken: String = "token",
        fetchResult: suspend (Boolean) -> TimedResult<PreflightResponse?> = { bypassCache ->
            fetchBypassCache = bypassCache
            TimedResult(preflightResponse, 100)
        },
        aceEnabled: Boolean = true,
    ) = AcePreflightOrchestrator(
        settingsProvider =
            InMemorySettingsProvider(settings, aceAuthToken = aceAuthToken).also { settingsProvider = it },
        logger = TestLogger,
        serviceName = "Test",
        fetchPreflight = fetchResult,
        onStatusChange = { statusChanges.add(it) },
        aceEnabled = aceEnabled,
    )

    @Test
    fun `runPreflight returns response when token configured`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            val result = orchestrator.runPreflight()
            assertNotNull(result)
        }

    @Test
    fun `runPreflight returns null when token not configured`() =
        runBlocking {
            val settings = CodeSceneGlobalSettings(aceTokenConfigured = false)
            val orchestrator = createOrchestrator(settings = settings)
            val result = orchestrator.runPreflight()
            assertNull(result)
        }

    @Test
    fun `runPreflight does not bypass cache on normal run`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(force = false)
            assertEquals(false, fetchBypassCache)
        }

    @Test
    fun `runPreflight bypasses cache on forced run`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(force = true)
            assertEquals(true, fetchBypassCache)
        }

    @Test
    fun `runPreflight calls onStatusChange with SIGNED_IN on forced success with token`() =
        runBlocking {
            val orchestrator = createOrchestrator()
            orchestrator.runPreflight(force = true)
            assertTrue(statusChanges.contains(AceStatus.SIGNED_IN))
        }

    @Test
    fun `runPreflight calls onStatusChange with SIGNED_OUT on forced success without token`() =
        runBlocking {
            val settings = CodeSceneGlobalSettings(aceTokenConfigured = true)
            val orchestrator = createOrchestrator(settings = settings, aceAuthToken = "")
            orchestrator.runPreflight(force = true)
            assertTrue(statusChanges.contains(AceStatus.SIGNED_OUT))
        }

    @Test
    fun `runPreflight returns null and sets OFFLINE on timeout`() =
        runBlocking {
            val orchestrator =
                createOrchestrator(
                    fetchResult = { throw HttpTimeoutException("timeout") },
                )
            val result = orchestrator.runPreflight()
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
            val result = orchestrator.runPreflight()
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
            val result = orchestrator.runPreflight()
            assertNull(result)
            assertTrue(statusChanges.contains(AceStatus.OFFLINE))
        }

    @Test
    fun `runPreflight is a no-op when ACE is unavailable`() =
        runBlocking {
            val initialAceStatus = AceStatus.SIGNED_IN
            val settings = CodeSceneGlobalSettings(aceTokenConfigured = true).also { it.aceStatus = initialAceStatus }
            var fetchInvoked = false
            val orchestrator =
                createOrchestrator(
                    settings = settings,
                    aceAuthToken = "token",
                    fetchResult = {
                        fetchInvoked = true
                        TimedResult(preflightResponse, 100)
                    },
                    aceEnabled = false,
                )

            val result = orchestrator.runPreflight(force = true)

            assertNull(result)
            assertEquals(false, fetchInvoked)
            assertEquals(0, statusChanges.size)
            assertEquals(initialAceStatus, settingsProvider.currentState().aceStatus)
        }

    private fun assertTrue(condition: Boolean) = org.junit.Assert.assertTrue(condition)
}
