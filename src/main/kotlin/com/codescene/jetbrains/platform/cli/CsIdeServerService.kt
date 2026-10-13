package com.codescene.jetbrains.platform.cli

import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeLaunch
import com.codescene.jetbrains.core.cli.CsIdeServer
import com.codescene.jetbrains.core.cli.CsIdeTransport
import com.codescene.jetbrains.core.cli.SystemCsIdeProcessFactory
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.nio.file.Path

@Service(Service.Level.APP)
class CsIdeServerService : Disposable {
    private val launchEnvironmentProvider = CsIdeLaunchEnvironmentProvider()
    private val server =
        CsIdeServer(
            processFactory = SystemCsIdeProcessFactory { buildLaunchSpec() },
            logger = Log,
        )
    private val csIdeClient = CsIdeClient(CsIdeTransport(server::request))

    fun client(): CsIdeClient = csIdeClient

    override fun dispose() {
        server.close()
    }

    private fun buildLaunchSpec() =
        CsIdeLaunch.spec(
            launch = launchEnvironmentProvider.environment(),
            javaHome = Path.of(System.getProperty("java.home")),
            jar = CsIdeLaunch.extensionApiJar(),
            threads = CsIdeLaunch.defaultThreads(Runtime.getRuntime().availableProcessors()),
            osName = System.getProperty("os.name"),
            processEnvironment = System.getenv(),
        )

    companion object {
        fun getInstance(): CsIdeServerService = service()
    }
}
