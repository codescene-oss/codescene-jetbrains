plugins {
    alias(libs.plugins.kotlin)
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.ktlint)
    jacoco
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(17)
}

val codeSceneExtensionAPIVersion = rootProject.providers.gradleProperty("codeSceneExtensionAPIVersion").get()
val codeSceneRepository = rootProject.providers.gradleProperty("codeSceneRepository").get()
val kotlinxSerializationVersion = rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()
val mockkVersion = rootProject.providers.gradleProperty("mockkVersion").get()
val jsonSchemaValidatorVersion = rootProject.providers.gradleProperty("jsonSchemaValidatorVersion").get()
val slf4jNopVersion = rootProject.providers.gradleProperty("slf4jNopVersion").get()
val kotlinxCoroutinesVersion = rootProject.providers.gradleProperty("kotlinxCoroutinesVersion").get()

fun requiredEnv(name: String): String =
    System.getenv(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: throw GradleException("Missing required environment variable: $name")

fun optionalEnv(name: String): String? =
    System.getenv(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

fun requireGithubPackageCredentialsForDependencyResolution() {
    configurations.configureEach {
        incoming.beforeResolve {
            requiredEnv("GH_USERNAME")
            requiredEnv("GH_PACKAGE_TOKEN")
        }
    }
}

requireGithubPackageCredentialsForDependencyResolution()

repositories {
    if (providers.gradleProperty("useMavenLocal").isPresent) {
        mavenLocal()
    }
    mavenCentral()
    maven {
        url = uri(codeSceneRepository)
        credentials {
            username = optionalEnv("GH_USERNAME")
            password = optionalEnv("GH_PACKAGE_TOKEN")
        }
    }
}

dependencies {
    implementation("codescene.extension:api:$codeSceneExtensionAPIVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.networknt:json-schema-validator:$jsonSchemaValidatorVersion")
    testRuntimeOnly("org.slf4j:slf4j-nop:$slf4jNopVersion")
}

val cliContract by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    cliContract("codescene.extension:api:$codeSceneExtensionAPIVersion:contract@zip")
}

val contractBundleOverride = optionalEnv("CS_IDE_CONTRACT_BUNDLE")

val unpackCliContract by tasks.registering(Sync::class) {
    if (contractBundleOverride == null) {
        from(provider { zipTree(cliContract.singleFile) })
    } else {
        val override = file(contractBundleOverride)
        if (override.isDirectory) from(override) else from(provider { zipTree(override) })
    }
    into(layout.buildDirectory.dir("cs-ide-contract"))
}

tasks.test {
    dependsOn(unpackCliContract)
    inputs.dir(unpackCliContract.map { it.destinationDir })
    systemProperty("cs.ide.contract.dir", unpackCliContract.get().destinationDir.absolutePath)
}

val ktlintFailOnError =
    rootProject.providers.gradleProperty("ktlintFailOnError")
        .map(String::toBoolean)
        .orElse(false)

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}

ktlint {
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
