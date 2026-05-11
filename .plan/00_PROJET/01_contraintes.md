# 🔒 Contraintes Techniques et Fonctionnelles

**Projet** : WebGPU-KTypes Shader Transpiler  
**Code** : `naga-kt`  
**Version** : 1.0  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

---

## 🏗️ CONTRAINTES TECHNIQUES

### 1. Environnement d'Exécution

| Contrainte | Valeur | Justification |
|-----------|--------|----------------|
| **Langage** | Kotlin/JVM | Cible principale |
| **Version Kotlin** | 1.9.0+ | Stabilité, features modernes |
| **Version Java** | 17+ | LTS, support coroutines |
| **Plateformes** | JVM (Linux, macOS, Windows) | Cible principale |
| **Android** | Optionnel (Kotlin Native) | Futur |
| **Web** | Non supporté (Kotlin/JS) | Hors scope |

### 2. Contraintes de Performance

| Opération | Contrainte | Justification |
|-----------|------------|----------------|
| Parsing WGSL | < 100ms/shader (100 lignes) | Acceptable pour build-time |
| IR → MSL | < 50ms/shader | Acceptable pour build-time |
| IR → HLSL | < 50ms/shader | Acceptable pour build-time |
| Mémoire | < 100MB pour un shader complexe | Éviter OOM |
| Thread-safe | Oui | Utilisation concurrente possible |

**Note** : Ces contraintes sont pour des **shaders de taille moyenne** (~100-200 lignes).  
Les performances peuvent être linéairement pires pour des shaders très grands.

### 3. Contraintes de Mémoire

| Contrainte | Valeur | Justification |
|-----------|--------|----------------|
| **Allocation minimale** | Éviter les allocations inutiles | Performance |
| **Object Pooling** | Pour les objets temporaires | Réduire GC pressure |
| **Structures de données** | Préférer Array/ArrayList | Performance prévisible |
| **Cache** | Utiliser LRU cache pour les résultats | Éviter recomputation |

### 4. Contraintes de Compatibilité

| Contrainte | Valeur | Justification |
|-----------|--------|----------------|
| **WGSL Spec** | Dernière version stable | Compatibilité wgpu |
| **MSL** | Metal Shading Language 2.3+ | Support Apple moderne |
| **HLSL** | Shader Model 5.0+ | Support DirectX 11/12 |
| **GLSL** | 330 (Desktop) / ES 300 (Mobile) | Minimum viable |
| **SPIR-V** | 1.3+ (optionnel) | Vulkan moderne |

---

## 📦 CONTRAINTES SUR LES DÉPENDANCES

### ✅ Dépendances **AUTORISÉES**

| Bibliothèque | Version | Usage | Justification |
|-------------|---------|-------|----------------|
| `kotlin-stdlib` | 1.9.0+ | Standard | Incontournable |
| `kotlinx-coroutines-core` | 1.7.0+ | Async | Optionnel pour CLI |
| `kotlinx-serialization-json` | 1.6.0+ | Sérialisation | Pour golden files |
| `kotlinx-io` | - | E/S | Si besoin pour fichiers |

### ⚠️ Dépendances **À ÉVITER** (si possible)

| Bibliothèque | Alternative | Justification |
|-------------|-------------|----------------|
| ANTLR | Parser manuel | Complexité, taille |
| Jackson/Gson | kotlinx.serialization | Déjà autorisée |
| Log4j/SLF4J | println (dev) | Simplicité |
| JUnit 4 | JUnit 5 | Moderne |

### ❌ Dépendances **INTERDITES**

| Bibliothèque | Raison |
|-------------|--------|
| LuaJ | Peu utile |
| Reflection intensif | Performance, sécurité |
| Native code (JNI) | Portabilité |
| Guava | Trop lourde |
| Apache Commons | Trop lourde |

---

## 🔐 CONTRAINTES DE SÉCURITÉ

### 1. Sécurité du Code
- [ ] **Pas de unsafe code** (Kotlin n'a pas d'équivalent)
- [ ] **Validation des entrées** : Tous les inputs doivent être validés
- [ ] **Pas de code injection** : Éviter eval(), reflection sur inputs
- [ ] **Gestion des erreurs** : Pas de panic/crash sur input invalide

### 2. Sécurité des Fichiers
- [ ] **Lecture sécurisée** : Vérifier les paths, éviter path traversal
- [ ] **Écriture sécurisée** : Atomic writes pour éviter corruption
- [ ] **Taille des fichiers** : Limiter la taille des shaders (< 1MB par défaut)

