// ./build.gradle.kts
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.plugins.signing.SigningExtension

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:2.0.0")
    }
}

plugins {
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.36.0"
    kotlin("multiplatform") version "2.3.10"
    id("org.jetbrains.dokka") version "2.0.0"
}

group = "com.decimal128"
version = "0.9.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dokka {
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("documentation/html"))
    }
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
    linuxX64 { }
    iosArm64 { }
    iosSimulatorArm64 { }
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
                create("unsigned_mul_hi") {
                    defFile(project.file("src/nativeInterop/cinterop/unsigned_mul_hi.def"))
                }
            }
        }
    }

    sourceSets {
        all {
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// configure *all* Test tasks (including the MPP-generated jvmTest) to use JUnit Platform:
tasks.withType<Test> {

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

tasks.matching { it.name == "iosSimulatorArm64Test" }.configureEach {
    enabled = System.getenv("CI") != null
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("com.decimal128", "decimal128-kotlin", "0.9.0")

    pom {
        name.set("decimal128-kotlin")
        description.set("IEEE 754-2019 decimal128 floating-point arithmetic for Kotlin Multiplatform.")
        url.set("https://github.com/migueldecimal128/decimal128-kotlin")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("migueldecimal128")
                name.set("MiguelDecimal128")
                email.set("miguel@decimal128.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/migueldecimal128/decimal128-kotlin.git")
            developerConnection.set("scm:git:ssh://github.com/migueldecimal128/decimal128-kotlin.git")
            url.set("https://github.com/migueldecimal128/decimal128-kotlin")
        }
    }
}


extensions.getByType<SigningExtension>().useGpgCmd()

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


