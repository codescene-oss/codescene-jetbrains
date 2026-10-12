package com.codescene.jetbrains.core.cli

import com.codescene.ExtensionAPI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.max

data class CsIdeProxy(
    val host: String,
    val port: Int,
    val exceptions: String?,
)

data class CsIdeLaunchEnvironment(
    val shellEnvironment: Map<String, String> = emptyMap(),
    val pathPrefix: List<String> = emptyList(),
    val proxy: CsIdeProxy? = null,
    val extraJvmArgs: List<String> = emptyList(),
)

data class CsIdeLaunchSpec(
    val command: List<String>,
    val environment: Map<String, String>,
)

object CsIdeLaunch {
    const val MAX_HEAP: String = "-Xmx2g"

    private val PROXY_VARIABLES = listOf("https_proxy", "http_proxy")

    fun javaExecutable(
        javaHome: Path,
        osName: String,
    ): Path {
        val binary = if (isWindows(osName)) "java.exe" else "java"
        val executable = javaHome.resolve("bin").resolve(binary)
        if (!Files.isRegularFile(executable)) {
            throw IllegalStateException("Java executable not found: $executable")
        }
        return executable
    }

    fun extensionApiJar(): Path {
        val location =
            ExtensionAPI::class.java.protectionDomain?.codeSource?.location
                ?: throw IllegalStateException("Extension API jar location unavailable")
        val jar = Path.of(location.toURI())
        if (!Files.isRegularFile(jar)) {
            throw IllegalStateException("Extension API jar not found: $jar")
        }
        return jar
    }

    fun defaultThreads(cpus: Int): Int = max(1, cpus / 2)

    fun command(
        java: Path,
        jar: Path,
        threads: Int,
        extraJvmArgs: List<String>,
    ): List<String> =
        buildList {
            add(java.toString())
            add("--enable-native-access=ALL-UNNAMED")
            add(MAX_HEAP)
            addAll(extraJvmArgs)
            add("-cp")
            add(jar.toString())
            add("clojure.main")
            add("-m")
            add("codescene.cli.ide")
            add("server")
            add("--threads")
            add(threads.toString())
        }

    fun environment(
        processEnvironment: Map<String, String>,
        launch: CsIdeLaunchEnvironment,
        windows: Boolean,
    ): Map<String, String> {
        val merged = processEnvironment.toMutableMap()
        merged.putAll(launch.shellEnvironment)
        applyPathPrefix(merged, launch.pathPrefix, windows)
        applyProxy(merged, launch.proxy)
        return merged
    }

    fun spec(
        launch: CsIdeLaunchEnvironment,
        javaHome: Path,
        jar: Path,
        threads: Int,
        osName: String,
        processEnvironment: Map<String, String>,
    ): CsIdeLaunchSpec {
        val windows = isWindows(osName)
        val java = javaExecutable(javaHome, osName)
        val environment = environment(processEnvironment, launch, windows)
        val command = command(java, jar, threads, launch.extraJvmArgs)
        return CsIdeLaunchSpec(command, environment)
    }

    private fun isWindows(osName: String): Boolean = osName.lowercase().startsWith("windows")

    private fun applyPathPrefix(
        environment: MutableMap<String, String>,
        pathPrefix: List<String>,
        windows: Boolean,
    ) {
        if (pathPrefix.isEmpty()) return
        val pathKey = environment.keys.firstOrNull { it.equals("PATH", ignoreCase = true) } ?: "PATH"
        val separator = if (windows) ";" else ":"
        val existing = environment[pathKey]?.split(separator)?.filter { it.isNotBlank() } ?: emptyList()
        val prefixKeys = pathPrefix.map { pathEntryKey(it, windows) }.toSet()
        val prefix = pathPrefix.distinctBy { pathEntryKey(it, windows) }
        val rest = existing.filterNot { pathEntryKey(it, windows) in prefixKeys }
        environment[pathKey] = (prefix + rest).joinToString(separator)
    }

    private fun pathEntryKey(
        entry: String,
        windows: Boolean,
    ): String {
        val unquoted = entry.trim().removeSurrounding("\"")
        if (!windows) return unquoted.trimEnd('/').ifEmpty { unquoted }
        return unquoted.replace('/', '\\').trimEnd('\\').ifEmpty { unquoted }.lowercase()
    }

    private fun applyProxy(
        environment: MutableMap<String, String>,
        proxy: CsIdeProxy?,
    ) {
        if (proxy == null || hasEnvironment(environment, PROXY_VARIABLES)) return
        val proxyUrl = "http://${proxy.host}:${proxy.port}"
        environment["HTTPS_PROXY"] = proxyUrl
        environment["HTTP_PROXY"] = proxyUrl
        if (hasEnvironment(environment, listOf("no_proxy"))) return
        proxy.exceptions?.let { environment["NO_PROXY"] = it }
    }

    private fun hasEnvironment(
        environment: Map<String, String>,
        names: List<String>,
    ): Boolean = environment.keys.any { key -> names.any { it.equals(key, ignoreCase = true) } }
}