### 3. Sécurité Thread
- [ ] **Immutable par défaut** : Préférer val/data class
- [ ] **Synchronisation explicite** : Utiliser Mutex/ReadWriteLock si mutable
- [ ] **Pas de race conditions** : Design thread-safe dès le début

---

## 📁 CONTRAINTES SUR LES FICHIERS

### 1. Structure du Projet

```
webgpu-ktypes/
├── settings.gradle.kts          # Configuration multi-module
├── build.gradle.kts             # Build root
├── gradle.properties            # Propriétés Gradle
├── .plan/                       # Ce plan (NE PAS COMMITER)
│
├── naga-core/                   # Module IR + Processing
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── dev/gfxrs/naga/
│           ├── ir/              # IR Structures
│           ├── arena/          # Arena System
│           ├── proc/           # Processing
│           └── valid/          # Validation
│
├── naga-wgsl/                   # Frontend WGSL
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── dev/gfxrs/naga/front/wgsl/
│
├── naga-msl/                    # Backend MSL
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── dev/gfxrs/naga/back/msl/
│
├── naga-hlsl/                   # Backend HLSL
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── dev/gfxrs/naga/back/hlsl/
│
├── naga-glsl/                   # Backend GLSL
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── dev/gfxrs/naga/back/glsl/
│
├── naga-cli/                    # Outil CLI
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│
└── tests/                      # Tests partagés
    ├── golden/                 # Golden files (copiés depuis Naga Rust)
    │   ├── in/                  # Fichiers d'entrée
    │   └── out/                 # Fichiers de sortie attendus
    └── src/test/kotlin/         # Tests unitaires/intégration
```

### 2. Conventions de Nommage

| Type | Convention | Exemple |
|------|------------|---------|
| Package | lowercase, séparés par points | `dev.gfxrs.naga.ir` |
| Class | PascalCase | `Module`, `Type`, `Expression` |
| Interface | PascalCase | `ExpressionVisitor` |
| Sealed Class | PascalCase | `Expression` |
| Data Class | PascalCase | `Expression.Binary` |
| Object | PascalCase | `ExpressionPool` |
| Function | camelCase | `parseWgsl()`, `writeMsl()` |
| Property | camelCase | `type`, `name`, `expressions` |
| Constant | UPPER_SNAKE_CASE | `MAGIC_NUMBER`, `WGSL_VERSION` |
| File | lowercase-hyphen | `module.kt`, `expression.kt` |

### 3. Conventions de Code

```kotlin
// ✅ BON
sealed class Expression {
    data class Literal(val value: Literal) : Expression()
    data class Binary(
        val op: BinaryOperator,
        val left: Handle<Expression>,
        val right: Handle<Expression>
    ) : Expression()
}

// ❌ MAUVAIS (à éviter)
sealed class Expression
class Literal(val value: Literal) : Expression()  // Pas data class
class Binary(val op: BinaryOperator, val left: Handle<Expression>, val right: Handle<Expression>) : Expression()  // Ligne trop longue
```

**Règles :**
- [ ] **120 colonnes max** par ligne
- [ ] **4 espaces** pour l'indentation (pas de tabs)
- [ ] **Pas de wildcards imports** (`import dev.gfxrs.naga.*`)
- [ ] **Ordre des imports** : Kotlin stdlib → 3rd party → Local
- [ ] **Documentation** : KDoc pour toutes les classes/fonctions publiques

### 4. Gestion des Versions

- **Versioning** : Semantic Versioning (SemVer)
  - MAJOR : Changements breaking
  - MINOR : Ajouts rétrocompatibles
  - PATCH : Bug fixes
- **Changelog** : Maintenir un CHANGELOG.md
- **API Stability** : Pas de breaking changes dans les versions MINOR

---

## 🧪 CONTRAINTES DE TEST

### 1. Types de Tests

| Type | Outil | Couverture Cible | Exécution |
|------|-------|------------------|------------|
| Unit Tests | Kotlin Test + JUnit 5 | > 80% | CI + Local |
| Integration Tests | Kotlin Test | > 70% | CI |
| Snapshot Tests | Custom | 100% golden files | CI |
| Performance Tests | JMH | Benchmark | CI Nightly |

### 2. Golden Files

- **Source** : Copiés depuis `/Users/chaos/RustroverProjects/wgpu/naga/tests/`
- **Format** : Fichiers `.wgsl`, `.msl`, `.hlsl`, `.glsl`
- **Validation** : Comparaison textuelle exacte (sauf whitespace optionnel)
- **Mise à jour** : Script pour régénérer les golden files depuis Naga Rust

### 3. Validateurs Natifs

