# 📦 Dépendances du Projet : Naga → Kotlin

**Projet** : WebGPU-KTypes Shader Transpiler  
**Code** : `naga-kt`  
**Version** : 1.0  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

---

## 🎯 PRINCIPE DIRECTEUR

> **"Moins de dépendances = Moins de problèmes"**
>
> **Règle d'or** : N'ajouter une dépendance que si :
> 1. C'est **absolument nécessaire** (pas d'alternative standard)
> 2. Elle est **légère** (< 1MB, pas de dépendances transitives lourdes)
> 3. Elle est **bien maintenue** (activement développée, compatible Kotlin)
> 4. Elle est **approuvée** dans la liste ci-dessous

---

## ✅ DÉPENDANCES **AUTORISÉES** (Approuvées)

### 1. Kotlin Standard Library

| Librairie | Version | Usage | Taille | Justification |
|----------|---------|-------|-------|----------------|
| `org.jetbrains.kotlin:kotlin-stdlib` | 1.9.0 | Langage de base | ~1MB | **Incontournable** |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | 1.9.0 | Multiplatform | ~500KB | Incontournable |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | 1.9.0 | Compat JDK8 | ~200KB | Optionnel |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | 1.9.0 | Compat JDK7 | ~200KB | Optionnel |

**Configuration Gradle :**
```kotlin
// Déjà inclus avec le plugin Kotlin
```

---

### 2. KotlinX (Approuvées - Minimales)

| Librairie | Version | Usage | Taille | Justification |
|----------|---------|-------|-------|----------------|
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.6.0 | Sérialisation golden files | ~500KB | **Approuvée** - Nécessaire pour tests |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.3 | Async pour CLI | ~200KB | Optionnelle - Pour CLI non-bloquant |
| `org.jetbrains.kotlinx:kotlinx-io` | 0.3.5 | E/S performante | ~100KB | Optionnelle - Si besoin buffer efficace |

**Configuration Gradle :**
```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    // Optionnel :
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // implementation("org.jetbrains.kotlinx:kotlinx-io:0.3.5")
}
```

---

### 3. Testing (Approuvées)

| Librairie | Version | Usage | Taille | Justification |
|----------|---------|-------|-------|----------------|
| `org.junit.jupiter:junit-jupiter-api` | 5.10.0 | Tests unitaires | ~300KB | **Approuvée** - Standard industrie |
| `org.junit.jupiter:junit-jupiter-engine` | 5.10.0 | Exécution tests | ~200KB | **Approuvée** |
| `org.assertj:assertj-core` | 3.24.2 | Assertions riches | ~500KB | **Approuvée** - Meilleure que JUnit assertions |
| `org.mockito:mockito-core` | 5.3.1 | Mocks | ~400KB | Optionnelle - Pour tests complexes |
| `org.mockito-kotlin:mockito-kotlin` | 5.1.0 | Mocks Kotlin-friendly | ~50KB | Optionnelle - Avec Mockito |

