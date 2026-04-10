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
    kotlin("multiplatform") version "2.3.10"
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
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvmToolchain(21)

    jvm { }

    macosArm64 { }

    macosX64 { }

    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10000"  // 10 seconds
                }
            }
        }
    }

    // Configure cinterop for ALL native targets
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.getByName("main") {
            cinterops {
                val unsigned_mul_hi by creating {
                    defFile(project.file("src/nativeInterop/cinterop/unsigned_mul_hi.def"))
                }
            }
        }
    }

    sourceSets {
        all {
        }
	    val commonMain by getting
	    val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
		val jvmMain   by getting
		val jvmTest   by getting {
			dependencies {
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
/*

afterEvaluate {
    tasks.named<Test>("jvmTest") {
        jvmArgs(
            "-XX:+UnlockDiagnosticVMOptions",
            "-XX:+PrintCompilation",
            "-XX:+PrintAssembly",
            "-XX:PrintAssemblyOptions=syntax=intel",
            "-XX:CompileCommand=print,com.decimal128.decimal.DivBarrett::barrettDivModPow10"
        )
    }
}

 */


