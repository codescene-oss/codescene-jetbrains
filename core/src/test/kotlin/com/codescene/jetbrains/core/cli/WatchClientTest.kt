package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchClientTest {
    private val notifyInSessionCalls = mutableListOf<Pair<String, JsonNode>>()
    private val notifyIfRunningCalls = mutableListOf<Pair<String, JsonNode>>()
    private var notifyInSessionResult = 7L
    private var notifyIfRunningResult = false
    private var requestMethod: String? = null
    private var requestParams: JsonNode? = null
    private var requestResult: CompletableFuture<JsonNode> = CompletableFuture()

    private val client =
        WatchClient(
            notifyInSession = { method, params ->
                notifyInSessionCalls.add(method to params)
                notifyInSessionResult
            },
            notifyIfRunning = { method, params ->
                notifyIfRunningCalls.add(method to params)
                notifyIfRunningResult
            },
            transport =
                CsIdeTransport { method, params ->
                    requestMethod = method
                    requestParams = params
                    requestResult
                },
        )

    @Test
    fun `watchFiles writes repo-root and relative-paths only and returns the session`() {
        val session = client.watchFiles("/repo", listOf("src", "test"))

        assertEquals(7L, session)
        val (method, params) = notifyInSessionCalls.single()
        assertEquals("cs-ide/watchFiles", method)
        assertEquals(listOf("repo-root", "relative-paths"), params.fieldNames().asSequence().toList())
        assertEquals("/repo", params.path("repo-root").asText())
        assertEquals(listOf("src", "test"), params.path("relative-paths").map { it.asText() })
    }

    @Test
    fun `stopWatchFiles goes through notifyIfRunning`() {
        notifyIfRunningResult = false

        val sent = client.stopWatchFiles("/repo")

        assertFalse(sent)
        assertTrue(notifyInSessionCalls.isEmpty())
        val (method, params) = notifyIfRunningCalls.single()
        assertEquals("cs-ide/stopWatchFiles", method)
        assertEquals("""{"repo-root":"/repo"}""", params.toString())
    }

    @Test
    fun `getWatchInventory requests the root and parses the result`() {
        requestResult =
            CompletableFuture.completedFuture(
                RpcJson.mapper.readTree("""{"repoRoot":"C:\\repo","files":["b.kt","a\\c.kt"]}"""),
            )

        val inventory = client.getWatchInventory("/repo").get()

        assertEquals("cs-ide/getWatchInventory", requestMethod)
        assertEquals("""{"repo-root":"/repo"}""", requestParams.toString())
        assertEquals(WatchInventory("C:\\repo", setOf("b.kt", "a/c.kt")), inventory)
    }

    @Test
    fun `a malformed result fails the future`() {
        requestResult = CompletableFuture.completedFuture(RpcJson.mapper.readTree("{}"))

        val error =
            runCatching { client.getWatchInventory("/repo").get() }
                .exceptionOrNull()

        assertTrue(error is ExecutionException)
        assertTrue(error!!.cause is IllegalStateException)
        assertEquals("Malformed cs-ide/getWatchInventory result", error.cause!!.message)
    }

    @Test
    fun `isNotWatched is true only for the not-watched RPC error`() {
        assertTrue(
            WatchClient.isNotWatched(
                ExecutionException(JsonRpcException(-32603, "Repository is not watched")),
            ),
        )
        assertFalse(WatchClient.isNotWatched(JsonRpcException(-32603, "boom")))
        assertFalse(WatchClient.isNotWatched(CsIdeUnavailableException("cs-ide server is closed")))
    }

    @Test
    fun `inventory parses camelCase and kebab-case`() {
        val camel =
            WatchNotices.inventory(
                RpcJson.mapper.readTree("""{"repoRoot":"/repo","files":["a.kt"]}"""),
            )
        val kebab =
            WatchNotices.inventory(
                RpcJson.mapper.readTree("""{"repo-root":"/repo","files":["b.kt"]}"""),
            )

        assertEquals(WatchInventory("/repo", setOf("a.kt")), camel)
        assertEquals(WatchInventory("/repo", setOf("b.kt")), kebab)
    }

    @Test
    fun `inventory with empty files is an empty set`() {
        val inventory =
            WatchNotices.inventory(
                RpcJson.mapper.readTree("""{"repoRoot":"/repo","files":[]}"""),
            )

        assertEquals(WatchInventory("/repo", emptySet()), inventory)
    }

    @Test
    fun `inventory without repoRoot or with non-array files is null`() {
        assertNull(WatchNotices.inventory(RpcJson.mapper.readTree("""{"files":["a.kt"]}""")))
        assertNull(WatchNotices.inventory(RpcJson.mapper.readTree("""{"repoRoot":"/repo","files":"a.kt"}""")))
    }
}
