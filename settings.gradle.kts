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

rootProject.name = "webgpu-ktypes"

include(
    "core",
    "wgsl",
    "msl",
    "hlsl",
    "glsl",
    "cli"
)
