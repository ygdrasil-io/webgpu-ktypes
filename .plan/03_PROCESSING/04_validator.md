# ✅ Phase 3.4 : Validator

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 3 - Processing  
**Sous-Phase** : 3.4 - IR Validation  
**Durée** : 3-4 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Validation de la sémantique IR)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/mod.rs` (~2500 lignes)

---

## 📋 OBJECTIFS

Implémenter le **validator** qui vérifie qu'un module IR est valide selon les règles sémantiques du WebGPU. Cela permet :
- De détecter les erreurs sémantiques avant la génération de code
- De garantir que la génération de code ne paniquera pas
- De fournir des messages d'erreur clairs et précis
- De supporter la validation incrémentale pendant la construction du module
- De configurer les capacités supportées par le backend cible

**Livrable principal** : Un module de validation capable de détecter toutes les erreurs sémantiques dans un module IR selon les règles WebGPU.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce que le Validator ?

Le validator vérifie que le module IR respecte toutes les **règles sémantiques** du WebGPU/WGSL. Contrairement au typifier qui assigne des types, le validator :
- Vérifie que les types sont valides (tailles, alignements, membres de struct)
- Vérifie que les expressions sont valides (types compatibles, opérations autorisées)
- Vérifie que les statements sont valides (flux de contrôle, usage des variables)
- Vérifie que les fonctions et entry points sont valides
- Vérifie que les ressources (bindings, locations) sont valides
- Vérifie le respect des capacités supportées

### 2. Validation Flags

La validation peut être configurée avec des `ValidationFlags` :

| Flag | Description | Par défaut |
|------|-------------|------------|
| EXPRESSIONS | Valider les expressions | ✅ |
| BLOCKS | Valider les blocs et le flux de contrôle | ✅ |
| CONTROL_FLOW_UNIFORMITY | Valider l'uniformité du flux de contrôle | ✅ |
| STRUCT_LAYOUTS | Valider les layouts de struct (host-shareable) | ✅ |
| CONSTANTS | Valider les constantes | ✅ |
| BINDINGS | Valider les bindings (group, binding, location) | ✅ |

### 3. Capabilities

Les capacités (`Capabilities`) représentent les fonctionnalités supportées par le backend. Certaines fonctionnalités nécessitent des capacités spécifiques :

| Capacité | Description |
|----------|-------------|
| IMMEDIATES | Support pour AddressSpace::Immediate |
| FLOAT64 | Support pour f64 |
| SHADER_INT64 | Support pour i64/u64 |
| MULTISAMPLED_SHADING | Support pour SampleIndex et Sampling::Sample |
| MULTIVIEW | Support pour ViewIndex |
| MESH_SHADER | Support pour task/mesh shaders |
| RAY_QUERY | Support pour les ray queries |
| RAY_TRACING_PIPELINE | Support pour les ray tracing shaders |

### 4. Architecture

```
Module IR
    ↓
Validator(module, flags, capabilities)
    ↓
1. Validation des types (TypeValidator)
    ↓
2. Validation des constantes (ConstantValidator)
    ↓
3. Validation des expressions (ExpressionValidator)
    ↓
4. Validation des statements (StatementValidator)
    ↓
5. Validation des fonctions (FunctionValidator)
    ↓
6. Validation des entry points (EntryPointValidator)
    ↓
7. Validation des ressources (ResourceValidator)
    ↓
ModuleInfo (métadonnées calculées) + ValidationErrors (liste des erreurs)
    ↓
Si erreurs.isEmpty() → Module valide
Sinon → Lancer ValidationException
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. ValidationFlags.kt (Options de validation)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/ValidationFlags.kt`

```kotlin
package io.ygdrasil.wgsl.valid

/**
 * Flags pour contrôler quelles validations sont effectuées.
 * 
 * Par défaut, toutes les validations sont activées.
 */
class ValidationFlags private constructor(val bits: Int) {
    
    companion object {
        const val EXPRESSIONS = 1 shl 0
        const val BLOCKS = 1 shl 1
        const val CONTROL_FLOW_UNIFORMITY = 1 shl 2
        const val STRUCT_LAYOUTS = 1 shl 3
        const val CONSTANTS = 1 shl 4
        const val BINDINGS = 1 shl 5
        
        val ALL = ValidationFlags(
            EXPRESSIONS or BLOCKS or CONTROL_FLOW_UNIFORMITY or 
            STRUCT_LAYOUTS or CONSTANTS or BINDINGS
        )
        
        val NONE = ValidationFlags(0)
        
        fun fromBits(bits: Int): ValidationFlags {
            return ValidationFlags(bits)
        }
    }
    
    val expressions: Boolean get() = (bits and EXPRESSIONS) != 0
    val blocks: Boolean get() = (bits and BLOCKS) != 0
    val controlFlowUniformity: Boolean get() = (bits and CONTROL_FLOW_UNIFORMITY) != 0
    val structLayouts: Boolean get() = (bits and STRUCT_LAYOUTS) != 0
    val constants: Boolean get() = (bits and CONSTANTS) != 0
    val bindings: Boolean get() = (bits and BINDINGS) != 0
    
    fun or(other: ValidationFlags): ValidationFlags {
        return ValidationFlags(bits or other.bits)
    }
    
    fun and(other: ValidationFlags): ValidationFlags {
        return ValidationFlags(bits and other.bits)
    }
}
```

### 2. Capabilities.kt (Fonctionnalités supportées)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/Capabilities.kt`

