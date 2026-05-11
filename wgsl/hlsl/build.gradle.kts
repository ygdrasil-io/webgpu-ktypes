plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":wgsl:core"))
}

kotlin {
    jvmToolchain(17)
}
