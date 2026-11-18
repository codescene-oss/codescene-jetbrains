package com.codescene.jetbrains.core.util

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryLinksTest {
    private val masterTree = "${Constants.REPOSITORY_URL}/tree/master/"

    private fun repositoryRoot(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile && File(it, "core").isDirectory }

    private fun assertLinksToRepositoryPath(url: String) {
        assertTrue("$url is not under $masterTree", url.startsWith(masterTree))
        val target = File(repositoryRoot(), url.removePrefix(masterTree))
        assertTrue("$url points at ${target.path}, which does not exist", target.exists())
    }

    @Test
    fun `the telemetry events link points at a file on master`() {
        assertLinksToRepositoryPath(Constants.TELEMETRY_EVENTS_URL)
    }

    @Test
    fun `the telemetry samples link points at a directory on master`() {
        assertLinksToRepositoryPath(Constants.TELEMETRY_SAMPLES_URL)
    }
}