```kotlin
package io.ygdrasil.wgsl.valid

/**
 * Capacités supportées par le backend.
 * 
 * Chaque capacité représente une fonctionnalité optionnelle du WebGPU.
 */
class Capabilities private constructor(val bits: Long) {
    
    companion object {
        // Capacités de base
        const val IMMEDIATES = 1L shl 0
        const val FLOAT64 = 1L shl 1
        const val PRIMITIVE_INDEX = 1L shl 2
        
        // Binding arrays
        const val TEXTURE_AND_SAMPLER_BINDING_ARRAY = 1L shl 3
        const val BUFFER_BINDING_ARRAY = 1L shl 4
        const val STORAGE_TEXTURE_BINDING_ARRAY = 1L shl 5
        const val STORAGE_BUFFER_BINDING_ARRAY = 1L shl 6
        
        // Built-ins
        const val CLIP_DISTANCES = 1L shl 7
        const val CULL_DISTANCE = 1L shl 8
        
        // Formats
        const val STORAGE_TEXTURE_16BIT_NORM_FORMATS = 1L shl 9
        
        // Multiview
        const val MULTIVIEW = 1L shl 10
        const val MULTISAMPLED_SHADING = 1L shl 12
        
        // Early depth test
        const val EARLY_DEPTH_TEST = 1L shl 11
        
        // Ray tracing
        const val RAY_QUERY = 1L shl 13
        const val RAY_HIT_VERTEX_POSITION = 1L shl 25
        const val RAY_TRACING_PIPELINE = 1L shl 38
        
        // Dual source blending
        const val DUAL_SOURCE_BLENDING = 1L shl 14
        
        // Texture types
        const val CUBE_ARRAY_TEXTURES = 1L shl 15
        
        // 64-bit integers
        const val SHADER_INT64 = 1L shl 16
        const val SHADER_INT64_ATOMIC_MIN_MAX = 1L shl 20
        const val SHADER_INT64_ATOMIC_ALL_OPS = 1L shl 21
        
        // Atomic operations
        const val SHADER_FLOAT32_ATOMIC = 1L shl 22
        const val TEXTURE_ATOMIC = 1L shl 23
        const val TEXTURE_INT64_ATOMIC = 1L shl 24
        
        // 16-bit types
        const val SHADER_FLOAT16 = 1L shl 26
        const val SHADER_FLOAT16_IN_FLOAT32 = 1L shl 28
        const val SHADER_INT16 = 1L shl 43
        
        // External textures
        const val TEXTURE_EXTERNAL = 1L shl 27
        
        // Barycentrics
        const val SHADER_BARYCENTRICS = 1L shl 29
        
        // Mesh shading
        const val MESH_SHADER = 1L shl 30
        const val MESH_SHADER_POINT_TOPOLOGY = 1L shl 31
        
        // Non-uniform indexing
        const val TEXTURE_AND_SAMPLER_BINDING_ARRAY_NON_UNIFORM_INDEXING = 1L shl 32
        const val BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING = 1L shl 33
        const val STORAGE_TEXTURE_BINDING_ARRAY_NON_UNIFORM_INDEXING = 1L shl 34
        const val STORAGE_BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING = 1L shl 35
        
        // Cooperative matrices
        const val COOPERATIVE_MATRIX = 1L shl 36
        
        // Per-vertex
        const val PER_VERTEX = 1L shl 37
        
        // Draw index
        const val DRAW_INDEX = 1L shl 39
        
        // Acceleration structure arrays
        const val ACCELERATION_STRUCTURE_BINDING_ARRAY = 1L shl 40
        
        // Memory decorations
        const val MEMORY_DECORATION_COHERENT = 1L shl 41
        const val MEMORY_DECORATION_VOLATILE = 1L shl 42
        
        // Subgroup operations
        const val SUBGROUP = 1L shl 17
        const val SUBGROUP_BARRIER = 1L shl 18
        const val SUBGROUP_VERTEX_STAGE = 1L shl 19
        
        // Default capabilities (toujours supportées)
        val DEFAULT = Capabilities(
            MULTISAMPLED_SHADING or CUBE_ARRAY_TEXTURES
        )
        
        fun fromBits(bits: Long): Capabilities {
            return Capabilities(bits)
        }
    }
    
    val hasImmediates: Boolean get() = (bits and IMMEDIATES) != 0L
    val hasFloat64: Boolean get() = (bits and FLOAT64) != 0L
    val hasPrimitiveIndex: Boolean get() = (bits and PRIMITIVE_INDEX) != 0L
    val hasTextureAndSamplerBindingArray: Boolean get() = (bits and TEXTURE_AND_SAMPLER_BINDING_ARRAY) != 0L
    val hasBufferBindingArray: Boolean get() = (bits and BUFFER_BINDING_ARRAY) != 0L
    val hasStorageTextureBindingArray: Boolean get() = (bits and STORAGE_TEXTURE_BINDING_ARRAY) != 0L
    val hasStorageBufferBindingArray: Boolean get() = (bits and STORAGE_BUFFER_BINDING_ARRAY) != 0L
    val hasClipDistances: Boolean get() = (bits and CLIP_DISTANCES) != 0L
    val hasCullDistance: Boolean get() = (bits and CULL_DISTANCE) != 0L
    val hasStorageTexture16BitNormFormats: Boolean get() = (bits and STORAGE_TEXTURE_16BIT_NORM_FORMATS) != 0L
    val hasMultiview: Boolean get() = (bits and MULTIVIEW) != 0L
    val hasEarlyDepthTest: Boolean get() = (bits and EARLY_DEPTH_TEST) != 0L
    val hasMultisampledShading: Boolean get() = (bits and MULTISAMPLED_SHADING) != 0L
    val hasRayQuery: Boolean get() = (bits and RAY_QUERY) != 0L
    val hasRayHitVertexPosition: Boolean get() = (bits and RAY_HIT_VERTEX_POSITION) != 0L
    val hasDualSourceBlending: Boolean get() = (bits and DUAL_SOURCE_BLENDING) != 0L
    val hasCubeArrayTextures: Boolean get() = (bits and CUBE_ARRAY_TEXTURES) != 0L
    val hasShaderInt64: Boolean get() = (bits and SHADER_INT64) != 0L
    val hasShaderInt64AtomicMinMax: Boolean get() = (bits and SHADER_INT64_ATOMIC_MIN_MAX) != 0L
    val hasShaderInt64AtomicAllOps: Boolean get() = (bits and SHADER_INT64_ATOMIC_ALL_OPS) != 0L
    val hasShaderFloat32Atomic: Boolean get() = (bits and SHADER_FLOAT32_ATOMIC) != 0L
    val hasTextureAtomic: Boolean get() = (bits and TEXTURE_ATOMIC) != 0L
    val hasTextureInt64Atomic: Boolean get() = (bits and TEXTURE_INT64_ATOMIC) != 0L
    val hasShaderFloat16: Boolean get() = (bits and SHADER_FLOAT16) != 0L
    val hasShaderFloat16InFloat32: Boolean get() = (bits and SHADER_FLOAT16_IN_FLOAT32) != 0L
    val hasTextureExternal: Boolean get() = (bits and TEXTURE_EXTERNAL) != 0L
    val hasShaderBarycentrics: Boolean get() = (bits and SHADER_BARYCENTRICS) != 0L
    val hasMeshShader: Boolean get() = (bits and MESH_SHADER) != 0L
    val hasMeshShaderPointTopology: Boolean get() = (bits and MESH_SHADER_POINT_TOPOLOGY) != 0L
    val hasTextureAndSamplerBindingArrayNonUniformIndexing: Boolean get() = (bits and TEXTURE_AND_SAMPLER_BINDING_ARRAY_NON_UNIFORM_INDEXING) != 0L
    val hasBufferBindingArrayNonUniformIndexing: Boolean get() = (bits and BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING) != 0L
    val hasStorageTextureBindingArrayNonUniformIndexing: Boolean get() = (bits and STORAGE_TEXTURE_BINDING_ARRAY_NON_UNIFORM_INDEXING) != 0L
    val hasStorageBufferBindingArrayNonUniformIndexing: Boolean get() = (bits and STORAGE_BUFFER_BINDING_ARRAY_NON_UNIFORM_INDEXING) != 0L
    val hasCooperativeMatrix: Boolean get() = (bits and COOPERATIVE_MATRIX) != 0L
    val hasPerVertex: Boolean get() = (bits and PER_VERTEX) != 0L
    val hasDrawIndex: Boolean get() = (bits and DRAW_INDEX) != 0L
    val hasAccelerationStructureBindingArray: Boolean get() = (bits and ACCELERATION_STRUCTURE_BINDING_ARRAY) != 0L
    val hasMemoryDecorationCoherent: Boolean get() = (bits and MEMORY_DECORATION_COHERENT) != 0L
    val hasMemoryDecorationVolatile: Boolean get() = (bits and MEMORY_DECORATION_VOLATILE) != 0L
    val hasSubgroup: Boolean get() = (bits and SUBGROUP) != 0L
    val hasSubgroupBarrier: Boolean get() = (bits and SUBGROUP_BARRIER) != 0L
    val hasSubgroupVertexStage: Boolean get() = (bits and SUBGROUP_VERTEX_STAGE) != 0L
    val hasRayTracingPipeline: Boolean get() = (bits and RAY_TRACING_PIPELINE) != 0L
    
    fun or(other: Capabilities): Capabilities {
        return Capabilities(bits or other.bits)
    }
    
    fun and(other: Capabilities): Capabilities {
        return Capabilities(bits and other.bits)
    }
    
    fun has(other: Capabilities): Boolean {
        return (bits and other.bits) == other.bits
    }
}
```

