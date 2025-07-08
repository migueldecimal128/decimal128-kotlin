import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "com.decimal128"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url="https://dl.bintray.com/kotlin/dokka")
}

dependencies {
    testImplementation(kotlin("test"))

    // Is applied universally
    dokkaPlugin("org.jetbrains.dokka:mathjax-plugin:2.0.0")

    // Is applied for the single-module dokkaHtml task only
    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:2.0.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.dokkaHtml {
    outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
}

tasks.dokkaGfm {
    outputDirectory.set(layout.buildDirectory.dir("documentation/markdown"))
}

tasks.register<Test>("testHsdis") {
    group = "verification"
    description = "Runs tests with JIT disassembly enabled"

    dependsOn("testClasses")
    useJUnitPlatform()

    jvmArgs(
        "-XX:+UnlockDiagnosticVMOptions",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:+PrintInlining",
        "-XX:+PrintAssembly",
        "-XX:PrintAssemblyOptions=syntax=intel",
        "-XX:CompileThreshold=1"
    )

    testLogging {
        events = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
    }
}


kotlin {
    jvmToolchain(21)
}