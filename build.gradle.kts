// Root build script — aggregator only.
// Declares plugin versions for subprojects without applying them at the root.
plugins {
    kotlin("multiplatform") version "2.3.10" apply false
    kotlin("jvm") version "2.3.10" apply false
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
    id("org.jetbrains.dokka") version "2.0.0" apply false
}
