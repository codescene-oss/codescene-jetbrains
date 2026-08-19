import groovy.json.JsonSlurper
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipInputStream
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.ktlint)
    jacoco
    kotlin("plugin.serialization") version "2.2.0"
}

group = providers.gradleProperty("pluginGroup").get()
val basePluginVersion = providers.gradleProperty("pluginVersion")
val effectivePluginVersion = providers.gradleProperty("releaseVersion").orElse(basePluginVersion)
version = effectivePluginVersion.get()

val mockkVersion = providers.gradleProperty("mockkVersion").get()
val kotlinxSerializationVersion = providers.gradleProperty("kotlinxSerializationVersion").get()
val slf4jNopVersion = providers.gradleProperty("slf4jNopVersion").get()
val kotlinxCoroutinesVersion = providers.gradleProperty("kotlinxCoroutinesVersion").get()
val jacksonVersion = providers.gradleProperty("jacksonVersion").get()
val csIdeRequiredSha = providers.gradleProperty("csIdeRequiredSha").get()

fun requiredEnv(name: String): String =
    System.getenv(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("Missing required environment variable: $name")

fun optionalEnv(name: String): String? =
    System.getenv(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val cwfTokenEnvName =
    if (System.getenv("CI") == "true") {
        "CODESCENE_IDE_DOCS_AND_WEBVIEW_TOKEN"
    } else {
        "GH_PACKAGE_TOKEN"
    }

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenLocal()
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")

    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")

    // Provide a no-op SLF4J binding during tests to avoid "No binding found" errors.
    // Some libraries (e.g., MockK or IntelliJ SDK components) rely on SLF4J at runtime.
    testRuntimeOnly("org.slf4j:slf4j-nop:$slf4jNopVersion")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = effectivePluginVersion

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description =
            providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                with(it.lines()) {
                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
                }
            }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes =
            effectivePluginVersion.map { pluginVersion ->
                with(changelog) {
                    renderItem(
                        (getOrNull(pluginVersion) ?: getUnreleased())
                            .withHeader(false)
                            .withEmptySections(false),
                        Changelog.OutputType.HTML,
                    )
                }
            }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-custom-channel
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels =
            effectivePluginVersion
                .map { listOf(it.substringAfter('-', "").ifEmpty { "default" }) }
        hidden = true
    }

    pluginVerification {
        failureLevel.set(listOf(VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS))
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.3.7") { useInstaller = true }
            create(IntelliJPlatformType.IntellijIdea, "2025.3.2") { useInstaller = true }
            create(IntelliJPlatformType.IntellijIdea, "2026.2.1") { useInstaller = true }
            select {
                // 2026.1 and later
                sinceBuild = "261"
                types = listOf(IntelliJPlatformType.IntellijIdea)
                channels =
                    listOf(
                        ProductRelease.Channel.EAP,
                    )
            }
        }
    }
}

val ktlintFailOnError =
    providers.gradleProperty("ktlintFailOnError")
        .map(String::toBoolean)
        .orElse(false)

