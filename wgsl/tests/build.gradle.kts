@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotest)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
        }
    }

    androidLibrary {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
        namespace = "io.ygdrasil.wgsl.tests"
        compileSdk = 36
        minSdk = 28
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":wgsl:core"))
                implementation(project(":wgsl:wgsl"))
                implementation(project(":wgsl:msl"))
                implementation(project(":wgsl:hlsl"))
                implementation(project(":wgsl:glsl"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.bundles.kotest)
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.kotest.runner.junit5)
            }
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStandardStreams = true
        events = setOf(
            org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
            org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
        )
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
