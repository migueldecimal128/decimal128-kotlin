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
    jmh(project(":decimal128"))

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
