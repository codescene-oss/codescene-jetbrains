package com.codescene.jetbrains.core.cli

import com.codescene.ExtensionAPI
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeLaunchTest {
    @Test
    fun `jarFromResourceUrl resolves jar paths containing spaces`() {
        val jar = Path.of(System.getProperty("java.io.tmpdir"), "dir with space", "api-1.0.jar").toAbsolutePath()
        val url = URL("jar:${jar.toUri()}!/com/codescene/ExtensionAPI.class")

        assertEquals(jar, CsIdeLaunch.jarFromResourceUrl(url))
    }

    @Test
    fun `jarFromResourceUrl ignores non-jar urls`() {
        val url = Path.of(System.getProperty("java.io.tmpdir"), "ExtensionAPI.class").toUri().toURL()

        assertNull(CsIdeLaunch.jarFromResourceUrl(url))
    }

    @Test
    fun `javaExecutable uses java exe on Windows`() {
        val home = Files.createTempDirectory("cs-ide-win-java").also { it.toFile().deleteOnExit() }
        val executable = home.resolve("bin").resolve("java.exe")
        Files.createDirectories(executable.parent)
        Files.createFile(executable)
        try {
            assertEquals(executable, CsIdeLaunch.javaExecutable(home, "Windows 11"))
        } finally {
            Files.deleteIfExists(executable)
            Files.deleteIfExists(executable.parent)
            Files.deleteIfExists(home)
        }
    }

    @Test
    fun `javaExecutable uses java elsewhere`() {
        val home = Files.createTempDirectory("cs-ide-unix-java").also { it.toFile().deleteOnExit() }
        val executable = home.resolve("bin").resolve("java")
        Files.createDirectories(executable.parent)
        Files.createFile(executable)
        try {
            assertEquals(executable, CsIdeLaunch.javaExecutable(home, "Linux"))
            assertEquals(executable, CsIdeLaunch.javaExecutable(home, "Darwin"))
        } finally {
            Files.deleteIfExists(executable)
            Files.deleteIfExists(executable.parent)
            Files.deleteIfExists(home)
        }
    }

    @Test
    fun `javaExecutable fails when missing`() {
        val home = Files.createTempDirectory("cs-ide-launch").also { it.toFile().deleteOnExit() }
        try {
            val ex =
                kotlin.runCatching {
                    CsIdeLaunch.javaExecutable(home, "Linux")
                }.exceptionOrNull()
            assertTrue(ex is IllegalStateException)
            assertTrue(ex!!.message!!.contains(home.resolve("bin").resolve("java").toString()))
        } finally {
            Files.deleteIfExists(home)
        }
    }

    @Test
    fun `defaultThreads halves cpus with a floor of one`() {
        assertEquals(4, CsIdeLaunch.defaultThreads(8))
        assertEquals(1, CsIdeLaunch.defaultThreads(1))
    }

    @Test
    fun `command lists java flags jar and server entrypoint`() {
        val java = Path.of("/jbr/bin/java")
        val jar = Path.of("/lib/extension-api.jar")
        val command = CsIdeLaunch.command(java, jar, 2, listOf("-Dfoo=bar"))

        assertEquals(
            listOf(
                java.toString(),
                "--enable-native-access=ALL-UNNAMED",
                CsIdeLaunch.MAX_HEAP,
                "-Dfoo=bar",
                "-cp",
                jar.toString(),
                "clojure.main",
                "-m",
                "codescene.cli.ide",
                "server",
                "--threads",
                "2",
            ),
            command,
        )
    }

    @Test
    fun `environment overlays shell variables onto process environment`() {
        val process = mapOf("A" to "process", "B" to "old")
        val launch = CsIdeLaunchEnvironment(shellEnvironment = mapOf("B" to "shell", "C" to "new"))

        val env = CsIdeLaunch.environment(process, launch, windows = false)

        assertEquals("process", env["A"])
        assertEquals("shell", env["B"])
        assertEquals("new", env["C"])
    }

    @Test
    fun `environment prepends pathPrefix once on Unix`() {
        val launch =
            CsIdeLaunchEnvironment(
                pathPrefix = listOf("/opt/git/bin", "/extra"),
            )
        val env =
            CsIdeLaunch.environment(
                mapOf("PATH" to "/usr/bin:/bin"),
                launch,
                windows = false,
            )

        assertEquals("/opt/git/bin:/extra:/usr/bin:/bin", env["PATH"])
    }

    @Test
    fun `environment moves pathPrefix already on PATH to the front case insensitively on Windows`() {
        val launch = CsIdeLaunchEnvironment(pathPrefix = listOf("C:\\Git\\cmd"))
        val env =
            CsIdeLaunch.environment(
                mapOf("Path" to "C:\\Windows;c:\\git\\cmd\\"),
                launch,
                windows = true,
            )

        assertEquals("C:\\Git\\cmd;C:\\Windows", env["Path"])
    }

    @Test
    fun `environment tolerates quoted and invalid PATH entries on Windows`() {
        val launch = CsIdeLaunchEnvironment(pathPrefix = listOf("C:\\Git\\cmd"))
        val env =
            CsIdeLaunch.environment(
                mapOf("PATH" to "\"C:\\Program Files\\Tool\";C:\\odd<dir>;\"C:\\Git\\cmd\""),
                launch,
                windows = true,
            )

        assertEquals("C:\\Git\\cmd;\"C:\\Program Files\\Tool\";C:\\odd<dir>", env["PATH"])
    }

    @Test
    fun `environment uses semicolon separator on Windows`() {
        val launch = CsIdeLaunchEnvironment(pathPrefix = listOf("D:\\tools"))
        val env =
            CsIdeLaunch.environment(
                mapOf("PATH" to "C:\\Windows"),
                launch,
                windows = true,
            )

        assertEquals("D:\\tools;C:\\Windows", env["PATH"])
    }

    @Test
    fun `environment sets proxy variables when none exist`() {
        val launch = CsIdeLaunchEnvironment(proxy = CsIdeProxy("proxy.example", 8080, "localhost,127.0.0.1"))
        val env = CsIdeLaunch.environment(emptyMap(), launch, windows = false)

        assertEquals("http://proxy.example:8080", env["HTTPS_PROXY"])
        assertEquals("http://proxy.example:8080", env["HTTP_PROXY"])
        assertEquals("localhost,127.0.0.1", env["NO_PROXY"])
    }

    @Test
    fun `environment leaves proxy variables unchanged when already set`() {
        val launch = CsIdeLaunchEnvironment(proxy = CsIdeProxy("proxy.example", 8080, "localhost"))
        val env =
            CsIdeLaunch.environment(
                mapOf("HTTP_PROXY" to "http://existing:3128"),
                launch,
                windows = false,
            )

        assertEquals("http://existing:3128", env["HTTP_PROXY"])
        assertFalse(env.containsKey("HTTPS_PROXY"))
        assertFalse(env.containsKey("NO_PROXY"))
    }

    @Test
    fun `environment sets proxy but keeps an existing no_proxy`() {
        val launch = CsIdeLaunchEnvironment(proxy = CsIdeProxy("proxy.example", 8080, "localhost"))
        val env =
            CsIdeLaunch.environment(
                mapOf("no_proxy" to ".corp.example"),
                launch,
                windows = false,
            )

        assertEquals("http://proxy.example:8080", env["HTTPS_PROXY"])
        assertEquals("http://proxy.example:8080", env["HTTP_PROXY"])
        assertEquals(".corp.example", env["no_proxy"])
        assertFalse(env.containsKey("NO_PROXY"))
    }

    @Test
    fun `extensionApiJar resolves to an existing jar`() {
        val jar = CsIdeLaunch.extensionApiJar()
        assertTrue(jar.toString().endsWith(".jar", ignoreCase = true))
        assertTrue(Files.isRegularFile(jar))
        val fromApi = Path.of(ExtensionAPI::class.java.protectionDomain.codeSource.location.toURI())
        assertEquals(fromApi.toAbsolutePath().normalize(), jar.toAbsolutePath().normalize())
    }

    @Test
    fun `spec builds command and environment together`() {
        val javaHome = Path.of(System.getProperty("java.home"))
        val jar = Path.of("/lib/api.jar")
        val launch = CsIdeLaunchEnvironment(extraJvmArgs = listOf("-Dtest=1"))
        val spec =
            CsIdeLaunch.spec(
                launch,
                javaHome,
                jar,
                threads = 1,
                osName = System.getProperty("os.name"),
                processEnvironment = mapOf("HOME" to "/home"),
            )

        assertTrue(spec.command.first().endsWith("java") || spec.command.first().endsWith("java.exe"))
        assertEquals("/home", spec.environment["HOME"])
        assertTrue(spec.command.contains("-Dtest=1"))
    }
}
