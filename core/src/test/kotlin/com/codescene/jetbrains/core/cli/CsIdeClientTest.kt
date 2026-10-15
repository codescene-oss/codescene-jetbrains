package com.codescene.jetbrains.core.cli

import com.codescene.ExtensionAPI.CacheParams
import com.codescene.ExtensionAPI.CodeParams
import com.codescene.ExtensionAPI.ReviewParams
import com.codescene.data.ace.RefactoringOptions
import com.codescene.data.telemetry.TelemetryEvent
import com.fasterxml.jackson.databind.JsonNode
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.ConnectException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeoutException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CsIdeClientTest {
    private lateinit var fixture: IdeServerFixture
    private lateinit var client: CsIdeClient
    private lateinit var connection: JsonRpcConnection

    @Before
    fun setUp() {
        val serverToClient = PipedInputStream(PIPE_BUFFER)
        val serverOut = PipedOutputStream(serverToClient)
        val clientToServer = PipedInputStream(PIPE_BUFFER)
        val clientOut = PipedOutputStream(clientToServer)
        fixture = IdeServerFixture(clientToServer, serverOut)
        fixture.start()
        connection =
            JsonRpcConnection(
                input = serverToClient,
                output = clientOut,
                onNotification = { _, _ -> },
                onClosed = { },
            )
        client = CsIdeClient(CsIdeTransport(connection::request))
    }

    @After
    fun tearDown() {
        connection.close()
        fixture.stop()
    }

    @Test
    fun `review sends kebab-case params and maps camelCase results`() {
        val review = client.review(ReviewParams("src/a.kt", "class A", "/repo"), CacheParams("/cache"))
        assertNotNull(review)
        assertEquals(9.68, review!!.score.get(), 0.001)
        val params = fixture.lastParams("cs-ide/review")
        assertEquals("src/a.kt", params?.path("path")?.asText())
        assertEquals("class A", params?.path("file-content")?.asText())
        assertEquals("/cache", params?.path("cache-path")?.asText())
        assertEquals("/repo", params?.path("repo-path")?.asText())
    }

    @Test
    fun `review with null code does not send a request`() {
        assertNull(client.review(ReviewParams("src/a.kt", null, null), CacheParams(null)))
        assertTrue(fixture.paramsHistory("cs-ide/review").isEmpty())
    }

    @Test
    fun `delta sends old-score and new-score`() {
        val delta = client.delta("old-raw", "new-raw")
        assertNotNull(delta)
        val params = fixture.lastParams("cs-ide/delta")
        assertEquals("old-raw", params?.path("old-score")?.asText())
        assertEquals("new-raw", params?.path("new-score")?.asText())
    }

    @Test
    fun `delta null result returns null`() {
        fixture.deltaResultNull = true
        assertNull(client.delta("old", "new"))
    }

    @Test
    fun `reviewDelta performs two reviews then delta on raw scores`() {
        val delta =
            client.reviewDelta(
                ReviewParams("old.kt", "old", "/repo"),
                ReviewParams("new.kt", "new", "/repo"),
                CacheParams("/cache"),
            )
        assertNotNull(delta)
        assertEquals(2, fixture.paramsHistory("cs-ide/review").size)
        assertNotNull(fixture.lastParams("cs-ide/delta"))
    }

    @Test
    fun `reviewDelta uses raw scores from reviews`() {
        client.reviewDelta(
            ReviewParams("a.kt", "a", null),
            ReviewParams("b.kt", "b", null),
            CacheParams(null),
        )
        val deltaParams = fixture.lastParams("cs-ide/delta")
        assertEquals("raw-score", deltaParams?.path("old-score")?.asText())
        assertEquals("raw-score", deltaParams?.path("new-score")?.asText())
    }

    @Test
    fun `preflight and fnsToRefactor include cached preflight`() {
        client.preflight(force = true)
        client.fnsToRefactor(CodeParams("function f() {}", "a.ts"), CacheParams("/cache"), emptyList())
        val params = fixture.lastParams("cs-ide/fns-to-refactor")
        assertEquals("a.ts", params?.path("file-name")?.asText())
        assertEquals("function f() {}", params?.path("file-content")?.asText())
        assertEquals("/cache", params?.path("cache-path")?.asText())
        assertTrue(params!!.has("preflight"))
        assertTrue(params.has("code-smells"))
    }

    @Test
    fun `fnsToRefactor delta overload sends delta-result`() {
        val delta = client.delta("a", "b")!!
        client.fnsToRefactor(CodeParams("function f() {}", "a.ts"), CacheParams(null), delta)
        val params = fixture.lastParams("cs-ide/fns-to-refactor")
        assertTrue(params!!.has("delta-result"))
    }

    @Test
    fun `refactor sends fn-to-refactor token and skip-cache`() {
        val fns = client.fnsToRefactor(CodeParams("function f() {}", "a.ts"), CacheParams(null), emptyList())
        val options =
            RefactoringOptions().apply {
                setToken("token-1")
                setSkipCache(true)
            }
        val response = client.refactor(fns.single(), options)
        assertNotNull(response)
        val params = fixture.lastParams("cs-ide/refactor")
        assertTrue(params!!.has("fn-to-refactor"))
        assertEquals("token-1", params.path("token").asText())
        assertTrue(params.path("skip-cache").asBoolean())
    }

    @Test
    fun `telemetry and deviceId`() {
        assertEquals(202, client.telemetry(TelemetryEvent()))
        assertEquals("device-42", client.deviceId())
        assertTrue(fixture.lastParams("cs-ide/telemetry")!!.has("event"))
    }

    @Test
    fun `rpc errors surface as ConnectException through client`() {
        fixture.reviewErrorMessage = "java.net.ConnectException: Connection refused"
        val thrown =
            runCatching {
                client.review(ReviewParams("a.kt", "code", null), CacheParams(null))
            }.exceptionOrNull()
        assertTrue(thrown is ConnectException)
    }

    @Test
    fun `CsIdeUnavailableException passes through client`() {
        val unavailableClient =
            CsIdeClient(
                CsIdeTransport { _, _ ->
                    CompletableFuture.failedFuture(CsIdeUnavailableException("server down"))
                },
            )
        val thrown = runCatching { unavailableClient.deviceId() }.exceptionOrNull()
        assertTrue(thrown is CsIdeUnavailableException)
    }

    @Test
    fun `timeout cancels the pending request`() {
        val pending = CompletableFuture<JsonNode>()
        val slowClient = CsIdeClient(CsIdeTransport { _, _ -> pending }, timeoutMs = 50)
        val thrown = runCatching { slowClient.deviceId() }.exceptionOrNull()
        assertTrue(thrown is TimeoutException)
        assertTrue(pending.isCancelled)
    }

    companion object {
        private const val PIPE_BUFFER = 64 * 1024
    }
}
