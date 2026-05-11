# Tests - Checklist

## Phase 6 : Tests
**Objectif** : Mettre en place une infrastructure complète de tests pour garantir la qualité, la robustesse et les performances de WebGPU-KTypes.

---

## 📋 Structure

- [ ] **00_test-strategy.md** - Stratégie de test globale ✅
- [ ] **01_test-coverage.md** - Couverture de test et métriques ✅
- [ ] **99_checklist.md** - Checklist complète pour la Phase 6 ✅

---

## 🎯 Objectifs de la Phase

### Objectifs Principaux
- [ ] Atteindre 90%+ de coverage sur tous les modules
- [ ] Atteindre 95%+ de coverage sur les modules critiques (core, wgsl)
- [ ] Mettre en place tous les types de tests (unitaires, intégration, E2E)
- [ ] Configurer l'intégration CI pour les tests
- [ ] Configurer le suivi de coverage (Codecov)
- [ ] Documenter la stratégie de test

### Objectifs Secondaires
- [ ] Atteindre 95%+ de coverage sur tous les modules
- [ ] Mettre en place des benchmarks de performance
- [ ] Configurer des tests de régression
- [ ] Documenter les procédures de test

---

## 📁 Structure des Répertoires

### Répertoires de Test
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/` pour tous les tests
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/core/` pour les tests naga-core
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/wgsl/` pour les tests naga-wgsl
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/msl/` pour les tests naga-msl
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/hlsl/` pour les tests naga-hlsl
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/glsl/` pour les tests naga-glsl
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/test/` pour les tests d'intégration
- [ ] Créer `src/test/kotlin/dev/gfxrs/naga/benchmark/` pour les benchmarks

### Répertoires de Ressources
- [ ] Créer `src/test/resources/` pour les fichiers de test
- [ ] Créer `src/test/resources/wgsl/` pour les fichiers WGSL
- [ ] Créer `src/test/resources/golden/` pour les fichiers golden

### Répertoires de Build
- [ ] Créer `build/reports/tests/` pour les rapports de test
- [ ] Créer `build/reports/jacoco/` pour les rapports de coverage
- [ ] Créer `build/reports/jmh/` pour les rapports de benchmark

---

## 📄 Fichiers de Configuration

### Configuration Gradle
- [ ] Configurer `build.gradle.kts` (racine) avec les plugins de test
- [ ] Configurer `build.gradle.kts` (naga-core) pour les tests
- [ ] Configurer `build.gradle.kts` (naga-wgsl) pour les tests
- [ ] Configurer `build.gradle.kts` (naga-msl) pour les tests
- [ ] Configurer `build.gradle.kts` (naga-hlsl) pour les tests
- [ ] Configurer `build.gradle.kts` (naga-glsl) pour les tests
- [ ] Configurer JaCoCo dans tous les modules

### Configuration CI
- [ ] Créer `.github/workflows/test.yml` pour les tests de base
- [ ] Créer `.github/workflows/coverage.yml` pour le coverage
- [ ] Créer `.github/workflows/benchmark.yml` pour les benchmarks
- [ ] Créer `.github/workflows/test-matrix.yml` pour la matrice de test
- [ ] Configurer Codecov dans `.codecov.yml`

---

## 🔧 Tests Unitaires

### naga-core

#### Classes à Tester
- [ ] `Handle<T>` - Tests de création, comparaison, sérialisation
- [ ] `Arena<T>` - Tests d'ajout, accès, itération, suppression
- [ ] `UniqueArena<T>` - Tests de création unique, accès
- [ ] `Range` - Tests de création, fusion, comparaison
- [ ] `Span` - Tests de création, fusion
- [ ] `Spanned<T>` - Tests de wrapping avec span

#### Structures IR
- [ ] `Module` - Tests de création, validation, sérialisation
- [ ] `Type` - Tests de tous les types (scalaires, vecteurs, matrices, etc.)
- [ ] `ScalarType` - Tests de ScalarKind et width
- [ ] `VectorType` - Tests de création et validation
- [ ] `MatrixType` - Tests de création et validation
- [ ] `ArrayType` - Tests de création et accès
- [ ] `StructType` - Tests de création, accès aux membres
- [ ] `PointerType` - Tests de création et validation
- [ ] `FunctionType` - Tests de création et validation

#### Expressions
- [ ] `Expression` - Tests de tous les types d'expressions
- [ ] `Literal` - Tests de tous les types de littéraux
- [ ] `Variable` - Tests de référence de variable
- [ ] `BinaryOperation` - Tests de toutes les opérations binaires
- [ ] `UnaryOperation` - Tests de toutes les opérations unaires
- [ ] `Call` - Tests d'appel de fonction
- [ ] `Access` - Tests d'accès aux membres
- [ ] `AccessIndex` - Tests d'accès par index
- [ ] `Select` - Tests d'opérateur ternaire
- [ ] `As` - Tests de cast et bitcast

#### Statements
- [ ] `Statement` - Tests de tous les types de statements
- [ ] `Emit` - Tests d'émission d'instruction
- [ ] `Block` - Tests de bloc de statements
- [ ] `If` - Tests de conditionnelle
- [ ] `Switch` - Tests de switch
- [ ] `Loop` - Tests de boucle
- [ ] `Break` - Tests de break
- [ ] `Continue` - Tests de continue
- [ ] `Return` - Tests de return

#### Fonctions
- [ ] `Function` - Tests de création et validation
- [ ] `EntryPoint` - Tests de points d'entrée
- [ ] `Parameter` - Tests de paramètres
- [ ] `LocalVariable` - Tests de variables locales
- [ ] `GlobalVariable` - Tests de variables globales

### naga-wgsl

#### Lexer
- [ ] `TokenKind` - Tests de tous les types de tokens
- [ ] `Token` - Tests de création et comparaison
- [ ] `Lexer` - Tests de tokenisation
  - [ ] Tokens simples (mots-clés)
  - [ ] Identifiants
  - [ ] Littéraux (nombres, strings, booléens)
  - [ ] Opérateurs
  - [ ] Attributs
  - [ ] Commentaires
  - [ ] Erreurs de lexing

#### Parser
- [ ] `Parser` - Tests de parsing complet
  - [ ] Déclarations de variables
  - [ ] Déclarations de types
  - [ ] Déclarations de fonctions
  - [ ] Déclarations de structures
  - [ ] Expressions
  - [ ] Statements
  - [ ] Points d'entrée
  - [ ] Bindings
  - [ ] Attributs
  - [ ] Erreurs de parsing

#### Writer
- [ ] `WgslWriter` - Tests de génération WGSL
  - [ ] Types
  - [ ] Expressions
  - [ ] Statements
  - [ ] Fonctions
  - [ ] Structures
  - [ ] Modules complets

#### Round-Trip
- [ ] Tests de round-trip (WGSL → IR → WGSL)
  - [ ] Expressions simples
  - [ ] Structures
  - [ ] Fonctions
  - [ ] Modules complets
  - [ ] Tous les fichiers golden

### naga-msl

#### Writer
- [ ] `MslWriter` - Tests de génération MSL
  - [ ] Types de base
  - [ ] Vecteurs et matrices
  - [ ] Structures
  - [ ] Variables
  - [ ] Fonctions
  - [ ] Expressions
  - [ ] Statements
  - [ ] Points d'entrée
  - [ ] Bindings
  - [ ] Fonctions builtin

#### Validation
- [ ] `MetalValidator` - Tests de validation avec Metal Compiler
  - [ ] Shaders simples
  - [ ] Shaders complexes
  - [ ] Tous les types de shaders (vertex, fragment, compute)

### naga-hlsl

#### Writer
- [ ] `HlslWriter` - Tests de génération HLSL
  - [ ] Types de base
  - [ ] Vecteurs et matrices
  - [ ] Structures
  - [ ] Variables
  - [ ] Fonctions
  - [ ] Expressions
  - [ ] Statements
  - [ ] Points d'entrée
  - [ ] Bindings
  - [ ] Fonctions builtin

#### Validation
- [ ] `HlslValidator` - Tests de validation avec DXC/FXC
  - [ ] Shaders simples
  - [ ] Shaders complexes
  - [ ] Tous les shader models (ps_5_0, ps_6_0, etc.)

### naga-glsl

#### Writer
- [ ] `GlslWriter` - Tests de génération GLSL
  - [ ] Types de base
  - [ ] Vecteurs et matrices
  - [ ] Structures
  - [ ] Variables
  - [ ] Fonctions
  - [ ] Expressions
  - [ ] Statements
  - [ ] Points d'entrée
  - [ ] Bindings
  - [ ] Fonctions builtin

#### Validation
- [ ] `GlslValidator` - Tests de validation avec glslangValidator
  - [ ] Shaders simples
  - [ ] Shaders complexes
  - [ ] Différentes versions GLSL (450, 460, etc.)

---

## 🔄 Tests d'Intégration

### Golden Tests
- [ ] `GoldenTestBase` - Classe de base pour les tests golden
- [ ] `IrGoldenTests` - Tests golden pour la sérialisation IR
- [ ] `MslGoldenTests` - Tests golden pour MSL
- [ ] `HlslGoldenTests` - Tests golden pour HLSL
- [ ] `GlslGoldenTests` - Tests golden pour GLSL
- [ ] `WgslRoundTripTests` - Tests golden pour le round-trip WGSL

### Tests de Validation Native
- [ ] `BackendValidator` - Interface commune pour les validateurs
- [ ] `ValidatorFactory` - Factory pour les validateurs
- [ ] `SpirvValidator` - Validateur SPIR-V (optionnel)
- [ ] `GlslValidator` - Validateur GLSL
- [ ] `MetalValidator` - Validateur MSL
- [ ] `HlslValidator` - Validateur HLSL
- [ ] `NativeValidatorTests` - Tests d'intégration des validateurs

### Tests de Round-Trip
- [ ] `RoundTripTestBase` - Classe de base pour les tests de round-trip
- [ ] `WgslNormalizer` - Normaliseur de code WGSL
- [ ] `DifferenceAnalyzer` - Analyseur de différences
- [ ] `RoundTripValidator` - Validateur de round-trip
- [ ] `RoundTripTests` - Tests paramétrés de round-trip
- [ ] `TargetedRoundTripTests` - Tests ciblés de round-trip
- [ ] `CrossBackendConsistencyTests` - Tests de cohérence multi-backend
- [ ] `RoundTripReport` - Génération de rapports
- [ ] `RoundTripConfig` - Configuration des tests de round-trip
- [ ] `RoundTripMetrics` - Métriques des tests de round-trip

### Tests de Conformité
- [ ] `WgslConformanceTest` - Tests de conformité WGSL
  - [ ] Types scalaires
  - [ ] Types vecteurs
  - [ ] Types matrices
  - [ ] Types tableaux
  - [ ] Types structures
  - [ ] Expressions
  - [ ] Statements
  - [ ] Fonctions
  - [ ] Points d'entrée
  - [ ] Bindings

### Tests End-to-End
- [ ] `EndToEndTest` - Tests de workflows complets
  - [ ] WGSL → IR (JSON)
  - [ ] WGSL → MSL
  - [ ] WGSL → HLSL
  - [ ] WGSL → GLSL
  - [ ] WGSL → IR → WGSL
  - [ ] Fichier → Validation Native

---

## ⚡ Benchmarks de Performance

### Configuration
- [ ] Configurer JMH dans `build.gradle.kts`
- [ ] Créer `src/main/kotlin/dev/gfxrs/naga/benchmark/`
- [ ] Configurer les dépendances JMH

### Benchmarks
- [ ] `ParsingBenchmark` - Benchmark du parsing WGSL
  - [ ] Parsing de shaders simples
  - [ ] Parsing de shaders complexes
  - [ ] Parsing de fichiers golden
- [ ] `IrCreationBenchmark` - Benchmark de création d'IR
  - [ ] Création de modules simples
  - [ ] Création de modules complexes
- [ ] `MslGenerationBenchmark` - Benchmark de génération MSL
  - [ ] Génération de shaders simples
  - [ ] Génération de shaders complexes
- [ ] `HlslGenerationBenchmark` - Benchmark de génération HLSL
  - [ ] Génération de shaders simples
  - [ ] Génération de shaders complexes
- [ ] `GlslGenerationBenchmark` - Benchmark de génération GLSL
  - [ ] Génération de shaders simples
  - [ ] Génération de shaders complexes
- [ ] `RoundTripBenchmark` - Benchmark de round-trip
  - [ ] Round-trip de shaders simples
  - [ ] Round-trip de shaders complexes

---

## 📊 Coverage de Code

### Configuration JaCoCo
- [ ] Configurer JaCoCo dans tous les modules
- [ ] Définir les seuils de coverage
  - [ ] Global : 90% instructions, 85% branches
  - [ ] naga-core : 95% instructions, 90% branches
  - [ ] naga-wgsl : 95% instructions, 90% branches
  - [ ] naga-msl : 90% instructions, 85% branches
  - [ ] naga-hlsl : 90% instructions, 85% branches
  - [ ] naga-glsl : 90% instructions, 85% branches
- [ ] Configurer les exclusions (tests, generated code)

### Exécution
- [ ] Exécuter `./gradlew jacocoTestReport`
- [ ] Vérifier les rapports HTML
- [ ] Identifier les lacunes de coverage
- [ ] Créer des tests pour les branches non couvertes

### Suivi
- [ ] Configurer Codecov
- [ ] Configurer le badge de coverage
- [ ] Configurer les notifications de coverage

---

## 🔌 Intégration CI

### GitHub Actions

#### Workflow de Test
- [ ] Créer `.github/workflows/test.yml`
- [ ] Configurer l'exécution sur push et pull_request
- [ ] Configurer l'exécution sur plusieurs OS (Ubuntu, macOS, Windows)
- [ ] Configurer l'exécution avec plusieurs JDK (17, 21)
- [ ] Configurer le cache Gradle
- [ ] Configurer l'upload des artefacts (rapports de test)

#### Workflow de Coverage
- [ ] Créer `.github/workflows/coverage.yml`
- [ ] Configurer l'exécution de JaCoCo
- [ ] Configurer l'upload du rapport de coverage
- [ ] Configurer la vérification des seuils
- [ ] Configurer l'upload vers Codecov

#### Workflow de Benchmark
- [ ] Créer `.github/workflows/benchmark.yml`
- [ ] Configurer l'exécution des benchmarks
- [ ] Configurer l'upload des résultats
- [ ] Configurer l'exécution périodique

#### Workflow de Matrice de Test
- [ ] Créer `.github/workflows/test-matrix.yml`
- [ ] Configurer la matrice OS × JDK
- [ ] Configurer l'exécution parallèle

---

## 📄 Fichiers à Créer

### Configuration
```
.plan/06_TESTS/
├── 00_test-strategy.md          # ✅ Déjà créé
├── 01_test-coverage.md          # ✅ Déjà créé
└── 99_checklist.md              # ✅ Ce document

