package com.codescene.jetbrains.core.cli

import java.io.File
import java.nio.file.Path

object CsIdeProcessTreeFixture {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.firstOrNull() != "child") {
            val child = ProcessBuilder(command("child")).start()
            println(child.pid())
            System.out.flush()
        }
        Thread.sleep(60_000)
    }

    fun command(vararg args: String): List<String> =
        listOf(javaExecutable(), "-cp", classpath(), CsIdeProcessTreeFixture::class.java.name) + args

    private fun javaExecutable(): String = Path.of(System.getProperty("java.home"), "bin", "java").toString()

    private fun classpath(): String =
        listOf(CsIdeProcessTreeFixture::class.java, Unit::class.java)
            .map { Path.of(it.protectionDomain.codeSource.location.toURI()).toString() }
            .distinct()
            .joinToString(File.pathSeparator)
}