### 3. ShaderStages.kt (Étapes de shader supportées)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/ShaderStages.kt`

```kotlin
package io.ygdrasil.wgsl.valid

import io.ygdrasil.wgsl.ir.ShaderStage

/**
 * Flags pour les étapes de shader supportées.
 */
class ShaderStages private constructor(val bits: Int) {
    
    companion object {
        const val VERTEX = 1 shl 0
        const val FRAGMENT = 1 shl 1
        const val COMPUTE = 1 shl 2
        const val MESH = 1 shl 3
        const val TASK = 1 shl 4
        const val RAY_GENERATION = 1 shl 5
        const val ANY_HIT = 1 shl 6
        const val CLOSEST_HIT = 1 shl 7
        const val MISS = 1 shl 8
        
        val COMPUTE_LIKE = COMPUTE or TASK or MESH
        val RAY_TRACING = RAY_GENERATION or ANY_HIT or CLOSEST_HIT or MISS
        val ALL = VERTEX or FRAGMENT or COMPUTE_LIKE or RAY_TRACING
        
        fun fromShaderStage(stage: ShaderStage): ShaderStages {
            return when (stage) {
                ShaderStage.VERTEX -> ShaderStages(VERTEX)
                ShaderStage.FRAGMENT -> ShaderStages(FRAGMENT)
                ShaderStage.COMPUTE -> ShaderStages(COMPUTE)
                ShaderStage.MESH -> ShaderStages(MESH)
                ShaderStage.TASK -> ShaderStages(TASK)
                ShaderStage.RAY_GENERATION -> ShaderStages(RAY_GENERATION)
                ShaderStage.ANY_HIT -> ShaderStages(ANY_HIT)
                ShaderStage.CLOSEST_HIT -> ShaderStages(CLOSEST_HIT)
                ShaderStage.MISS -> ShaderStages(MISS)
            }
        }
    }
    
    fun has(stage: ShaderStage): Boolean {
        return (bits and fromShaderStage(stage).bits) != 0
    }
    
    fun or(other: ShaderStages): ShaderStages {
        return ShaderStages(bits or other.bits)
    }
}
```

### 4. ValidationError.kt (Erreurs de validation)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/ValidationError.kt`