ktlint {
    // Keep default warning-only behavior unless explicitly overridden via -PktlintFailOnError=true.
    ignoreFailures.set(ktlintFailOnError.map { failOnError -> !failOnError })

    version.set("1.2.1")
    android.set(false)
    outputToConsole.set(true)
    enableExperimentalRules.set(false)

    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    runIde {
        classpath += sourceSets["main"].runtimeClasspath
        dependsOn("bundleCli")

        val devMode = project.properties["FEATURE_CWF_DEVMODE"]?.toString()?.toBoolean() ?: false

        systemProperty("FEATURE_CWF_DEVMODE", devMode)
    }

    register("bundleCli") {
        group = "codescene assets"
        description = "Download the host cs-ide JRE+jar distribution used by runIde."
        val sha = csIdeRequiredSha
        val distDir = layout.buildDirectory.dir("cs-ide")
        inputs.property("csIdeRequiredSha", sha)
        outputs.dir(distDir)
        doLast {
            bundleCsIdeDistribution(sha, distDir.get().asFile)
        }
    }

    withType<org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask>().configureEach {
        val pluginName = providers.gradleProperty("pluginName")
        val (platform, arch) = hostCsIdePlatform()
        val distName = "cs-$platform-$arch"
        dependsOn("bundleCli")
        from(layout.buildDirectory.dir("cs-ide/$distName")) {
            into("${pluginName.get()}/$distName")
        }
    }

    register<JavaExec>("run") {
        mainClass.set("com.codescene.Main")
        classpath += sourceSets["main"].runtimeClasspath
    }

    buildPlugin {
        dependsOn("fetchCwf")
        dependsOn("processResources")
    }

    withType<Test>().configureEach {
        extensions.configure<org.gradle.testing.jacoco.plugins.JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
        }
        val instrumentCodeTask = named("instrumentCode").get()
        classDirectories.setFrom(instrumentCodeTask.outputs.files)
    }

    jacocoTestCoverageVerification {
        classDirectories.setFrom(named("instrumentCode").get().outputs.files)
    }

    register<JacocoReport>("jacocoMergedReport") {
        dependsOn(test, ":core:test")

        val rootTestTask = test.get()
        val coreTestTask = project(":core").tasks.named<Test>("test").get()

        executionData(rootTestTask, coreTestTask)

        val instrumentCodeTask = named("instrumentCode").get()
        classDirectories.setFrom(
            instrumentCodeTask.outputs.files,
            project(":core").the<SourceSetContainer>()["main"].output,
        )
        sourceDirectories.setFrom(
            sourceSets["main"].allSource,
            project(":core").the<SourceSetContainer>()["main"].allSource,
        )

        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders +=
                    CommandLineArgumentProvider {
                        listOf(
                            "-Drobot-server.port=8082",
                            "-Dide.mac.message.dialogs.as.sheets=false",
                            "-Djb.privacy.policy.text=<!--999.999-->",
                            "-Djb.consents.confirmation.enabled=false",
                        )
                    }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}

// Replace placeholders in feature-flags.properties with actual Gradle properties
// so that the plugin can read configured flags at runtime.
tasks.processResources {
    // Only use properties if explicitly set via -P flag, default to "false" otherwise
    val cwfDevmodeProperty = project.findProperty("FEATURE_CWF_DEVMODE")

    inputs.property("FEATURE_CWF_DEVMODE", cwfDevmodeProperty ?: "false")

    filesMatching("feature-flags.properties") {
        val featureCwfDevMode =
            cwfDevmodeProperty?.let { devMode ->
                devMode.toString().takeIf { it.isNotBlank() } ?: "false"
            } ?: "false"

        expand(
            "FEATURE_CWF_DEVMODE" to featureCwfDevMode,
        )
    }
}

tasks.register("fetchCwf") {
    group = "codescene assets"
    description = "Get the CWF (webview) asset from the latest GitHub release."

    val assetName = "cs-webview"
    val assetType = "cs-cwf"
    val user = "empear-analytics"
    val repo = "cs-webview"

    doLast {
        val token = requiredEnv(cwfTokenEnvName)
        val apiUrl = "https://api.github.com/repos/$user/$repo/releases"

        val releasesJson =
            run {
                val url = URI.create(apiUrl).toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "token $token")
                connection.inputStream.reader().readText()
            }

        val (tag, assetUrl) = parseResponse(releasesJson, assetName)
        saveAsset(tag, assetUrl, token, assetType)
    }
}

