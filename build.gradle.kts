import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    kotlin("jvm") version "2.0.21"
}

group = "com.decimal128"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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