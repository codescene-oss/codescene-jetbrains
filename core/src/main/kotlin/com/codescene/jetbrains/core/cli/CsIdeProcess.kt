package com.codescene.jetbrains.core.cli

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

interface CsIdeProcess {
    val stdin: OutputStream
    val stdout: InputStream
    val stderr: InputStream
    val pid: Long

    fun isAlive(): Boolean

    fun onExit(): CompletableFuture<Int>

    fun destroyTree()
}

fun interface CsIdeProcessFactory {
    fun start(): CsIdeProcess
}

class SystemCsIdeProcessFactory(
    private val spec: () -> CsIdeLaunchSpec,
) : CsIdeProcessFactory {
    override fun start(): CsIdeProcess = SystemCsIdeProcess(startProcess(spec()))

    private fun startProcess(launch: CsIdeLaunchSpec): Process {
        val builder = ProcessBuilder(launch.command)
        builder.environment().putAll(launch.environment)
        return builder.redirectErrorStream(false).start()
    }
}

private class SystemCsIdeProcess(
    private val process: Process,
) : CsIdeProcess {
    override val stdin: OutputStream = process.outputStream
    override val stdout: InputStream = process.inputStream
    override val stderr: InputStream = process.errorStream
    override val pid: Long = process.pid()

    override fun isAlive(): Boolean = process.isAlive

    override fun onExit(): CompletableFuture<Int> = process.onExit().thenApply { it.exitValue() }

    override fun destroyTree() {
        destroyProcessTree(process)
    }
}

private fun destroyProcessTree(process: Process) {
    val descendants = process.descendants().toList()
    descendants.forEach { it.destroy() }
    process.destroy()
    if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly()
    descendants.filter { it.isAlive }.forEach { it.destroyForcibly() }
}
