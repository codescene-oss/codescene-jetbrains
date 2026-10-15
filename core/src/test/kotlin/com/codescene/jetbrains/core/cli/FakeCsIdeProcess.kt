package com.codescene.jetbrains.core.cli

import com.fasterxml.jackson.databind.JsonNode
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class FakeCsIdePeer {
    private val toClient = TestPipe()
    private val fromClient = TestPipe()
    private val stderrPipe = TestPipe()

    val processStdin: OutputStream get() = fromClient.output
    val processStdout: InputStream get() = toClient.input
    val processStderr: InputStream get() = stderrPipe.input

    fun writeToClient(payload: Any) {
        val body = RpcJson.mapper.writeValueAsBytes(payload)
        LspFraming.write(toClient.output, body)
    }

    fun appendStderr(text: String) {
        stderrPipe.output.write(text.toByteArray(Charsets.UTF_8))
        stderrPipe.output.flush()
    }

    fun closeClientInput() {
        toClient.output.close()
    }

    fun readClientMessage(): JsonNode? = LspFraming.read(fromClient.input)?.let { RpcJson.mapper.readTree(it) }
}

class FakeCsIdeProcess(
    private val peer: FakeCsIdePeer,
    override val pid: Long = FakeCsIdeProcessFactory.nextPid(),
) : CsIdeProcess {
    private val alive = AtomicBoolean(true)
    private val exitFuture = CompletableFuture<Int>()
    var completeOnDestroy: Boolean = true

    override val stdin: OutputStream = peer.processStdin
    override val stdout: InputStream = peer.processStdout
    override val stderr: InputStream = peer.processStderr

    override fun isAlive(): Boolean = alive.get()

    override fun onExit(): CompletableFuture<Int> = exitFuture

    fun completeExit(code: Int) {
        alive.set(false)
        exitFuture.complete(code)
    }

    override fun destroyTree() {
        if (!alive.compareAndSet(true, false)) return
        runCatching { stdin.close() }
        runCatching { peer.closeClientInput() }
        if (completeOnDestroy) exitFuture.complete(0)
    }
}

class FakeCsIdeProcessFactory(
    private val onStart: (FakeCsIdePeer) -> Unit = {},
) : CsIdeProcessFactory {
    val starts = AtomicInteger(0)
    private val lastPeer = AtomicReference<FakeCsIdePeer?>(null)
    private val lastProcess = AtomicReference<FakeCsIdeProcess?>(null)

    fun currentPeer(): FakeCsIdePeer = lastPeer.get() ?: error("no fake process started")

    fun currentProcess(): FakeCsIdeProcess = lastProcess.get() ?: error("no fake process started")

    fun killCurrentProcess() {
        lastProcess.get()?.destroyTree()
    }

    override fun start(): CsIdeProcess {
        val peer = FakeCsIdePeer()
        lastPeer.set(peer)
        onStart(peer)
        val process = FakeCsIdeProcess(peer)
        lastProcess.set(process)
        starts.incrementAndGet()
        return process
    }

    companion object {
        private val NEXT_PID = AtomicLong(1000)

        fun nextPid(): Long = NEXT_PID.getAndIncrement()
    }
}
