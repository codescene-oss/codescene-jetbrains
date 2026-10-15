package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.cli.ReviewProgressNotice
import com.codescene.jetbrains.core.cli.WorkspacePaths
import com.codescene.jetbrains.core.git.isPathUnderRoot
import com.codescene.jetbrains.core.git.pathComparisonKey
import com.codescene.jetbrains.core.models.shared.AnalysisJob
import com.codescene.jetbrains.core.models.shared.FileMetaType
import com.codescene.jetbrains.core.util.Constants.DELTA_ANALYSIS_JOB
import com.codescene.jetbrains.core.util.Constants.JOB_STATE_QUEUED
import com.codescene.jetbrains.core.util.Constants.JOB_STATE_RUNNING

data class MonitorAnalysis(
    val jobs: List<AnalysisJob>,
    val analysisState: String,
    val totalCount: Int?,
    val remainingCount: Int,
)

fun buildMonitorAnalysis(
    runningPaths: Iterable<String>,
    progress: ReviewProgressNotice,
    repoRoots: List<String>,
    candidatePaths: Iterable<String>,
    windows: Boolean,
): MonitorAnalysis {
    val runningList = runningPaths.toList()
    val progressFiles = progress.files
    val (remainingCount, totalCount) =
        monitorJobCounts(
            running = runningList,
            queued = progressFiles,
            queueCount = progress.count,
            queueDone = progress.done,
            repoRoots = repoRoots,
            windows = windows,
        )
    val jobs = analysisJobsToCwf(runningList, progressFiles, repoRoots, candidatePaths, windows)
    val analysisState = if (remainingCount > 0) ANALYSIS_STATE_RUNNING else ANALYSIS_STATE_IDLE
    return MonitorAnalysis(
        jobs = jobs,
        analysisState = analysisState,
        totalCount = totalCount,
        remainingCount = remainingCount,
    )
}

fun monitorJobCounts(
    running: Iterable<String>,
    queued: List<String>,
    queueCount: Int,
    queueDone: Int,
    repoRoots: List<String>,
    windows: Boolean = false,
): Pair<Int, Int?> {
    val localOnly = localJobsMissingFromProgress(running, queued, repoRoots, windows).size
    val remainingCount = queueCount + localOnly
    if (remainingCount <= 0) return Pair(0, null)
    return Pair(remainingCount, queueDone + remainingCount)
}

fun localJobsMissingFromProgress(
    running: Iterable<String>,
    files: List<String>,
    repoRoots: List<String>,
    windows: Boolean = false,
): List<String> {
    val listed = files.map { WorkspacePaths.normalizeRel(it) }.toSet()
    val missing = mutableListOf<String>()
    for (fileName in running) {
        val relative = repoRelativePath(fileName, repoRoots, windows)
        if (relative != null && listed.contains(relative)) continue
        missing.add(fileName)
    }
    return missing
}

fun analysisJobsToCwf(
    runningList: List<String>,
    progressFiles: List<String>,
    repoRoots: List<String>,
    candidatePaths: Iterable<String>,
    windows: Boolean,
): List<AnalysisJob> {
    val candidates = (candidatePaths.toList() + runningList).distinct()
    val jobs = listedProgressJobs(progressFiles, runningList, repoRoots, candidates, windows).toMutableList()
    appendEditorJobs(jobs, runningList, progressFiles, repoRoots, windows)
    return jobs
}

private const val ANALYSIS_STATE_IDLE = "idle"
private const val ANALYSIS_STATE_RUNNING = "running"

private fun listedProgressJobs(
    progressFiles: List<String>,
    runningList: List<String>,
    repoRoots: List<String>,
    candidates: List<String>,
    windows: Boolean,
): List<AnalysisJob> {
    val listedAbs = mutableSetOf<String>()
    val jobs = mutableListOf<AnalysisJob>()
    for (relPath in progressFiles) {
        val job = progressJob(relPath, runningList, repoRoots, candidates, windows)
        val key = pathComparisonKey(job.file.fileName)
        if (listedAbs.contains(key)) continue
        listedAbs.add(key)
        jobs.add(job)
    }
    return jobs
}

