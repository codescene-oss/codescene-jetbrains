package com.codescene.jetbrains.platform.cli

import com.codescene.jetbrains.core.cli.CsIdeDistribution
import com.codescene.jetbrains.platform.util.PlatformConstants.CODESCENE_PLUGIN_ID
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.extensions.PluginId
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.zip.ZipInputStream

object CsIdeRuntime {
    fun resolve(): Path {
        val env = CsIdeDistribution.envDistributionPath()
        if (env != null && CsIdeDistribution.isComplete(env)) {
            return env
        }
        val bundled = bundledDistribution()
        if (bundled != null && CsIdeDistribution.isComplete(bundled)) {
            return bundled
        }
        val cached = cachedDistribution()
        if (CsIdeDistribution.isComplete(cached)) {
            return cached
        }
        download(cached)
        return cached
    }

    private fun bundledDistribution(): Path? {
        val pluginPath =
            PluginManagerCore.getPlugin(PluginId.getId(CODESCENE_PLUGIN_ID))?.pluginPath ?: return null
        val dist = pluginPath.resolve(CsIdeDistribution.distributionName())
        return dist.takeIf { CsIdeDistribution.isComplete(it) }
    }

    private fun cachedDistribution(): Path =
        Path.of(
            PathManager.getSystemPath(),
            "codescene",
            "cs-ide",
            CsIdeDistribution.requiredSha(),
            CsIdeDistribution.distributionName(),
        )

    private fun download(target: Path) {
        val artifact = CsIdeDistribution.artifactName()
        val url = URI.create("${CsIdeDistribution.DOWNLOAD_BASE}/$artifact")
        val parent = target.parent
        Files.createDirectories(parent)
        val zip = parent.resolve(artifact)
        val client =
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build()
        val request =
            HttpRequest.newBuilder(url)
                .timeout(Duration.ofMinutes(5))
                .header("cache-control", "max-age=0")
                .GET()
                .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofFile(zip))
        if (response.statusCode() != 200) {
            Files.deleteIfExists(zip)
            throw IllegalStateException("Failed to download cs-ide distribution: HTTP ${response.statusCode()}")
        }
        val extractDir = parent.resolve(".extract-${CsIdeDistribution.distributionName()}")
        deleteRecursively(extractDir)
        Files.createDirectories(extractDir)
        unzip(zip, extractDir)
        val extracted =
            if (Files.isRegularFile(extractDir.resolve("cs-ide.jar"))) {
                extractDir
            } else {
                Files.list(extractDir).use { stream ->
                    stream.filter { Files.isDirectory(it) }.findFirst().orElse(extractDir)
                }
            }
        deleteRecursively(target)
        copyRecursively(extracted, target)
        if (CsIdeDistribution.hostPlatform() != "win32") {
            CsIdeDistribution.javaExecutable(target).toFile().setExecutable(true)
        }
        Files.deleteIfExists(zip)
        deleteRecursively(extractDir)
        if (!CsIdeDistribution.isComplete(target)) {
            throw IllegalStateException("Downloaded cs-ide distribution is incomplete: $target")
        }
    }

    private fun unzip(
        zip: Path,
        outputDir: Path,
    ) {
        ZipInputStream(Files.newInputStream(zip)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = outputDir.resolve(entry.name).normalize()
                if (!out.startsWith(outputDir)) {
                    throw IllegalStateException("Zip entry escapes target directory: ${entry.name}")
                }
                if (entry.isDirectory) {
                    Files.createDirectories(out)
                } else {
                    Files.createDirectories(out.parent)
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    private fun copyRecursively(
        source: Path,
        target: Path,
    ) {
        Files.walk(source).use { paths ->
            paths.forEach { path ->
                val relative = source.relativize(path)
                val dest = target.resolve(relative.toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(dest)
                } else {
                    Files.createDirectories(dest.parent)
                    Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
