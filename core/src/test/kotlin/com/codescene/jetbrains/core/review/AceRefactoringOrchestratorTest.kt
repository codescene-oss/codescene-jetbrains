package com.codescene.jetbrains.core.review

import com.codescene.data.ace.FnToRefactor
import com.codescene.data.ace.RefactorResponse
import com.codescene.data.ace.RefactoringOptions
import com.codescene.jetbrains.core.TestLogger
import com.codescene.jetbrains.core.models.RefactoringRequest
import com.codescene.jetbrains.core.models.settings.AceStatus
import com.codescene.jetbrains.core.util.AceEntryPoint
import io.mockk.every
import io.mockk.mockk
import java.net.ConnectException
import java.net.http.HttpTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AceRefactoringOrchestratorTest {
    private lateinit var statusChanges: MutableList<AceStatus>
    private lateinit var requestedRequests: MutableList<RefactoringRequest>
    private lateinit var performanceRecords: MutableList<Pair<RefactoringRequest, Long>>
    private val mockResponse: RefactorResponse = mockk(relaxed = true)
    private val mockOptions: RefactoringOptions = mockk(relaxed = true)

    @Before
    fun setUp() {
        statusChanges = mutableListOf()
        requestedRequests = mutableListOf()
        performanceRecords = mutableListOf()
    }

    private fun createRequest(): RefactoringRequest {
        val fn = mockk<FnToRefactor>(relaxed = true)
        every { fn.name } returns "testFn"
        return RefactoringRequest(
            filePath = "a.kt",
            language = "kotlin",
            function = fn,
            source = AceEntryPoint.INTENTION_ACTION,
        )
    }

    private fun createOrchestrator(
        token: String = "token",
        executeRefactor: (RefactoringRequest, RefactoringOptions) -> TimedResult<RefactorResponse?> = { _, _ ->
            TimedResult(mockResponse, 42)
        },
    ) = AceRefactoringOrchestrator(
        logger = TestLogger,
        serviceName = "Test",
        executeRefactor = executeRefactor,
        getToken = { token },
        onStatusChange = { statusChanges.add(it) },
        onRequested = { requestedRequests.add(it) },
        onPerformance = { req, ms -> performanceRecords.add(req to ms) },
    )

    @Test
    fun `runRefactor returns result on success`() {
        val orchestrator = createOrchestrator()
        val result = orchestrator.runRefactor(createRequest(), mockOptions)
        assertNotNull(result)
        assertEquals(mockResponse, result!!.response)
        assertEquals(42L, result.elapsedMs)
    }

    @Test
    fun `runRefactor calls onRequested`() {
        val orchestrator = createOrchestrator()
        val request = createRequest()
        orchestrator.runRefactor(request, mockOptions)
        assertEquals(1, requestedRequests.size)
        assertEquals(request, requestedRequests[0])
    }

    @Test
    fun `runRefactor records performance`() {
        val orchestrator = createOrchestrator()
        val request = createRequest()
        orchestrator.runRefactor(request, mockOptions)
        assertEquals(1, performanceRecords.size)
        assertEquals(42L, performanceRecords[0].second)
    }

    @Test
    fun `runRefactor sets SIGNED_IN status when token present`() {
        val orchestrator = createOrchestrator(token = "valid-token")
        orchestrator.runRefactor(createRequest(), mockOptions)
        assertTrue(statusChanges.contains(AceStatus.SIGNED_IN))
    }

    @Test
    fun `runRefactor sets SIGNED_OUT status when token empty`() {
        val orchestrator = createOrchestrator(token = "")
        orchestrator.runRefactor(createRequest(), mockOptions)
        assertTrue(statusChanges.contains(AceStatus.SIGNED_OUT))
    }

    @Test
    fun `runRefactor returns null when executeRefactor returns null result`() {
        val orchestrator =
            createOrchestrator(
                executeRefactor = { _, _ -> TimedResult(null, 10) },
            )
        val result = orchestrator.runRefactor(createRequest(), mockOptions)
        assertEquals(null, result)
    }

    @Test(expected = ConnectException::class)
    fun `runRefactor throws and sets OFFLINE on ConnectException`() {
        val orchestrator =
            createOrchestrator(
                executeRefactor = { _, _ -> throw ConnectException("refused") },
            )
        try {
            orchestrator.runRefactor(createRequest(), mockOptions)
        } finally {
            assertTrue(statusChanges.contains(AceStatus.OFFLINE))
        }
    }

    @Test(expected = HttpTimeoutException::class)
    fun `runRefactor throws and sets OFFLINE on HttpTimeoutException`() {
        val orchestrator =
            createOrchestrator(
                executeRefactor = { _, _ -> throw HttpTimeoutException("timeout") },
            )
        try {
            orchestrator.runRefactor(createRequest(), mockOptions)
        } finally {
            assertTrue(statusChanges.contains(AceStatus.OFFLINE))
        }
    }

    @Test(expected = RuntimeException::class)
    fun `runRefactor throws and sets ERROR on generic exception`() {
        val orchestrator =
            createOrchestrator(
                executeRefactor = { _, _ -> throw RuntimeException("fail") },
            )
        try {
            orchestrator.runRefactor(createRequest(), mockOptions)
        } finally {
            assertTrue(statusChanges.contains(AceStatus.ERROR))
        }
    }

    @Test
    fun `runRefactor still records onRequested even when exception occurs`() {
        val orchestrator =
            createOrchestrator(
                executeRefactor = { _, _ -> throw RuntimeException("fail") },
            )
        try {
            orchestrator.runRefactor(createRequest(), mockOptions)
        } catch (_: RuntimeException) {
        }
        assertEquals(1, requestedRequests.size)
    }
}
