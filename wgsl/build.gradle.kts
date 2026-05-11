plugins {
    kotlin("jvm") version "1.9.0" apply false
}

group = "io.ygdrasil"
version = "0.1.0-SNAPSHOT"

// Configuration commune pour tous les sous-projets
subprojects {
    repositories {
        mavenCentral()
    }

    dependencies {
        // Dépendances de test communes
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
        testImplementation("org.assertj:assertj-core:3.24.2")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