tasks.register("verifyCwfImportantLinks") {
    group = "codescene assets"
    description = "Verify important external links in the generated CWF bundle."

    dependsOn("fetchCwf")

    doLast {
        val bundleFile = File("src/main/resources/cs-cwf/index.js")
        if (!bundleFile.isFile) {
            throw GradleException(
                "Expected generated CWF bundle at ${bundleFile.absolutePath}. " +
                    "Run fetchCwf before verifying CWF links.",
            )
        }

        val bundle = bundleFile.readText()
        val expectedLinks =
            listOf(
                "Documentation" to "https://codescene.io/docs",
                "Terms and Policies" to "https://codescene.com/policies",
                "AI Privacy Principles" to "https://codescene.com/product/ace/principles",
                "Contact CodeScene" to "https://codescene.com/company/contact-us",
                "Help Center" to "https://helpcenter.codescene.com/",
                "Report a Bug" to (
                    "https://forms.clickup.com/9015696197/f/" +
                        "8cp16u5-7955/P24KVTPFDHW9G36D17"
                ),
            )

        val objectPattern = Regex("""\{[^{}]{0,1500}}""")

        expectedLinks.forEach { (label, url) ->
            val labelPattern = Regex("""label\s*:\s*["']${Regex.escape(label)}["']""")
            val linkPattern = Regex("""link\s*:\s*["']${Regex.escape(url)}["']""")
            val hasMatchingObject =
                objectPattern.findAll(bundle).any { match ->
                    val item = match.value
                    labelPattern.containsMatchIn(item) && linkPattern.containsMatchIn(item)
                }

            if (!hasMatchingObject) {
                throw GradleException(
                    "Expected generated CWF bundle to contain important link " +
                        "[$label] -> [$url].",
                )
            }
        }
    }
}

tasks.named("buildPlugin") {
    dependsOn("verifyCwfImportantLinks")
}

@Suppress("UNCHECKED_CAST")
fun parseResponse(
    json: String,
    assetName: String,
): Pair<String, String> {
    val releases = JsonSlurper().parseText(json) as List<Map<String, Any>>
    val release =
        releases.find { it["prerelease"] == false && it["draft"] == false }
            ?: throw GradleException("No suitable release found.")
    val tag = release["tag_name"] as String

    val assets = release["assets"] as List<Map<String, Any>>

    val latest = "$assetName-$tag.zip"
    val asset =
        assets.find { (it["name"] as String) == latest }
            ?: throw GradleException("No $assetName found in the latest release.")

    val assetUrl = asset["url"] as String

    logger.trace("Found $assetName for release: $tag")

    return tag to assetUrl
}

fun saveAsset(
    tag: String,
    assetUrl: String,
    token: String,
    assetType: String = "",
) {
    logger.lifecycle("Downloading '$assetType' assets for release: $tag")

    val resources = File("src/main/resources")
    val assetFolder = File(resources, assetType)
    val outputFile = File(assetFolder, "$tag.zip")

    if (assetFolder.exists()) {
        assetFolder.listFiles()?.forEach { it.deleteRecursively() }
        logger.debug("Deleted old asset files: ${assetFolder.absolutePath}")
    } else {
        assetFolder.mkdirs()
        logger.debug("Created asset folder: ${assetFolder.absolutePath}")
    }

    val url = URI.create(assetUrl).toURL()
    val assets =
        url.openConnection().apply {
            setRequestProperty("Authorization", "token $token")
            setRequestProperty("Accept", "application/octet-stream")
            connect()
        }

    outputFile.outputStream().use {
        assets.inputStream.use { input ->
            input.copyTo(it)
        }
    }

    logger.lifecycle("Download completed: ${outputFile.absolutePath}")

    unzip(outputFile, assetFolder)
}

fun unzip(
    zipFile: File,
    outputDir: File,
) {
    logger.info("Unzipping ${zipFile.name}...")

    ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry

        while (entry != null) {
            val relativePath =
                entry.name
                    .split("/")
                    .drop(1)
                    .joinToString("/")
                    .takeIf { it.isNotEmpty() }
                    ?: entry.name

            val outFile = File(outputDir, relativePath)
            val outDirCanonical = outputDir.canonicalPath
            val outCanonical = outFile.canonicalPath

            require(outCanonical == outDirCanonical || outCanonical.startsWith(outDirCanonical + File.separator)) {
                "Zip entry escapes target directory: ${entry.name}"
            }

            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile.mkdirs()
                outFile.outputStream().use { fos -> zis.copyTo(fos) }
            }

            zis.closeEntry()
            entry = zis.nextEntry
        }
    }

    logger.info("Unzip completed to: ${outputDir.absolutePath}")

    if (zipFile.exists() && zipFile.delete()) {
        logger.debug("Cleaned up ZIP file: ${zipFile.absolutePath}")
    } else {
        logger.warn("Failed to delete ZIP file: ${zipFile.absolutePath}")
    }
}

