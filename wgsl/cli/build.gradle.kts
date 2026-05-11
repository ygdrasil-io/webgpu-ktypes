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

kotlin {
    jvmToolchain(17)
}

// Configuration pour créer un JAR exécutable
tasks {
    val jar = jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from {
            configurations.runtimeClasspath.collect { it.isDirectory() ? it : zipTree(it) }
        }
        manifest {
            attributes(
                "Main-Class" to "io.ygdrasil.wgsl.cli.MainKt"
            )
        }
    }
}