**Configuration Gradle :**
```kotlin
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    // Optionnel :
    // testImplementation("org.mockito:mockito-core:5.3.1")
    // testImplementation("org.mockito-kotlin:mockito-kotlin:5.1.0")
}

---

### 4. Benchmarking (Optionnelle)

| Librairie | Version | Usage | Taille | Justification |
|----------|---------|-------|-------|----------------|
| `org.openjdk.jmh:jmh-core` | 1.36 | Micro-benchmarks | ~1MB | Optionnelle - Pour optimisations |
| `org.openjdk.jmh:jmh-generator-annprocess` | 1.36 | Annotation processing | ~200KB | Optionnelle |

**Configuration Gradle :**
```kotlin
// Optionnel - Module séparé pour benchmarks
dependencies {
    implementation("org.openjdk.jmh:jmh-core:1.36")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.36")
}
```

---

## ⚠️ DÉPENDANCES **DÉCONSEILLÉES** (Éviter si possible)

### 1. Parsing

| Librairie | Alternative | Raison |
|----------|-------------|--------|
| `org.antlr:antlr4-runtime` | Parser manuel | Trop lourde (~2MB), complexité |
| `com.github.h0tk3y:better-parse` | Parser manuel | Moins mature |
| `org.codehaus.jparsec:jparsec` | Parser manuel | Peu maintenu |

**Recommandation :** Implémenter un **parser manuel** pour WGSL (grammaire simple).

### 2. Logging

| Librairie | Alternative | Raison |
|----------|-------------|--------|
| `org.slf4j:slf4j-api` | `println` | Complexité inutile pour un outil CLI |
| `ch.qos.logback:logback-classic` | `println` | Dépendances transitives lourdes |
| `org.apache.logging.log4j:log4j-core` | `println` | Trop lourde |

**Recommandation :** Utiliser **`println`** pour le développement, **aucune librairie** pour la production.

### 3. Collections

| Librairie | Alternative | Raison |
|----------|-------------|--------|
| `it.unimi.dsi:fastutil` | `ArrayList`, `HashMap` | Dépendance inutile pour taille projet |
| `org.eclipse.collections:eclipse-collections` | Collections stdlib | Trop lourde |
| `com.google.guava:guava` | Collections stdlib | **Interdite** - 10MB+ |

**Recommandation :** Utiliser les **collections Kotlin/Java standard** (`List`, `MutableList`, `Map`, `MutableMap`, `Set`, `MutableSet`).

### 4. JSON/Serialization

| Librairie | Alternative | Raison |
|----------|-------------|--------|
| `com.fasterxml.jackson.core:jackson-databind` | `kotlinx.serialization` | **Approuvée** déjà disponible |
| `com.google.gson:gson` | `kotlinx.serialization` | **Approuvée** déjà disponible |
| `org.json:json` | `kotlinx.serialization` | Moins typée, moins performante |

**Recommandation :** Utiliser **`kotlinx.serialization-json`** (déjà approuvée).

---

## ❌ DÉPENDANCES **INTERDITES**

| Librairie | Raison |
|----------|--------|
| `com.google.guava:guava` | **10MB+**, trop lourde, dépendances transitives |
| `org.apache.commons:commons-*` | Trop lourdes, peu typées |
| `commons-io:commons-io` | `kotlinx-io` est suffisante |
| `commons-lang:commons-lang3` | Fonctionnalités déjà dans Kotlin stdlib |
| `joda-time:joda-time` | Dépréciée, utiliser `java.time` |
| `com.google.code.gson:gson` | Utiliser `kotlinx.serialization` |
| `org.json:json` | Moins performant que `kotlinx.serialization` |
| `org.apache.log4j:log4j-*` | Trop lourde, utiliser `println` |
| `org.slf4j:slf4j-*` | Complexité inutile |
| `com.fasterxml.jackson:jackson-*` | Utiliser `kotlinx.serialization` |
| `org.antlr:antlr4` | Générateur, trop lourd |
| `org.antlr:antlr4-runtime` | Trop lourde pour parser manuel |
| `org.eclipse.jgit:org.eclipse.jgit` | Hors scope |
| `org.apache.httpcomponents:httpclient` | Hors scope (pas de HTTP) |
| `io.netty:netty-*` | Hors scope (pas de réseau) |
| `io.grpc:grpc-*` | Hors scope |
| `org.springframework:*` | Hors scope (pas de web) |
| `javax.servlet:javax.servlet-api` | Hors scope |
| `androidx.*` | Hors scope (sauf pour module Android futur) |

---

## 📊 ANALYSE DES DÉPENDANCES PAR MODULE

### core (Module Racine)

| Type | Dépendance | Version | Obligatoire | Taille |
|------|------------|---------|-------------|-------|
| Kotlin | kotlin-stdlib | 1.9.0 | ✅ Oui | ~1MB |
| KotlinX | kotlinx-serialization-json | 1.6.0 | ⚠️ Optionnelle | ~500KB |
| Test | junit-jupiter-api | 5.10.0 | ❌ Non (test) | ~300KB |
| Test | junit-jupiter-engine | 5.10.0 | ❌ Non (test) | ~200KB |
| Test | assertj-core | 3.24.2 | ❌ Non (test) | ~500KB |

**Total (Production) :** ~1-1.5MB  
**Total (Test) :** ~2.5-3MB

### wgsl (Frontend WGSL)

| Type | Dépendance | Version | Obligatoire | Taille |
|------|------------|---------|-------------|-------|
| Module | core | - | ✅ Oui | - |
| Kotlin | kotlin-stdlib | 1.9.0 | ✅ Oui | Déjà inclus |

**Total :** ~0KB (dépendances déjà dans core)

### msl (Backend MSL)

| Type | Dépendance | Version | Obligatoire | Taille |
|------|------------|---------|-------------|-------|
| Module | core | - | ✅ Oui | - |
| Kotlin | kotlin-stdlib | 1.9.0 | ✅ Oui | Déjà inclus |
| KotlinX | kotlinx-io | 0.3.5 | ⚠️ Optionnelle | ~100KB |

**Total :** ~0-100KB

### hlsl (Backend HLSL)

| Type | Dépendance | Version | Obligatoire | Taille |
|------|------------|---------|-------------|-------|
| Module | core | - | ✅ Oui | - |
| Kotlin | kotlin-stdlib | 1.9.0 | ✅ Oui | Déjà inclus |

**Total :** ~0KB

### glsl (Backend GLSL)

| Type | Dépendance | Version | Obligatoire | Taille |
|------|------------|---------|-------------|-------|
| Module | core | - | ✅ Oui | - |
| Kotlin | kotlin-stdlib | 1.9.0 | ✅ Oui | Déjà inclus |

**Total :** ~0KB

### cli (CLI)

| Type | Dépendance | Version | Obligatoire | Taille |
|------|------------|---------|-------------|-------|
| Module | core | - | ✅ Oui | - |
| Module | wgsl | - | ✅ Oui | - |
| Module | msl | - | ✅ Oui | - |
| Module | hlsl | - | ✅ Oui | - |
| Module | glsl | - | ✅ Oui | - |
| Kotlin | kotlin-stdlib | 1.9.0 | ✅ Oui | Déjà inclus |
| KotlinX | kotlinx-coroutines-core | 1.7.3 | ⚠️ Optionnelle | ~200KB |
| CLI | picocli | 4.7.4 | ⚠️ Optionnelle | ~300KB |

**Total :** ~0-500KB

---

## 📦 TAILLE TOTALE ESTIMÉE

| Configuration | Taille | Détail |
|---------------|--------|--------|
| **Production (minimale)** | **~1-2MB** | kotlin-stdlib + kotlinx-serialization |
| **Production (complète)** | **~2-3MB** | + kotlinx-io + coroutines |
| **Test** | **~3-4MB** | JUnit + AssertJ + Mockito |
| **Benchmark** | **+~1.5MB** | JMH |

**Comparaison avec Naga Rust :**
- Naga Rust (release build) : ~2-3MB
- Naga Kotlin (JAR avec dépendances) : ~5-8MB (acceptable pour outil build-time)

---

## 🔧 CONFIGURATION GRADLE COMPLÈTE

### settings.gradle.kts

```kotlin
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
```

### build.gradle.kts (Root)

```kotlin
plugins {
    kotlin("jvm") version "1.9.0" apply false
}

