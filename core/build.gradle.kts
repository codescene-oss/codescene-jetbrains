plugins {
    alias(libs.plugins.kotlin)
    kotlin("plugin.serialization") version "2.2.0"
    alias(libs.plugins.ktlint)
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
val slf4jNopVersion = rootProject.providers.gradleProperty("slf4jNopVersion").get()
val kotlinxCoroutinesVersion = rootProject.providers.gradleProperty("kotlinxCoroutinesVersion").get()

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
}

dependencies {
    implementation("codescene.extension:api:$codeSceneExtensionAPIVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:$mockkVersion")
    testRuntimeOnly("org.slf4j:slf4j-nop:$slf4jNopVersion")
}

val ktlintFailOnError =
    rootProject.providers.gradleProperty("ktlintFailOnError")
        .map(String::toBoolean)
        .orElse(false)

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