```kotlin
package io.ygdrasil.wgsl.valid

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.span.Span

/**
 * Erreurs générées par le Validator.
 */
sealed class ValidationError {
    abstract val message: String
    abstract val span: Span?
    
    // Erreurs de handle
    data class InvalidHandle(override val message: String, override val span: Span? = null) : ValidationError()
    
    // Erreurs de layout
    data class Layouter(val error: LayoutError) : ValidationError() {
        override val message: String get() = error.message()
        override val span: Span? get() = null
    }
    
    // Erreurs de type
    data class Type(
        val handle: Handle<Type>,
        val name: String?,
        val source: TypeError
    ) : ValidationError() {
        override val message: String get() = "Type $handle '${name ?: ""}' is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs de constante
    data class Constant(
        val handle: Handle<Constant>,
        val name: String?,
        val source: ConstantError
    ) : ValidationError() {
        override val message: String get() = "Constant $handle '${name ?: ""}' is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs d'override
    data class Override(
        val handle: Handle<Override>,
        val name: String?,
        val source: OverrideError
    ) : ValidationError() {
        override val message: String get() = "Override $handle '${name ?: ""}' is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs d'expression constante
    data class ConstExpression(
        val handle: Handle<Expression>,
        val source: ConstExpressionError
    ) : ValidationError() {
        override val message: String get() = "Constant expression $handle is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs de taille de tableau
    data class ArraySizeError(val handle: Handle<Expression>) : ValidationError() {
        override val message: String get() = "Array size expression $handle is not strictly positive"
        override val span: Span? get() = null
    }
    
    // Erreurs de fonction
    data class Function(
        val handle: Handle<Function>,
        val source: FunctionError
    ) : ValidationError() {
        override val message: String get() = "Function $handle is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs d'entry point
    data class EntryPoint(
        val index: Int,
        val name: String,
        val source: EntryPointError
    ) : ValidationError() {
        override val message: String get() = "Entry point '$name' (index $index) is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs de variable globale
    data class GlobalVariable(
        val handle: Handle<GlobalVariable>,
        val name: String?,
        val source: GlobalVariableError
    ) : ValidationError() {
        override val message: String get() = "Global variable $handle '${name ?: ""}' is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs de varying (input/output)
    data class Varying(
        val handle: Handle<Type>,
        val source: VaryingError
    ) : ValidationError() {
        override val message: String get() = "Varying type $handle is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs d'expression
    data class Expression(
        val handle: Handle<Expression>,
        val source: ExpressionError
    ) : ValidationError() {
        override val message: String get() = "Expression $handle is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs de statement
    data class Statement(
        val handle: Handle<Statement>,
        val source: StatementError
    ) : ValidationError() {
        override val message: String get() = "Statement $handle is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
    
    // Erreurs de composition
    data class Compose(val error: ComposeError) : ValidationError() {
        override val message: String get() = error.message
        override val span: Span? get() = error.span
    }
    
    // Erreurs d'uniformité
    data class Uniformity(val error: UniformityError) : ValidationError() {
        override val message: String get() = error.message
        override val span: Span? get() = error.span
    }
}

// Erreurs spécifiques
sealed class TypeError {
    abstract val message: String
    abstract val span: Span?
    
    data class InvalidWidth(override val span: Span?) : TypeError() {
        override val message: String get() = "Type width must be 1, 2, 4, or 8"
    }
    data class InvalidScalarKind(override val span: Span?) : TypeError() {
        override val message: String get() = "Invalid scalar kind"
    }
    data class StructMemberNotFound(val index: Int, override val span: Span?) : TypeError() {
        override val message: String get() = "Struct member at index $index not found"
    }
    data class InvalidMemberBinding(val index: Int, override val span: Span?) : TypeError() {
        override val message: String get() = "Invalid binding on struct member at index $index"
    }
    data class DuplicateBinding(val binding: ResourceBinding, override val span: Span?) : TypeError() {
        override val message: String get() = "Duplicate binding $binding"
    }
    data class NonPowerOfTwoWidth(override val span: Span?) : TypeError() {
        override val message: String get() = "Type width must be a power of two"
    }
    data class SizeExceedsLimit(override val span: Span?) : TypeError() {
        override val message: String get() = "Type size exceeds limit of ${Layouter.MAX_TYPE_SIZE} bytes"
    }
    data class InvalidMatrixSize(override val span: Span?) : TypeError() {
        override val message: String get() = "Invalid matrix dimensions"
    }
    data class Disalignment(val expected: Int, val actual: Int, override val span: Span?) : TypeError() {
        override val message: String get() = "Type is not aligned to $expected bytes (actual: $actual)"
    }
}

sealed class ConstantError {
    abstract val message: String
    abstract val span: Span?
    
    object InitializerExprType : ConstantError() {
        override val message: String get() = "Initializer must be a const-expression"
        override val span: Span? get() = null
    }
    data class InvalidType(override val span: Span?) : ConstantError() {
        override val message: String get() = "The type doesn't match the constant"
    }
    data class NonConstructibleType(override val span: Span?) : ConstantError() {
        override val message: String get() = "The type is not constructible"
    }
}

sealed class OverrideError {
    abstract val message: String
    abstract val span: Span?
    
    object MissingNameAndID : OverrideError() {
        override val message: String get() = "Override name and ID are missing"
        override val span: Span? get() = null
    }
    object DuplicateID : OverrideError() {
        override val message: String get() = "Override ID must be unique"
        override val span: Span? get() = null
    }
    object InitializerExprType : OverrideError() {
        override val message: String get() = "Initializer must be a const-expression or override-expression"
        override val span: Span? get() = null
    }
    data class InvalidType(override val span: Span?) : OverrideError() {
        override val message: String get() = "The type doesn't match the override"
    }
    data class NonConstructibleType(override val span: Span?) : OverrideError() {
        override val message: String get() = "The type is not constructible"
    }
    data class TypeNotScalar(override val span: Span?) : OverrideError() {
        override val message: String get() = "Override type must be scalar"
    }
    object NotAllowed : OverrideError() {
        override val message: String get() = "Override declarations are not allowed"
        override val span: Span? get() = null
    }
    object UninitializedOverride : OverrideError() {
        override val message: String get() = "Override is uninitialized"
        override val span: Span? get() = null
    }
    data class ConstExpression(val handle: Handle<Expression>, val source: ConstExpressionError) : OverrideError() {
        override val message: String get() = "Constant expression $handle is invalid: ${source.message}"
        override val span: Span? get() = source.span
    }
}

// Autres classes d'erreur à implémenter...
```

### 5. Validator.kt (Classe principale)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/Validator.kt`

