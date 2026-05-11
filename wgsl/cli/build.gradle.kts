plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":wgsl:core"))
    implementation(project(":wgsl:wgsl"))
    implementation(project(":wgsl:msl"))
    implementation(project(":wgsl:hlsl"))
    implementation(project(":wgsl:glsl"))
}
