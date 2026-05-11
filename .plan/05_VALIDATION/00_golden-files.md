# Validation par Fichiers Golden

## Sommaire

1. [Principe](#principe)
2. [Structure des Fichiers](#structure-des-fichiers)
3. [Intégration Kotlin](#intégration-kotlin)
4. [Génération des Snapshots](#génération-des-snapshots)
5. [Validation des Résultats](#validation-des-résultats)
6. [Maintenance](#maintenance)

---

## Principe

### Concept

Les **fichiers golden** (ou *golden master files*) sont une technique de test de régression qui consiste à :

1. **Capturer** la sortie d'un processus (sérialisation IR, code généré MSL/HLSL/GLSL)
2. **Stocker** cette sortie dans un fichier de référence
3. **Comparer** les exécutions futures contre ces références

Tout écart signale une régression ou une modification intentionnelle du comportement.

### Avantages

- **Détection de régression** : Changement de comportement involontaire immédiatement identifié
- **Documentation vivante** : Les fichiers golden servent de documentation concrète
- **Validation croisée** : Permet de vérifier la cohérence entre backends
- **Intégration CI** : Exécutable automatiquement dans les pipelines

### Cas d'Usage

| Scénario | Fichier Source | Fichier Golden | Backend |
|----------|---------------|----------------|---------|
| Parsing WGSL | `input.wgsl` | `output.ir.json` | IR Sérialisée |
| Génération MSL | `input.wgsl` | `output.msl` | MSL |
| Génération HLSL | `input.wgsl` | `output.hlsl` | HLSL |
| Génération GLSL | `input.wgsl` | `output.glsl` | GLSL |
| Round-trip WGSL | `input.wgsl` | `output.wgsl` | WGSL |

---

## Structure des Fichiers

### Organisation des Répertoires

```
.webgpu-ktypes/
├── tests/
│   ├── golden/
│   │   ├── inputs/
│   │   │   └── wgsl/
│   │   │       ├── expressions.wgsl
│   │   │       ├── functions.wgsl
│   │   │       ├── types.wgsl
│   │   │       ├── control-flow.wgsl
│   │   │       ├── builtins.wgsl
│   │   │       └── ...
│   │   └── outputs/
│   │       ├── ir/
│   │       │   └── *.json
│   │       ├── msl/
│   │       │   └── *.msl
│   │       ├── hlsl/
│   │       │   └── *.hlsl
│   │       ├── glsl/
│   │       │   └── *.glsl
│   │       └── wgsl/
│   │           └── *.wgsl
│   └── snapshots/
│       └── *.snap  (tests de snapshot)
└── src/test/kotlin/... (code de test)
```

### Répertoire Source Rust

Les fichiers golden de référence sont situés dans :
```
/Users/chaos/RustroverProjects/wgpu/naga/tests/
├── in/
│   └── wgsl/
│       ├── const-exprs.wgsl
│       ├── conv-bvec.wgsl
│       ├── entry-point-args.wgsl
│       ├── global-variable.wgsl
│       ├── local-variable.wgsl
│       ├── matrix.wgsl
│       ├── pointers.wgsl
│       ├── structs.wgsl
│       ├── type-inference.wgsl
│       └── ... (50+ fichiers)
└── out/
    └── wgsl/
        └── *.wgsl (snapshots de sortie)
```

### Catégories de Tests

#### 1. Tests de Parsing (WGSL → IR)
Fichiers : `tests/golden/inputs/wgsl/*.wgsl`
Sortie : `tests/golden/outputs/ir/*.json`

Valide que le parser produit la bonne représentation IR.

#### 2. Tests de Génération MSL
Fichiers : `tests/golden/inputs/wgsl/*.wgsl`
Sortie : `tests/golden/outputs/msl/*.msl`

Valide que le backend MSL produit du code Metal valide.

#### 3. Tests de Génération HLSL
Fichiers : `tests/golden/inputs/wgsl/*.wgsl`
Sortie : `tests/golden/outputs/hlsl/*.hlsl`

Valide que le backend HLSL produit du code DirectX valide.

#### 4. Tests de Génération GLSL
Fichiers : `tests/golden/inputs/wgsl/*.wgsl`
Sortie : `tests/golden/outputs/glsl/*.glsl`

Valide que le backend GLSL produit du code OpenGL valide.

#### 5. Tests Round-Trip (WGSL → IR → WGSL)
Fichiers : `tests/golden/inputs/wgsl/*.wgsl`
Sortie : `tests/golden/outputs/wgsl/*.wgsl`

Valide que la sérialisation WGSL est cohérente avec l'entrée.

### Nommage des Fichiers

- **Input** : `{feature}-{scenario}.wgsl` (ex: `const-exprs.wgsl`, `matrix-multiplication.wgsl`)
- **Output IR** : `{feature}-{scenario}.ir.json`
- **Output MSL** : `{feature}-{scenario}.msl`
- **Output HLSL** : `{feature}-{scenario}.hlsl`
- **Output GLSL** : `{feature}-{scenario}.glsl`
- **Output WGSL** : `{feature}-{scenario}.wgsl`

---

## Intégration Kotlin

### Dépendances

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
}
```

### Classe de Base pour les Tests Golden

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/GoldenTestBase.kt

package io.ygdrasil.wgsl.test

import io.ygdrasil.wgsl.backends.msl.writeMsl
import io.ygdrasil.wgsl.backends.hlsl.writeHlsl
import io.ygdrasil.wgsl.backends.glsl.writeGlsl
import io.ygdrasil.wgsl.backends.wgsl.writeWgsl
import io.ygdrasil.wgsl.frontends.wgsl.parseWgsl
import io.ygdrasil.wgsl serializeIrToJson
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals

/**
 * Base class for golden file tests.
 * 
 * Provides utilities for:
 * - Loading input WGSL files
 * - Parsing to IR
 * - Generating output for various backends
 * - Comparing against golden files
 * - Updating golden files (when GOLDEN_UPDATE env var is set)
 */
abstract class GoldenTestBase {
    
    protected val goldenDir: Path = Paths.get("tests/golden")
    protected val inputDir: Path = goldenDir.resolve("inputs/wgsl")
    protected val outputDir: Path = goldenDir.resolve("outputs")
    
    protected val irOutputDir: Path = outputDir.resolve("ir")
    protected val mslOutputDir: Path = outputDir.resolve("msl")
    protected val hlslOutputDir: Path = outputDir.resolve("hlsl")
    protected val glslOutputDir: Path = outputDir.resolve("glsl")
    protected val wgslOutputDir: Path = outputDir.resolve("wgsl")
    
    protected val updateGolden: Boolean = 
        System.getenv("GOLDEN_UPDATE")?.toBoolean() ?: false
    
    protected val jsonFormatter: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Load WGSL source from file
     */
    protected fun loadWgsl(name: String): String {
        val path = inputDir.resolve("$name.wgsl")
        require(path.exists()) { "WGSL input file not found: $path" }
        return path.readText()
    }
    
    /**
     * Parse WGSL to IR Module
     */
    protected fun parseToIr(source: String): Result<Module> = parseWgsl(source)
    
    /**
     * Generate MSL from IR
     */
    protected fun generateMsl(module: Module): String = writeMsl(module)
    
    /**
     * Generate HLSL from IR
     */
    protected fun generateHlsl(module: Module): String = writeHlsl(module)
    
    /**
     * Generate GLSL from IR
     */
    protected fun generateGlsl(module: Module): String = writeGlsl(module)
    
    /**
     * Generate WGSL from IR
     */
    protected fun generateWgsl(module: Module): String = writeWgsl(module)
    
    /**
     * Serialize IR to JSON
     */
    protected fun serializeIr(module: Module): String = serializeIrToJson(module)
    
    /**
     * Get golden file path
     */
    protected fun getGoldenPath(
        name: String,
        backend: BackendType
    ): Path = when (backend) {
        BackendType.IR -> irOutputDir.resolve("$name.ir.json")
        BackendType.MSL -> mslOutputDir.resolve("$name.msl")
        BackendType.HLSL -> hlslOutputDir.resolve("$name.hlsl")
        BackendType.GLSL -> glslOutputDir.resolve("$name.glsl")
        BackendType.WGSL -> wgslOutputDir.resolve("$name.wgsl")
    }
    
    /**
     * Assert output matches golden file, or update if GOLDEN_UPDATE is set
     */
    protected fun assertOrUpdateGolden(
        name: String,
        backend: BackendType,
        actual: String
    ) {
        val goldenPath = getGoldenPath(name, backend)
        
        if (updateGolden) {
            // Create parent directories if needed
            goldenPath.parent?.let { parent ->
                if (!parent.exists()) {
                    Files.createDirectories(parent)
                }
            }
            // Update golden file
            goldenPath.writeText(actual)
            println("[GOLDEN UPDATE] Updated: $goldenPath")
        } else {
            // Assert against golden file
            require(goldenPath.exists()) {
                "Golden file not found: $goldenPath. Run with GOLDEN_UPDATE=true to create."
            }
            val expected = goldenPath.readText()
            
            // Normalize line endings for comparison
            val normalizedActual = actual.replace("\r\n", "\n").replace("\r", "\n")
            val normalizedExpected = expected.replace("\r\n", "\n").replace("\r", "\n")
            
            assertEquals(
                normalizedExpected,
                normalizedActual,
                "Golden file mismatch: $goldenPath"
            )
        }
    }
    
    enum class BackendType {
        IR, MSL, HLSL, GLSL, WGSL
    }
}
```

### Tests Concrets

#### Test de Parsing IR

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/golden/IrGoldenTests.kt

package io.ygdrasil.wgsl.test.golden

import io.ygdrasil.wgsl.test.GoldenTestBase
import io.ygdrasil.wgsl.test.GoldenTestBase.BackendType.IR
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class IrGoldenTests : GoldenTestBase() {
    
    @ParameterizedTest
    @ValueSource(strings = [
        "const-exprs",
        "conv-bvec",
        "entry-point-args",
        "global-variable",
        "local-variable",
        "matrix",
        "pointers",
        "structs",
        "type-inference"
    ])
    @DisplayName("IR Golden Test: {0}")
    fun `test IR generation`(name: String) {
        val source = loadWgsl(name)
        val module = parseToIr(source).getOrThrow()
        val actual = serializeIr(module)
        
        assertOrUpdateGolden(name, IR, actual)
    }
}
```

#### Test de Génération MSL

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/golden/MslGoldenTests.kt

package io.ygdrasil.wgsl.test.golden

import io.ygdrasil.wgsl.test.GoldenTestBase
import io.ygdrasil.wgsl.test.GoldenTestBase.BackendType.MSL
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class MslGoldenTests : GoldenTestBase() {
    
    @ParameterizedTest
    @ValueSource(strings = [
        "const-exprs",
        "entry-point-args",
        "global-variable",
        "local-variable",
        "matrix",
        "structs"
    ])
    @DisplayName("MSL Golden Test: {0}")
    fun `test MSL generation`(name: String) {
        val source = loadWgsl(name)
        val module = parseToIr(source).getOrThrow()
        val actual = generateMsl(module)
        
        assertOrUpdateGolden(name, MSL, actual)
    }
}
```

#### Test Round-Trip WGSL

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/golden/WgslRoundTripTests.kt

package io.ygdrasil.wgsl.test.golden

import io.ygdrasil.wgsl.test.GoldenTestBase
import io.ygdrasil.wgsl.test.GoldenTestBase.BackendType.WGSL
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class WgslRoundTripTests : GoldenTestBase() {
    
    @ParameterizedTest
    @ValueSource(strings = [
        "const-exprs",
        "entry-point-args",
        "global-variable",
        "local-variable",
        "matrix",
        "structs"
    ])
    @DisplayName("WGSL Round-Trip Test: {0}")
    fun `test WGSL round-trip`(name: String) {
        val source = loadWgsl(name)
        val module = parseToIr(source).getOrThrow()
        val actual = generateWgsl(module)
        
        assertOrUpdateGolden(name, WGSL, actual)
    }
}
```

---

## Génération des Snapshots

### Initialisation

1. **Créer la structure de répertoires** :
```bash
mkdir -p tests/golden/{inputs/wgsl,outputs/{ir,msl,hlsl,glsl,wgsl}}
```

2. **Copier les fichiers WGSL depuis Rust** :
```bash
# Depuis le répertoire wgpu
cp /Users/chaos/RustroverProjects/wgpu/naga/tests/in/wgsl/*.wgsl \
   /Users/chaos/IdeaProjects/webgpu-ktypes/tests/golden/inputs/wgsl/
```

### Génération Initiale

Exécuter les tests avec `GOLDEN_UPDATE=true` pour générer les fichiers de référence :

```bash
# macOS/Linux
GOLDEN_UPDATE=true ./gradlew test --tests "io.ygdrasil.wgsl.test.golden.*"

# Windows (PowerShell)
$env:GOLDEN_UPDATE="true"
./gradlew test --tests "io.ygdrasil.wgsl.test.golden.*"
```

### Mise à Jour des Snapshots

Lorsqu'un changement intentionnel est apporté (correction de bug, nouvelle feature) :

1. Mettre à jour le code
2. Exécuter avec `GOLDEN_UPDATE=true`
3. Vérifier les diffs dans git
4. Valider les changements

```bash
# Voir les fichiers modifiés
git diff tests/golden/outputs/

# Valider les changements
git add tests/golden/outputs/
git commit -m "Update golden files for [reason]"
```

---

## Validation des Résultats

### Vérification Manuelle

Pour valider un fichier MSL généré :

```bash
# Compiler avec Metal Shader Compiler (macOS)
xcrun -sdk macosx metal /path/to/output.msl -o /dev/null

# Voir les erreurs
xcrun -sdk macosx metal --print-ast /path/to/output.msl
```

Pour valider un fichier HLSL :

```bash
# Utiliser DXC (Windows)
dxc /path/to/output.hlsl -T ps_6_0 /Zi /Fd output.pdb /Fe output.bin

# Ou FXC (legacy)
fxc /path/to/output.hlsl /T ps_5_0 /Fo output.obj
```

### Intégration CI

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Golden Tests
        run: ./gradlew test --tests "io.ygdrasil.wgsl.test.golden.*"
      
      - name: Update Golden Files (if changed)
        if: github.ref == 'refs/heads/main'
        run: |
          GOLDEN_UPDATE=true ./gradlew test --tests "io.ygdrasil.wgsl.test.golden.*"
          git config --global user.name "GitHub Actions"
          git config --global user.email "actions@github.com"
          git add tests/golden/outputs/
          git commit -m "Update golden files [skip ci]" || echo "No changes to commit"
          git push
```

---

## Maintenance

### Ajout d'un Nouveau Test

1. **Créer le fichier WGSL** :
   - Placer dans `tests/golden/inputs/wgsl/{name}.wgsl`
   - Suivre les conventions de nommage

2. **Ajouter au test** :
   - Ajouter le nom au `@ValueSource` dans le test approprié

3. **Générer les golden files** :
   ```bash
   GOLDEN_UPDATE=true ./gradlew test --tests "io.ygdrasil.wgsl.test.golden.*" -k "{name}"
   ```

4. **Valider les résultats** :
   - Vérifier que le code généré est correct
   - Valider avec les compilateurs natifs si nécessaire

### Suppression d'un Test

1. Supprimer le fichier WGSL
2. Supprimer les fichiers golden associés
3. Supprimer le nom du `@ValueSource`

### Migration des Fichiers Rust

Pour importer de nouveaux fichiers depuis le projet Rust :

```bash
#!/bin/bash
RUST_DIR="/Users/chaos/RustroverProjects/wgpu/naga/tests/in/wgsl"
KTYPE_DIR="/Users/chaos/IdeaProjects/webgpu-ktypes/tests/golden/inputs/wgsl"

# Copier les nouveaux fichiers
rsync -av --ignore-existing "$RUST_DIR/" "$KTYPE_DIR/"

# ou copier tous les fichiers (écrase les existants)
cp "$RUST_DIR"/*.wgsl "$KTYPE_DIR/"
```

### Bonnes Pratiques

1. **Commits atomiques** : Un commit par changement logique
2. **Messages clairs** : Expliquer POURQUOI les golden files ont changé
3. **Revue de code** : Toujours review les changements de golden files
4. **Tests ciblés** : Exécuter seulement les tests concernés lors du développement

```bash
# Exécuter un test spécifique
./gradlew test --tests "io.ygdrasil.wgsl.test.golden.MslGoldenTests.test MSL generation:matrix"
```

---

## Références

- [Naga Rust Tests](https://github.com/gfx-rs/naga/tree/trunk/tests)
- [Golden Master Testing Pattern](https://martinfowler.com/bliki/GoldenMaster.html)
- [JUnit 5 Parameterized Tests](https://junit.org/junit5/docs/current/user-guide/#writing-tests-parameterized-tests)
- [AssertJ Assertions](https://assertj.github.io/doc/#assertj-core)