val CS_IDE_SHA_STAMP = ".cs-ide-sha"

fun hostCsIdePlatform(): Pair<String, String> {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val platform =
        when {
            os.contains("win") -> "win32"
            os.contains("mac") -> "darwin"
            else -> "linux"
        }
    val cliArch = if (arch.contains("aarch64") || arch.contains("arm64")) "arm64" else "x64"
    return platform to cliArch
}

fun csIdeArtifactName(
    sha: String,
    platform: String,
    arch: String,
): String {
    val osToken =
        when (platform) {
            "win32" -> "windows"
            "darwin" -> "macos"
            else -> "linux"
        }
    val archToken = if (arch == "arm64") "aarch64" else "amd64"
    return "cs-ide-jre-$osToken-$archToken-$sha.zip"
}

fun bundleCsIdeDistribution(
    sha: String,
    outputRoot: File,
) {
    val envPath = System.getenv("CS_IDE_DISTRIBUTION_PATH")?.trim()?.takeIf { it.isNotEmpty() }
    val (platform, arch) = hostCsIdePlatform()
    val distName = "cs-$platform-$arch"
    val target = File(outputRoot, distName)
    if (envPath != null) {
        val source = File(envPath)
        copyCsIdeDistribution(source, target, platform)
        File(target, CS_IDE_SHA_STAMP).writeText(sha)
        return
    }
    val javaFile = File(target, if (platform == "win32") "jre/bin/java.exe" else "jre/bin/java")
    val jarFile = File(target, "cs-ide.jar")
    val stamp = File(target, CS_IDE_SHA_STAMP)
    if (javaFile.isFile && jarFile.isFile && stamp.isFile && stamp.readText().trim() == sha) {
        logger.lifecycle("Using existing cs-ide distribution at ${target.absolutePath}")
        return
    }
    val artifact = csIdeArtifactName(sha, platform, arch)
    val url = URI.create("https://downloads.codescene.io/enterprise/cli/$artifact").toURL()
    val zipFile = File(outputRoot, artifact)
    outputRoot.mkdirs()
    logger.lifecycle("Downloading $url")
    url.openStream().use { input ->
        zipFile.outputStream().use { output -> input.copyTo(output) }
    }
    val extractDir = File(outputRoot, ".extract-$distName")
    extractDir.deleteRecursively()
    extractDir.mkdirs()
    unzipKeepRoot(zipFile, extractDir)
    val extracted =
        if (File(extractDir, "cs-ide.jar").isFile) {
            extractDir
        } else {
            extractDir.listFiles()?.singleOrNull { it.isDirectory } ?: extractDir
        }
    copyCsIdeDistribution(extracted, target, platform)
    File(target, CS_IDE_SHA_STAMP).writeText(sha)
    zipFile.delete()
    extractDir.deleteRecursively()
}

fun copyCsIdeDistribution(
    source: File,
    target: File,
    platform: String,
) {
    val javaName = if (platform == "win32") "java.exe" else "java"
    val javaFile = File(source, "jre/bin/$javaName")
    val jarFile = File(source, "cs-ide.jar")
    require(javaFile.isFile && jarFile.isFile) {
        "cs-ide distribution is incomplete at ${source.absolutePath}"
    }
    target.deleteRecursively()
    source.copyRecursively(target, overwrite = true)
    if (platform != "win32") {
        File(target, "jre/bin/java").setExecutable(true)
    }
    logger.lifecycle("Bundled cs-ide distribution at ${target.absolutePath}")
}

fun unzipKeepRoot(
    zipFile: File,
    outputDir: File,
) {
    ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry
        while (entry != null) {
            val outFile = File(outputDir, entry.name)
            val outDirCanonical = outputDir.canonicalPath
            val outCanonical = outFile.canonicalPath
            require(outCanonical == outDirCanonical || outCanonical.startsWith(outDirCanonical + File.separator)) {
                "Zip entry escapes target directory: ${entry.name}"
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile.mkdirs()
                outFile.outputStream().use { fos -> zis.copyTo(fos) }
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
    }
}
