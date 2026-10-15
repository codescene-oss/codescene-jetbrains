package com.codescene.jetbrains.platform.cli

import com.codescene.jetbrains.core.cli.CsIdeLaunchEnvironment
import com.codescene.jetbrains.core.cli.CsIdeProxy
import com.codescene.jetbrains.core.cli.CsIdeTrustStore
import com.codescene.jetbrains.platform.util.Log
import com.intellij.openapi.application.PathManager
import com.intellij.util.EnvironmentUtil
import com.intellij.util.net.ProxyConfiguration
import com.intellij.util.net.ProxySettings
import com.intellij.util.net.ssl.CertificateManager
import git4idea.config.GitExecutableManager
import java.nio.file.Path
import java.security.cert.X509Certificate

class CsIdeLaunchEnvironmentProvider {
    fun environment(): CsIdeLaunchEnvironment {
        val shellEnvironment =
            runCatching { EnvironmentUtil.getEnvironmentMap() }
                .onFailure { error ->
                    Log.warn(
                        "Failed to read shell environment: ${error.message}",
                        SERVICE,
                    )
                }
                .getOrDefault(emptyMap())

        val pathPrefix =
            runCatching {
                val pathToGit = Path.of(GitExecutableManager.getInstance().pathToGit)
                if (pathToGit.isAbsolute) {
                    listOf(pathToGit.parent.toString())
                } else {
                    emptyList()
                }
            }
                .onFailure { error ->
                    Log.warn(
                        "Failed to resolve git executable path: ${error.message}",
                        SERVICE,
                    )
                }
                .getOrDefault(emptyList())

        val proxy =
            runCatching { resolveProxy() }
                .onFailure { error ->
                    Log.warn(
                        "Failed to resolve HTTP proxy: ${error.message}",
                        SERVICE,
                    )
                }
                .getOrNull()

        val extraJvmArgs = mutableListOf<String>()
        runCatching { writeTrustStoreJvmArgs(extraJvmArgs) }
            .onFailure { error ->
                Log.warn(
                    "Failed to write cs-ide trust store: ${error.message}",
                    SERVICE,
                )
            }

        return CsIdeLaunchEnvironment(
            shellEnvironment = shellEnvironment,
            pathPrefix = pathPrefix,
            proxy = proxy,
            extraJvmArgs = extraJvmArgs,
        )
    }

    private fun resolveProxy(): CsIdeProxy? {
        val configuration = ProxySettings.getInstance().getProxyConfiguration()
        if (configuration !is ProxyConfiguration.StaticProxyConfiguration) return null
        if (configuration.protocol != ProxyConfiguration.ProxyProtocol.HTTP) return null
        return CsIdeProxy(
            host = configuration.host,
            port = configuration.port,
            exceptions = configuration.exceptions,
        )
    }

    private fun writeTrustStoreJvmArgs(extraJvmArgs: MutableList<String>) {
        val target =
            Path.of(
                PathManager.getSystemPath(),
                "codescene",
                "cs-ide-truststore.p12",
            )
        val certificates = trustStoreCertificates()
        CsIdeTrustStore.write(certificates, target)
        extraJvmArgs.addAll(CsIdeTrustStore.jvmArgs(target))
    }

    private fun trustStoreCertificates(): List<X509Certificate> {
        val ideIssuers = CertificateManager.getInstance().trustManager.acceptedIssuers.toList()
        return ideIssuers + CsIdeTrustStore.jdkDefaultIssuers()
    }

    private companion object {
        const val SERVICE = "CsIdeLaunchEnvironmentProvider"
    }
}
