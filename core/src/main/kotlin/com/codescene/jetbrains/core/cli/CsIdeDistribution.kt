package com.codescene.jetbrains.core.cli

import java.nio.file.Files
import java.nio.file.Path

object CsIdeDistribution {
    const val REQUIRED_SHA = "21ac9263043b0bca2f216edbd61255f54edc77b3"
    const val DOWNLOAD_BASE = "https://downloads.codescene.io/enterprise/cli"
    const val DISTRIBUTION_PATH_ENV = "CS_IDE_DISTRIBUTION_PATH"
    const val REQUIRED_VERSION_ENV = "CS_IDE_REQUIRED_VERSION"
    const val STARTUP_TIMEOUT_MS = 30_000L

    fun requiredSha(): String = System.getenv(REQUIRED_VERSION_ENV)?.trim()?.takeIf { it.isNotEmpty() } ?: REQUIRED_SHA

    fun envDistributionPath(): Path? =
        System.getenv(DISTRIBUTION_PATH_ENV)?.trim()?.takeIf { it.isNotEmpty() }?.let { Path.of(it) }

    fun hostPlatform(): String {
        val os = System.getProperty("os.name").lowercase()
        return when {
            os.contains("win") -> "win32"
            os.contains("mac") -> "darwin"
            else -> "linux"
        }
    }

    fun hostArch(): String {
        val arch = System.getProperty("os.arch").lowercase()
        return if (arch.contains("aarch64") || arch.contains("arm64")) "arm64" else "x64"
    }

    fun distributionName(
        platform: String = hostPlatform(),
        arch: String = hostArch(),
    ): String = "cs-$platform-$arch"

    fun artifactName(
        sha: String = requiredSha(),
        platform: String = hostPlatform(),
        arch: String = hostArch(),
    ): String {
        val osToken =
            when (platform) {
                "win32" -> "windows"
                "darwin" -> "macos"
                else -> "linux"
            }
        val archToken =
            when (arch) {
                "arm64" -> "aarch64"
                else -> "amd64"
            }
        return "cs-ide-jre-$osToken-$archToken-$sha.zip"
    }

    fun javaExecutable(distribution: Path): Path {
        val binary = if (hostPlatform() == "win32") "java.exe" else "java"
        return distribution.resolve("jre").resolve("bin").resolve(binary)
    }

    fun jar(distribution: Path): Path = distribution.resolve("cs-ide.jar")

    fun isComplete(distribution: Path): Boolean =
        Files.isRegularFile(javaExecutable(distribution)) && Files.isRegularFile(jar(distribution))
}