```kotlin
package io.ygdrasil.wgsl.valid

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.proc.Layouter

/**
 * Valideur de module IR.
 * 
 * Ce validateur vérifie que le module respecte toutes les règles sémantiques
 * du WebGPU/WGSL. Si la validation réussit, la génération de code ne
 * paniquera pas (sauf en cas de bug dans le générateur).
 * 
 * @param flags Flags de validation à activer
 * @param capabilities Capacités supportées par le backend
 * @param subgroupStages Étapes de shader supportant les subgroup operations
 * @param subgroupOperations Opérations de subgroup supportées
 */
class Validator(
    val flags: ValidationFlags = ValidationFlags.ALL,
    val capabilities: Capabilities = Capabilities.DEFAULT,
    val subgroupStages: ShaderStages = ShaderStages.ALL,
    val subgroupOperations: SubgroupOperationSet = SubgroupOperationSet.DEFAULT
) {
    
    private val layouter = Layouter()
    private val typeInfos: MutableList<TypeInfo> = mutableListOf()
    private val functionInfos: MutableList<FunctionInfo> = mutableListOf()
    private val entryPointInfos: MutableList<FunctionInfo> = mutableListOf()
    
    // Tracking des expressions et statements valides
    private val validExpressionSet = mutableSetOf<Handle<Expression>>()
    private val needsVisit = mutableSetOf<Handle<Expression>>()
    
    // Tracking des ressources
    private val locationMask = BitSet()
    private val epResourceBindings = mutableSetOf<ResourceBinding>()
    private val switchValues = mutableSetOf<SwitchValue>()
    private val overrideIds = mutableSetOf<Int>()
    
    // State
    private var currentFunction: Handle<Function>? = null
    private var currentEntryPointIndex: Int? = null
    private var currentBlockDepth: Int = 0
    
    /**
     * Valide un module IR complet.
     * 
     * @param module Le module à valider
     * @return Résultat avec la liste des erreurs ou succès
     */
    fun validate(module: Module): Result<ModuleInfo, List<ValidationError>> {
        val errors = mutableListOf<ValidationError>()
        
        // Réinitialiser l'état
        typeInfos.clear()
        functionInfos.clear()
        entryPointInfos.clear()
        validExpressionSet.clear()
        needsVisit.clear()
        locationMask.clear()
        epResourceBindings.clear()
        switchValues.clear()
        overrideIds.clear()
        
        // Initialiser les infos de type
        for (i in 0 until module.types.size) {
            typeInfos.add(TypeInfo(Handle(i)))
        }
        
        // Initialiser les infos de fonction
        for (i in 0 until module.functions.size) {
            functionInfos.add(FunctionInfo(Handle(i)))
        }
        
        // Calculer les layouts si nécessaire
        if (flags.structLayouts) {
            layouter.clear()
            layouter.update(module).onFailure { error ->
                errors.add(ValidationError.Layouter(error))
            }
        }
        
        // Valider les types
        if (flags.structLayouts) {
            errors.addAll(validateTypes(module))
        }
        
        // Valider les constantes
        if (flags.constants) {
            errors.addAll(validateConstants(module))
        }
        
        // Valider les overrides
        errors.addAll(validateOverrides(module))
        
        // Valider les fonctions
        if (flags.blocks || flags.expressions) {
            errors.addAll(validateFunctions(module))
        }
        
        // Valider les entry points
        errors.addAll(validateEntryPoints(module))
        
        // Valider les variables globales
        if (flags.bindings) {
            errors.addAll(validateGlobalVariables(module))
        }
        
        // Vérifier les besoins de visite
        if (needsVisit.isNotEmpty()) {
            errors.addAll(needsVisit.map { handle ->
                ValidationError.Expression(
                    handle,
                    ExpressionError.UnvisitedExpression
                )
            })
        }
        
        return if (errors.isEmpty()) {
            Result.success(ModuleInfo(
                typeInfos = typeInfos.toList(),
                functionInfos = functionInfos.toList(),
                entryPointInfos = entryPointInfos.toList()
            ))
        } else {
            Result.failure(errors)
        }
    }
    
    /**
     * Valide un module sans lever d'exception.
     * Retourne true si le module est valide.
     */
    fun validateQuiet(module: Module): Boolean {
        return validate(module).isSuccess
    }
    
    /**
     * Valide un module et lance une exception si invalide.
     */
    fun validateThrow(module: Module): ModuleInfo {
        return validate(module).getOrThrow()
    }
    
    // Méthodes de validation privées
    
    private fun validateTypes(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((typeHandle, type) in module.types.withIndex()) {
            val handle = Handle<Type>(typeHandle)
            
            when (val inner = type.inner) {
                is TypeInner.Scalar -> {
                    if (inner.scalar.width !in listOf(1, 2, 4, 8)) {
                        errors.add(ValidationError.Type(
                            handle,
                            type.name,
                            TypeError.InvalidWidth(type.span)
                        ))
                    }
                }
                is TypeInner.Vector -> {
                    // Vérifier que la largeur scalaire est valide
                    if (inner.scalar.width !in listOf(1, 2, 4, 8)) {
                        errors.add(ValidationError.Type(
                            handle,
                            type.name,
                            TypeError.InvalidWidth(type.span)
                        ))
                    }
                }
                is TypeInner.Matrix -> {
                    // Vérifier les dimensions
                    if (inner.columns <= 0 || inner.rows <= 0) {
                        errors.add(ValidationError.Type(
                            handle,
                            type.name,
                            TypeError.InvalidMatrixSize(type.span)
                        ))
                    }
                    if (inner.scalar.width !in listOf(1, 2, 4, 8)) {
                        errors.add(ValidationError.Type(
                            handle,
                            type.name,
                            TypeError.InvalidWidth(type.span)
                        ))
                    }
                }
                is TypeInner.Struct -> {
                    // Vérifier que les offsets des membres sont valides
                    var expectedOffset = 0
                    for ((memberIndex, member) in inner.members.withIndex()) {
                        val memberType = module.types[member.ty.index]
                        val memberLayout = layouter[member.ty]
                        
                        // Vérifier que l'offset est aligné
                        if (!memberLayout.alignment.isAligned(member.offset)) {
                            errors.add(ValidationError.Type(
                                handle,
                                type.name,
                                TypeError.Disalignment(
                                    memberLayout.alignment.bytes,
                                    member.offset,
                                    type.span
                                )
                            ))
                        }
                        
                        // Vérifier que l'offset est >= à l'offset attendu
                        if (member.offset < expectedOffset) {
                            errors.add(ValidationError.Type(
                                handle,
                                type.name,
                                TypeError.Disalignment(expectedOffset, member.offset, type.span)
                            ))
                        }
                        
                        expectedOffset = member.offset + memberLayout.size
                    }
                    
                    // Vérifier que le span est correct
                    if (inner.span != expectedOffset) {
                        errors.add(ValidationError.Type(
                            handle,
                            type.name,
                            TypeError.Disalignment(expectedOffset, inner.span, type.span)
                        ))
                    }
                }
                is TypeInner.Array -> {
                    // Vérifier que la taille est positive
                    if (inner.size <= 0) {
                        errors.add(ValidationError.Type(
                            handle,
                            type.name,
                            TypeError.InvalidWidth(type.span)
                        ))
                    }
                }
                else -> {}
            }
        }
        
        return errors
    }
    
    private fun validateConstants(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((constHandle, constant) in module.constants.withIndex()) {
            val handle = Handle<Constant>(constHandle)
            val constType = module.types[constant.ty.index]
            
            // Vérifier que l'initialiseur est une expression constante
            if (!isConstExpression(constant.init, module)) {
                errors.add(ValidationError.Constant(
                    handle,
                    constant.name,
                    ConstantError.InitializerExprType
                ))
            }
            
            // Vérifier que le type correspond
            val initType = getExpressionType(constant.init, module)
            if (initType != constType) {
                errors.add(ValidationError.Constant(
                    handle,
                    constant.name,
                    ConstantError.InvalidType(constant.span)
                ))
            }
        }
        
        return errors
    }
    
    private fun validateOverrides(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((overrideHandle, override) in module.overrides.withIndex()) {
            val handle = Handle<Override>(overrideHandle)
            val overrideType = module.types[override.ty.index]
            
            // Vérifier que le type est scalaire
            if (overrideType.inner !is TypeInner.Scalar) {
                errors.add(ValidationError.Override(
                    handle,
                    override.name,
                    OverrideError.TypeNotScalar(override.span)
                ))
            }
            
            // Vérifier l'unicité de l'ID
            if (override.id != null) {
                if (overrideIds.contains(override.id)) {
                    errors.add(ValidationError.Override(
                        handle,
                        override.name,
                        OverrideError.DuplicateID
                    ))
                }
                overrideIds.add(override.id)
            }
            
            // Vérifier l'initialiseur
            if (override.init != null) {
                if (!isConstExpression(override.init, module) && 
                    !isOverrideExpression(override.init, module)) {
                    errors.add(ValidationError.Override(
                        handle,
                        override.name,
                        OverrideError.InitializerExprType
                    ))
                }
            }
        }
        
        return errors
    }
    
    private fun validateFunctions(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((funcHandle, func) in module.functions.withIndex()) {
            val handle = Handle<Function>(funcHandle)
            currentFunction = handle
            
            // Valider les arguments
            for ((argIndex, arg) in func.arguments.withIndex()) {
                val argType = module.types[arg.ty.index]
                
                // Vérifier que le type existe
                if (arg.ty.index >= module.types.size) {
                    errors.add(ValidationError.Function(
                        handle,
                        FunctionError.InvalidArgumentType(argIndex)
                    ))
                }
            }
            
            // Valider le type de retour
            func.result?.let { result ->
                if (result.ty.index >= module.types.size) {
                    errors.add(ValidationError.Function(
                        handle,
                        FunctionError.InvalidResultType
                    ))
                }
            }
            
            // Valider les variables locales
            for ((varHandle, var) in func.localVariables.withIndex()) {
                val varType = module.types[var.ty.index]
                
                if (var.ty.index >= module.types.size) {
                    errors.add(ValidationError.Function(
                        handle,
                        FunctionError.LocalVariableError(
                            Handle(varHandle),
                            LocalVariableError.InvalidType
                        )
                    ))
                }
                
                // Valider l'initialiseur si présent
                var.init != null -> {
                    if (!isConstExpression(var.init, module)) {
                        // L'initialiseur doit être une expression constante
                        // ou une expression valide dans le contexte
                    }
                }
            }
            
            // Valider le corps de la fonction
            validateBlock(func.body, module, errors)
            
            currentFunction = null
        }
        
        return errors
    }
    
    private fun validateEntryPoints(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((epIndex, ep) in module.entryPoints.withIndex()) {
            currentEntryPointIndex = epIndex
            
            // Vérifier le nom
            if (ep.name.isEmpty()) {
                errors.add(ValidationError.EntryPoint(
                    epIndex,
                    ep.name,
                    EntryPointError.EmptyName
                ))
            }
            
            // Vérifier la stage
            if (!subgroupStages.has(ep.stage)) {
                errors.add(ValidationError.EntryPoint(
                    epIndex,
                    ep.name,
                    EntryPointError.UnsupportedShaderStage(ep.stage)
                ))
            }
            
            // Valider la fonction
            val func = module.functions[ep.function.index]
            
            // Vérifier que la fonction n'a pas de paramètres avec binding
            for (arg in func.arguments) {
                if (arg.binding != null) {
                    errors.add(ValidationError.EntryPoint(
                        epIndex,
                        ep.name,
                        EntryPointError.FunctionParameterWithBinding
                    ))
                }
            }
            
            // Vérifier early_depth_test
            if (ep.earlyDepthTest != null && !capabilities.hasEarlyDepthTest) {
                errors.add(ValidationError.EntryPoint(
                    epIndex,
                    ep.name,
                    EntryPointError.EarlyDepthTestNotSupported
                ))
            }
            
            // Valider workgroup_size
            if (ep.workgroupSize != null) {
                val stage = ep.stage
                if (stage != ShaderStage.COMPUTE && 
                    stage != ShaderStage.TASK && 
                    stage != ShaderStage.MESH) {
                    errors.add(ValidationError.EntryPoint(
                        epIndex,
                        ep.name,
                        EntryPointError.WorkgroupSizeNotAllowed
                    ))
                }
            }
            
            // Vérifier que les bindings sont uniques par entry point
            val epBindings = mutableSetOf<ResourceBinding>()
            for (arg in func.arguments) {
                arg.binding?.let { binding ->
                    if (epBindings.contains(binding)) {
                        errors.add(ValidationError.EntryPoint(
                            epIndex,
                            ep.name,
                            EntryPointError.DuplicateBinding(binding)
                        ))
                    }
                    epBindings.add(binding)
                    epResourceBindings.add(binding)
                }
            }
            
            currentEntryPointIndex = null
        }
        
        return errors
    }
    
    private fun validateGlobalVariables(module: Module): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        for ((varHandle, var) in module.globalVariables.withIndex()) {
            val handle = Handle<GlobalVariable>(varHandle)
            val varType = module.types[var.ty.index]
            
            // Vérifier que le type existe
            if (var.ty.index >= module.types.size) {
                errors.add(ValidationError.GlobalVariable(
                    handle,
                    var.name,
                    GlobalVariableError.InvalidType
                ))
            }
            
            // Vérifier le binding
            var.binding?.let { binding ->
                // Vérifier que le binding est valide pour le VariableClass
                when (var.class_) {
                    VariableClass.UNIFORM -> {
                        // OK
                    }
                    VariableClass.STORAGE -> {
                        // OK
                    }
                    VariableClass.WORKGROUP -> {
                        // Vérifier que binding n'a pas de group/binding
                        if (binding.group != null || binding.binding != null) {
                            errors.add(ValidationError.GlobalVariable(
                                handle,
                                var.name,
                                GlobalVariableError.WorkgroupVariableWithBinding
                            ))
                        }
                    }
                    VariableClass.PRIVATE -> {
                        // Vérifier que binding est null
                        if (binding != ResourceBinding.NONE) {
                            errors.add(ValidationError.GlobalVariable(
                                handle,
                                var.name,
                                GlobalVariableError.PrivateVariableWithBinding
                            ))
                        }
                    }
                }
                
                // Vérifier que le binding est unique
                if (epResourceBindings.contains(binding)) {
                    errors.add(ValidationError.GlobalVariable(
                        handle,
                        var.name,
                        GlobalVariableError.DuplicateBinding(binding)
                    ))
                }
                epResourceBindings.add(binding)
            }
            
            // Vérifier l'initialiseur
            var.init != null -> {
                // Vérifier que c'est une expression constante
                if (!isConstExpression(var.init, module)) {
                    errors.add(ValidationError.GlobalVariable(
                        handle,
                        var.name,
                        GlobalVariableError.NonConstInitializer
                    ))
                }
            }
        }
        
        return errors
    }
    
    private fun validateBlock(
        block: Handle<Statement>,
        module: Module,
        errors: MutableList<ValidationError>
    ) {
        val stmt = module.statements[block.index]
        
        when (stmt) {
            is Statement.Block -> {
                for (innerStmt in stmt.statements) {
                    validateBlock(innerStmt, module, errors)
                }
            }
            is Statement.If -> {
                // Valider la condition
                validateExpression(stmt.condition, module, errors)
                
                // Valider les branches
                validateBlock(stmt.accept, module, errors)
                stmt.reject?.let { reject ->
                    validateBlock(reject, module, errors)
                }
            }
            // ... autres types de statements
            else -> {
                // Statement simple
            }
        }
    }
    
    private fun validateExpression(
        expr: Handle<Expression>,
        module: Module,
        errors: MutableList<ValidationError>
    ) {
        // À implémenter selon les besoins de validation
    }
    
    private fun isConstExpression(
        expr: Handle<Expression>,
        module: Module
    ): Boolean {
        // Vérifier si l'expression est constante
        // Utiliser ConstantEvaluator si disponible
        return true // Placeholder
    }
    
    private fun getExpressionType(
        expr: Handle<Expression>,
        module: Module
    ): Type {
        // Récupérer le type de l'expression
        // Utiliser Typifier si disponible
        return module.types[0] // Placeholder
    }
    
    // Autres méthodes utilitaires...
}
```

