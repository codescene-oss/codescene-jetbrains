package com.codescene.jetbrains.platform.webview

import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertTrue
import org.junit.Test

class CwfImportantLinksBundleTest {
    @Test
    fun `cwf bundle contains important external links`() {
        val bundle = readCwfBundle()

        val expectedLinks =
            listOf(
                "Documentation" to "https://codescene.io/docs",
                "Terms and Policies" to "https://codescene.com/policies",
                "AI Privacy Principles" to "https://codescene.com/product/ace/principles",
                "Contact CodeScene" to "https://codescene.com/company/contact-us",
                "Help Center" to "https://helpcenter.codescene.com/",
                "Report a Bug" to "https://forms.clickup.com/9015696197/f/8cp16u5-7955/P24KVTPFDHW9G36D17",
            )

        expectedLinks.forEach { (label, url) ->
            assertContainsImportantLink(bundle, label, url)
        }
    }

    private fun readCwfBundle(): String {
        val bundlePath = findCwfBundlePath()

        return Files.readString(bundlePath)
    }

    private fun findCwfBundlePath(): Path {
        val candidateRoots =
            listOfNotNull(
                System.getenv("GITHUB_WORKSPACE")?.let { Path.of(it) },
                Path.of(System.getProperty("user.dir")),
            ).flatMap { root ->
                generateSequence(root.toAbsolutePath()) { it.parent }.take(12).toList()
            }.distinct()

        val searchedPaths = candidateRoots.map { it.resolve(CWF_BUNDLE_PATH) }
        return searchedPaths.firstOrNull { Files.isRegularFile(it) }
            ?: error("Expected CWF bundle at one of: ${searchedPaths.joinToString()}")
    }

    private fun assertContainsImportantLink(
        bundle: String,
        label: String,
        url: String,
    ) {
        val pattern =
            Regex(
                "label:\\s*\"" + Regex.escape(label) + "\"[\\s\\S]{0,300}?link:\\s*\"" + Regex.escape(url) + "\"",
            )

        assertTrue(
            "Expected CWF bundle to contain important link [$label] -> [$url].",
            pattern.containsMatchIn(bundle),
        )
    }

    private companion object {
        private val CWF_BUNDLE_PATH = Path.of("src/main/resources/cs-cwf/index.js")
    }
}
