package com.codescene.jetbrains.core.cli

import java.nio.file.Path

object CsIdeProcess {
    fun startDistribution(
        distribution: Path,
        threads: Int,
        onStdErr: (String) -> Unit = {},
    ): Process {
        val java = CsIdeDistribution.javaExecutable(distribution)
        val jar = CsIdeDistribution.jar(distribution)
        val process =
            ProcessBuilder(
                java.toAbsolutePath().toString(),
                "--enable-native-access=ALL-UNNAMED",
                "-jar",
                jar.toAbsolutePath().toString(),
                "server",
                "--threads",
                threads.toString(),
            ).redirectErrorStream(false).start()
        Thread({
            val buffer = ByteArray(4096)
            val stream = process.errorStream
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break
                onStdErr(String(buffer, 0, read, Charsets.UTF_8))
            }
        }, "cs-ide-stderr").apply {
            isDaemon = true
            start()
        }
        return process
    }

    fun kill(process: Process) {
        if (!process.isAlive) return
        if (CsIdeDistribution.hostPlatform() == "win32") {
            val pid = process.pid()
            val taskkill =
                ProcessBuilder("taskkill", "/pid", pid.toString(), "/T", "/F")
                    .redirectErrorStream(true)
                    .start()
            taskkill.waitFor()
            if (process.isAlive) {
                process.destroyForcibly()
            }
            return
        }
        process.destroy()
        if (!process.waitFor(3, java.util.concurrent.TimeUnit.SECONDS) && process.isAlive) {
            process.destroyForcibly()
        }
    }
}