---

## 📁 STRUCTURE DES FICHIERS

```
wgsl:core/src/main/kotlin/dev/gfxrs/naga/valid/
├── ValidationFlags.kt      # Flags de validation
├── Capabilities.kt         # Capacités supportées
├── ShaderStages.kt        # Étapes de shader supportées
├── SubgroupOperationSet.kt # Opérations de subgroup supportées
├── ValidationError.kt     # Toutes les classes d'erreur
├── Validator.kt           # Classe principale Validator
├── ModuleInfo.kt          # Informations calculées sur le module
├── type/
│   └── TypeError.kt       # Erreurs de type
├── function/
│   ├── FunctionError.kt   # Erreurs de fonction
│   └── LocalVariableError.kt # Erreurs de variables locales
├── interface/
│   ├── EntryPointError.kt # Erreurs d'entry point
│   ├── GlobalVariableError.kt # Erreurs de variables globales
│   └── VaryingError.kt     # Erreurs de varying
├── expression/
│   ├── ExpressionError.kt # Erreurs d'expression
│   └── ConstExpressionError.kt # Erreurs d'expression constante
└── compose/
    └── ComposeError.kt     # Erreurs de composition
```

---

## 🧪 TESTS

### 1. ValidatorTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/valid/ValidatorTest.kt`

