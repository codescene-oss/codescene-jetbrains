package com.codescene.jetbrains.core.cli

sealed interface WatchEffect {
    data class FetchInventory(
        val repoRoot: String,
        val generation: Long,
    ) : WatchEffect

    data class ScheduleConfirm(
        val repoRoot: String,
        val token: Long,
        val delayMs: Long,
    ) : WatchEffect

    data class ReportLost(
        val repoRoot: String,
    ) : WatchEffect

    data class SyncMonitor(
        val repoRoot: String,
        val absolutePaths: Set<String>,
        val excludedRoots: Set<String> = emptySet(),
    ) : WatchEffect

    data class Reseed(
        val repoRoot: String,
    ) : WatchEffect
}

data class ResolvedPath(
    val repoRoot: String,
    val absolutePath: String,
)