private fun appendEditorJobs(
    jobs: MutableList<AnalysisJob>,
    runningList: List<String>,
    progressFiles: List<String>,
    repoRoots: List<String>,
    windows: Boolean,
) {
    val listedAbs = jobs.map { pathComparisonKey(it.file.fileName) }.toSet()
    for (fileName in localJobsMissingFromProgress(runningList, progressFiles, repoRoots, windows)) {
        if (listedAbs.contains(pathComparisonKey(fileName))) continue
        jobs.add(
            AnalysisJob(
                type = DELTA_ANALYSIS_JOB,
                state = JOB_STATE_RUNNING,
                file = FileMetaType(fileName = fileName),
            ),
        )
    }
}

private fun progressJob(
    relPath: String,
    runningList: List<String>,
    repoRoots: List<String>,
    candidates: List<String>,
    windows: Boolean,
): AnalysisJob {
    val fileName = resolveProgressFile(relPath, repoRoots, runningList, candidates, windows)
    val state =
        if (matchesRunning(fileName, relPath, runningList, repoRoots, windows)) {
            JOB_STATE_RUNNING
        } else {
            JOB_STATE_QUEUED
        }
    return AnalysisJob(
        type = DELTA_ANALYSIS_JOB,
        state = state,
        file = FileMetaType(fileName = fileName),
    )
}

private fun resolveProgressFile(
    relPath: String,
    repoRoots: List<String>,
    runningList: List<String>,
    candidates: List<String>,
    windows: Boolean,
): String {
    val matches = matchingAbsolutePaths(relPath, repoRoots, candidates, windows)
    if (matches.size == 1) return matches[0]
    if (repoRoots.size == 1) return joinUnderRoot(repoRoots[0], relPath, windows)
    return WorkspacePaths.normalizeRel(relPath)
}

private fun joinUnderRoot(
    repoRoot: String,
    relPath: String,
    windows: Boolean,
): String {
    val posix = WorkspacePaths.normalizeRel(relPath)
    val base = repoRoot.trimEnd('/', '\\')
    val joined =
        posix.split('/').fold(base) { acc, segment ->
            if (segment.isEmpty()) acc else "$acc/$segment"
        }
    return if (windows) joined.replace('/', '\\') else joined
}

private fun matchingAbsolutePaths(
    relPath: String,
    repoRoots: List<String>,
    candidates: List<String>,
    windows: Boolean,
): List<String> {
    val posixRel = WorkspacePaths.normalizeRel(relPath)
    val matches = linkedMapOf<String, String>()
    for (fileName in candidates) {
        for (root in repoRoots) {
            if (!isPathUnderRoot(fileName, root)) continue
            val relative = WorkspacePaths.relativeTo(root, fileName, windows)
            if (relative == null || WorkspacePaths.normalizeRel(relative) != posixRel) continue
            matches[pathComparisonKey(fileName)] = fileName
        }
    }
    return matches.values.toList()
}

private fun matchesRunning(
    fileName: String,
    relPath: String,
    running: List<String>,
    repoRoots: List<String>,
    windows: Boolean,
): Boolean {
    val key = pathComparisonKey(fileName)
    val posixRel = WorkspacePaths.normalizeRel(relPath)
    return running.any { path ->
        pathComparisonKey(path) == key || repoRelativePath(path, repoRoots, windows) == posixRel
    }
}

private fun repoRelativePath(
    fileName: String,
    repoRoots: List<String>,
    windows: Boolean,
): String? {
    val roots =
        repoRoots
            .filter { isPathUnderRoot(fileName, it) }
            .sortedByDescending { pathComparisonKey(it).length }
    if (roots.isEmpty()) return null
    val relative = WorkspacePaths.relativeTo(roots.first(), fileName, windows)
    return relative?.let { WorkspacePaths.normalizeRel(it) }
}
