# Tests de Round-Trip (WGSL → IR → WGSL)

## Sommaire

1. [Principe](#principe)
2. [Architecture](#architecture)
3. [Implémentation Kotlin](#implémentation-kotlin)
4. [Cas de Test](#cas-de-test)
5. [Validation de la Cohérence](#validation-de-la-cohérence)
6. [Gestion des Différences Acceptables](#gestion-des-différences-acceptables)
7. [Intégration CI](#intégration-ci)
8. [Metrics et Coverage](#metrics-et-coverage)

---

## Principe

### Définition

Un **test de round-trip** valide que la chaîne de transformation complète préserve la sémantique du shader :

```
WGSL Source
    │
    ▼
┌─────────────────┐
│   Parse → IR    │  (Frontend)
└─────────────────┘
    │
    ▼
┌─────────────────┐
│  IR → WGSL       │  (Backend WGSL Writer)
└─────────────────┘
    │
    ▼
WGSL Generated
    │
    ▼
┌─────────────────────────────────────────┐
│  Compare: Source ≡ Generated (modulo     │
│           normalisation)                  │
└─────────────────────────────────────────┘
```

### Objectifs

1. **Validation de la fidélité** : Le code généré produit le même comportement que l'original
2. **Détection de régression** : Les modifications du parser ou du writer n'introduisent pas d'erreurs
3. **Validation de la sérialisation** : La représentation IR est complète et exacte
4. **Documentation** : Les fichiers de test servent de documentation concrète

### Levels de Round-Trip

| Niveau | Description | Exemple |
|--------|-------------|---------|
| **Niveau 1** | Identique à la chaîne | `a + b` → `a + b` |
| **Niveau 2** | Équivalent sémantique | `a + b` → `b + a` (si commutatif) |
| **Niveau 3** | Équivalent après optimisation | `(a + 0)` → `a` |
| **Niveau 4** | Équivalent fonctionnel | Fonctions différentes mais même résultat |

**Stratégie WebGPU-KTypes** : Cibler le **Niveau 1** (identique) ou **Niveau 2** (équivalent sémantique) avec une liste blanche de différences acceptables.

---

## Architecture

### Composants

```
┌─────────────────────────────────────────────────────────────┐
│                    RoundTripTestEngine                       │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │ WGSL Parser │    │ IR → WGSL   │    │  Comparator │     │
│  │             │───▶│ Writer      │───▶│             │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
│           ▲                  ▲                  ▲          │
│           │                  │                  │          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐     │
│  │  Input      │    │  IR Module  │    │  Normalized │     │
│  │  WGSL       │    │             │    │  Comparison │     │
│  └─────────────┘    └─────────────┘    └─────────────┘     │
└─────────────────────────────────────────────────────────────┘
                    │
                    ▼
            ┌─────────────┐
            │ Test Result │
            └─────────────┘
```

### Flux de Données

1. **Input** : Fichier WGSL source (`input.wgsl`)
2. **Parsing** : `parseWgsl(input)` → `Module` (IR)
3. **Génération** : `writeWgsl(module)` → `String` (WGSL généré)
4. **Normalisation** : `normalizeWgsl(generated)` → `String` (normalisé)
5. **Comparaison** : `normalizeWgsl(input)` == `normalizeWgsl(generated)`

---

## Implémentation Kotlin

### Structure des Classes

```
src/test/kotlin/dev/gfxrs/naga/test/roundtrip/
├── RoundTripTestBase.kt      # Classe de base
├── RoundTripValidator.kt     # Validateur de round-trip
├── WgslNormalizer.kt         # Normaliseur WGSL
├── DifferenceAnalyzer.kt     # Analyse des différences
├── RoundTripTests.kt         # Tests concrets
└── RoundTripReport.kt        # Rapport de test
```

### Classe de Base

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripTestBase.kt

package dev.gfxrs.naga.test.roundtrip

import dev.gfxrs.naga.backends.wgsl.writeWgsl
import dev.gfxrs.naga.frontends.wgsl.parseWgsl
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText

/**
 * Base class for round-trip tests.
 * 
 * Provides utilities for:
 * - Loading WGSL files
 * - Parsing to IR
 * - Generating WGSL from IR
 * - Normalizing WGSL for comparison
 * - Comparing source vs generated
 */
abstract class RoundTripTestBase {
    
    protected val roundTripDir: Path = Paths.get("tests/roundtrip")
    protected val inputDir: Path = roundTripDir.resolve("inputs")
    protected val outputDir: Path = roundTripDir.resolve("outputs")
    protected val reportDir: Path = roundTripDir.resolve("reports")
    
    protected val normalizer: WgslNormalizer = WgslNormalizer()
    protected val analyzer: DifferenceAnalyzer = DifferenceAnalyzer()
    
    /**
     * Load WGSL source from file
     */
    protected fun loadWgsl(name: String): String {
        val path = inputDir.resolve("$name.wgsl")
        require(path.exists()) { "WGSL input file not found: $path" }
        return path.readText()
    }
    
    /**
     * Perform round-trip: WGSL → IR → WGSL
     */
    protected fun roundTrip(source: String): Result<RoundTripResult> {
        return try {
            // Parse to IR
            val parseResult = parseWgsl(source)
            if (parseResult.isFailure) {
                return Result.failure(parseResult.exceptionOrNull()!!)
            }
            val module = parseResult.getOrThrow()
            
            // Generate WGSL
            val generated = writeWgsl(module)
            
            // Normalize both
            val normalizedSource = normalizer.normalize(source)
            val normalizedGenerated = normalizer.normalize(generated)
            
            // Compare
            val differences = analyzer.analyze(
                source, generated, normalizedSource, normalizedGenerated
            )
            
            // Check for acceptable differences
            val isValid = differences.all { diff ->
                diff.isAcceptable || diff.isWhitelisted()
            }
            
            Result.success(RoundTripResult(
                source = source,
                generated = generated,
                normalizedSource = normalizedSource,
                normalizedGenerated = normalizedGenerated,
                differences = differences,
                isValid = isValid,
                module = module
            ))
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Assert that round-trip is valid
     */
    protected fun assertRoundTripValid(name: String) {
        val source = loadWgsl(name)
        val result = roundTrip(source).getOrThrow()
        
        if (!result.isValid) {
            val report = RoundTripReport(result)
            println(report.generateTextReport())
        }
        
        assert(result.isValid) {
            "Round-trip validation failed for $name. See report above."
        }
    }
}

/**
 * Result of a round-trip test
 */
data class RoundTripResult(
    val source: String,
    val generated: String,
    val normalizedSource: String,
    val normalizedGenerated: String,
    val differences: List<Difference>,
    val isValid: Boolean,
    val module: Module
)
```

### Normaliseur WGSL

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/WgslNormalizer.kt

package dev.gfxrs.naga.test.roundtrip

/**
 * Normalizes WGSL code for comparison.
 * 
 * Handles:
 * - Whitespace normalization (spaces, tabs, newlines)
 * - Comment removal
 * - Blank line removal
 * - Statement reordering (where semantically equivalent)
 * - Identifier renaming (for generated names)
 */
class WgslNormalizer {
    
    /**
     * Normalize WGSL code for comparison
     */
    fun normalize(source: String): String {
        return source
            .let { removeComments(it) }
            .let { normalizeWhitespace(it) }
            .let { normalizeIdentifiers(it) }
            .let { normalizeLiterals(it) }
            .trim()
    }
    
    /**
     * Remove all comments (single-line and multi-line)
     */
    private fun removeComments(source: String): String {
        // Remove multi-line comments first
        var result = source.replace(Regex("/\*.*?\*\"/", setOf(RegexOption.DOT_MATCHES_ALL)), "")
        // Remove single-line comments
        result = result.replace(Regex("//.*?$", setOf(RegexOption.MULTILINE)), "")
        return result
    }
    
    /**
     * Normalize whitespace:
     * - Convert tabs to spaces
     * - Collapse multiple spaces to single space
     * - Normalize newlines
     */
    private fun normalizeWhitespace(source: String): String {
        return source
            .replace("\t", " ")
            .replace(Regex("[ \t]+"), " ")
            .replace(Regex("\n+"), "\n")
            .replace(Regex("\n\s+"), "\n")
    }
    
    /**
     * Normalize identifiers:
     * - Preserve user-defined names
     * - Normalize generated names (e.g., _expr_123, _var_456)
     */
    private fun normalizeIdentifiers(source: String): String {
        // This is a simplified version - a full implementation would
        // need to parse the AST to distinguish user vs generated names
        
        // Normalize common generated name patterns
        return source
            .replace(Regex("_expr_\\d+"), "_expr_")
            .replace(Regex("_var_\\d+"), "_var_")
            .replace(Regex("_func_\\d+"), "_func_")
            .replace(Regex("_type_\\d+"), "_type_")
    }
    
    /**
     * Normalize numeric literals:
     * - 1.0 → 1.0 (preserve)
     * - 1 → 1 (preserve)
     * - 0xFF → 255 (convert hex to decimal)
     * - 1e6 → 1000000 (convert scientific to decimal)
     */
    private fun normalizeLiterals(source: String): String {
        // Convert hex integers to decimal
        var result = source.replace(Regex("0x([0-9A-Fa-f]+)")) { match ->
            match.groupValues[1].toLong(16).toString()
        }
        
        // Convert scientific notation to decimal (for simple cases)
        result = result.replace(Regex("(\\d+)e(\\d+)")) { match ->
            val base = match.groupValues[1].toDouble()
            val exponent = match.groupValues[2].toInt()
            (base * Math.pow(10.0, exponent.toDouble())).toLong().toString()
        }
        
        return result
    }
    
    /**
     * Normalize for semantic comparison (more aggressive)
     * This handles cases where the order of statements doesn't matter.
     */
    fun normalizeForSemanticComparison(source: String): String {
        val normalized = normalize(source)
        
        // Sort top-level declarations (functions, structs, variables, etc.)
        // This requires parsing the AST
        // For now, just return the basic normalization
        return normalized
    }
}
```

### Analyseur de Différences

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/DifferenceAnalyzer.kt

package dev.gfxrs.naga.test.roundtrip

/**
 * Analyzes differences between source and generated WGSL.
 */
class DifferenceAnalyzer {
    
    private val whitelist: Set<DifferencePattern> = buildSet {
        // Whitespace differences
        add(DifferencePattern(
            pattern = Regex("\\s+"),
            description = "Whitespace difference",
            isAcceptable = true
        ))
        
        // Comment differences (should be removed by normalizer)
        add(DifferencePattern(
            pattern = Regex("//.*?$|/\\*.*?\\*/", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)),
            description = "Comment difference",
            isAcceptable = true
        ))
        
        // Generated name differences
        add(DifferencePattern(
            pattern = Regex("_expr_\\d+|"_var_\\d+|"_func_\\d+|"_type_\\d+"),
            description = "Generated identifier difference",
            isAcceptable = true
        ))
        
        // Literal formatting differences (1.0 vs 1.0f)
        add(DifferencePattern(
            pattern = Regex("\\d+\\.\\d+f?|0x[0-9A-Fa-f]+"),
            description = "Numeric literal formatting",
            isAcceptable = true
        ))
        
        // Attribute order differences
        add(DifferencePattern(
            pattern = Regex("@[a-z_]+\\(.*?\\)"),
            description = "Attribute order",
            isAcceptable = true
        ))
        
        // Type annotation differences (explicit vs inferred)
        add(DifferencePattern(
            pattern = Regex(":\\s*[a-z_<>,\\s]+"),
            description = "Type annotation",
            isAcceptable = false // This might indicate a real difference
        ))
    }
    
    /**
     * Analyze differences between source and generated code
     */
    fun analyze(
        source: String,
        generated: String,
        normalizedSource: String,
        normalizedGenerated: String
    ): List<Difference> {
        val differences = mutableListOf<Difference>()
        
        // If normalized versions are identical, no differences
        if (normalizedSource == normalizedGenerated) {
            return differences
        }
        
        // Compare line by line
        val sourceLines = normalizedSource.lines()
        val generatedLines = normalizedGenerated.lines()
        
        val maxLines = maxOf(sourceLines.size, generatedLines.size)
        
        for (i in 0 until maxLines) {
            val sourceLine = sourceLines.getOrNull(i) ?: ""
            val generatedLine = generatedLines.getOrNull(i) ?: ""
            
            if (sourceLine != generatedLine) {
                val diff = analyzeLineDifference(sourceLine, generatedLine, i + 1)
                differences.addAll(diff)
            }
        }
        
        return differences
    }
    
    private fun analyzeLineDifference(
        sourceLine: String,
        generatedLine: String,
        lineNumber: Int
    ): List<Difference> {
        val differences = mutableListOf<Difference>()
        
        // Check if the difference matches any whitelisted pattern
        for (pattern in whitelist) {
            if (pattern.pattern.containsMatchIn(sourceLine) ||
                pattern.pattern.containsMatchIn(generatedLine)) {
                
                differences.add(Difference(
                    lineNumber = lineNumber,
                    source = sourceLine,
                    generated = generatedLine,
                    type = DifferenceType.WHITELISTED,
                    description = pattern.description,
                    isAcceptable = true
                ))
                return differences
            }
        }
        
        // Check for specific difference types
        if (sourceLine.isBlank() != generatedLine.isBlank()) {
            differences.add(Difference(
                lineNumber = lineNumber,
                source = sourceLine,
                generated = generatedLine,
                type = DifferenceType.WHITESPACE,
                description = "Blank line difference",
                isAcceptable = true
            ))
        } else {
            // Structural difference - needs investigation
            differences.add(Difference(
                lineNumber = lineNumber,
                source = sourceLine,
                generated = generatedLine,
                type = DifferenceType.STRUCTURAL,
                description = "Line content differs",
                isAcceptable = false
            ))
        }
        
        return differences
    }
}

/**
 * A pattern for identifying acceptable differences
 */
data class DifferencePattern(
    val pattern: Regex,
    val description: String,
    val isAcceptable: Boolean
)

/**
 * Type of difference
 */
enum class DifferenceType {
    WHITESPACE,
    COMMENT,
    IDENTIFIER,
    LITERAL,
    WHITELISTED,
    STRUCTURAL,
    SEMANTIC
}

/**
 * A difference between source and generated code
 */
data class Difference(
    val lineNumber: Int,
    val source: String,
    val generated: String,
    val type: DifferenceType,
    val description: String,
    val isAcceptable: Boolean
) {
    fun isWhitelisted(): Boolean = isAcceptable
}
```

### Validateur de Round-Trip

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripValidator.kt

package dev.gfxrs.naga.test.roundtrip

import dev.gfxrs.naga.test.validator.BackendType
import dev.gfxrs.naga.test.validator.BackendValidator

/**
 * Validates round-trip consistency for WGSL.
 */
class RoundTripValidator : BackendValidator {
    
    override val name: String = "Round-Trip Validator"
    override val backendType: BackendType = BackendType.WGSL
    
    private val normalizer: WgslNormalizer = WgslNormalizer()
    private val analyzer: DifferenceAnalyzer = DifferenceAnalyzer()
    
    /**
     * Validate that WGSL code can be parsed and regenerated without loss
     * @param code WGSL source code
     * @return ValidationResult with round-trip information
     */
    override fun validate(
        code: String,
        target: String?,
        stage: ShaderStage?
    ): ValidationResult {
        try {
            // Parse to IR
            val module = parseWgsl(code).getOrThrow()
            
            // Generate WGSL
            val generated = writeWgsl(module)
            
            // Normalize
            val normalizedSource = normalizer.normalize(code)
            val normalizedGenerated = normalizer.normalize(generated)
            
            // Analyze differences
            val differences = analyzer.analyze(
                code, generated, normalizedSource, normalizedGenerated
            )
            
            // Check validity
            val isValid = differences.all { it.isAcceptable }
            
            val output = if (isValid) {
                "Round-trip validation PASSED"
            } else {
                buildString {
                    appendLine("Round-trip validation FAILED")
                    appendLine("Differences:")
                    differences.forEach { diff ->
                        appendLine("  Line ${diff.lineNumber}: ${diff.description}")
                        if (!diff.isAcceptable) {
                            appendLine("    Source:    ${diff.source}")
                            appendLine("    Generated: ${diff.generated}")
                        }
                    }
                }
            }
            
            return ValidationResult(
                success = isValid,
                output = output,
                exitCode = if (isValid) 0 else 1,
                command = "roundtrip"
            )
            
        } catch (e: Exception) {
            return ValidationResult(
                success = false,
                output = "Round-trip validation error: ${e.message}",
                exitCode = 2,
                command = "roundtrip"
            )
        }
    }
    
    companion object {
        fun isAvailable(): Boolean = true // Always available
    }
}
```

---

## Cas de Test

### Tests Unitaires

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripTests.kt

package dev.gfxrs.naga.test.roundtrip

import dev.gfxrs.naga.test.GoldenTestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RoundTripTests : RoundTripTestBase() {
    
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
        "type-inference",
        "control-flow",
        "functions",
        "builtins"
    ])
    @DisplayName("Round-Trip Test: {0}")
    fun `test round-trip`(name: String) {
        assertRoundTripValid(name)
    }
    
    @ParameterizedTest
    @ValueSource(strings = [
        "simple-fragment",
        "simple-vertex",
        "simple-compute",
        "texture-sampling",
        "array-operations",
        "struct-access",
        "function-call",
        "loop-statements",
        "if-statements",
        "switch-statements"
    ])
    @DisplayName("Round-Trip Syntax Test: {0}")
    fun `test round-trip syntax`(name: String) {
        assertRoundTripValid(name)
    }
}
```

### Tests Ciblés

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/TargetedRoundTripTests.kt

package dev.gfxrs.naga.test.roundtrip

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TargetedRoundTripTests : RoundTripTestBase() {
    
    @Test
    @DisplayName("Simple expression round-trip")
    fun testSimpleExpression() {
        val source = """
            fn add(a: i32, b: i32) -> i32 {
                return a + b;
            }
        """.trimIndent()
        
        val result = roundTrip(source).getOrThrow()
        assertEquals(result.normalizedSource, result.normalizedGenerated)
    }
    
    @Test
    @DisplayName("Struct definition round-trip")
    fun testStructDefinition() {
        val source = """
            struct Vertex {
                @location(0) position: vec4<f32>,
                @location(1) color: vec4<f32>,
            }
        """.trimIndent()
        
        val result = roundTrip(source).getOrThrow()
        assertEquals(result.normalizedSource, result.normalizedGenerated)
    }
    
    @Test
    @DisplayName("Entry point round-trip")
    fun testEntryPoint() {
        val source = """
            struct VertexInput {
                @location(0) position: vec4<f32>,
            };
            
            @vertex
            fn main(in: VertexInput) -> @builtin(position) vec4<f32> {
                return in.position;
            }
        """.trimIndent()
        
        val result = roundTrip(source).getOrThrow()
        assertEquals(result.normalizedSource, result.normalizedGenerated)
    }
    
    @Test
    @DisplayName("Type alias round-trip")
    fun testTypeAlias() {
        val source = """
            type MyVector = vec4<f32>;
            type MyMatrix = mat4x4<f32>;
            
            fn transform(v: MyVector, m: MyMatrix) -> MyVector {
                return m * v;
            }
        """.trimIndent()
        
        val result = roundTrip(source).getOrThrow()
        assertEquals(result.normalizedSource, result.normalizedGenerated)
    }
    
    @Test
    @DisplayName("Array operations round-trip")
    fun testArrayOperations() {
        val source = """
            fn arraySum(arr: array<i32, 10>) -> i32 {
                var sum = 0;
                for (var i = 0; i < 10; i = i + 1) {
                    sum = sum + arr[i];
                }
                return sum;
            }
        """.trimIndent()
        
        val result = roundTrip(source).getOrThrow()
        assertEquals(result.normalizedSource, result.normalizedGenerated)
    }
    
    @Test
    @DisplayName("Control flow round-trip")
    fun testControlFlow() {
        val source = """
            fn conditional(x: i32) -> i32 {
                if (x > 0) {
                    return 1;
                } else if (x < 0) {
                    return -1;
                } else {
                    return 0;
                }
            }
        """.trimIndent()
        
        val result = roundTrip(source).getOrThrow()
        assertEquals(result.normalizedSource, result.normalizedGenerated)
    }
}
```

---

## Validation de la Cohérence

### Tests de Cohérence Multi-Backend

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/CrossBackendConsistencyTests.kt

package dev.gfxrs.naga.test.roundtrip

import dev.gfxrs.naga.backends.msl.writeMsl
import dev.gfxrs.naga.backends.hlsl.writeHlsl
import dev.gfxrs.naga.backends.glsl.writeGlsl
import dev.gfxrs.naga.frontends.wgsl.parseWgsl
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Tests that verify consistency across multiple backends.
 * If WGSL → IR → WGSL is valid, then WGSL → IR → MSL/HLSL/GLSL should also be valid.
 */
class CrossBackendConsistencyTests {
    
    @Test
    @DisplayName("WGSL → IR consistency")
    fun testWgslToIrConsistency() {
        val wgsl = """
            @compute @workgroup_size(8, 8, 1)
            fn main() {
                // Simple compute shader
            }
        """.trimIndent()
        
        // Parse to IR
        val module1 = parseWgsl(wgsl).getOrThrow()
        
        // Parse the regenerated WGSL
        val regeneratedWgsl = writeWgsl(module1)
        val module2 = parseWgsl(regeneratedWgsl).getOrThrow()
        
        // Both modules should be equivalent (same structure)
        // This is a simplified check - a full implementation would
        // need to compare the IR structures
        assert(module1.functions.size == module2.functions.size)
        assert(module1.types.size == module2.types.size)
    }
    
    @Test
    @DisplayName("IR → Backends consistency")
    fun testIrToBackendsConsistency() {
        val wgsl = """
            struct VertexInput {
                @location(0) position: vec4<f32>,
            };
            
            @vertex
            fn main(in: VertexInput) -> @builtin(position) vec4<f32> {
                return in.position;
            }
        """.trimIndent()
        
        val module = parseWgsl(wgsl).getOrThrow()
        
        // All backends should be able to generate code
        val msl = writeMsl(module)
        val hlsl = writeHlsl(module)
        val glsl = writeGlsl(module)
        
        // Basic checks
        assert(msl.isNotBlank())
        assert(hlsl.isNotBlank())
        assert(glsl.isNotBlank())
    }
}
```

---

## Gestion des Différences Acceptables

### Liste Blanche des Différences

| Catégorie | Exemple | Raison | Acceptable |
|-----------|---------|--------|------------|
| **Noms générés** | `_expr_123` vs `_expr_456` | Numérotation différente | ✅ Oui |
| **Espaces** | `a+b` vs `a + b` | Formatage différent | ✅ Oui |
| **Commentaires** | Présence/absence | Supprimés par normalisation | ✅ Oui |
| **Littéraux numériques** | `1.0` vs `1.0f` | Format différent | ✅ Oui |
| **Ordre des attributs** | `@location(0) @interpolate` vs `@interpolate @location(0)` | Ordre non significatif | ✅ Oui |
| **Types explicites** | `let x: i32 = 1;` vs `let x = 1;` | Inférence de type | ⚠️ Partiel |
| **Ordre des déclarations** | Ordre des fonctions | Ordre non déterministe | ⚠️ Partiel |
| **Différences structurelles** | `a + b` vs `b + a` | Sémantique différente | ❌ Non |
| **Types différents** | `i32` vs `u32` | Type incorrect | ❌ Non |

### Configuration des Différences Acceptables

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripConfig.kt

package dev.gfxrs.naga.test.roundtrip

/**
 * Configuration for round-trip tests.
 */
object RoundTripConfig {
    
    /**
     * Patterns that are considered acceptable differences.
     */
    val acceptablePatterns: Set<DifferencePattern> = setOf(
        // Generated identifiers
        DifferencePattern(
            pattern = Regex("_expr_\\d+"),
            description = "Generated expression identifier",
            isAcceptable = true
        ),
        DifferencePattern(
            pattern = Regex("_var_\\d+"),
            description = "Generated variable identifier",
            isAcceptable = true
        ),
        DifferencePattern(
            pattern = Regex("_func_\\d+"),
            description = "Generated function identifier",
            isAcceptable = true
        ),
        DifferencePattern(
            pattern = Regex("_type_\\d+"),
            description = "Generated type identifier",
            isAcceptable = true
        ),
        
        // Whitespace
        DifferencePattern(
            pattern = Regex("\\s+"),
            description = "Whitespace difference",
            isAcceptable = true
        ),
        
        // Comments
        DifferencePattern(
            pattern = Regex("//.*?$|/\\*.*?\\*/", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)),
            description = "Comment difference",
            isAcceptable = true
        ),
        
        // Numeric literals
        DifferencePattern(
            pattern = Regex("\\d+\\.\\d+f?"),
            description = "Floating-point literal formatting",
            isAcceptable = true
        ),
        DifferencePattern(
            pattern = Regex("0x[0-9A-Fa-f]+"),
            description = "Hexadecimal literal",
            isAcceptable = true
        ),
        
        // Attribute order
        DifferencePattern(
            pattern = Regex("@[a-z_]+\\([^)]*\\)"),
            description = "Attribute order",
            isAcceptable = true
        )
    )
    
    /**
     * Patterns that indicate real problems.
     */
    val problematicPatterns: Set<DifferencePattern> = setOf(
        // Type differences
        DifferencePattern(
            pattern = Regex("i32|u32|f32|f16"),
            description = "Scalar type difference",
            isAcceptable = false
        ),
        
        // Vector type differences
        DifferencePattern(
            pattern = Regex("vec[2-4]<[iu]?32>"),
            description = "Vector type difference",
            isAcceptable = false
        ),
        
        // Matrix type differences
        DifferencePattern(
            pattern = Regex("mat[2-4]x[2-4]<f32>"),
            description = "Matrix type difference",
            isAcceptable = false
        ),
        
        // Missing/extra statements
        DifferencePattern(
            pattern = Regex("fn|if|for|while|return"),
            description = "Control flow difference",
            isAcceptable = false
        )
    )
    
    /**
     * Check if a difference is acceptable
     */
    fun isAcceptableDifference(difference: String): Boolean {
        return acceptablePatterns.any { pattern ->
            pattern.pattern.containsMatchIn(difference)
        }
    }
    
    /**
     * Check if a difference indicates a problem
     */
    fun isProblematicDifference(difference: String): Boolean {
        return problematicPatterns.any { pattern ->
            pattern.pattern.containsMatchIn(difference)
        }
    }
}
```

---

## Intégration CI

### GitHub Actions

```yaml
# .github/workflows/roundtrip-tests.yml
name: Round-Trip Tests

on: [push, pull_request]

jobs:
  roundtrip:
    runs-on: ubuntu-22.04
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Round-Trip Tests
        run: ./gradlew test --tests "dev.gfxrs.naga.test.roundtrip.*"
      
      - name: Generate Round-Trip Report
        if: failure()
        run: |
          # Generate a report of all failures
          ./gradlew test --tests "dev.gfxrs.naga.test.roundtrip.*" --info 2>&1 | \
          grep -E "(FAILED|Round-trip validation)" > roundtrip-report.txt
          
          # Upload as artifact
          mkdir -p reports
          cp roundtrip-report.txt reports/
      
      - name: Upload Report
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: roundtrip-report
          path: reports/
```

### Génération de Rapport

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripReport.kt

package dev.gfxrs.naga.test.roundtrip

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Generates reports for round-trip test failures.
 */
class RoundTripReport(private val result: RoundTripResult) {
    
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    /**
     * Generate a text report
     */
    fun generateTextReport(): String = buildString {
        appendLine("=" * 80)
        appendLine("ROUND-TRIP TEST REPORT")
        appendLine("=" * 80)
        appendLine()
        appendLine("Timestamp: ${LocalDateTime.now().format(formatter)}")
        appendLine("Status: ${if (result.isValid) "PASS" else "FAIL"}")
        appendLine()
        
        if (result.isValid) {
            appendLine("✓ Round-trip validation passed")
        } else {
            appendLine("✗ Round-trip validation failed")
            appendLine()
            appendLine("Differences:")
            appendLine("-" * 40)
            
            result.differences.forEach { diff ->
                appendLine()
                appendLine("  Line ${diff.lineNumber}: ${diff.description}")
                appendLine("    Type: ${diff.type}")
                appendLine("    Acceptable: ${diff.isAcceptable}")
                
                if (!diff.isAcceptable) {
                    appendLine("    Source:    ${diff.source}")
                    appendLine("    Generated: ${diff.generated}")
                }
            }
        }
        
        appendLine()
        appendLine("=" * 80)
    }
    
    /**
     * Generate an HTML report
     */
    fun generateHtmlReport(): String = buildString {
        appendLine("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Round-Trip Test Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 20px; }
                    .pass { color: green; }
                    .fail { color: red; }
                    .diff { background: #f0f0f0; padding: 10px; margin: 10px 0; border-left: 4px solid ${if (result.isValid) "#4CAF50" else "#F44336"}; }
                    .acceptable { color: #666; }
                    .problematic { color: #F44336; font-weight: bold; }
                    .source { background: #e3f2fd; padding: 5px; }
                    .generated { background: #fff3e0; padding: 5px; }
                    pre { white-space: pre-wrap; word-wrap: break-word; }
                </style>
            </head>
            <body>
                <h1>Round-Trip Test Report</h1>
                <p>Timestamp: ${LocalDateTime.now().format(formatter)}</p>
                <p>Status: <span class="${if (result.isValid) "pass" else "fail"}">${if (result.isValid) "PASS" else "FAIL"}</span></p>
        """.trimIndent())
        
        if (!result.isValid) {
            appendLine("<h2>Differences</h2>")
            
            result.differences.forEach { diff ->
                val cssClass = if (diff.isAcceptable) "acceptable" else "problematic"
                appendLine("<div class=\"diff\">")
                appendLine("  <p>Line <strong>${diff.lineNumber}</strong>: <span class=\"$cssClass\">${diff.description}</span> (${diff.type})</p>")
                
                if (!diff.isAcceptable) {
                    appendLine("  <div class=\"source\">Source: <pre>${escapeHtml(diff.source)}</pre></div>")
                    appendLine("  <div class=\"generated\">Generated: <pre>${escapeHtml(diff.generated)}</pre></div>")
                }
                
                appendLine("</div>")
            }
        }
        
        appendLine("""
            </body>
            </html>
        """.trimIndent())
    }
    
    /**
     * Save report to file
     */
    fun saveToFile(directory: File = File("build/reports/roundtrip")) {
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val reportFile = File(directory, "roundtrip_$timestamp.html")
        reportFile.writeText(generateHtmlReport())
        
        val textReportFile = File(directory, "roundtrip_$timestamp.txt")
        textReportFile.writeText(generateTextReport())
    }
    
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
    
    companion object {
        /**
         * Generate a summary report for multiple tests
         */
        fun generateSummaryReport(results: List<Pair<String, RoundTripResult>>): String = buildString {
            appendLine("=" * 80)
            appendLine("ROUND-TRIP TEST SUMMARY")
            appendLine("=" * 80)
            appendLine()
            
            val passed = results.count { it.second.isValid }
            val failed = results.count { !it.second.isValid }
            
            appendLine("Total: ${results.size}")
            appendLine("Passed: $passed")
            appendLine("Failed: $failed")
            appendLine()
            
            if (failed > 0) {
                appendLine("Failed tests:")
                appendLine("-" * 40)
                
                results.filter { !it.second.isValid }.forEach { (name, _) ->
                    appendLine("  - $name")
                }
            }
            
            appendLine()
            appendLine("=" * 80)
        }
    }
}
```

---

## Metrics et Coverage

### Metrics de Round-Trip

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripMetrics.kt

package dev.gfxrs.naga.test.roundtrip

/**
 * Collects metrics for round-trip tests.
 */
object RoundTripMetrics {
    
    private val results: MutableList<RoundTripResult> = mutableListOf()
    
    /**
     * Record a test result
     */
    fun recordResult(result: RoundTripResult) {
        results.add(result)
    }
    
    /**
     * Get statistics
     */
    fun getStatistics(): RoundTripStatistics {
        val total = results.size
        val passed = results.count { it.isValid }
        val failed = total - passed
        
        val totalDifferences = results.sumOf { it.differences.size }
        val acceptableDifferences = results.sumOf { 
            it.differences.count { diff -> diff.isAcceptable }
        }
        val problematicDifferences = totalDifferences - acceptableDifferences
        
        return RoundTripStatistics(
            totalTests = total,
            passedTests = passed,
            failedTests = failed,
            totalDifferences = totalDifferences,
            acceptableDifferences = acceptableDifferences,
            problematicDifferences = problematicDifferences,
            passRate = if (total > 0) (passed.toDouble() / total * 100) else 0.0
        )
    }
    
    /**
     * Get most common differences
     */
    fun getMostCommonDifferences(limit: Int = 10): List<Pair<String, Int>> {
        return results.flatMap { it.differences }
            .groupBy { it.description }
            .mapValues { (_, diffs) -> diffs.size }
            .toList()
            .sortedByDescending { (_, count) -> count }
            .take(limit)
    }
    
    /**
     * Clear all recorded results
     */
    fun clear() {
        results.clear()
    }
}

/**
 * Statistics for round-trip tests
 */
data class RoundTripStatistics(
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val totalDifferences: Int,
    val acceptableDifferences: Int,
    val problematicDifferences: Int,
    val passRate: Double
)
```

### Intégration avec JUnit

```kotlin
// src/test/kotlin/dev/gfxrs/naga/test/roundtrip/RoundTripTestListener.kt

package dev.gfxrs.naga.test.roundtrip

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier

/**
 * JUnit listener for collecting round-trip metrics.
 */
class RoundTripTestListener : TestExecutionListener {
    
    override fun executionFinished(
        testIdentifier: TestIdentifier,
        testExecutionResult: TestExecutionResult
    ) {
        if (testIdentifier.displayName.contains("Round-Trip")) {
            // This is a simplified version - a full implementation would
            // need to extract the result from the test execution
            
            val passed = testExecutionResult.status == TestExecutionResult.Status.SUCCESSFUL
            
            // Create a mock result for metrics
            val result = RoundTripResult(
                source = "",
                generated = "",
                normalizedSource = "",
                normalizedGenerated = "",
                differences = emptyList(),
                isValid = passed,
                module = Module.empty()
            )
            
            RoundTripMetrics.recordResult(result)
        }
    }
}
```

---

## Résumé

### Checklist d'Implémentation

- [ ] Implémenter `WgslNormalizer` pour la normalisation du code
- [ ] Implémenter `DifferenceAnalyzer` pour l'analyse des différences
- [ ] Créer la classe de base `RoundTripTestBase`
- [ ] Implémenter `RoundTripValidator`
- [ ] Créer les tests unitaires dans `RoundTripTests`
- [ ] Créer les tests ciblés dans `TargetedRoundTripTests`
- [ ] Implémenter `RoundTripReport` pour la génération de rapports
- [ ] Implémenter `RoundTripConfig` pour la configuration
- [ ] Implémenter `RoundTripMetrics` pour la collecte de métriques
- [ ] Intégrer avec GitHub Actions
- [ ] Configurer les listes blanches de différences acceptables

### Fichiers à Créer

```
.plans/05_VALIDATION/
├── 00_golden-files.md          # ✅ Déjà créé
├── 01_native-validators.md      # ✅ Déjà créé
├── 02_roundtrip-tests.md        # ✅ Ce document
└── 99_checklist.md              # À créer

tests/roundtrip/
├── inputs/                      # Fichiers WGSL d'entrée
│   └── *.wgsl                   # Copiés depuis Rust
├── outputs/                     # Fichiers générés (optionnel)
└── reports/                     # Rapports de test

src/test/kotlin/dev/gfxrs/naga/test/roundtrip/
├── RoundTripTestBase.kt
├── RoundTripValidator.kt
├── WgslNormalizer.kt
├── DifferenceAnalyzer.kt
├── RoundTripTests.kt
├── TargetedRoundTripTests.kt
├── CrossBackendConsistencyTests.kt
├── RoundTripReport.kt
├── RoundTripConfig.kt
└── RoundTripMetrics.kt
```

### Commandes Utiles

```bash
# Exécuter tous les tests de round-trip
./gradlew test --tests "dev.gfxrs.naga.test.roundtrip.*"

# Exécuter un test spécifique
./gradlew test --tests "dev.gfxrs.naga.test.roundtrip.RoundTripTests.test round-trip:const-exprs"

# Générer un rapport
./gradlew test --tests "dev.gfxrs.naga.test.roundtrip.*" --info 2>&1 | grep -E "(FAILED|PASSED|Round-trip)"
```

---

## Références

- [Naga WGSL Parser](https://github.com/gfx-rs/naga/blob/trunk/src/frontends/wgsl/mod.rs)
- [Naga WGSL Writer](https://github.com/gfx-rs/naga/blob/trunk/src/backends/wgsl/mod.rs)
- [WGSL Specification](https://gpuweb.github.io/gpuweb/wgsl/)
- [Golden Master Testing](https://martinfowler.com/bliki/GoldenMaster.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Kotlin Testing](https://kotlinlang.org/docs/testing.html)
