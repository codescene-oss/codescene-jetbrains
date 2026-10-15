package com.codescene.jetbrains.core.cli

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeProcessTest {
    @Test(timeout = 30_000)
    fun `destroyTree stops the process and its descendants`() {
        val process =
            SystemCsIdeProcessFactory {
                CsIdeLaunchSpec(CsIdeProcessTreeFixture.command(), mapOf("CS_IDE_PROCESS_TEST" to "1"))
            }.start()
        val childPid = process.stdout.bufferedReader().readLine().trim().toLong()
        val child = ProcessHandle.of(childPid).orElseThrow()

        assertTrue(process.isAlive())
        assertTrue(child.isAlive)
        assertTrue(ProcessHandle.of(process.pid).orElseThrow().descendants().anyMatch { it.pid() == childPid })

        process.destroyTree()

        process.onExit().get(10, TimeUnit.SECONDS)
        child.onExit().get(10, TimeUnit.SECONDS)
        assertFalse(process.isAlive())
        assertFalse(child.isAlive)
    }
}