.github/workflows/
├── test.yml                     # Workflow de test principal
├── coverage.yml                 # Workflow de coverage
├── benchmark.yml                # Workflow de benchmark
└── test-matrix.yml              # Workflow de matrice de test

.codecov.yml                    # Configuration Codecov

build.gradle.kts                # Configuration Gradle (mise à jour)
settings.gradle.kts             # Configuration Gradle (mise à jour)
```

### Tests Unitaires
```
src/test/kotlin/dev/gfxrs/naga/
├── core/
│   ├── ArenaTest.kt
│   ├── HandleTest.kt
│   ├── ModuleTest.kt
│   ├── TypeTest.kt
│   ├── ExpressionTest.kt
│   ├── StatementTest.kt
│   ├── FunctionTest.kt
│   └── ...
├── wgsl/
│   ├── lexer/
│   │   ├── LexerTest.kt
│   │   └── TokenTest.kt
│   ├── parser/
│   │   ├── ParserTest.kt
│   │   ├── ExpressionParserTest.kt
│   │   ├── StatementParserTest.kt
│   │   └── ...
│   ├── writer/
│   │   └── WgslWriterTest.kt
│   └── RoundTripTest.kt
├── msl/
│   ├── MslWriterTest.kt
│   └── MslExpressionTest.kt
├── hlsl/
│   ├── HlslWriterTest.kt
│   └── HlslExpressionTest.kt
├── glsl/
│   ├── GlslWriterTest.kt
│   └── GlslExpressionTest.kt
└── test/
    ├── golden/
    │   ├── GoldenTestBase.kt
    │   ├── IrGoldenTests.kt
    │   ├── MslGoldenTests.kt
    │   ├── HlslGoldenTests.kt
    │   ├── GlslGoldenTests.kt
    │   └── WgslRoundTripTests.kt
    ├── validator/
    │   ├── BackendValidator.kt
    │   ├── ValidatorFactory.kt
    │   ├── SpirvValidator.kt
    │   ├── GlslValidator.kt
    │   ├── MetalValidator.kt
    │   ├── HlslValidator.kt
    │   ├── ValidationLogger.kt
    │   └── NativeValidatorTests.kt
    ├── roundtrip/
    │   ├── RoundTripTestBase.kt
    │   ├── RoundTripValidator.kt
    │   ├── WgslNormalizer.kt
    │   ├── DifferenceAnalyzer.kt
    │   ├── RoundTripTests.kt
    │   ├── TargetedRoundTripTests.kt
    │   ├── CrossBackendConsistencyTests.kt
    │   ├── RoundTripReport.kt
    │   ├── RoundTripConfig.kt
    │   └── RoundTripMetrics.kt
    ├── conformance/
    │   └── WgslConformanceTest.kt
    └── e2e/
        ├── EndToEndTest.kt
        └── FileWorkflowTest.kt
