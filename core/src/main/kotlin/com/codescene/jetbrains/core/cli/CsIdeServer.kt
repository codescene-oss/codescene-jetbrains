package com.codescene.jetbrains.core.cli

import com.codescene.jetbrains.core.contracts.ILogger
import com.fasterxml.jackson.databind.JsonNode
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

data class CsIdeServerMetadata(
    val sha: String,
    val version: String,
)

private data class LiveConnection(val rpc: JsonRpcConnection, val session: Long)

private data class RunningServer(
    val connection: JsonRpcConnection?,
    val process: CsIdeProcess?,
)

class CsIdeServer(
    private val processFactory: CsIdeProcessFactory,
    private val logger: ILogger,
    private val startupTimeoutMs: Long = 30_000,
    private val shutdownGraceMs: Long = 3_000,
    private val backoff: CsIdeBackoff = CsIdeBackoff(),
    private val clock: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private val lock = Any()
    private val startLock = Any()
    private val closed = AtomicBoolean(false)
    private var process: CsIdeProcess? = null
    private var connection: JsonRpcConnection? = null
    private var liveSessionId: Long = 0
    private var nextSessionId: Long = 1

    @Volatile
    private var pendingStart: CompletableFuture<Unit>? = null

    @Volatile
    private var metadata: CsIdeServerMetadata? = null
    private val stderrTail = StringBuilder()
    private val hub = CsIdeNotificationHub(logger)

    fun request(
        method: String,
        params: JsonNode,
    ): CompletableFuture<JsonNode> =
        try {
            ensureAvailable()
            val future = ensureStarted().rpc.request(method, params)
            future.whenComplete { _, error ->
                if (error == null) backoff.reset()
            }
            future
        } catch (e: CsIdeUnavailableException) {
            failed(e)
        }

    fun notify(
        method: String,
        params: JsonNode,
    ) {
        notifyInSession(method, params)
    }

    fun notifyInSession(
        method: String,
        params: JsonNode,
    ): Long {
        ensureAvailable()
        val live = ensureStarted()
        try {
            live.rpc.notify(method, params)
        } catch (e: Exception) {
            throw CsIdeUnavailableException("cs-ide server failed to send $method: ${e.message}", e)
        }
        backoff.reset()
        return live.session
    }

    fun notifyIfRunning(
        method: String,
        params: JsonNode,
    ): Boolean {
        val rpc =
            synchronized(lock) {
                if (closed.get() || process?.isAlive() != true) return false
                connection ?: return false
            }
        return runCatching { rpc.notify(method, params) }.isSuccess
    }

    fun liveSession(): Long =
        synchronized(lock) {
            if (connection != null && process?.isAlive() == true) liveSessionId else 0
        }

    fun cooldownRemainingMs(): Long = backoff.remainingMs(clock())

    fun addNotificationHandler(handler: (String, JsonNode) -> Unit): AutoCloseable = hub.addHandler(handler)

    fun addSessionListener(listener: () -> Unit): AutoCloseable = hub.addSessionListener(listener)

    fun addSessionStartListener(listener: (Long) -> Unit): AutoCloseable = hub.addSessionStartListener(listener)

    fun metadata(): CsIdeServerMetadata? = metadata

    fun stderrTail(): String = synchronized(lock) { stderrTail.toString() }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pendingStart?.completeExceptionally(CsIdeUnavailableException("cs-ide server is closed"))
        shutDown(synchronized(lock) { detach() }, graceful = true)
        hub.sessionLost()
    }

    private fun ensureAvailable() {
        if (closed.get()) throw CsIdeUnavailableException("cs-ide server is closed")
        val cooldown = backoff.remainingMs(clock())
        if (cooldown > 0) throw CsIdeUnavailableException("cs-ide server is unavailable (retry in ${cooldown}ms)")
    }

    private fun ensureStarted(): LiveConnection =
        synchronized(startLock) {
            val stale =
                synchronized(lock) {
                    if (closed.get()) throw CsIdeUnavailableException("cs-ide server is closed")
                    val existing = connection
                    if (existing != null && process?.isAlive() == true) return LiveConnection(existing, liveSessionId)
                    detach()
                }
            shutDown(stale, graceful = false)
            if (stale.process != null) hub.sessionLost()
            startProcess()
        }

    private fun startProcess(): LiveConnection {
        val sessionId = synchronized(lock) { nextSessionId++ }
        val started = processFactory.start()
        synchronized(lock) { stderrTail.clear() }
        startStderrReader(started)
        val startSignal = CompletableFuture<Unit>()
        pendingStart = startSignal
        val rpc =
            JsonRpcConnection(
                input = started.stdout,
                output = started.stdin,
                onNotification = { method, params -> onNotification(method, params, startSignal, sessionId) },
                onClosed = { error -> onTransportClosed(started, error) },
            )
        started.onExit().thenAccept { code ->
            startSignal.completeExceptionally(CsIdeUnavailableException("cs-ide server exited with code $code"))
            onProcessExit(started, code)
        }
        try {
            waitForStart(startSignal, started)
            synchronized(lock) {
                if (closed.get()) throw CsIdeUnavailableException("cs-ide server is closed")
                process = started
                connection = rpc
                liveSessionId = sessionId
            }
            return LiveConnection(rpc, sessionId)
        } catch (e: CsIdeUnavailableException) {
            shutDown(RunningServer(rpc, started), graceful = false)
            throw e
        } finally {
            pendingStart = null
        }
    }

    private fun waitForStart(
        startSignal: CompletableFuture<Unit>,
        started: CsIdeProcess,
    ) {
        try {
            startSignal.get(startupTimeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            if (closed.get()) throw CsIdeUnavailableException("cs-ide server is closed", e)
            backoff.recordFailure(clock())
            val reason =
                if (e is TimeoutException) {
                    "did not start within ${startupTimeoutMs}ms"
                } else {
                    "failed to start: ${CsIdeErrors.unwrap(e).message}"
                }
            throw CsIdeUnavailableException("cs-ide server $reason: ${stderrTail()}", e)
        }
        if (!started.isAlive()) {
            backoff.recordFailure(clock())
            throw CsIdeUnavailableException("cs-ide server exited during startup: ${stderrTail()}")
        }
    }

    private fun onNotification(
        method: String,
        params: JsonNode,
        startSignal: CompletableFuture<Unit>,
        sessionId: Long,
    ) {
        val isStart = method == START_METHOD
        if (isStart) recordMetadata(params)
        if (isStart) hub.sessionStarted(sessionId)
        hub.dispatch(method, params)
        if (isStart) startSignal.complete(Unit)
    }

    private fun recordMetadata(params: JsonNode) {
        metadata =
            CsIdeServerMetadata(
                sha = RpcJson.text(params, "sha").orEmpty(),
                version = RpcJson.text(params, "version").orEmpty(),
            )
        logger.info("cs-ide server started: ${metadata?.version} (${metadata?.sha})", SERVICE)
    }

    private fun onTransportClosed(
        owner: CsIdeProcess,
        error: Throwable,
    ) {
        val detached =
            synchronized(lock) {
                if (closed.get() || process !== owner) return
                logger.warn("cs-ide JSON-RPC closed: ${error.message}", SERVICE)
                backoff.recordFailure(clock())
                detach()
            }
        shutDown(detached, graceful = false)
        hub.sessionLost()
    }

    private fun onProcessExit(
        exited: CsIdeProcess,
        code: Int,
    ) {
        val detached =
            synchronized(lock) {
                if (closed.get()) return
                logger.warn("cs-ide server exited with code $code", SERVICE)
                if (process !== exited) return
                connection?.failPending(CsIdeUnavailableException("cs-ide server exited with code $code"))
                backoff.recordFailure(clock())
                detach()
            }
        shutDown(detached, graceful = false)
        hub.sessionLost()
    }

    private fun detach(): RunningServer {
        val detached = RunningServer(connection, process)
        connection = null
        process = null
        metadata = null
        liveSessionId = 0
        return detached
    }

    private fun shutDown(
        running: RunningServer,
        graceful: Boolean,
    ) {
        running.connection?.close()
        val proc = running.process ?: return
        if (graceful) {
            runCatching { proc.stdin.close() }
            runCatching { proc.onExit().get(shutdownGraceMs, TimeUnit.MILLISECONDS) }
        }
        if (proc.isAlive()) proc.destroyTree()
    }

    private fun startStderrReader(started: CsIdeProcess) {
        Thread({
            val buffer = ByteArray(4096)
            val stream = started.stderr
            while (true) {
                val read =
                    try {
                        stream.read(buffer)
                    } catch (_: Exception) {
                        break
                    }
                if (read < 0) break
                appendStderr(String(buffer, 0, read, StandardCharsets.UTF_8))
            }
        }, "cs-ide-stderr").apply {
            isDaemon = true
            start()
        }
    }

    private fun appendStderr(chunk: String) {
        logger.debug("cs-ide stderr: ${chunk.trimEnd()}", SERVICE)
        synchronized(lock) {
            stderrTail.append(chunk)
            if (stderrTail.length > STDERR_TAIL_LIMIT) {
                stderrTail.delete(0, stderrTail.length - STDERR_TAIL_LIMIT)
            }
        }
    }

    private fun failed(error: CsIdeUnavailableException): CompletableFuture<JsonNode> =
        CompletableFuture<JsonNode>().also { it.completeExceptionally(error) }

    private companion object {
        private const val SERVICE = "CsIdeServer"
        private const val START_METHOD = "cs-ide/start"
        private const val STDERR_TAIL_LIMIT = 4_000
    }
}