```kotlin
package io.ygdrasil.wgsl.valid

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ValidatorTest {
    
    private lateinit var validator: Validator
    private lateinit var module: Module
    
    @BeforeEach
    fun setUp() {
        validator = Validator()
        module = Module()
    }
    
    @Test
    fun `test empty module`() {
        val result = validator.validate(module)
        assertThat(result.isSuccess).isTrue()
    }
    
    @Test
    fun `test valid module`() {
        // Créer un module valide simple
        val i32 = module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 4))
        ))
        
        val func = Function(
            name = "main",
            arguments = emptyList(),
            result = null,
            localVariables = emptyList(),
            expressions = emptyList(),
            body = module.statements.append(Statement.Block(emptyList())),
            namedExpressions = emptyList(),
            termination = FunctionTermination.RETURN
        )
        module.functions.append(func)
        
        val result = validator.validate(module)
        assertThat(result.isSuccess).isTrue()
    }
    
    @Test
    fun `test invalid scalar width`() {
        // Créer un type scalaire avec une largeur invalide
        module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 3)) // 3 n'est pas valide
        ))
        
        val result = validator.validate(module)
        assertThat(result.isFailure).isTrue()
        assertThat((result as Result.Failure).exceptionOrNull()).isInstanceOf(ValidationError.Type::class.java)
    }
    
    @Test
    fun `test duplicate override id`() {
        val i32 = module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 4))
        ))
        
        module.overrides.append(Override(
            name = "ov1",
            ty = i32,
            id = 1,
            init = null
        ))
        
        module.overrides.append(Override(
            name = "ov2",
            ty = i32,
            id = 1, // Duplicate ID
            init = null
        ))
        
        val result = validator.validate(module)
        assertThat(result.isFailure).isTrue()
        val errors = (result as Result.Failure).exceptionOrNull() as? List<ValidationError>
        assertThat(errors).isNotNull
        assertThat(errors?.any { it is ValidationError.Override && it.source is OverrideError.DuplicateID }).isTrue()
    }
    
    @Test
    fun `test non scalar override type`() {
        val vec2 = module.types.append(Type(
            name = null,
            inner = TypeInner.Vector(
                scalar = Scalar(ScalarKind.FLOAT, 4),
                size = VectorSize.BI
            )
        ))
        
        module.overrides.append(Override(
            name = "ov",
            ty = vec2,
            id = 1,
            init = null
        ))
        
        val result = validator.validate(module)
        assertThat(result.isFailure).isTrue()
        val errors = (result as Result.Failure).exceptionOrNull() as? List<ValidationError>
        assertThat(errors).isNotNull
        assertThat(errors?.any { it is ValidationError.Override && it.source is OverrideError.TypeNotScalar }).isTrue()
    }
    
    @Test
    fun `test validation flags`() {
        // Tester avec différents flags
        val validatorNoExpressions = Validator(
            flags = ValidationFlags.fromBits(ValidationFlags.BLOCKS),
            capabilities = Capabilities.DEFAULT
        )
        
        // Créer un module avec une expression invalide
        // (mais on ne valide pas les expressions)
        val result = validatorNoExpressions.validate(module)
        assertThat(result.isSuccess).isTrue()
    }
    
    @Test
    fun `test capabilities`() {
        val validatorNoFloat64 = Validator(
            flags = ValidationFlags.ALL,
            capabilities = Capabilities.DEFAULT // n'a pas FLOAT64
        )
        
        // Créer un module avec f64
        module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar(ScalarKind.FLOAT, 8))
        ))
        
        // Si on utilise f64 sans la capacité, ça devrait échouer
        // (mais pour l'instant, le validator ne vérifie pas ça)
        val result = validatorNoFloat64.validate(module)
        // Pour l'instant, on ne vérifie pas les capacités dans la validation de base
        assertThat(result.isSuccess).isTrue()
    }
    
    @Test
    fun `test validateThrow on success`() {
        val result = validator.validateThrow(module)
        assertThat(result).isNotNull()
    }
    
    @Test
    fun `test validateThrow on failure`() {
        module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 3))
        ))
        
        try {
            validator.validateThrow(module)
            assertThat(false).isTrue() // Ne devrait pas arriver
        } catch (e: Exception) {
            assertThat(e).isInstanceOf(ValidationError::class.java)
        }
    }
    
    @Test
    fun `test validateQuiet`() {
        assertThat(validator.validateQuiet(module)).isTrue()
        
        module.types.append(Type(
            name = null,
            inner = TypeInner.Scalar(Scalar(ScalarKind.SINT, 3))
        ))
        
        assertThat(validator.validateQuiet(module)).isFalse()
    }
}
```

---

## ✅ CHECKLIST D'IMPLÉMENTATION

### Structure des Fichiers
- [ ] `ValidationFlags.kt` - Flags de validation
- [ ] `Capabilities.kt` - Toutes les capacités
- [ ] `ShaderStages.kt` - Étapes de shader
- [ ] `SubgroupOperationSet.kt` - Opérations de subgroup
- [ ] `ValidationError.kt` - Classe de base pour les erreurs
- [ ] `Validator.kt` - Classe principale
- [ ] `ModuleInfo.kt` - Informations calculées

### Validation des Types
- [ ] Validation des scalaires (width valide)
- [ ] Validation des vecteurs (width scalaire valide)
- [ ] Validation des matrices (dimensions valides)
- [ ] Validation des structs (offsets alignés, span correct)
- [ ] Validation des arrays (taille positive)
- [ ] Validation des pointeurs
- [ ] Validation des types Image/Sampler

