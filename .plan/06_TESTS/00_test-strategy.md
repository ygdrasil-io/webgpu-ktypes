# Stratégie de Test

## Sommaire

1. [Objectifs](#objectifs)
2. [Pyramide de Test](#pyramide-de-test)
3. [Types de Tests](#types-de-tests)
4. [Organisation des Tests](#organisation-des-tests)
5. [Outils et Frameworks](#outils-et-frameworks)
6. [Stratégie d'Exécution](#stratégie-d'exécution)
7. [Gestion des Dépendances de Test](#gestion-des-dépendances-de-test)
8. [Qualité des Tests](#qualité-des-tests)

---

## Objectifs

### Objectifs Principaux

1. **Validation de la Correctedness**
   - Garantir que chaque composant produit les résultats attendus
   - Vérifier la conformité avec les spécifications WebGPU/WGSL
   - Détecter les régressions dans le comportement existant

2. **Validation de la Robustesse**
   - Tester les cas limites et les entrées invalides
   - Vérifier la gestion des erreurs
   - Garantir la stabilité face aux entrées malformées

3. **Validation des Performances**
   - Mesurer le temps de parsing et de génération
   - Identifier les goulots d'étranglement
   - Garantir que les performances sont acceptables (3-10x plus lent que Rust)

4. **Validation de la Compatibilité**
   - Tester sur différentes plateformes (JVM)
   - Tester avec différentes versions de Kotlin/JVM
   - Vérifier la compatibilité avec les outils natifs

### Métriques Clés

| Métrique | Cible | Mesure |
|----------|-------|--------|
| Coverage des instructions | > 95% | JaCoCo |
| Coverage des branches | > 90% | JaCoCo |
| Coverage des backends | > 90% | Tests manuels |
| Taux de succès des tests | 100% | CI |
| Temps d'exécution moyen | < 5 min | CI |
| Temps de parsing (shader moyen) | < 100ms | Benchmark |
| Temps de génération (shader moyen) | < 100ms | Benchmark |

---

## Pyramide de Test

```
                        ┌─────────────────┐
                        │   Tests E2E     │  ~5%
                        │   (Intégration) │
                        └────────┬────────┘
                                 │
                    ┌────────────────┴────────────────┐
                    │                             │
              ┌─────▼─────┐               ┌─────▼─────┐
              │ Contract  │               │  Native    │
              │  Tests    │               │ Validation │
              │  (WGSL)   │               │  (Golden)  │
              └────────────┘               └────────────┘
                    │                             │
                    └────────────────┬────────────────┘
                                 │
        ┌────────────────────────────┼────────────────────────────┐
        │                            │                            │
  ┌─────▼─────┐              ┌─────▼─────┐              ┌─────▼─────┐
  │ Backend   │              │  Round-   │              │   Unit    │
  │ Tests     │              │  Trip     │              │   Tests   │
  │ (MSL/HLSL)│              │  Tests    │              │  (Core)   │
  └────────────┘              └────────────┘              └────────────┘
        │                            │                            │
        └────────────────────────────┴────────────────────────────┘
                                 │
                    ┌────────────────▼────────────────┐
                    │                             │
              ┌─────▼─────┐               ┌─────▼─────┐
              │ Parsing   │               │ Lexer     │
              │ Tests     │               │ Tests     │
              └────────────┘               └────────────┘
```

### Répartition des Tests

| Niveau | Type | Nombre | Temps | Objectif |
|--------|------|--------|-------|----------|
| L1 | Unit Tests | 500-1000 | < 10s | Valider les composants individuels |
| L2 | Parsing Tests | 200-500 | < 30s | Valider le parser WGSL |
| L3 | Backend Tests | 200-500 | < 1min | Valider la génération de code |
| L4 | Round-Trip Tests | 50-100 | < 2min | Valider la fidélité WGSL → IR → WGSL |
| L5 | Golden Tests | 50-100 | < 2min | Valider contre les références Rust |
| L6 | Native Validation | 50-100 | < 3min | Valider avec les compilateurs natifs |
| L7 | Contract Tests | 20-50 | < 1min | Valider la conformité WGSL |
| L8 | E2E Tests | 10-20 | < 1min | Valider les workflows complets |

---

## Types de Tests

### 1. Tests Unitaires (L1)

**Objectif** : Valider les composants individuels en isolation.

**Cible** : Classes et fonctions dans `wgsl:core`

#### Composants à Tester

| Module | Composants | Exemples |
|--------|-------------|----------|
| Arena | Handle, Arena, UniqueArena | Test création, accès, itération |
| IR | Module, Type, Expression, Statement | Test sérialisation, désérialisation |
| Primitive Types | ScalarKind, VectorSize, MatrixSize | Test conversions, validations |
| Span | Span, Spanned | Test fusion, comparaison |
| Proc | Proc, BinaryOperation | Test évaluation des expressions |

#### Exemple de Test Unitaire

```kotlin
// src/test/kotlin/dev/gfxrs/naga/core/ArenaTest.kt

package io.ygdrasil.wgsl.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class ArenaTest {
    
    @Test
    fun `test Arena creation and allocation`() {
        val arena = Arena<Int>()
        
        val handle1 = arena.append(42)
        val handle2 = arena.append(100)
        
        assertEquals(0, handle1.index)
        assertEquals(1, handle2.index)
        assertEquals(42, arena[handle1])
        assertEquals(100, arena[handle2])
    }
    
    @Test
    fun `test Arena iteration`() {
        val arena = Arena<String>()
        arena.append("first")
        arena.append("second")
        arena.append("third")
        
        val items = arena.iter().toList()
        assertEquals(3, items.size)
        assertEquals("first", items[0])
        assertEquals("second", items[1])
        assertEquals("third", items[2])
    }
    
    @Test
    fun `test Arena clear`() {
        val arena = Arena<Double>()
        val handle = arena.append(3.14)
        
        arena.clear()
        
        assertEquals(0, arena.len())
    }
}
```

#### Frameworks
- **JUnit 5** : Framework principal
- **AssertJ** : Assertions avancées
- **Kotlin Test** : Utilitaires Kotlin

### 2. Tests de Lexer (L2)

**Objectif** : Valider que le lexer tokenise correctement le code WGSL.

**Cible** : `wgsl:wgsl` module, classe `Lexer`

#### Catégories de Tests

| Catégorie | Exemples | Nombre |
|----------|----------|--------|
| Tokens simples | `fn`, `let`, `if`, `return` | 50+ |
| Littéraux | `123`, `1.5`, `0xFF`, `"string"` | 30+ |
| Identifiants | `my_var`, `_private`, `__global` | 20+ |
| Opérateurs | `+`, `-`, `*`, `/`, `==`, `!=` | 30+ |
| Attributs | `@vertex`, `@fragment`, `@location(0)` | 20+ |
| Commentaires | `//`, `/* */` | 10+ |
| Erreurs | Token invalide, string non terminé | 10+ |

#### Exemple de Test de Lexer

```kotlin
// src/test/kotlin/dev/gfxrs/naga/wgsl/LexerTest.kt

package io.ygdrasil.wgsl.wgsl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LexerTest {
    
    @Test
    fun `test simple function`() {
        val source = "fn add(a: i32, b: i32) -> i32 { return a + b; }"
        val tokens = Lexer(source).tokenize()
        
        assertEquals(13, tokens.size)
        assertEquals(TokenKind.Fn, tokens[0].kind)
        assertEquals(TokenKind.Identifier, tokens[1].kind)
        assertEquals("add", tokens[1].text)
    }
    
    @Test
    fun `test numeric literals`() {
        val tests = listOf(
            "42" to TokenKind.IntegerLiteral,
            "1.5" to TokenKind.FloatLiteral,
            "0xFF" to TokenKind.IntegerLiteral,
            "1e10" to TokenKind.FloatLiteral,
            "1.5f" to TokenKind.FloatLiteral
        )
        
        tests.forEach { (text, expectedKind) ->
            val tokens = Lexer(text).tokenize()
            assertEquals(expectedKind, tokens[0].kind)
        }
    }
    
    @Test
    fun `test attributes`() {
        val source = "@vertex @location(0) fn main() {}"
        val tokens = Lexer(source).tokenize()
        
        assertEquals(TokenKind.At, tokens[0].kind)
        assertEquals(TokenKind.Identifier, tokens[1].kind)
        assertEquals("vertex", tokens[1].text)
    }
}
```

### 3. Tests de Parsing (L2)

**Objectif** : Valider que le parser produit la bonne IR à partir du code WGSL.

**Cible** : `wgsl:wgsl` module, classe `Parser`

#### Catégories de Tests

| Catégorie | Exemples | Nombre |
|----------|----------|--------|
| Expressions | `a + b`, `a * b + c`, `fn_call(x, y)` | 100+ |
| Déclarations | `let x = 1;`, `var y: i32;` | 50+ |
| Types | `i32`, `vec4<f32>`, `mat4x4<f32>` | 50+ |
| Structures | `struct S { x: i32, y: i32 }` | 30+ |
| Fonctions | `fn f() -> i32 { ... }` | 50+ |
| Flux de contrôle | `if`, `for`, `while`, `switch` | 50+ |
| Points d'entrée | `@vertex`, `@fragment`, `@compute` | 20+ |
| Bindings | `@group(0) @binding(0)` | 20+ |

#### Exemple de Test de Parsing

```kotlin
// src/test/kotlin/dev/gfxrs/naga/wgsl/ParserTest.kt

package io.ygdrasil.wgsl.wgsl

import io.ygdrasil.wgsl.core.Module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ParserTest {
    
    @Test
    fun `test simple expression`() {
        val source = "fn main() { let x = 1 + 2; }"
        val module = parseWgsl(source).getOrThrow()
        
        assertEquals(1, module.functions.size)
        assertEquals("main", module.functions[0].name)
    }
    
    @Test
    fun `test struct definition`() {
        val source = """
            struct Vertex {
                @location(0) position: vec4<f32>,
                @location(1) color: vec4<f32>
            }
        """.trimIndent()
        
        val module = parseWgsl(source).getOrThrow()
        
        assertEquals(1, module.types.size)
    }
    
    @Test
    fun `test error handling - invalid syntax`() {
        val source = "fn main( {}"
        
        assertThrows<ParseError> {
            parseWgsl(source).getOrThrow()
        }
    }
}
```

### 4. Tests de Backend (L3)

**Objectif** : Valider que chaque backend génère du code correct.

**Cible** : `wgsl:msl`, `wgsl:hlsl`, `wgsl:glsl`, `wgsl:wgsl` modules

#### Tests par Backend

| Backend | Fichier de Test | Nombre | Objectif |
|---------|----------------|--------|----------|
| MSL | `MslWriterTest.kt` | 50-100 | Valider la génération MSL |
| HLSL | `HlslWriterTest.kt` | 50-100 | Valider la génération HLSL |
| GLSL | `GlslWriterTest.kt` | 50-100 | Valider la génération GLSL |
| WGSL | `WgslWriterTest.kt` | 50-100 | Valider le round-trip WGSL |

#### Exemple de Test de Backend

```kotlin
// src/test/kotlin/dev/gfxrs/naga/msl/MslWriterTest.kt

package io.ygdrasil.wgsl.msl

import io.ygdrasil.wgsl.core.Module
import io.ygdrasil.wgsl.core.ScalarKind
import io.ygdrasil.wgsl.core.VectorSize
import io.ygdrasil.wgsl.wgsl.parseWgsl
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MslWriterTest {
    
    @Test
    fun `test simple function`() {
        val source = """
            @fragment
            fn main() -> @location(0) vec4<f32> {
                return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
        
        val module = parseWgsl(source).getOrThrow()
        val msl = writeMsl(module)
        
        assertTrue(msl.contains("fragment"))
        assertTrue(msl.contains("float4"))
        assertTrue(msl.contains("return"))
    }
    
    @Test
    fun `test vertex shader`() {
        val source = """
            struct VertexInput {
                @location(0) position: vec4<f32>
            };
            
            @vertex
            fn main(in: VertexInput) -> @builtin(position) vec4<f32> {
                return in.position;
            }
        """.trimIndent()
        
        val module = parseWgsl(source).getOrThrow()
        val msl = writeMsl(module)
        
        assertTrue(msl.contains("vertex"))
        assertTrue(msl.contains("main"))
    }
}
```

### 5. Tests de Round-Trip (L4)

**Objectif** : Valider que WGSL → IR → WGSL préserve la sémantique.

**Cible** : `wgsl:wgsl` module (parser + writer)

#### Stratégie
- Parser le WGSL → IR
- Générer WGSL à partir de l'IR
- Normaliser les deux versions
- Comparer pour détecter les différences

#### Exemple de Test de Round-Trip

```kotlin
// src/test/kotlin/dev/gfxrs/naga/wgsl/RoundTripTest.kt

package io.ygdrasil.wgsl.wgsl

import io.ygdrasil.wgsl.core.Module
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundTripTest {
    
    @Test
    fun `test simple round-trip`() {
        val source = "fn add(a: i32, b: i32) -> i32 { return a + b; }"
        
        val module = parseWgsl(source).getOrThrow()
        val generated = writeWgsl(module)
        
        // Normaliser les deux versions (supprimer les espaces, etc.)
        val normalizedSource = source.replace(Regex("\\s+"), "")
        val normalizedGenerated = generated.replace(Regex("\\s+"), "")
        
        assertEquals(normalizedSource, normalizedGenerated)
    }
    
    @Test
    fun `test struct round-trip`() {
        val source = """
            struct Vertex {
                @location(0) position: vec4<f32>,
                @location(1) color: vec4<f32>
            }
        """.trimIndent()
        
        val module = parseWgsl(source).getOrThrow()
        val generated = writeWgsl(module)
        
        // Pour les structures, on vérifie que le parsing fonctionne
        // La comparaison exacte peut être plus complexe
        assertEquals(1, module.types.size)
    }
}
```

### 6. Tests Golden (L5)

**Objectif** : Valider que le code généré correspond aux références Rust.

**Cible** : Tous les backends

#### Exemple de Test Golden

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/golden/GoldenTestBase.kt

package io.ygdrasil.wgsl.test.golden

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText

abstract class GoldenTestBase {
    
    protected val goldenDir: Path = Paths.get("tests/golden")
    protected val updateGolden: Boolean = 
        System.getenv("GOLDEN_UPDATE")?.toBoolean() ?: false
    
    protected fun assertGolden(
        name: String,
        backend: String,
        actual: String
    ) {
        val goldenPath = goldenDir.resolve("$name.$backend")
        
        if (updateGolden) {
            Files.createDirectories(goldenPath.parent)
            goldenPath.writeText(actual)
            println("[GOLDEN UPDATE] $goldenPath")
        } else {
            require(goldenPath.exists()) { 
                "Golden file not found: $goldenPath. Run with GOLDEN_UPDATE=true." 
            }
            val expected = goldenPath.readText()
            assertEquals(expected, actual)
        }
    }
}
```

### 7. Tests de Validation Native (L5)

**Objectif** : Valider que le code généré peut être compilé par les outils natifs.

**Cible** : Intégration avec spirv-val, glslangValidator, Metal Compiler, DXC

#### Exemple de Test de Validation Native

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/validator/NativeValidatorTest.kt

package io.ygdrasil.wgsl.test.validator

import io.ygdrasil.wgsl.msl.writeMsl
import io.ygdrasil.wgsl.wgsl.parseWgsl
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test

class NativeValidatorTest {
    
    @Test
    fun `test MSL validation with Metal compiler`() {
        assumeTrue(MetalValidator.isAvailable())
        
        val source = """
            @fragment
            fn main() -> @location(0) vec4<f32> {
                return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
        
        val module = parseWgsl(source).getOrThrow()
        val msl = writeMsl(module)
        
        val result = MetalValidator().validate(msl)
        assertTrue(result.success, "Metal validation failed: ${result.output}")
    }
    
    @Test
    fun `test GLSL validation with glslangValidator`() {
        assumeTrue(GlslValidator.isAvailable())
        
        val source = """
            @fragment
            fn main() -> @location(0) vec4<f32> {
                return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
        
        val module = parseWgsl(source).getOrThrow()
        val glsl = writeGlsl(module)
        
        val result = GlslValidator().validate(glsl)
        assertTrue(result.success, "GLSL validation failed: ${result.output}")
    }
}
```

### 8. Tests de Conformité (L6)

**Objectif** : Valider la conformité avec la spécification WebGPU/WGSL.

**Cible** : Tous les modules

#### Exemples de Tests de Conformité

| Spécification | Version | Tests |
|---------------|---------|-------|
| WGSL | [Draft](https://gpuweb.github.io/gpuweb/wgsl/) | Tests de parsing, validation |
| WebGPU API | [Draft](https://gpuweb.github.io/gpuweb/) | Tests d'intégration |

#### Exemple de Test de Conformité

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/conformance/WgslConformanceTest.kt

package io.ygdrasil.wgsl.test.conformance

import io.ygdrasil.wgsl.wgsl.parseWgsl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests de conformité avec la spécification WGSL.
 * Basé sur : https://gpuweb.github.io/gpuweb/wgsl/
 */
class WgslConformanceTest {
    
    @Test
    fun `test scalar types`() {
        // Test tous les types scalaires définis dans la spec
        val types = listOf("bool", "i32", "u32", "f32", "f16")
        
        types.forEach { type ->
            val source = "fn test() { let x: $type; }"
            val result = parseWgsl(source)
            assertTrue(result.isSuccess, "Failed to parse $type")
        }
    }
    
    @Test
    fun `test vector types`() {
        // Test tous les types vecteurs définis dans la spec
        val sizes = listOf("2", "3", "4")
        val scalars = listOf("i32", "u32", "f32", "f16")
        
        sizes.forEach { size ->
            scalars.forEach { scalar ->
                val source = "fn test() { let x: vec$size<$scalar>; }"
                val result = parseWgsl(source)
                assertTrue(result.isSuccess, "Failed to parse vec$size<$scalar>")
            }
        }
    }
    
    @Test
    fun `test matrix types`() {
        // Test tous les types matrices définis dans la spec
        val rows = listOf("2", "3", "4")
        val cols = listOf("2", "3", "4")
        
        rows.forEach { row ->
            cols.forEach { col ->
                val source = "fn test() { let x: mat${row}x$col<f32>; }"
                val result = parseWgsl(source)
                assertTrue(result.isSuccess, "Failed to parse mat${row}x$col<f32>")
            }
        }
    }
    
    @Test
    fun `test invalid type - should fail`() {
        val source = "fn test() { let x: vec5<f32>; }"
        
        assertThrows<ParseError> {
            parseWgsl(source).getOrThrow()
        }
    }
}
```

### 9. Tests End-to-End (L7)

**Objectif** : Valider les workflows complets de bout en bout.

**Cible** : CLI et intégration avec les outils

#### Exemples de Workflows E2E

```
1. WGSL → IR (JSON) : Tester la sérialisation IR
2. WGSL → MSL : Tester la génération MSL complète
3. WGSL → HLSL : Tester la génération HLSL complète
4. WGSL → GLSL : Tester la génération GLSL complète
5. WGSL → IR → WGSL : Tester le round-trip complet
6. Fichier → Validation Native : Tester la validation complète
```

#### Exemple de Test E2E

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/e2e/EndToEndTest.kt

package io.ygdrasil.wgsl.test.e2e

import io.ygdrasil.wgsl.core.Module
import io.ygdrasil.wgsl.msl.writeMsl
import io.ygdrasil.wgsl.wgsl.parseWgsl
import io.ygdrasil.wgsl.wgsl.writeWgsl
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class EndToEndTest {
    
    @TempDir
    private lateinit var tempDir: Path
    
    @Test
    fun `test WGSL to IR to WGSL file workflow`() {
        val input = tempDir.resolve("input.wgsl")
        val output = tempDir.resolve("output.wgsl")
        
        val source = "fn add(a: i32, b: i32) -> i32 { return a + b; }"
        input.writeText(source)
        
        // Parse
        val module = parseWgsl(input.readText()).getOrThrow()
        
        // Generate
        val generated = writeWgsl(module)
        output.writeText(generated)
        
        // Re-parse to verify
        val module2 = parseWgsl(output.readText()).getOrThrow()
        
        // Should have same structure
        assertEquals(module.functions.size, module2.functions.size)
    }
    
    @Test
    fun `test WGSL to MSL file workflow`() {
        val input = tempDir.resolve("input.wgsl")
        val output = tempDir.resolve("output.msl")
        
        val source = """
            @fragment
            fn main() -> @location(0) vec4<f32> {
                return vec4<f32>(1.0, 0.0, 0.0, 1.0);
            }
        """.trimIndent()
        input.writeText(source)
        
        // Parse
        val module = parseWgsl(input.readText()).getOrThrow()
        
        // Generate MSL
        val msl = writeMsl(module)
        output.writeText(msl)
        
        // Verify file exists and is not empty
        assertTrue(output.exists())
        assertTrue(output.readText().isNotBlank())
    }
}
```

---

## Organisation des Tests

### Structure des Répertoires

```
src/test/kotlin/dev/gfxrs/naga/
├── core/                    # Tests pour wgsl:core
│   ├── ArenaTest.kt
│   ├── HandleTest.kt
│   ├── ModuleTest.kt
│   ├── TypeTest.kt
│   └── ...
├── wgsl/                   # Tests pour wgsl:wgsl
│   ├── lexer/
│   │   ├── LexerTest.kt
│   │   ├── TokenTest.kt
│   │   └── ...
│   ├── parser/
│   │   ├── ParserTest.kt
│   │   ├── ExpressionTest.kt
│   │   ├── StatementTest.kt
│   │   └── ...
│   ├── writer/
│   │   └── WgslWriterTest.kt
│   └── RoundTripTest.kt
├── msl/                    # Tests pour wgsl:msl
│   ├── MslWriterTest.kt
│   ├── MslExpressionTest.kt
│   └── ...
├── hlsl/                   # Tests pour wgsl:hlsl
│   ├── HlslWriterTest.kt
│   └── ...
├── glsl/                   # Tests pour wgsl:glsl
│   ├── GlslWriterTest.kt
│   └── ...
├── test/                   # Tests d'intégration
│   ├── golden/
│   │   ├── GoldenTestBase.kt
│   │   ├── IrGoldenTests.kt
│   │   ├── MslGoldenTests.kt
│   │   └── ...
│   ├── validator/
│   │   ├── BackendValidator.kt
│   │   ├── GlslValidator.kt
│   │   ├── MetalValidator.kt
│   │   ├── HlslValidator.kt
│   │   └── NativeValidatorTest.kt
│   ├── roundtrip/
│   │   ├── RoundTripTestBase.kt
│   │   ├── RoundTripTest.kt
│   │   └── ...
│   ├── conformance/
│   │   ├── WgslConformanceTest.kt
│   │   └── ...
│   └── e2e/
│       ├── EndToEndTest.kt
│       └── ...
└── benchmark/               # Tests de performance
    ├── ParsingBenchmark.kt
    ├── GenerationBenchmark.kt
    └── ...
```

### Nommage des Classes de Test

| Type | Convention | Exemple |
|------|------------|---------|
| Test unitaire | `{Class}Test` | `ArenaTest` |
| Test de module | `{Module}{Component}Test` | `WgslLexerTest` |
| Test d'intégration | `{Feature}Test` | `RoundTripTest` |
| Test golden | `{Backend}GoldenTest` | `MslGoldenTest` |
| Test de conformité | `{Spec}{Component}Test` | `WgslConformanceTest` |
| Test E2E | `{Workflow}Test` | `FileWorkflowTest` |
| Test de benchmark | `{Component}Benchmark` | `ParsingBenchmark` |

### Organisation par Catégorie

```
Tests
├── Unit (50-60%)
│   ├── Core (200-300 tests)
│   ├── WGSL Parser (100-150 tests)
│   └── Backends (150-200 tests)
├── Integration (20-30%)
│   ├── Golden (50-100 tests)
│   ├── Round-Trip (50-100 tests)
│   └── Native Validation (20-50 tests)
├── Conformance (10-15%)
│   └── WGSL/WebGPU (30-50 tests)
└── E2E (5-10%)
    └── Workflows (10-20 tests)
```

---

## Outils et Frameworks

### Frameworks de Test

| Framework | Version | Usage |
|-----------|---------|-------|
| JUnit 5 | 5.9.2 | Tests unitaires et d'intégration |
| AssertJ | 3.24.2 | Assertions avancées |
| Kotlin Test | 1.9.0 | Utilitaires Kotlin |
| JaCoCo | 0.8.11 | Coverage de code |
| JMH | 1.36 | Benchmarks de performance |

### Dépendances

```kotlin
// build.gradle.kts

plugins {
    kotlin("jvm") version "1.9.0"
    jacoco
}

dependencies {
    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-commons:1.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.9.2")
    
    // Assertions
    testImplementation("org.assertj:assertj-core:3.24.2")
    
    // Kotlin Test
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.9.0")
    
    // JaCoCo (pour le coverage)
    jacocoAgent("org.jacoco:jacoco:0.8.11")
    
    // JMH (pour les benchmarks)
    testImplementation("org.openjdk.jmh:jmh-core:1.36")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.36")
}

tasks.test {
    useJUnitPlatform()
    
    // Configuration JaCoCo
    extensions.getByType<JacocoPluginExtension>().toolVersion = "0.8.11"
}
```

### Configuration JaCoCo

```kotlin
// build.gradle.kts

jacoco {
    toolVersion = "0.8.11"
    reportsDirectory.set(layout.buildDirectory.dir("jacocoReports"))
    
    coverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = 0.95
                }
            }
        }
    }
}
```

### Configuration JMH

```kotlin
// src/main/kotlin/dev/gfxrs/naga/benchmark/BenchmarkConfig.kt

package io.ygdrasil.wgsl.benchmark

import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.Warmup
import org.openjdk.jmh.runner.options.OptionsBuilder

/**
 * Configuration de base pour les benchmarks JMH.
 */
object BenchmarkConfig {
    
    /**
     * Exécuter un benchmark avec la configuration par défaut.
     */
    fun runBenchmark(benchmarkClass: Class<*>) {
        org.openjdk.jmh.Main.array(arrayOf(
            "-i", "10",
            "-wi", "5",
            "-w", "2",
            "-f", "1",
            "-t", "1",
            "-mode", "thrpt",
            benchmarkClass.name
        ))
    }
}
```

---

## Stratégie d'Exécution

### Exécution Locale

```bash
# Exécuter tous les tests
./gradlew test

# Exécuter un module spécifique
./gradlew :wgsl:core:test
./gradlew :wgsl:wgsl:test
./gradlew :wgsl:msl:test

# Exécuter une classe de test spécifique
./gradlew test --tests "io.ygdrasil.wgsl.wgsl.LexerTest"

# Exécuter un test spécifique
./gradlew test --tests "io.ygdrasil.wgsl.wgsl.LexerTest.test simple function"

# Exécuter avec coverage
./gradlew test jacocoTestReport

# Exécuter les benchmarks
./gradlew jmh
```

### Exécution en CI

#### GitHub Actions

```yaml
# .github/workflows/test.yml
name: Test

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-22.04
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Tests
        run: ./gradlew test
      
      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport
      
      - name: Upload Coverage Report
        uses: actions/upload-artifact@v3
        with:
          name: coverage-report
          path: build/reports/jacoco/
      
      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: build/reports/tests/
```

#### Matrice de Test

```yaml
# .github/workflows/test-matrix.yml
name: Test Matrix

on: [push, pull_request]

jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-22.04, macos-13, windows-2022]
        jdk: ['17', '21']
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK ${{ matrix.jdk }}
        uses: actions/setup-java@v3
        with:
          java-version: ${{ matrix.jdk }}
          distribution: 'temurin'
      
      - name: Run Tests
        run: ./gradlew test
```

### Exécution des Benchmarks

```yaml
# .github/workflows/benchmark.yml
name: Benchmark

on:
  workflow_dispatch:
  schedule:
    - cron: '0 0 * * 0'  # Tous les dimanches à minuit

jobs:
  benchmark:
    runs-on: ubuntu-22.04
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Benchmarks
        run: ./gradlew jmh
      
      - name: Upload Benchmark Results
        uses: actions/upload-artifact@v3
        with:
          name: benchmark-results
          path: build/reports/jmh/
```

---

## Gestion des Dépendances de Test

### Fichiers de Test

Les fichiers de test WGSL sont stockés dans :
- `src/test/resources/wgsl/` - Fichiers WGSL pour les tests
- `src/test/resources/golden/` - Fichiers golden
- `tests/golden/` - Fichiers golden pour les tests d'intégration

### Organisation

```
src/test/resources/
├── wgsl/
│   ├── expressions/
│   │   ├── arithmetic.wgsl
│   │   ├── logical.wgsl
│   │   └── ...
│   ├── types/
│   │   ├── scalars.wgsl
│   │   ├── vectors.wgsl
│   │   └── ...
│   ├── functions/
│   │   ├── simple.wgsl
│   │   └── ...
│   ├── control-flow/
│   │   ├── if.wgsl
│   │   ├── for.wgsl
│   │   └── ...
│   └── shaders/
│       ├── vertex.wgsl
│       ├── fragment.wgsl
│       └── compute.wgsl
└── golden/
    ├── inputs/
    │   └── *.wgsl
    └── outputs/
        ├── msl/
        │   └── *.msl
        ├── hlsl/
        │   └── *.hlsl
        ├── glsl/
        │   └── *.glsl
        └── wgsl/
            └── *.wgsl
```

### Chargement des Fichiers

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/TestUtils.kt

package io.ygdrasil.wgsl.test

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Utilitaires pour les tests.
 */
object TestUtils {
    
    private val resourcesDir: Path = Paths.get("src/test/resources")
    
    /**
     * Charger un fichier WGSL depuis les ressources.
     */
    fun loadWgslResource(path: String): String {
        val fullPath = resourcesDir.resolve("wgsl/$path")
        require(Files.exists(fullPath)) { "Resource not found: $fullPath" }
        return Files.readString(fullPath)
    }
    
    /**
     * Charger un fichier golden.
     */
    fun loadGoldenFile(path: String): String {
        val goldenPath = Paths.get("tests/golden/$path")
        require(Files.exists(goldenPath)) { "Golden file not found: $goldenPath" }
        return Files.readString(goldenPath)
    }
    
    /**
     * Lister tous les fichiers WGSL dans un répertoire.
     */
    fun listWgslFiles(directory: String): List<Path> {
        val dirPath = resourcesDir.resolve("wgsl/$directory")
        require(Files.exists(dirPath)) { "Directory not found: $dirPath" }
        
        return Files.list(dirPath)
            .filter { it.fileName.toString().endsWith(".wgsl") }
            .toList()
    }
}
```

---

## Qualité des Tests

### Bonnes Pratiques

1. **Nommage des Tests**
   - Utiliser des noms descriptifs : `testAddition()` au lieu de `test1()`
   - Utiliser le format backtick pour les tests paramétrés : `` `test addition: 1 + 1` ``
   - Éviter les noms génériques comme `test()`, `test1()`

2. **Structure des Tests**
   - Un test = une assertion principale
   - Utiliser Arrange-Act-Assert (AAA) pattern
   - Garder les tests courts et focalisés

3. **Isolation des Tests**
   - Chaque test doit être indépendant
   - Utiliser `@BeforeEach` et `@AfterEach` pour l'initialisation
   - Éviter les effets de bord

4. **Gestion des Erreurs**
   - Tester les cas d'erreur explicitement
   - Utiliser `assertThrows` pour les exceptions
   - Vérifier les messages d'erreur

5. **Performance des Tests**
   - Éviter les opérations lourdes dans les tests
   - Utiliser `@Disabled` pour les tests temporairement désactivés
   - Utiliser `@Tag` pour catégoriser les tests

### Exemple de Test Bien Structuré

```kotlin
class GoodTestExample {
    
    @Test
    fun `test addition of positive numbers`() {
        // Arrange
        val a = 5
        val b = 3
        
        // Act
        val result = a + b
        
        // Assert
        assertEquals(8, result)
    }
    
    @Test
    fun `test addition of negative numbers`() {
        // Arrange
        val a = -5
        val b = -3
        
        // Act
        val result = a + b
        
        // Assert
        assertEquals(-8, result)
    }
    
    @Test
    fun `test addition with overflow throws exception`() {
        // Arrange
        val a = Int.MAX_VALUE
        val b = 1
        
        // Act & Assert
        assertThrows<ArithmeticException> {
            a + b
        }
    }
}
```

### Anti-Patterns à Éviter

```kotlin
class BadTestExample {
    
    // ❌ Mauvaise pratique : nom non descriptif
    @Test
    fun test1() {
        // Teste trop de choses à la fois
        val a = 5
        val b = 3
        val sum = a + b
        val diff = a - b
        
        assertEquals(8, sum)
        assertEquals(2, diff)
    }
    
    // ❌ Mauvaise pratique : dépendance entre tests
    var sharedState = 0
    
    @Test
    fun testFirst() {
        sharedState = 5
        assertEquals(5, sharedState)
    }
    
    @Test
    fun testSecond() {
        // Dépend de testFirst
        assertEquals(5, sharedState)
    }
    
    // ❌ Mauvaise pratique : test trop lent
    @Test
    fun testSlowOperation() {
        Thread.sleep(10000)
        assertTrue(true)
    }
    
    // ❌ Mauvaise pratique : catch générique
    @Test
    fun testWithCatch() {
        try {
            // code qui lance une exception
        } catch (e: Exception) {
            // Ne vérifie pas le type d'exception
        }
    }
}
```

---

## Résumé

### Checklist de la Stratégie de Test

- [ ] Définir les objectifs de test (correctedness, robustesse, performance)
- [ ] Définir la pyramide de test et les proportions
- [ ] Identifier tous les types de tests nécessaires
- [ ] Définir l'organisation des tests (structure des répertoires)
- [ ] Configurer les frameworks de test (JUnit 5, AssertJ, JaCoCo, JMH)
- [ ] Définir la stratégie d'exécution (locale, CI, benchmarks)
- [ ] Mettre en place la gestion des fichiers de test
- [ ] Documenter les bonnes pratiques de test
- [ ] Configurer les workflows CI
- [ ] Définir les métriques de qualité (coverage, performance)

### Prochaines Étapes

1. Implémenter les tests unitaires pour `wgsl:core`
2. Implémenter les tests de lexer pour `wgsl:wgsl`
3. Implémenter les tests de parsing pour `wgsl:wgsl`
4. Implémenter les tests de backend pour chaque backend
5. Implémenter les tests d'intégration (golden, round-trip)
6. Implémenter les tests de conformité
7. Implémenter les tests E2E
8. Configurer les benchmarks de performance

---

## Références

- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [AssertJ Documentation](https://assertj.github.io/doc/)
- [JaCoCo Documentation](https://www.eclemma.org/jacoco/)
- [JMH Tutorial](https://openjdk.java.net/projects/code-tools/jmh/)
- [Kotlin Testing Documentation](https://kotlinlang.org/docs/testing.html)
- [Martin Fowler - Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)
- [Google Testing Blog](https://testing.googleblog.com/)
