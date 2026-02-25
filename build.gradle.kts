import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.10.4"
}

group = "no.msr"
version = "1.1.3"

kotlin {
    jvmToolchain(21) // Required for 2025.3
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        intellijIdea("2025.3.2")
        bundledPlugin("com.intellij.java")
        testFramework(TestFrameworkType.Platform)
    }

    // Use implementation, but we need to tell Gradle to include it in the distribution
    implementation("org.duckdb:duckdb_jdbc:1.1.3")
}

intellijPlatform {
    buildSearchableOptions = false
    pluginConfiguration {
        id = "no.msr.parquet.sql"
        name = "Parquet SQL Viewer"
        ideaVersion {
            sinceBuild = "253"
            untilBuild = "253.*"
        }
    }
}