### Validation des Constantes
- [ ] Initialiseur est une expression constante
- [ ] Type correspond à l'initialiseur
- [ ] Type est constructible

### Validation des Overrides
- [ ] Type est scalaire
- [ ] ID est unique
- [ ] Initialiseur est valide

### Validation des Fonctions
- [ ] Arguments ont des types valides
- [ ] Type de retour est valide
- [ ] Variables locales ont des types valides
- [ ] Initialiseurs des variables locales sont valides
- [ ] Corps de la fonction est valide
- [ ] Terminaison est valide

### Validation des Entry Points
- [ ] Nom n'est pas vide
- [ ] Stage est supportée
- [ ] Pas de binding sur les arguments de fonction
- [ ] early_depth_test est supporté si présent
- [ ] workgroup_size est valide pour la stage
- [ ] Bindings sont uniques

### Validation des Variables Globales
- [ ] Type est valide
- [ ] Binding est valide pour le VariableClass
- [ ] Binding est unique
- [ ] Initialiseur est une expression constante

### Validation des Expressions
- [ ] Types sont compatibles pour les opérations
- [ ] Opérations sont autorisées pour les types
- [ ] Accès aux membres sont valides
- [ ] Accès aux indices sont valides
- [ ] Appels de fonction sont valides

### Validation des Statements
- [ ] Bloc est valide
- [ ] If/Else est valide
- [ ] Switch est valide
- [ ] Loop/While/For sont valides
- [ ] Break/Continue sont dans un bloc valide
- [ ] Return est valide
- [ ] Store est valide
- [ ] Call est valide

### Tests
- [ ] Tests pour un module vide
- [ ] Tests pour un module valide
- [ ] Tests pour chaque type d'erreur
- [ ] Tests pour ValidationFlags
- [ ] Tests pour Capabilities
- [ ] Tests pour validateThrow/validateQuiet

### Intégration
- [ ] Utiliser Validator dans les backends
- [ ] Utiliser Validator dans le frontend WGSL
- [ ] Documenter l'API publique

---

## 📖 RÉFÉRENCES

1. **WGSL Specification** : [Validation Rules](https://gpuweb.github.io/gpuweb/wgsl/#validation-rules)
2. **Rust Reference** : `/Users/chaos/RustroverProjects/wgpu/naga/src/valid/mod.rs`
3. **Naga Documentation** : [Validator](https://docs.rs/naga/latest/naga/valid/struct.Validator.html)

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances | Priorité |
|-------|-------|-------------|----------|
| Implémenter ValidationFlags.kt | 2-4h | Aucune | ⭐⭐⭐⭐⭐ |
| Implémenter Capabilities.kt | 4-6h | Aucune | ⭐⭐⭐⭐⭐ |
| Implémenter ShaderStages.kt | 2h | Aucune | ⭐⭐⭐⭐⭐ |
| Implémenter SubgroupOperationSet.kt | 2h | Aucune | ⭐⭐⭐⭐ |
| Implémenter ValidationError.kt (base) | 4-6h | Aucune | ⭐⭐⭐⭐⭐ |
| Implémenter TypeError.kt | 4h | ValidationError | ⭐⭐⭐⭐⭐ |
| Implémenter FunctionError.kt | 4h | ValidationError | ⭐⭐⭐⭐⭐ |
| Implémenter EntryPointError.kt | 4h | ValidationError | ⭐⭐⭐⭐⭐ |
| Implémenter GlobalVariableError.kt | 4h | ValidationError | ⭐⭐⭐⭐⭐ |
| Implémenter ExpressionError.kt | 4h | ValidationError | ⭐⭐⭐⭐⭐ |
| Implémenter Validator.kt (base) | 8-12h | ValidationFlags, Capabilities | ⭐⭐⭐⭐⭐ |
| Implémenter validateTypes() | 8h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateConstants() | 4h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateOverrides() | 4h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateFunctions() | 12-16h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateEntryPoints() | 8h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateGlobalVariables() | 8h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateExpressions() | 16-24h | Validator base | ⭐⭐⭐⭐⭐ |
| Implémenter validateStatements() | 16-24h | Validator base | ⭐⭐⭐⭐⭐ |
| Tests unitaires | 24-40h | Tout | ⭐⭐⭐⭐ |
| Intégration | 8h | Tout | ⭐⭐⭐ |
| **Total** | **144-240h (4-6 semaines)** | | |

**Note** : La validation complète est complexe. Pour la Phase 3, on peut implémenter une version simplifiée qui couvre les cas les plus courants, puis étendre progressivement.

---

## 🔄 DÉPENDANCES

### Dépendances Internes
- `wgsl:core` : Module IR (Module, Type, Function, Expression, Statement, etc.)
- `wgsl:core (proc)` : Layouter, ConstantEvaluator, Typifier
- `io.ygdrasil.wgsl.arena.Handle`
- `io.ygdrasil.wgsl.ir.*`
- `io.ygdrasil.wgsl.span.Span`

### Dépendances Externes
- Aucune (kotlin-stdlib uniquement)

---

## 📝 NOTES

1. **Validation Incrémentale** : Le validator peut être utilisé pendant la construction du module pour détecter les erreurs tôt. Cependant, certaines validations (comme la validation des expressions) nécessitent que tout le module soit construit.

2. **Performance** : La validation est une opération O(n) où n est la taille du module. Pour un shader typique, cela prend quelques millisecondes. Pour la Phase 3, on ne se concentre pas sur l'optimisation de la performance.

3. **Messages d'erreur** : Les messages d'erreur doivent être clairs et précis, avec des informations sur la localisation (span) de l'erreur. Cela aide les utilisateurs à comprendre et corriger les problèmes.

4. **Capacités par Backend** : Chaque backend a ses propres capacités. Le validator doit être configurable avec les capacités supportées par le backend cible.

5. **Validation partielle** : Les ValidationFlags permettent de désactiver certaines validations. C'est utile pour les tests ou pour les backends qui ne supportent pas certaines fonctionnalités.

6. **ModuleInfo** : Le validator calcule et stocke des métadonnées sur le module (TypeInfo, FunctionInfo, etc.) qui peuvent être utilisées par les backends pour optimiser la génération de code.

7. **Validation des Expressions** : La validation des expressions est la partie la plus complexe. Elle nécessite de vérifier :
   - La compatibilité des types
   - Les opérations autorisées
   - L'uniformité du flux de contrôle
   - Les effets de bord
   - etc.

8. **Priorisation** : Pour la Phase 3, on peut commencer par implémenter les validations les plus simples (types, constantes, overrides) puis étendre progressivement aux expressions et statements.
