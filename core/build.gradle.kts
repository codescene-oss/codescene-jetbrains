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

val kotlinxSerializationVersion = rootProject.providers.gradleProperty("kotlinxSerializationVersion").get()
val mockkVersion = rootProject.providers.gradleProperty("mockkVersion").get()
val slf4jNopVersion = rootProject.providers.gradleProperty("slf4jNopVersion").get()
val kotlinxCoroutinesVersion = rootProject.providers.gradleProperty("kotlinxCoroutinesVersion").get()
val jacksonVersion = rootProject.providers.gradleProperty("jacksonVersion").get()

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("commons-codec:commons-codec:1.17.1")
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
        exclude("**/com/codescene/data/**")
    }
}