```

### Benchmarks
```
src/main/kotlin/dev/gfxrs/naga/benchmark/
├── BenchmarkConfig.kt
├── ParsingBenchmark.kt
├── IrCreationBenchmark.kt
├── MslGenerationBenchmark.kt
├── HlslGenerationBenchmark.kt
├── GlslGenerationBenchmark.kt
└── RoundTripBenchmark.kt
```

### Ressources
```
src/test/resources/
├── wgsl/
│   ├── expressions/
│   │   ├── arithmetic.wgsl
│   │   ├── logical.wgsl
│   │   ├── bitwise.wgsl
│   │   └── ...
│   ├── types/
│   │   ├── scalars.wgsl
│   │   ├── vectors.wgsl
│   │   ├── matrices.wgsl
│   │   ├── arrays.wgsl
│   │   └── structs.wgsl
│   ├── functions/
│   │   ├── simple.wgsl
│   │   ├── recursion.wgsl
│   │   └── ...
│   ├── control-flow/
│   │   ├── if.wgsl
│   │   ├── for.wgsl
│   │   ├── while.wgsl
│   │   ├── switch.wgsl
│   │   ├── break-continue.wgsl
│   │   └── ...
│   └── shaders/
│       ├── vertex.wgsl
│       ├── fragment.wgsl
│       ├── compute.wgsl
│       └── ...
└── golden/
    ├── inputs/
    │   └── *.wgsl
    └── outputs/
        ├── ir/
        │   └── *.json
        ├── msl/
        │   └── *.msl
        ├── hlsl/
        │   └── *.hlsl
        ├── glsl/
        │   └── *.glsl
        └── wgsl/
            └── *.wgsl
