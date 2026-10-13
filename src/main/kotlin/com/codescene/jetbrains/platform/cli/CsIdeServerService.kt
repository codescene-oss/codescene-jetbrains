package com.codescene.jetbrains.platform.cli

import com.codescene.jetbrains.core.cli.CsIdeClient
import com.codescene.jetbrains.core.cli.CsIdeLaunch
import com.codescene.jetbrains.core.cli.CsIdeServer
import com.codescene.jetbrains.core.cli.CsIdeTransport
import com.codescene.jetbrains.core.cli.SystemCsIdeProcessFactory
import com.codescene.jetbrains.core.cli.WatchClient
import com.codescene.jetbrains.platform.util.Log
import com.fasterxml.jackson.databind.JsonNode
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
    private val watchClient =
        WatchClient(
            server::notifyInSession,
            server::notifyIfRunning,
            CsIdeTransport(server::request),
        )

    fun client(): CsIdeClient = csIdeClient

    fun watchClient(): WatchClient = watchClient

    fun addSessionStartListener(listener: (Long) -> Unit): AutoCloseable = server.addSessionStartListener(listener)

    fun liveSession(): Long = server.liveSession()

    fun cooldownRemainingMs(): Long = server.cooldownRemainingMs()

    fun notify(
        method: String,
        params: JsonNode,
    ) {
        server.notify(method, params)
    }

    fun addNotificationHandler(handler: (String, JsonNode) -> Unit): AutoCloseable =
        server.addNotificationHandler(handler)

    fun addSessionListener(listener: () -> Unit): AutoCloseable = server.addSessionListener(listener)

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
