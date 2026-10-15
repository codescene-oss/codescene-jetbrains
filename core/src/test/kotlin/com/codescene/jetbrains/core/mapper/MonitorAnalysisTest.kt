package com.codescene.jetbrains.core.mapper

import com.codescene.jetbrains.core.cli.ReviewProgressNotice
import com.codescene.jetbrains.core.util.Constants.JOB_STATE_QUEUED
import com.codescene.jetbrains.core.util.Constants.JOB_STATE_RUNNING
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonitorAnalysisTest {
    private val root = "/repo"

    private fun jobSummary(jobs: List<com.codescene.jetbrains.core.models.shared.AnalysisJob>) =
        jobs.map { JobSummary(it.file.fileName, it.state) }

    private data class JobSummary(val fileName: String, val state: String)

    @Test
    fun `monitorJobCounts zero when nothing is running or queued`() {
        val (remaining, total) = monitorJobCounts(emptyList(), emptyList(), 0, 0, listOf(root))
        assertEquals(0, remaining)
        assertNull(total)
    }

    @Test
    fun `monitorJobCounts adds finished reviews to the remaining count`() {
        val (remaining, total) = monitorJobCounts(emptyList(), emptyList(), 2, 1, listOf(root))
        assertEquals(2, remaining)
        assertEquals(3, total)
    }

    @Test
    fun `monitorJobCounts uses queue count when the files list is capped`() {
        val queued = List(20) { i -> "f$i.ts" }
        val (remaining, total) = monitorJobCounts(emptyList(), queued, 500, 10, listOf(root))
        assertEquals(500, remaining)
        assertEquals(510, total)
    }

    @Test
    fun `monitorJobCounts counts editor reviews that are not in the progress list`() {
        val running = listOf("$root/dirty.ts")
        val (remaining, total) =
            monitorJobCounts(running, listOf("a.ts"), 1, 0, listOf(root))
        assertEquals(2, remaining)
        assertEquals(2, total)
    }

    @Test
    fun `monitorJobCounts does not double-count an editor review listed in progress`() {
        val running = listOf("$root/a.ts")
        val (remaining, total) =
            monitorJobCounts(running, listOf("a.ts", "b.ts"), 5, 2, listOf(root))
        assertEquals(5, remaining)
        assertEquals(7, total)
    }

    @Test
    fun `monitorJobCounts counts an editor review before the first progress report`() {
        val running = listOf("$root/a.ts")
        val (remaining, total) = monitorJobCounts(running, emptyList(), 0, 0, listOf(root))
        assertEquals(1, remaining)
        assertEquals(1, total)
    }

    @Test
    fun `monitorJobCounts clears the total when the batch has drained`() {
        val (remaining, total) = monitorJobCounts(emptyList(), emptyList(), 0, 3, listOf(root))
        assertEquals(0, remaining)
        assertNull(total)
    }

    @Test
    fun `analysisJobsToCwf empty when neither running nor queued`() {
        assertEquals(
            emptyList<JobSummary>(),
            jobSummary(analysisJobsToCwf(emptyList(), emptyList(), listOf(root), emptyList(), false)),
        )
    }

    @Test
    fun `analysisJobsToCwf maps running jobs as running`() {
        val running = listOf("$root/a.ts")
        assertEquals(
            listOf(JobSummary("$root/a.ts", JOB_STATE_RUNNING)),
            jobSummary(analysisJobsToCwf(running, emptyList(), listOf(root), running, false)),
        )
    }

    @Test
    fun `analysisJobsToCwf maps queued jobs as queued`() {
        assertEquals(
            listOf(
                JobSummary("$root/b.ts", JOB_STATE_QUEUED),
                JobSummary("$root/c.ts", JOB_STATE_QUEUED),
            ),
            jobSummary(analysisJobsToCwf(emptyList(), listOf("b.ts", "c.ts"), listOf(root), emptyList(), false)),
        )
    }

    @Test
    fun `analysisJobsToCwf marks a listed path running when it matches an editor review`() {
        val running = listOf("$root/a.ts")
        assertEquals(
            listOf(
                JobSummary("$root/a.ts", JOB_STATE_RUNNING),
                JobSummary("$root/b.ts", JOB_STATE_QUEUED),
            ),
            jobSummary(analysisJobsToCwf(running, listOf("a.ts", "b.ts"), listOf(root), running, false)),
        )
    }

    @Test
    fun `analysisJobsToCwf appends editor reviews that are not in the progress list`() {
        val running = listOf("$root/dirty.ts")
        assertEquals(
            listOf(
                JobSummary("$root/a.ts", JOB_STATE_QUEUED),
                JobSummary("$root/dirty.ts", JOB_STATE_RUNNING),
            ),
            jobSummary(analysisJobsToCwf(running, listOf("a.ts"), listOf(root), running, false)),
        )
    }

    @Test
    fun `analysisJobsToCwf keeps a relative path when it matches more than one repo`() {
        val repoA = "/repo-a"
        val repoB = "/repo-b"
        assertEquals(
            listOf(JobSummary("src/a.ts", JOB_STATE_QUEUED)),
            jobSummary(analysisJobsToCwf(emptyList(), listOf("src/a.ts"), listOf(repoA, repoB), emptyList(), false)),
        )
    }

    @Test
    fun `analysisJobsToCwf resolves a progress file to the one open document when several repos are known`() {
        val repoA = "/repo-a"
        val fileName = "$repoA/src/a.ts"
        assertEquals(
            listOf(JobSummary(fileName, JOB_STATE_QUEUED)),
            jobSummary(
                analysisJobsToCwf(emptyList(), listOf("src/a.ts"), listOf(repoA, "/repo-b"), listOf(fileName), false),
            ),
        )
    }

    @Test
    fun `buildMonitorAnalysis matches home view progress case`() {
        val running = listOf("$root/dirty.ts")
        val progress = ReviewProgressNotice(count = 2, done = 1, files = listOf("a.ts"))
        val analysis =
            buildMonitorAnalysis(
                runningPaths = running,
                progress = progress,
                repoRoots = listOf(root),
                candidatePaths = running,
                windows = false,
            )
        assertEquals("running", analysis.analysisState)
        assertEquals(3, analysis.remainingCount)
        assertEquals(4, analysis.totalCount)
        assertEquals(
            listOf(
                JobSummary("$root/a.ts", JOB_STATE_QUEUED),
                JobSummary("$root/dirty.ts", JOB_STATE_RUNNING),
            ),
            jobSummary(analysis.jobs),
        )
    }

    @Test
    fun `buildMonitorAnalysis clears counts when the batch has drained`() {
        val progress = ReviewProgressNotice(count = 0, done = 3, files = emptyList())
        val analysis =
            buildMonitorAnalysis(
                runningPaths = emptyList(),
                progress = progress,
                repoRoots = listOf(root),
                candidatePaths = emptyList(),
                windows = false,
            )
        assertEquals("idle", analysis.analysisState)
        assertEquals(0, analysis.remainingCount)
        assertNull(analysis.totalCount)
        assertEquals(emptyList<JobSummary>(), jobSummary(analysis.jobs))
    }
}
