pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "wgsl"

include(
    "core",
    "wgsl",
    "msl",
    "hlsl",
    "glsl",
    "cli"
)
