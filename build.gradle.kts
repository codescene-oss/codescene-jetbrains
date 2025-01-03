import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.net.HttpURLConnection
import java.net.URL

plugins {
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    kotlin("plugin.serialization") version "2.0.21"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

val codeSceneDevToolsVersion = providers.gradleProperty("codeSceneDevToolsVersion").get()
val codeSceneRepository = providers.gradleProperty("codeSceneRepository").get()
val kotlinxSerializationVersion = providers.gradleProperty("kotlinxSerializationVersion").get()
val reflectionsVersion = providers.gradleProperty("reflectionsVersion").get()
val mockkVersion = providers.gradleProperty("mockkVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(17)
}

// Configure project's dependencies
repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri(codeSceneRepository)
        credentials {
            username = System.getenv("GH_USERNAME")
            password = System.getenv("GH_PACKAGE_TOKEN")
        }
    }

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    implementation("org.reflections:reflections:$reflectionsVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("codescene.devtools.ide:api:$codeSceneDevToolsVersion")

    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:${mockkVersion}")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        instrumentationTools()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)
    }
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
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
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
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
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
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

    publishPlugin {
        dependsOn(patchChangelog)
    }

    runIde {
        classpath += sourceSets["main"].runtimeClasspath
    }

    register<JavaExec>("run") {
        mainClass.set("com.codescene.Main")
        classpath += sourceSets["main"].runtimeClasspath
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
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

tasks.register("fetchDocs") {
    group = "documentation"
    description = "Get the docs asset from the latest GitHub release."

    val user = "empear-analytics"
    val repo = "codescene-ide-protocol"

    doLast {
        val apiUrl = "https://api.github.com/repos/$user/$repo/releases"
        val token = System.getenv("GITHUB_TOKEN") ?: throw GradleException("GitHub token not found!")

        val releasesJson = run {
            val connection = URL(apiUrl).openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "token $token")
            connection.inputStream.reader().readText()
        }

        val releases = groovy.json.JsonSlurper().parseText(releasesJson) as List<Map<String, Any>>
        val release = releases.find { it["prerelease"] == false && it["draft"] == false }
            ?: throw GradleException("No suitable release found.")

        println("Downloading assets for release: ${release["tag_name"]}")

        @Suppress("UNCHECKED_CAST")
        val assets = release["assets"] as List<Map<String, Any>>
        val docsAsset = assets.find { (it["name"] as String) == "docs.zip" }
            ?: throw GradleException("No asset named 'docs.zip' found in the latest release.")

        val assetUrl = docsAsset["browser_download_url"] as String

        val outputFile = file("docs.zip")

        val connection2 = URL(assetUrl).openConnection()
        connection2.setRequestProperty("Authorization", "Bearer $token")
        connection2.connect()

        outputFile.outputStream().use { outputStream ->
            connection2.inputStream.use { inputStream ->
                inputStream.copyTo(outputStream)
            }

        }

        println("Download completed: ${outputFile.absolutePath}")
    }
}
