import org.gradle.api.file.DuplicatesStrategy

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.jmh)
    alias(libs.plugins.ktlint)
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(17)
}

val slf4jNopVersion = rootProject.providers.gradleProperty("slf4jNopVersion").get()

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    add("jmh", project(":core"))
    add("jmh", kotlin("stdlib"))
    add("jmhRuntimeOnly", "org.slf4j:slf4j-nop:$slf4jNopVersion")
}

jmh {
    benchmarkMode = listOf("avgt")
    timeUnit = "ms"
    warmupIterations = 2
    iterations = 5
    fork = 1
    resultFormat = "JSON"
    resultsFile = project.file("${layout.buildDirectory.get().asFile}/reports/jmh/results.json")
    duplicateClassesStrategy = DuplicatesStrategy.WARN
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