| Langage | Outil | Commande | Installation |
|---------|-------|----------|--------------|
| **MSL** | Metal compiler | `metal -c file.metal` | Xcode Command Line Tools |
| **HLSL** | DXC | `dxc -T ps_5_0 file.hlsl` | Windows SDK |
| **HLSL** | FXC | `fxc /T ps_5_0 file.hlsl` | Windows SDK |
| **GLSL** | glslangValidator | `glslangValidator -V file.glsl` | Vulkan SDK |
| **SPIR-V** | spirv-val | `spirv-val file.spv` | Vulkan SDK |

**Note** : Ces outils doivent être disponibles dans le **PATH** pour les tests CI.

---

## 🔧 CONTRAINTES DE BUILD

### 1. Build System

| Outil | Version | Usage |
|-------|---------|-------|
| Gradle | 8.4+ | Build principal |
| Kotlin Gradle Plugin | 1.9.0+ | Plugin Kotlin |
| JDK | 17+ | Compilation |

### 2. Configuration Gradle

**Requirements :**
- [ ] **Multi-module build** : Chaque module est indépendant
- [ ] **Dependencies resolution** : Cache local + remote
- [ ] **Incremental build** : Récompilation minimale
- [ ] **Test filtering** : Exécuter tests spécifiques
- [ ] **Publishing** : Maven Local + Maven Central (futur)

### 3. CI/CD Requirements

| Outil | Usage | Configuration |
|-------|-------|----------------|
| GitHub Actions | CI principale | `.github/workflows/` |
| Azure Pipelines | CI alternative | Optionnel |
| GitLab CI | CI alternative | Optionnel |

**Pipeline CI minimale :**
1. Build tous les modules
2. Exécuter tous les tests unitaires
3. Exécuter tests d'intégration
4. Valider golden files
5. Générer rapport de couverture

---

## 📊 CONTRAINTES DE DOCUMENTATION

### 1. Documentation Code

- [ ] **KDoc** : Toutes les classes/fonctions/méthodes publiques
- [ ] **Exemples** : Dans le KDoc pour les APIs publiques
- [ ] **Paramètres** : Description de chaque paramètre
- [ ] **Retour** : Description de la valeur de retour
- [ ] **Exceptions** : Documentation des exceptions possibles

### 2. Documentation Utilisateur

| Document | Contenu | Format |
|----------|---------|--------|
| README.md | Overview, installation, usage | Markdown |
| CHANGELOG.md | Historique des versions | Markdown |
| API Documentation | Générée depuis KDoc | HTML (Dokka) |
| Examples | Exemples d'utilisation | Kotlin files dans `/examples` |

### 3. Documentation Technique

| Document | Contenu | Format | Emplacement |
|----------|---------|--------|-------------|
| Architecture | Architecture globale | Markdown | `/docs/architecture.md` |
| Design Decisions | Décisions de design | Markdown | `/docs/adr/` (Architecture Decision Records) |
| Performance | Benchmarks, optimisations | Markdown | `/docs/performance.md` |

---

## 🎯 CONTRAINTES SPÉCIFIQUES PAR MODULE

### naga-core

| Contrainte | Valeur |
|-----------|--------|
| Dépendances | Aucune (module root) |
| Taille estimée | 5-8k lignes |
| Couverture tests | > 90% |
| Performance | Critique |

### naga-wgsl

| Contrainte | Valeur |
|-----------|--------|
| Dépendances | naga-core |
| Taille estimée | 3-5k lignes |
| Couverture tests | > 85% |
| Performance | Élevée |

### naga-msl

| Contrainte | Valeur |
|-----------|--------|
| Dépendances | naga-core |
| Taille estimée | 2-3k lignes |
| Couverture tests | > 80% |
| Performance | Élevée |

### naga-hlsl

| Contrainte | Valeur |
|-----------|--------|
| Dépendances | naga-core |
| Taille estimée | 2-3k lignes |
| Couverture tests | > 80% |
| Performance | Élevée |

### naga-glsl

| Contrainte | Valeur |
|-----------|--------|
| Dépendances | naga-core |
| Taille estimée | 2-3k lignes |
| Couverture tests | > 80% |
| Performance | Moyenne |

### naga-cli

| Contrainte | Valeur |
|-----------|--------|
| Dépendances | Tous les modules |
| Taille estimée | 500-1000 lignes |
| Couverture tests | > 70% |
| Performance | Faible (CLI) |

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Valider ces contraintes avec l'équipe
2. [ ] Configurer l'environnement de build (Gradle, Kotlin)
3. [ ] Créer la structure de fichiers de base
4. [ ] Passer à la phase de design détaillé (fichier `02_dependances.md`)
