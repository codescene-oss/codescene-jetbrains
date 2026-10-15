package com.codescene.jetbrains.benchmarks

import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeLaunch
import com.codescene.jetbrains.core.cli.CsIdeLaunchEnvironment
import com.codescene.jetbrains.core.cli.CsIdeServer
import com.codescene.jetbrains.core.cli.CsIdeTransport
import com.codescene.jetbrains.core.cli.SystemCsIdeProcessFactory
import com.codescene.jetbrains.core.contracts.ILogger
import java.nio.file.Files
import java.nio.file.Path
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown

@State(Scope.Benchmark)
abstract class CsIdeBenchmarkState {
    lateinit var client: CsIdeClient
    lateinit var environment: BenchmarkEnvironment
    private lateinit var server: CsIdeServer

    @Setup(Level.Trial)
    fun trialSetup() {
        val jar = extensionApiJarPath()
        val javaHome = jdkHome()
        val launchSpec =
            CsIdeLaunch.spec(
                CsIdeLaunchEnvironment(),
                javaHome,
                jar,
                CsIdeLaunch.defaultThreads(Runtime.getRuntime().availableProcessors()),
                System.getProperty("os.name"),
                System.getenv(),
            )
        server = CsIdeServer(SystemCsIdeProcessFactory { launchSpec }, BenchmarkLogger)
        client = CsIdeClient(CsIdeTransport(server::request))
        environment = BenchmarkEnvironment()
    }

    @TearDown(Level.Trial)
    fun trialTearDown() {
        server.close()
        environment.close()
    }

    private fun extensionApiJarPath(): Path {
        val raw =
            System.getProperty(EXTENSION_API_JAR_PROPERTY)?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Missing system property: $EXTENSION_API_JAR_PROPERTY")
        val jar = Path.of(raw)
        if (!Files.isRegularFile(jar)) {
            throw IllegalStateException("Extension API jar not found: $jar")
        }
        return jar
    }

    private fun jdkHome(): Path {
        val fromEnv = System.getenv("JAVA_HOME")?.trim()?.takeIf { it.isNotEmpty() }
        if (fromEnv != null) return Path.of(fromEnv)
        val javaHome = Path.of(System.getProperty("java.home"))
        return if (javaHome.fileName.toString().equals("jre", ignoreCase = true)) {
            javaHome.parent
        } else {
            javaHome
        }
    }

    private companion object {
        const val EXTENSION_API_JAR_PROPERTY = "codescene.extension.api.jar"
    }
}

private object BenchmarkLogger : ILogger {
    override fun info(
        message: String,
        service: String?,
    ) = Unit

    override fun warn(
        message: String,
        service: String?,
    ) = Unit

    override fun debug(
        message: String,
        service: String?,
    ) = Unit

    override fun error(
        message: String,
        service: String?,
    ) = Unit
}
