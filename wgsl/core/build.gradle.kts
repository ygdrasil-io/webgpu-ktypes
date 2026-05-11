plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    // Test
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

kotlin {
    jvmToolchain(17)
}
