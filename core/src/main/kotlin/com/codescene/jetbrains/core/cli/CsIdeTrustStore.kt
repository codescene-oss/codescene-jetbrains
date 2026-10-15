package com.codescene.jetbrains.core.cli

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

object CsIdeTrustStore {
    const val PASSWORD: String = "changeit"

    fun write(
        certificates: Collection<X509Certificate>,
        target: Path,
    ): Path {
        val store = KeyStore.getInstance("PKCS12")
        store.load(null, null)
        certificates.forEachIndexed { index, certificate ->
            store.setCertificateEntry("cert-$index", certificate)
        }
        target.parent?.let { Files.createDirectories(it) }
        Files.newOutputStream(target).use { output ->
            store.store(output, PASSWORD.toCharArray())
        }
        return target
    }

    fun jvmArgs(path: Path): List<String> =
        listOf(
            "-Djavax.net.ssl.trustStore=$path",
            "-Djavax.net.ssl.trustStoreType=PKCS12",
            "-Djavax.net.ssl.trustStorePassword=$PASSWORD",
        )

    fun jdkDefaultIssuers(): List<X509Certificate> {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .flatMap { manager -> manager.acceptedIssuers?.toList() ?: emptyList() }
    }
}
