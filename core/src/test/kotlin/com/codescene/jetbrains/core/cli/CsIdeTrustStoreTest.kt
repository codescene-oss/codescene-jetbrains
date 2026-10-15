package com.codescene.jetbrains.core.cli

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.X509Certificate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CsIdeTrustStoreTest {
    @Test
    fun `write produces PKCS12 store with every certificate`() {
        val issuers = CsIdeTrustStore.jdkDefaultIssuers().take(3)
        assertFalse(issuers.isEmpty())
        val dir = Files.createTempDirectory("cs-ide-trust")
        val target = dir.resolve("trust.p12")
        try {
            CsIdeTrustStore.write(issuers, target)
            val store = KeyStore.getInstance("PKCS12")
            Files.newInputStream(target).use { input ->
                store.load(input, CsIdeTrustStore.PASSWORD.toCharArray())
            }
            val aliases = store.aliases().asSequence().toList()
            assertEquals(issuers.size, aliases.size)
            issuers.forEach { cert ->
                assertTrue(
                    aliases.any {
                            alias ->
                        certificatesEqual(cert, store.getCertificate(alias) as X509Certificate)
                    },
                )
            }
        } finally {
            Files.walk(dir).sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    @Test
    fun `jvmArgs names trust store path type and password`() {
        val path = Path.of("/tmp/trust.p12")
        val args = CsIdeTrustStore.jvmArgs(path)

        assertEquals(
            listOf(
                "-Djavax.net.ssl.trustStore=$path",
                "-Djavax.net.ssl.trustStoreType=PKCS12",
                "-Djavax.net.ssl.trustStorePassword=${CsIdeTrustStore.PASSWORD}",
            ),
            args,
        )
    }

    @Test
    fun `jdkDefaultIssuers is non-empty`() {
        assertTrue(CsIdeTrustStore.jdkDefaultIssuers().isNotEmpty())
    }

    private fun certificatesEqual(
        left: X509Certificate,
        right: X509Certificate,
    ): Boolean = left.serialNumber == right.serialNumber && left.subjectX500Principal == right.subjectX500Principal
}