```

---

## 🎯 Critères d'Acceptation

### Pour la Phase 6
- [ ] Tous les fichiers de configuration créés
- [ ] Tous les types de tests implémentés
- [ ] 90%+ de coverage sur tous les modules
- [ ] 95%+ de coverage sur les modules critiques
- [ ] Intégration CI configurée et fonctionnelle
- [ ] Benchmarks configurés et exécutables
- [ ] Documentation complète

### Pour chaque Module
- [ ] naga-core : 95%+ coverage, tous les tests passent
- [ ] naga-wgsl : 95%+ coverage, tous les tests passent
- [ ] naga-msl : 90%+ coverage, tous les tests passent
- [ ] naga-hlsl : 90%+ coverage, tous les tests passent
- [ ] naga-glsl : 90%+ coverage, tous les tests passent

### Pour chaque Type de Test
- [ ] Tests unitaires : 500-1000 tests, tous passent
- [ ] Tests d'intégration : 200-500 tests, tous passent
- [ ] Tests de conformité : 30-50 tests, tous passent
- [ ] Tests E2E : 10-20 tests, tous passent
- [ ] Benchmarks : 6+ benchmarks, exécutables

---

## 📅 Planning

### Semaine 1-2 : Infrastructure
- [ ] Configurer Gradle pour les tests
- [ ] Configurer JaCoCo
- [ ] Configurer JMH
- [ ] Créer la structure des répertoires
- [ ] Configurer GitHub Actions

### Semaine 3-4 : Tests Unitaires (Core)
- [ ] Implémenter les tests pour Arena, Handle, Module
- [ ] Implémenter les tests pour les types IR
- [ ] Implémenter les tests pour les expressions
- [ ] Implémenter les tests pour les statements
- [ ] Atteindre 95%+ coverage sur naga-core

### Semaine 5-6 : Tests Unitaires (WGSL)
- [ ] Implémenter les tests pour le lexer
- [ ] Implémenter les tests pour le parser
- [ ] Implémenter les tests pour le writer
- [ ] Implémenter les tests de round-trip
- [ ] Atteindre 95%+ coverage sur naga-wgsl

### Semaine 7-8 : Tests Unitaires (Backends)
- [ ] Implémenter les tests pour MSL writer
- [ ] Implémenter les tests pour HLSL writer
- [ ] Implémenter les tests pour GLSL writer
- [ ] Atteindre 90%+ coverage sur chaque backend

### Semaine 9-10 : Tests d'Intégration
- [ ] Implémenter les golden tests
- [ ] Implémenter les validateurs natifs
- [ ] Implémenter les tests de round-trip
- [ ] Implémenter les tests de conformité
- [ ] Implémenter les tests E2E

### Semaine 11-12 : Benchmarks et Finalisation
- [ ] Implémenter les benchmarks de performance
- [ ] Configurer Codecov
- [ ] Vérifier le coverage global
- [ ] Identifier et corriger les lacunes
- [ ] Finaliser la documentation

---

## 🔍 Vérification

### Vérification des Fichiers
- [ ] Vérifier que tous les fichiers de configuration existent
- [ ] Vérifier que tous les fichiers de test existent
- [ ] Vérifier que tous les benchmarks existent
- [ ] Vérifier que la structure des répertoires est correcte

### Vérification des Tests
- [ ] Vérifier que tous les tests compilent
- [ ] Vérifier que tous les tests passent localement
- [ ] Vérifier que tous les tests passent en CI
- [ ] Vérifier que le coverage est > 90% sur tous les modules
- [ ] Vérifier que le coverage est > 95% sur les modules critiques

### Vérification CI
- [ ] Vérifier que tous les workflows CI existent
- [ ] Vérifier que tous les workflows CI passent
- [ ] Vérifier que les artefacts sont générés
- [ ] Vérifier que Codecov est configuré

### Vérification des Benchmarks
- [ ] Vérifier que tous les benchmarks exécutent
- [ ] Vérifier que les résultats sont cohérents
- [ ] Vérifier que les benchmarks sont exécutés périodiquement

---

## 📚 Documentation

### Documentation Technique
- [ ] Documenter la configuration des tests
- [ ] Documenter la stratégie de test
- [ ] Documenter l'architecture des tests
- [ ] Documenter l'intégration CI
- [ ] Documenter les benchmarks

### Documentation Utilisateur
- [ ] Documenter comment exécuter les tests localement
- [ ] Documenter comment exécuter les benchmarks
- [ ] Documenter comment générer les rapports de coverage
- [ ] Documenter comment ajouter de nouveaux tests
- [ ] Documenter comment mettre à jour les golden files

---

## ✅ État Actuel

| Élément | État | Date | Notes |
|---------|------|------|-------|
| 00_test-strategy.md | ✅ | 2024-XX-XX | Complété |
| 01_test-coverage.md | ✅ | 2024-XX-XX | Complété |
| 99_checklist.md | ✅ | 2024-XX-XX | En cours |
| Configuration Gradle | ⬜ | - | À configurer |
| Configuration JaCoCo | ⬜ | - | À configurer |
| Configuration CI | ⬜ | - | À configurer |
| Tests Unitaires (Core) | ⬜ | - | À implémenter |
| Tests Unitaires (WGSL) | ⬜ | - | À implémenter |
| Tests Unitaires (Backends) | ⬜ | - | À implémenter |
| Tests d'Intégration | ⬜ | - | À implémenter |
| Benchmarks | ⬜ | - | À implémenter |

---

## 🎯 Prochaines Étapes

1. [ ] **Phase 7 - CLI** : Créer les documents pour l'interface en ligne de commande
2. [ ] **Phase 99 - Annexes** : Créer les documents annexes (glossaire, comparaison Rust/Kotlin)
3. [ ] Commencer l'implémentation des tests (si la phase de planification est terminée)

---

## 📞 Contacts et Ressources

### Ressources Internes
- Projet Rust : `/Users/chaos/RustroverProjects/wgpu/naga/`
- Fichiers de test Rust : `/Users/chaos/RustroverProjects/wgpu/naga/src/*/mod.rs`
- Tests Rust : `/Users/chaos/RustroverProjects/wgpu/naga/tests/`

### Outils Externes
- JUnit 5 : [https://junit.org/junit5/](https://junit.org/junit5/)
- AssertJ : [https://assertj.github.io/](https://assertj.github.io/)
- JaCoCo : [https://www.eclemma.org/jacoco/](https://www.eclemma.org/jacoco/)
- JMH : [https://openjdk.java.net/projects/code-tools/jmh/](https://openjdk.java.net/projects/code-tools/jmh/)
- Codecov : [https://codecov.io/](https://codecov.io/)
- GitHub Actions : [https://github.com/features/actions](https://github.com/features/actions)

### Documentation de Référence
- [Gradle Testing](https://docs.gradle.org/current/userguide/java_testing.html)
- [Kotlin Testing](https://kotlinlang.org/docs/testing.html)
- [JaCoCo Gradle Plugin](https://docs.gradle.org/current/userguide/jacoco_plugin.html)
- [JMH Tutorial](https://baeldung.com/jmh)
- [Codecov Documentation](https://docs.codecov.io/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

---

## 📝 Notes

### Notes d'Implémentation
- Utiliser JUnit 5 pour tous les tests
- Utiliser AssertJ pour les assertions avancées
- Utiliser JaCoCo pour le coverage
- Utiliser JMH pour les benchmarks
- Suivre les conventions de nommage Kotlin
- Utiliser les data classes pour les structures de test
- Utiliser les sealed classes pour les hiérarchies de test

### Notes de Performance
- Les tests de validation native peuvent être lents
- Prévoir un timeout pour les tests lents
- Exécuter les tests de validation native en parallèle
- Utiliser `@DisabledOnOs` pour les tests spécifiques à une plateforme

### Notes de Compatibilité
- Les validateurs natifs peuvent ne pas être disponibles sur toutes les plateformes
- Prévoir des fallbacks (skip des tests) quand les outils ne sont pas disponibles
- Documenter clairement les prérequis pour chaque validateur
- Utiliser `assumeTrue` pour skip les tests quand les dépendances ne sont pas disponibles

---

**Dernière mise à jour** : 2024-XX-XX
**Responsable** : Équipe WebGPU-KTypes
**Statut** : En cours
