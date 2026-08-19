package com.codescene.jetbrains.platform.cli

import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeDistribution
import com.codescene.jetbrains.core.cli.CsIdeListener
import com.codescene.jetbrains.core.cli.DeltaReviewEvent
import com.codescene.jetbrains.core.cli.FileReviewEvent
import com.codescene.jetbrains.core.cli.JsonRpcException
import com.codescene.jetbrains.core.cli.ReviewFailedEvent
import com.codescene.jetbrains.core.cli.ServerMetadata
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.ProjectManager
import java.util.concurrent.atomic.AtomicBoolean

@Service
class CsIdeServerService : Disposable {
    private val lock = Any()
    private var client: CsIdeClient? = null
    private var metadata: ServerMetadata? = null
    private val restarting = AtomicBoolean(false)
    private val disposing = AtomicBoolean(false)

    companion object {
        fun getInstance(): CsIdeServerService =
            ApplicationManager.getApplication().getService(CsIdeServerService::class.java)

        private const val SERVICE = "CsIdeServerService"
    }

    fun client(): CsIdeClient = ensureStarted()

    fun ensureStarted(): CsIdeClient {
        synchronized(lock) {
            client?.let { return it }
            return startLocked()
        }
    }

    override fun dispose() {
        disposing.set(true)
        synchronized(lock) {
            client?.close()
            client = null
            metadata = null
        }
    }

    private fun startLocked(): CsIdeClient {
        val distribution = CsIdeRuntime.resolve()
        val threads = workerThreads()
        Log.info("Starting cs-ide from $distribution threads=$threads", SERVICE)
        val started =
            CsIdeClient.fromDistribution(distribution, threads) { line ->
                Log.debug(line, "cs-ide")
            }
        started.addListener(
            object : CsIdeListener {
                override fun onFileReview(event: FileReviewEvent) {}

                override fun onDeltaReview(event: DeltaReviewEvent) {}

                override fun onReviewFailed(event: ReviewFailedEvent) {}

                override fun onError(error: Throwable) {
                    handleCrash(error)
                }
            },
        )
        val startedMetadata = started.start()
        val required = CsIdeDistribution.requiredSha()
        if (startedMetadata.sha != required && startedMetadata.sha.isNotBlank()) {
            started.close()
            throw JsonRpcException(
                null,
                "cs-ide distribution version ${startedMetadata.sha} does not match required $required",
            )
        }
        client = started
        metadata = startedMetadata
        Log.info("cs-ide ready sha=${startedMetadata.sha} version=${startedMetadata.version}", SERVICE)
        return started
    }

    private fun handleCrash(error: Throwable) {
        if (disposing.get()) return
        Log.warn("cs-ide error: ${error.message}", SERVICE)
        Thread({
            if (!restarting.compareAndSet(false, true)) return@Thread
            try {
                if (disposing.get()) return@Thread
                synchronized(lock) {
                    client?.close()
                    client = null
                    metadata = null
                    runCatching { startLocked() }
                        .onFailure { Log.error("Failed to restart cs-ide: ${it.message}", SERVICE) }
                }
                val restarted = synchronized(lock) { client }
                if (restarted != null) {
                    ProjectManager.getInstance().openProjects.forEach { project ->
                        if (!project.isDisposed) {
                            WorkspaceReviewService.getInstance(project).onServerRestarted()
                        }
                    }
                }
            } finally {
                restarting.set(false)
            }
        }, "cs-ide-restart").apply {
            isDaemon = true
            start()
        }
    }

    private fun workerThreads(): Int = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
}
