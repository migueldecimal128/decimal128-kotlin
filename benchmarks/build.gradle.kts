// ./benchmarks/build.gradle.kts

plugins {
    kotlin("jvm") // version "2.3.10"
    id("me.champeau.jmh") version "0.7.2"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // if first line fails, then try second line as a more explicit replacement
    jmh(rootProject)                        // the root project
    //jmh(project(mapOf("path" to ":", "configuration" to "jvmRuntimeElements")))

    jmh("org.openjdk.jmh:jmh-core:1.37")
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

jmh {
    warmupIterations.set(3)
    iterations.set(5)
    fork.set(2)
    timeOnIteration.set("2s")
    warmup.set("2s")
}

tasks.named("check") {
    dependsOn("jmhClasses")
}
