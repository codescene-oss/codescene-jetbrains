package com.codescene.jetbrains.platform.progress

import com.codescene.jetbrains.core.cli.ReviewProgressNotice
import com.codescene.jetbrains.core.cli.progressTitle
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportRawProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ReviewProgressIndicator(
    private val project: Project,
    private val scope: CoroutineScope,
) {
    private val lock = Any()
    private var latestTitle: String? = null
    private var runner: Job? = null
    private var wake: Channel<Unit>? = null

    fun onProgress(notice: ReviewProgressNotice) {
        val title = progressTitle(notice)
        synchronized(lock) {
            latestTitle = title
            wake?.trySend(Unit)
            startRunnerIfNeededLocked()
        }
    }

    fun sessionLost() {
        onProgress(ReviewProgressNotice(count = 0, done = 0, files = emptyList()))
    }

    private fun startRunnerIfNeededLocked() {
        if (latestTitle == null) return
        if (runner?.isActive == true) return
        runner = scope.launch { runBackgroundProgress() }
    }

    private suspend fun runBackgroundProgress() {
        try {
            val initialTitle =
                synchronized(lock) {
                    latestTitle
                } ?: return
            withBackgroundProgress(project, initialTitle, cancellable = false) {
                reportRawProgress { reporter ->
                    val channel = Channel<Unit>(Channel.CONFLATED)
                    synchronized(lock) {
                        wake = channel
                    }
                    try {
                        while (true) {
                            val current =
                                synchronized(lock) {
                                    latestTitle
                                } ?: return@reportRawProgress
                            reporter.text(current)
                            channel.receive()
                        }
                    } finally {
                        synchronized(lock) {
                            if (wake === channel) {
                                wake = null
                            }
                        }
                    }
                }
            }
        } finally {
            synchronized(lock) {
                runner = null
                startRunnerIfNeededLocked()
            }
        }
    }
}
