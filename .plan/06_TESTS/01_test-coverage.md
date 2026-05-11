# Couverture de Test

## Sommaire

1. [Objectifs de Coverage](#objectifs-de-coverage)
2. [Métriques de Coverage](#métriques-de-coverage)
3. [Outils de Coverage](#outils-de-coverage)
4. [Configuration JaCoCo](#configuration-jacoco)
5. [Rapport de Coverage](#rapport-de-coverage)
6. [Cibles par Module](#cibles-par-module)
7. [Tests Manquants](#tests-manquants)
8. [Intégration CI](#intégration-ci)
9. [Maintenance du Coverage](#maintenance-du-coverage)

---

## Objectifs de Coverage

### Cibles Globales

| Métrique | Cible Minimale | Cible Optimale | Justification |
|----------|----------------|----------------|---------------|
| Coverage des instructions | 90% | 95% | Détection des branches non testées |
| Coverage des branches | 85% | 90% | Validation des chemins conditionnels |
| Coverage des lignes | 90% | 95% | Couverture complète du code |
| Coverage des méthodes | 95% | 100% | Tous les points d'entrée testés |
| Coverage des classes | 100% | 100% | Toutes les classes ont des tests |

### Priorités

1. **Critique** : Code de parsing et de génération (100% coverage)
2. **Élevé** : Structures IR et transformations (95%+ coverage)
3. **Moyen** : Utilitaires et helpers (90%+ coverage)
4. **Faible** : Code généré ou trivial (80%+ coverage)

---

## Métriques de Coverage

### Définitions

| Métrique | Description | Importance |
|----------|-------------|------------|
| **Instruction Coverage** | % d'instructions exécutées | ⭐⭐⭐⭐ |
| **Branch Coverage** | % de branches (if/else, switch) testées | ⭐⭐⭐⭐⭐ |
| **Line Coverage** | % de lignes exécutées | ⭐⭐⭐ |
| **Method Coverage** | % de méthodes appelées | ⭐⭐⭐ |
| **Class Coverage** | % de classes instanciées | ⭐⭐ |
| **Complexité Cyclomatique** | Complexité du code | ⭐⭐⭐ |

### Interprétation

```
Coverage = (Éléments testés / Éléments totaux) × 100

Exemple:
- 100/120 instructions = 83.3% coverage
- 50/60 branches = 83.3% coverage
- 90/100 lignes = 90% coverage
```

### seuils JaCoCo

```kotlin
// build.gradle.kts

jacoco {
    coverageVerification {
        violationRules {
            // Règles globales
            rule {
                element = "BUNDLE"
                
                // Cibles minimales
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = 0.90
                }
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = 0.85
                }
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = 0.90
                }
                limit {
                    counter = "METHOD"
                    value = "COVEREDRATIO"
                    minimum = 0.95
                }
                limit {
                    counter = "CLASS"
                    value = "COVEREDRATIO"
                    minimum = 1.00
                }
                
                // Exclusions
                excludes = listOf(
                    "dev.gfxrs.naga.benchmark.*",
                    "dev.gfxrs.naga.generated.*",
                    "*Test*",
                    "*Spec*"
                )
            }
            
            // Règles par module
            rule {
                element = "CLASS"
                
                // naga-core : 100% coverage
                includes = listOf("dev.gfxrs.naga.core.*")
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = 0.95
                }
            }
            
            rule {
                element = "CLASS"
                
                // naga-wgsl : 95% coverage
                includes = listOf("dev.gfxrs.naga.wgsl.*")
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = 0.95
                }
            }
            
            rule {
                element = "CLASS"
                
                // Backends : 90% coverage
                includes = listOf(
                    "dev.gfxrs.naga.msl.*",
                    "dev.gfxrs.naga.hlsl.*",
                    "dev.gfxrs.naga.glsl.*"
                )
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = 0.90
                }
            }
        }
    }
}
```

---

## Outils de Coverage

### JaCoCo (Java Code Coverage)

**Pourquoi JaCoCo ?**
- Intégration native avec Gradle
- Support complet de Kotlin
- Rapports détaillés (HTML, XML, CSV)
- Vérification des seuils

**Alternatives**
- **Kover** : Plugin Kotlin moderne pour le coverage
- **Istanbul** : Alternative JavaScript (non applicable)
- **Cobertura** : Alternative plus ancienne

### Comparaison des Outils

| Outil | Support Kotlin | Intégration Gradle | Rapports | Vérification |
|-------|----------------|-------------------|----------|--------------|
| JaCoCo | ✅ Excellent | ✅ Native | ✅ Riches | ✅ Oui |
| Kover | ✅ Excellent | ✅ Plugin | ✅ Riches | ✅ Oui |
| Cobertura | ⚠️ Partiel | ⚠️ Possible | ✅ Basiques | ❌ Non |

---

## Configuration JaCoCo

### Configuration de Base

```kotlin
// build.gradle.kts

plugins {
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.11"
    reportsDirectory.set(layout.buildDirectory.dir("jacocoReports"))
}

tasks.test {
    // Activer le coverage pour les tests
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(test) // Les tests doivent être exécutés avant
    
    // Source directories
    sourceDirectories.setFrom(files(
        "src/main/kotlin",
        "src/main/java"
    ))
    
    // Class directories
    classDirectories.setFrom(files(
        layout.buildDirectory.dir("classes/kotlin/main"),
        layout.buildDirectory.dir("classes/java/main"),
        layout.buildDirectory.dir("classes/kotlin/test"),
        layout.buildDirectory.dir("classes/java/test")
    ))
    
    // Exclusions
    excludes = listOf(
        "*Test*",
        "*Spec*",
        "*Benchmark*",
        "**/generated/**"
    )
}
```

### Configuration Multi-Module

```kotlin
// settings.gradle.kts

pluginManagement {
    plugins {
        jacoco {
            id("jacoco")
            version("0.8.11")
        }
    }
}

// build.gradle.kts (racine)

subprojects {
    apply(plugin = "jacoco")
    
    configure<JacocoPluginExtension> {
        toolVersion = "0.8.11"
    }
    
    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
        }
    }
}
```

### Configuration par Module

```kotlin
// naga-core/build.gradle.kts

plugins {
    id("jacoco")
}

tasks.jacocoTestReport {
    dependsOn(test)
    
    sourceDirectories.setFrom(files("src/main/kotlin"))
    classDirectories.setFrom(files(layout.buildDirectory.dir("classes/kotlin/main")))
    
    // Cibles élevées pour naga-core
    coverageVerification {
        violationRules {
            rule {
                limit {
                    counter = "INSTRUCTION"
                    value = "COVEREDRATIO"
                    minimum = 0.95
                }
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = 0.90
                }
            }
        }
    }
}
```

---

## Rapport de Coverage

### Génération des Rapports

```bash
# Générer le rapport HTML
./gradlew jacocoTestReport

# Générer le rapport XML (pour CI)
./gradlew jacocoTestXmlReport

# Générer tous les rapports
./gradlew jacocoRootReport
```

### Structure des Rapports

```
build/
├── jacoco/
│   └── test.exec              # Données brutes de coverage
├── jacocoHtml/
│   ├── index.html            # Page d'accueil
│   ├── dev.gfxrs.naga/
│   │   ├── core/
│   │   │   ├── Arena.kt.html  # Rapport par fichier
│   │   │   └── ...
│   │   ├── wgsl/
│   │   │   └── ...
│   │   └── index.html        # Résumé du module
│   └── index.html            # Résumé global
└── jacocoReports/
    └── test.xml               # Rapport XML (pour CI)
```

### Exemple de Rapport HTML

```html
<!-- index.html -->

<!DOCTYPE html>
<html>
<head>
    <title>Code Coverage Report</title>
    <style>
        .covered { background-color: #4CAF50; }
        .partially-covered { background-color: #FFC107; }
        .not-covered { background-color: #F44336; }
    </style>
</head>
<body>
    <h1>Code Coverage Report</h1>
    
    <table>
        <tr>
            <th>Module</th>
            <th>Instructions</th>
            <th>Branches</th>
            <th>Lignes</th>
            <th>Méthodes</th>
            <th>Classes</th>
        </tr>
        <tr>
            <td>naga-core</td>
            <td><span class="covered">98%</span></td>
            <td><span class="partially-covered">92%</span></td>
            <td><span class="covered">99%</span></td>
            <td><span class="covered">100%</span></td>
            <td><span class="covered">100%</span></td>
        </tr>
        <tr>
            <td>naga-wgsl</td>
            <td><span class="partially-covered">94%</span></td>
            <td><span class="partially-covered">88%</span></td>
            <td><span class="partially-covered">93%</span></td>
            <td><span class="covered">100%</span></td>
            <td><span class="covered">100%</span></td>
        </tr>
    </table>
    
    <h2>Détails par Fichier</h2>
    <ul>
        <li><a href="dev/gfxrs/naga/core/Arena.kt.html">Arena.kt - 100%</a></li>
        <li><a href="dev/gfxrs/naga/core/Handle.kt.html">Handle.kt - 100%</a></li>
        <li><a href="dev/gfxrs/naga/wgsl/Lexer.kt.html">Lexer.kt - 92%</a></li>
    </ul>
</body>
</html>
```

### Rapport par Fichier

```
File: dev/gfxrs/naga/core/Arena.kt

┌─────────────────────────────────────────────────────────────┐
│ Coverage: 100% (120/120 instructions)                          │
├─────────────────────────────────────────────────────────────┤
│ Line | Coverage | Code                                           │
├──────┼──────────┼────────────────────────────────────────────┤
│ 1    | ✅      | class Arena<T> {                                │
│ 2    | ✅      |     private val items: MutableList<T> = ...   │
│ 3    | ✅      |     fun append(item: T): Handle<T> {           │
│ 4    | ✅      |         items.add(item)                        │
│ 5    | ✅      |         return Handle(items.size - 1)          │
│ ...  | ...     | ...                                            │
└─────────────────────────────────────────────────────────────┘

Legend:
  ✅ Covered
  ❌ Not Covered
  ⚠️ Partially Covered
```

---

## Cibles par Module

### naga-core

**Objectif** : 95%+ coverage (module critique)

| Classe | Instructions | Branches | Lignes | Méthodes | Statut |
|--------|--------------|----------|--------|----------|--------|
| Arena | 100% | 95% | 100% | 100% | ✅ |
| Handle | 100% | 100% | 100% | 100% | ✅ |
| Module | 98% | 92% | 98% | 100% | ⚠️ |
| Type | 99% | 94% | 99% | 100% | ✅ |
| Expression | 97% | 90% | 97% | 100% | ⚠️ |
| Statement | 98% | 91% | 98% | 100% | ⚠️ |

**Tests Manquants** :
- Branches rares dans Module (validation des capabilities)
- Cas limites dans Expression (opérations complexes)

### naga-wgsl

**Objectif** : 95%+ coverage (module critique)

| Classe | Instructions | Branches | Lignes | Méthodes | Statut |
|--------|--------------|----------|--------|----------|--------|
| Lexer | 92% | 88% | 91% | 100% | ⚠️ |
| Parser | 94% | 89% | 93% | 100% | ⚠️ |
| WgslWriter | 93% | 87% | 92% | 100% | ⚠️ |

**Tests Manquants** :
- Tokens rares (opérateurs combinés comme `+=`, `>>=`)
- Cas d'erreur dans le parser (récupération d'erreur)
- Formatage complexe dans le writer

### naga-msl

**Objectif** : 90%+ coverage

| Classe | Instructions | Branches | Lignes | Méthodes | Statut |
|--------|--------------|----------|--------|----------|--------|
| MslWriter | 91% | 86% | 90% | 100% | ⚠️ |
| MslExpressionWriter | 89% | 84% | 88% | 100% | ⚠️ |

**Tests Manquants** :
- Fonctions builtin spécifiques Metal
- Types avancés (textures, samplers)
- Optimisations spécifiques Metal

### naga-hlsl

**Objectif** : 90%+ coverage

| Classe | Instructions | Branches | Lignes | Méthodes | Statut |
|--------|--------------|----------|--------|----------|--------|
| HlslWriter | 90% | 85% | 89% | 100% | ⚠️ |
| HlslExpressionWriter | 88% | 83% | 87% | 100% | ⚠️ |

**Tests Manquants** :
- Fonctions builtin spécifiques HLSL
- Types spécifiques DirectX (StructuredBuffer, etc.)

### naga-glsl

**Objectif** : 90%+ coverage

| Classe | Instructions | Branches | Lignes | Méthodes | Statut |
|--------|--------------|----------|--------|----------|--------|
| GlslWriter | 89% | 84% | 88% | 100% | ⚠️ |
| GlslExpressionWriter | 87% | 82% | 86% | 100% | ⚠️ |

**Tests Manquants** :
- Extensions GLSL spécifiques
- Versions GLSL différentes (450, 460)
- Types spécifiques OpenGL

---

## Tests Manquants

### Identification des Tests Manquants

```bash
# Exécuter le rapport de coverage
./gradlew jacocoTestReport

# Ouvrir le rapport HTML
open build/jacocoHtml/index.html

# Filtrer les fichiers avec coverage < 100%
# Les lignes rouges/jaunes indiquent le code non couvert
```

### Stratégie pour les Tests Manquants

1. **Prioriser par criticité**
   - Code de parsing → Haute priorité
   - Code de génération → Haute priorité
   - Code d'optimisation → Moyenne priorité
   - Code utilitaire → Basse priorité

2. **Prioriser par coverage**
   - Branches non couvertes → Priorité 1
   - Instructions non couvertes → Priorité 2
   - Lignes non couvertes → Priorité 3

3. **Créer des tests ciblés**
   - Un test par branche manquante
   - Un test par instruction manquante
   - Regrouper les tests similaires

### Exemple : Ajout de Tests pour une Branche Manquante

```kotlin
// Suppose que cette branche n'est pas couverte dans Arena.kt

class Arena {
    fun getOrNull(handle: Handle<T>): T? {
        if (handle.index < 0 || handle.index >= items.size) {
            return null  // ← Branche non couverte
        }
        return items[handle.index]
    }
}

// Test pour couvrir la branche manquante

class ArenaTest {
    
    @Test
    fun `test getOrNull with invalid negative index`() {
        val arena = Arena<String>()
        val invalidHandle = Handle<String>(-1)
        
        val result = arena.getOrNull(invalidHandle)
        
        assertNull(result)
    }
    
    @Test
    fun `test getOrNull with invalid out-of-bounds index`() {
        val arena = Arena<String>()
        arena.append("test")
        val invalidHandle = Handle<String>(100)  // ← Out of bounds
        
        val result = arena.getOrNull(invalidHandle)
        
        assertNull(result)
    }
}
```

### Template pour les Nouveaux Tests

```kotlin
// Template pour ajouter des tests manquants

class {ClassName}MissingTests {
    
    // ⚠️ BRANCH NOT COVERED: [description de la branche]
    @Test
    fun `test [description du cas]`() {
        // Arrange
        // ...
        
        // Act
        // ...
        
        // Assert
        // Vérifier le comportement de la branche
    }
    
    // ⚠️ INSTRUCTION NOT COVERED: [description de l'instruction]
    @Test
    fun `test [description du cas]`() {
        // Arrange
        // ...
        
        // Act
        // Exécuter le code qui contient l'instruction
        
        // Assert
        // Vérifier le résultat
    }
}
```

---

## Intégration CI

### GitHub Actions

```yaml
# .github/workflows/coverage.yml
name: Coverage

on: [push, pull_request]

jobs:
  coverage:
    runs-on: ubuntu-22.04
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Tests with Coverage
        run: ./gradlew jacocoTestReport
      
      - name: Upload Coverage Report
        uses: actions/upload-artifact@v3
        with:
          name: coverage-report
          path: build/jacocoHtml/
      
      - name: Check Coverage Thresholds
        run: ./gradlew jacocoCoverageVerification
      
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: build/reports/jacoco/test/jacocoTestReport.xml
          flags: unittests
          name: codecov-umbrella
          fail_ci_if_error: true
```

### Badge de Coverage

```markdown
<!-- README.md -->

[![Coverage](https://codecov.io/gh/gfx-rs/webgpu-ktypes/branch/main/graph/badge.svg?token=YOUR_TOKEN)](https://codecov.io/gh/gfx-rs/webgpu-ktypes)

ou

[![Coverage Status](https://img.shields.io/codecov/c/github/gfx-rs/webgpu-ktypes/main.svg)](https://codecov.io/github/gfx-rs/webgpu-ktypes)
```

### Configuration Codecov

```yaml
# .codecov.yml

coverage:
  status:
    project:
      default:
        # Cible globale
        target: 90%
        threshold: 1%
        base: auto
    
    patch:
      default:
        # Cible pour les PR
        target: 80%
        threshold: 1%
        base: auto

  range: 70..100
  
  round: down
  precision: 2

comment:
  layout: "header, diff, changes, sunburst, tree, kpi"
  behavior: default
  require_changes: false
  require_base: no
  require_head: yes
  
parsers:
  jacoco:
    branch_coverage: true
```

---

## Maintenance du Coverage

### Vérification Régulière

1. **À chaque PR**
   - Vérifier que le coverage n'a pas baissé
   - Ajouter des tests pour le nouveau code
   - Mettre à jour les seuils si nécessaire

2. **À chaque release**
   - Vérifier le coverage global
   - Identifier les modules à améliorer
   - Planifier les améliorations

3. **Mensuellement**
   - Revoir les rapports de coverage
   - Prioriser les améliorations
   - Mettre à jour la documentation

### Scripts d'Automatisation

```kotlin
// build.gradle.kts

// Tâche pour vérifier le coverage
val checkCoverage = tasks.register("checkCoverage") {
    dependsOn(tasks.jacocoTestReport)
    
    doLast {
        val reportFile = file("${layout.buildDirectory}/reports/jacoco/test/jacocoTestReport.xml")
        
        if (reportFile.exists()) {
            val xml = reportFile.readText()
            
            // Parser le rapport XML et vérifier les seuils
            val coverage = parseCoverage(xml)
            
            if (coverage.instruction < 0.90) {
                throw GradleException("Instruction coverage too low: ${coverage.instruction * 100}%")
            }
            if (coverage.branch < 0.85) {
                throw GradleException("Branch coverage too low: ${coverage.branch * 100}%")
            }
            
            println("Coverage checks passed!")
            println("Instruction: ${coverage.instruction * 100}%")
            println("Branch: ${coverage.branch * 100}%")
            println("Line: ${coverage.line * 100}%")
        }
    }
}

// Helper pour parser le rapport JaCoCo
data class CoverageMetrics(
    val instruction: Double,
    val branch: Double,
    val line: Double,
    val method: Double,
    val class coverage: Double
)

fun parseCoverage(xml: String): CoverageMetrics {
    // Implémentation simplifiée
    // Utiliser un parser XML pour extraire les métriques
    return CoverageMetrics(0.95, 0.90, 0.95, 1.0, 1.0)
}
```

### Génération de Rapports de Progression

```kotlin
// Script pour générer un rapport de progression

fun generateCoverageProgressReport() {
    val currentCoverage = getCurrentCoverage()
    val previousCoverage = getPreviousCoverage()
    
    println("=" * 60)
    println("COVERAGE PROGRESS REPORT")
    println("=" * 60)
    println()
    
    println("Current Coverage:")
    println("  Instruction: ${currentCoverage.instruction * 100}%")
    println("  Branch: ${currentCoverage.branch * 100}%")
    println("  Line: ${currentCoverage.line * 100}%")
    println()
    
    println("Previous Coverage:")
    println("  Instruction: ${previousCoverage.instruction * 100}%")
    println("  Branch: ${previousCoverage.branch * 100}%")
    println("  Line: ${previousCoverage.line * 100}%")
    println()
    
    val diffInstruction = (currentCoverage.instruction - previousCoverage.instruction) * 100
    val diffBranch = (currentCoverage.branch - previousCoverage.branch) * 100
    val diffLine = (currentCoverage.line - previousCoverage.line) * 100
    
    println("Changes:")
    println("  Instruction: ${"%.2f".format(diffInstruction)}% (${if (diffInstruction >= 0) "↑" else "↓"})")
    println("  Branch: ${"%.2f".format(diffBranch)}% (${if (diffBranch >= 0) "↑" else "↓"})")
    println("  Line: ${"%.2f".format(diffLine)}% (${if (diffLine >= 0) "↑" else "↓"})")
    println()
    
    println("=" * 60)
}
```

---

## Résumé

### Checklist de Coverage

- [ ] Configurer JaCoCo dans build.gradle.kts
- [ ] Définir les seuils de coverage (global et par module)
- [ ] Configurer les exclusions (tests, generated code)
- [ ] Générer les rapports HTML et XML
- [ ] Configurer l'intégration CI (Codecov, GitHub Actions)
- [ ] Vérifier le coverage actuel de chaque module
- [ ] Identifier les tests manquants
- [ ] Créer un plan pour atteindre les cibles
- [ ] Documenter la stratégie de coverage
- [ ] Configurer les badges de coverage

### Commandes Utiles

```bash
# Générer le rapport de coverage
./gradlew jacocoTestReport

# Vérifier les seuils de coverage
./gradlew jacocoCoverageVerification

# Générer un rapport pour un module spécifique
./gradlew :naga-core:jacocoTestReport

# Ouvrir le rapport HTML
open build/jacocoHtml/index.html

# Exécuter les tests avec coverage
./gradlew test jacocoTestReport
```

### Prochaines Étapes

1. Configurer JaCoCo et exécuter le premier rapport
2. Analyser les résultats et identifier les lacunes
3. Créer des tests pour les branches non couvertes
4. Atteindre 90%+ coverage sur tous les modules
5. Atteindre 95%+ coverage sur les modules critiques
6. Configurer Codecov pour le suivi continu

---

## Références

- [JaCoCo Documentation](https://www.eclemma.org/jacoco/)
- [JaCoCo Gradle Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [Codecov](https://codecov.io/)
- [Kover (Alternative Kotlin)](https://github.com/Kotlin/kover)
- [Coverage.py (Python)](https://nedbatchelder.com/code/coveragepy/)
- [Istanbul (JavaScript)](https://istanbul.js.org/)
