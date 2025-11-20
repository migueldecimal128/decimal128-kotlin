import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

plugins {
    id("maven-publish")
    id("signing")
    kotlin("multiplatform") version "2.2.0"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "com.decimal128"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
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


@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
kotlin {
    applyDefaultHierarchyTemplate()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xsuppress=NOTHING_TO_INLINE")
    }

    jvmToolchain(21)

    jvm {
//	   	   withJava()
    }

    sourceSets {
	    val commonMain by getting {
            dependencies {
                implementation("com.decimal128:hugeint:0.9.0-SNAPSHOT")
            }
        }
	    val commonTest by getting
		val jvmMain   by getting  // your existing code lives here
		val jvmTest   by getting {
			dependencies {
                implementation(kotlin("test"))     // <-- pulls in kotlin-test on jvmTest
                implementation("net.java.dev.jna:jna:5.17.0")
			}
		}
    }
}

val nativeInputDir = layout.projectDirectory.dir("native/darwin-x86_64")
val nativeTestDir  = layout.buildDirectory.dir("native/darwin-x86_64")

tasks.register<Copy>("copyNativeForTests") {
    from(nativeInputDir)
    into(nativeTestDir)
}

// configure *all* Test tasks (including the MPP-generated jvmTest) to use JUnit Platform:
tasks.withType<Test> {
    dependsOn("copyNativeForTests")
    systemProperty("jna.library.path", nativeTestDir.get().asFile.absolutePath)

    useJUnitPlatform()
    testLogging {
        events          = setOf(
            TestLogEvent.FAILED,
            TestLogEvent.SKIPPED,
            TestLogEvent.STANDARD_OUT,
            TestLogEvent.STANDARD_ERROR
        )
        showStandardStreams = true
    }
}