group = "dev.gfxrs"
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
```

### core/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    // Production: Aucune dépendance externe (sauf kotlinx.serialization pour tests)
    
    // Test
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

kotlin {
    jvmToolchain(17)
}
```

### wgsl/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
}

kotlin {
    jvmToolchain(17)
}
```

### msl/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    // Optionnel pour E/S performante
    // implementation("org.jetbrains.kotlinx:kotlinx-io:0.3.5")
}

kotlin {
    jvmToolchain(17)
}
```

### hlsl/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
}

kotlin {
    jvmToolchain(17)
}
```

### glsl/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
}

kotlin {
    jvmToolchain(17)
}
```

### cli/build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":wgsl"))
    implementation(project(":msl"))
    implementation(project(":hlsl"))
    implementation(project(":glsl"))
    
    // Optionnel pour CLI asynchrone
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Optionnel pour parsing CLI
    // implementation("info.picocli:picocli:4.7.4")
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
                "Main-Class" to "dev.gfxrs.wgsl.cli.MainKt"
            )
        }
    }
}
```

---

## 🔍 VÉRIFICATION DES DÉPENDANCES

### Commande pour lister toutes les dépendances

```bash
# Depuis la racine du projet
./gradlew :dependencies
./gradlew :core:dependencies
./gradlew :wgsl:dependencies
```

### Commande pour vérifier la taille des dépendances

```bash
# Générer un rapport des dépendances
./gradlew buildDependencyTree

# Vérifier la taille du JAR final
find . -name "*.jar" -exec du -h {} \;
```

---

## 📝 RÈGLES DE GESTION DES DÉPENDANCES

### 1. Ajout d'une Nouvelle Dépendance

Pour ajouter une nouvelle dépendance :

1. **Vérifier** qu'elle est dans la liste **autorisées** ou obtenir une approval
2. **Justifier** pourquoi elle est nécessaire (pas d'alternative standard)
3. **Vérifier** sa taille et ses dépendances transitives
4. **Tester** son impact sur le build et les performances
5. **Documenter** dans ce fichier

### 2. Mise à Jour de Version

- **Patch versions** (x.x.PATCH) : Peut être mise à jour sans approval
- **Minor versions** (x.MINOR.x) : Doit être testée, approval recommandée
- **Major versions** (MAJOR.x.x) : Doit être approuvée, tests complets requis

### 3. Suppression de Dépendance

- Supprimer la dépendance du `build.gradle.kts`
- Supprimer tous les imports/utilisations dans le code
- Vérifier que le build passe
- Mettre à jour ce fichier

---

## 🎯 RÉSUMÉ

| Catégorie | Dépendances | Taille Totale | Statut |
|----------|-------------|---------------|--------|
| **Obligatoires** | kotlin-stdlib, kotlinx-serialization-json | ~1.5MB | ✅ Approuvées |
| **Optionnelles** | kotlinx-io, kotlinx-coroutines, JMH | ~1-2MB | ⚠️ À discuter |
| **Test** | JUnit 5, AssertJ, Mockito | ~2-3MB | ✅ Approuvées |
| **Interdites** | Guava, Commons, Log4j, etc. | - | ❌ Bloquées |

**Total Production Minimal :** **~1.5MB**  
**Total Production Complète :** **~2.5-3.5MB**  
**Total avec Tests :** **~5-6.5MB**

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Valider cette liste de dépendances avec l'équipe
2. [ ] Configurer les `build.gradle.kts` selon cette configuration
3. [ ] Tester le build avec les dépendances minimales
4. [ ] Vérifier la taille des JARs générés
5. [ ] Passer au fichier suivant : `03_references.md`
