# 🔤 Phase 1.2 : Types Primitifs et Énums

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-core`  
**Phase** : 1 - Fondations  
**Sous-Phase** : 1.2 - Types Primitifs  
**Durée** : 1-2 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Base de tout le système de types)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (lignes 536-816)

---

## 📋 OBJECTIFS

Implémenter **tous les types primitifs et énums** utilisés dans le système IR de Naga.  
Ces types forment la **base** sur laquelle tous les autres types sont construits.

**Livrable principal** : Tous les enums et types de base dans `naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Types.kt`

---

## 🎯 ORGANISATION DES TYPES

### Catégories de Types

```
Types Primitifs (Types.kt)
├── Types de base (Bytes, ScalarKind, VectorSize)
├── Espaces et Classes (AddressSpace, StorageClass, ImageClass)
├── Dimensions (ImageDimension, ArraySize)
├── Formats (StorageFormat)
├── Built-ins (BuiltIn, ShaderStage)
├── Access (ImageAccess, StorageAccess)
└── Autre (EarlyDepthTest, ConservativeDepth, etc.)
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. Types.kt (Fichier Principal)

**Fichier** : `naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Types.kt`

```kotlin
package dev.gfxrs.naga.ir

import dev.gfxrs.naga.arena.Handle
import dev.gfxrs.naga.arena.UniqueArena
import kotlinx.serialization.Serializable

// ============================================================================
// TYPES DE BASE
// ============================================================================

/**
 * Taille en bytes (1, 2, 4, 8).
 * Utilisé pour spécifier la taille des types scalaires.
 */
typealias Bytes = UInt

/**
 * Kind de scalaire (type de base).
 */
@Serializable
enum class ScalarKind {
    /** Boolean (true/false) */
    BOOL,
    
    /** Signed integer (entier signé) */
    SINT,
    
    /** Unsigned integer (entier non signé) */
    UINT,
    
    /** Floating point (nombre à virgule flottante) */
    FLOAT,
    
    /** Abstract integer (pour compute shaders, pas stocké en mémoire) */
    ABSTRACT_INT
}

/**
 * Taille de vecteur (nombre de composantes).
 */
@Serializable
enum class VectorSize {
    /** 2 composantes (x, y) */
    BI,
    
    /** 3 composantes (x, y, z) */
    TRI,
    
    /** 4 composantes (x, y, z, w) */
    QUAD
}

// ============================================================================
// ESPACES D'ADRESSAGE
// ============================================================================

/**
 * Espace d'adressage des variables.
 * Détermine où et comment une variable est stockée.
 */
@Serializable
enum class AddressSpace {
    /**
     * Variables locales à une fonction.
     * Durée de vie : durée de l'exécution de la fonction.
     */
    FUNCTION,
    
    /**
     * Données privées, par invocation, mutables.
     * Durée de vie : durée de l'exécution du shader.
     */
    PRIVATE,
    
    /**
     * Données partagées dans le workgroup, mutables.
     * Durée de vie : durée de l'exécution du shader.
     */
    WORKGROUP,
    
    /**
     * Buffer uniforme (lecture seule).
     * Durée de vie : durée du draw/dispatch.
     */
    UNIFORM,
    
    /**
     * Buffer de stockage, potentiellement mutable.
     * Durée de vie : durée du draw/dispatch.
     */
    STORAGE,
    
    /**
     * Opaque handles (samplers, images, etc.).
     * Ne pointe pas vers de la mémoire accessible.
     */
    HANDLE,
    
    /**
     * Données immédiates (pipeline overrides).
     * Fournies via SetImmediates commands.
     * Maximum 1 variable globale dans cet espace.
     */
    IMMEDIATE,
    
    /**
     * Payload de la task shader vers la mesh shader.
     */
    TASK_PAYLOAD,
    
    /**
     * Payload de ray tracing (sortant).
     */
    RAY_PAYLOAD,
    
    /**
     * Payload de ray tracing (entrant).
     */
    INCOMING_RAY_PAYLOAD
}

// ============================================================================
// CLASSES D'IMAGES
// ============================================================================

/**
 * Classe d'image (type de données stockées dans l'image).
 */
@Serializable
enum class ImageClass {
    /**
     * Image de profondeur.
     * Contient des valeurs de profondeur (float).
     */
    DEPTH,
    
    /**
     * Image de couleur.
     * Contient des couleurs (vecteurs de float).
     */
    COLOR,
    
    /**
     * Image de stockage.
     * Peut être lue et écrite en compute shaders.
     */
    STORAGE
}

/**
 * Dimension de l'image.
 */
@Serializable
enum class ImageDimension {
    /** 1D (ligne) */
    ONE_D,
    
    /** 2D (texture plate) */
    TWO_D,
    
    /** 3D (volume) */
    THREE_D,
    
    /** Cube map (6 faces 2D) */
    CUBE,
    
    /** Rectangle (2D non normalisée) */
    RECT,
    
    /** External (texture externe, ex: vidéo) */
    EXTERNAL,
    
    /** Subpass (attachment de render pass) */
    SUBPASS
}

/**
 * Accès à l'image (permissions de lecture/écriture).
 */
@Serializable
enum class ImageAccess {
    /** Lecture seule */
    LOAD,
    
    /** Écriture seule */
    STORE,
    
    /** Lecture et écriture */
    LOAD_STORE
}

// ============================================================================
// BINDING ET LOCATION
// ============================================================================

/**
 * Binding pour les variables d'entrée/sortie.
 * Détermine comment une variable est liée au pipeline.
 */
@Serializable
sealed class Binding {
    /**
     * Variable built-in (ex: @position, @vertex_index).
     */
    @Serializable
    data class BuiltIn(val builtin: BuiltIn) : Binding()
    
    /**
     * Variable à un location spécifique (ex: @location(0)).
     */
    @Serializable
    data class Location(val location: UInt) : Binding()
    
    /**
     * Variable liée à un resource (group et binding).
     */
    @Serializable
    data class Resource(
        val group: UInt,
        val binding: UInt
    ) : Binding()
}

/**
 * Variables built-in (spéciales).
 * Ces variables ont un sens spécial dans le pipeline.
 */
@Serializable
enum class BuiltIn {
    // ===== Inputs =====
    
    /**
     * Index de la primitive (fragment, mesh).
     * Type: u32
     */
    PRIMITIVE_INDEX,
    
    /**
     * Position du vertex/fragment (sortie vertex, entrée fragment).
     * Type: vec4<f32>
     * Peut avoir l'attribut [[invariant]] en HLSL
     */
    POSITION,
    
    /**
     * Index du view (multi-view rendering).
     * Type: u32
     */
    VIEW_INDEX,
    
    /**
     * Index de l'instance de base.
     * Type: u32
     */
    BASE_INSTANCE,
    
    /**
     * Index du vertex de base.
     * Type: u32
     */
    BASE_VERTEX,
    
    // ===== Outputs (Vertex/Mesh) =====
    
    /**
     * Distances de clipping (sortie vertex/mesh).
     * Type: array<f32, N>
     */
    CLIP_DISTANCES,
    
    /**
     * Distance de culling (sortie vertex/mesh).
     * Type: array<f32, N>
     */
    CULL_DISTANCE,
    
    /**
     * Taille du point (sortie vertex/mesh).
     * Type: f32
     */
    POINT_SIZE,
    
    // ===== Inputs (Fragment) =====
    
    /**
     * Index du vertex (entrée fragment).
     * Type: u32
     */
    VERTEX_INDEX,
    
    /**
     * Index de l'instance (entrée fragment).
     * Type: u32
     */
    INSTANCE_INDEX,
    
    /**
     * Index du draw call (entrée fragment).
     * Type: u32
     */
    DRAW_INDEX,
    
    /**
     * Profondeur du fragment (sortie fragment).
     * Type: f32
     */
    FRAG_DEPTH,
    
    /**
     * Coordonnées du point dans un point primitive (entrée fragment).
     * Type: vec2<f32>
     */
    POINT_COORD,
    
    /**
     * Le fragment fait face à l'avant (entrée fragment).
     * Type: bool
     */
    FRONT_FACING,
    
    /**
     * Coordonnées barycentriques (entrée fragment).
     * Type: vec3<f32>
     */
    BARYCENTRIC,
    
    /**
     * Index de l'échantillon (entrée fragment).
     * Type: u32
     */
    SAMPLE_INDEX,
    
    /**
     * Masque de l'échantillon (sortie fragment).
     * Type: u32
     */
    SAMPLE_MASK,
    
    // ===== Inputs (Compute/Task/Mesh) =====
    
    /**
     * ID global de l'invocation (entrée compute/task/mesh).
     * Type: vec3<u32>
     */
    GLOBAL_INVOCATION_ID,
    
    /**
     * ID local de l'invocation (entrée compute/task/mesh).
     * Type: vec3<u32>
     */
    LOCAL_INVOCATION_ID,
    
    /**
     * Index local de l'invocation (entrée compute/task/mesh).
     * Type: u32
     */
    LOCAL_INVOCATION_INDEX,
    
    /**
     * ID du workgroup (entrée compute/task/mesh).
     * Type: vec3<u32>
     */
    WORKGROUP_ID,
    
    /**
     * Taille du workgroup (entrée compute/task/mesh).
     * Type: vec3<u32>
     */
    WORKGROUP_SIZE,
    
    /**
     * Nombre de workgroups (entrée compute/task/mesh).
     * Type: vec3<u32>
     */
    NUM_WORKGROUPS,
    
    // ===== Subgroup =====
    
    /**
     * Nombre de subgroups (entrée compute/task/mesh).
     * Type: u32
     */
    NUM_SUBGROUPS,
    
    /**
     * ID du subgroup (entrée compute/task/mesh).
     * Type: u32
     */
    SUBGROUP_ID,
    
    /**
     * Taille du subgroup (entrée compute/fragment/task/mesh).
     * Type: u32
     */
    SUBGROUP_SIZE,
    
    /**
     * ID de l'invocation dans le subgroup (entrée compute/fragment/task/mesh).
     * Type: u32
     */
    SUBGROUP_INVOCATION_ID,
    
    // ===== Mesh Shader =====
    
    /**
     * Taille du mesh task (sortie task).
     * Type: vec3<u32>
     */
    MESH_TASK_SIZE,
    
    /**
     * Primitive à culller (sortie mesh).
     */
    CULL_PRIMITIVE,
    
    /**
     * Index du point (sortie mesh).
     * Type: u32
     */
    POINT_INDEX,
    
    /**
     * Indices des lignes (sortie mesh).
     * Type: vec2<u32>
     */
    LINE_INDICES,
    
    /**
     * Indices des triangles (sortie mesh).
     * Type: vec3<u32>
     */
    TRIANGLE_INDICES,
    
    // ===== Mesh Shader Output =====
    
    /**
     * Compteur de vertices (sortie mesh, variable workgroup).
     * Type: atomic<u32>
     */
    VERTEX_COUNT,
    
    /**
     * Vertices générés (sortie mesh, variable workgroup).
     */
    VERTICES,
    
    /**
     * Compteur de primitives (sortie mesh, variable workgroup).
     * Type: atomic<u32>
     */
    PRIMITIVE_COUNT,
    
    /**
     * Primitives générées (sortie mesh, variable workgroup).
     */
    PRIMITIVES,
    
    // ===== Ray Tracing =====
    
    /**
     * ID de l'invocation du ray (entrée ray tracing).
     * Type: u32
     */
    RAY_INVOCATION_ID,
    
    /**
     * Nombre d'invocations de ray (entrée ray tracing).
     * Type: u32
     */
    NUM_RAY_INVOCATIONS,
    
    /**
     * Données personnalisées de l'instance (entrée closest hit/any hit).
     */
    INSTANCE_CUSTOM_DATA,
    
    /**
     * Index de la géométrie dans le BLAS (entrée closest hit/any hit).
     * Type: u32
     */
    GEOMETRY_INDEX,
    
    /**
     * Origine du ray dans l'espace monde (entrée closest hit/any hit/miss).
     * Type: vec3<f32>
     */
    WORLD_RAY_ORIGIN,
    
    /**
     * Direction du ray dans l'espace monde (entrée closest hit/any hit/miss).
     * Type: vec3<f32>
     */
    WORLD_RAY_DIRECTION,
    
    /**
     * Origine du ray dans l'espace objet (entrée closest hit/any hit).
     * Type: vec3<f32>
     */
    OBJECT_RAY_ORIGIN,
    
    /**
     * Direction du ray dans l'espace objet (entrée closest hit/any hit).
     * Type: vec3<f32>
     */
    OBJECT_RAY_DIRECTION,
    
    /**
     * t-min du ray (entrée closest hit/any hit/miss).
     * Type: f32
     */
    RAY_TMIN,
    
    /**
     * t-max actuel du ray (entrée closest hit/any hit/miss).
     * Type: f32
     */
    RAY_TCURRENT_MAX,
    
    /**
     * Matrice objet → monde (entrée closest hit/any hit).
     * Type: mat4x4<f32>
     */
    OBJECT_TO_WORLD,
    
    /**
     * Matrice monde → objet (entrée closest hit/any hit).
     * Type: mat4x4<f32>
     */
    WORLD_TO_OBJECT,
    
    /**
     * Type de hit (entrée closest hit/any hit).
     * 254 (0xFE) = triangle face avant
     * 255 (0xFF) = triangle face arrière
     * Autres = valeur fournie par la fonction d'intersection
     * Type: u32
     */
    HIT_KIND
}

// ============================================================================
// STAGES DE SHADER
// ============================================================================

/**
 * Stage (type) de shader.
 * Détermine à quelle étape du pipeline le shader est exécuté.
 */
@Serializable
enum class ShaderStage {
    /** Vertex shader (render pipeline) */
    VERTEX,
    
    /** Task shader (mesh pipeline) */
    TASK,
    
    /** Mesh shader (mesh pipeline) */
    MESH,
    
    /** Fragment shader (render pipeline) */
    FRAGMENT,
    
    /** Compute shader */
    COMPUTE,
    
    // ===== Ray Tracing =====
    
    /** Ray generation shader (ray tracing pipeline) */
    RAY_GENERATION,
    
    /** Miss shader (ray tracing pipeline) */
    MISS,
    
    /** Any hit shader (ray tracing pipeline) */
    ANY_HIT,
    
    /** Closest hit shader (ray tracing pipeline) */
    CLOSEST_HIT
}

// ============================================================================
// TEST DE PROFONDEUR PRÉCOCE (Early Depth Test)
// ============================================================================

/**
 * Contrôle du test de profondeur précoce.
 */
@Serializable
sealed class EarlyDepthTest {
    /**
     * Force le test de profondeur à être exécuté avant le fragment shader.
     * Désactive les tests de profondeur après le fragment shader.
     */
    @Serializable
    object FORCE : EarlyDepthTest()
    
    /**
     * Autorise un test de profondeur précoce, mais ne le force pas.
     */
    @Serializable
    data class ALLOW(
        /**
         * Restrictions sur comment la profondeur peut être modifiée.
         */
        val conservative: ConservativeDepth
    ) : EarlyDepthTest()
}

/**
 * Contrôle comment la profondeur peut être ajustée.
 */
@Serializable
enum class ConservativeDepth {
    /**
     * Le shader ne peut écrire une profondeur que plus grande que celle calculée.
     */
    GREATER_EQUAL,
    
    /**
     * Le shader ne peut écrire une profondeur que plus petite que celle calculée.
     */
    LESS_EQUAL,
    
    /**
     * Le shader ne peut pas modifier la profondeur.
     */
    UNCHANGED
}

// ============================================================================
// TAISE DE TRAVAIL (Workgroup Size)
// ============================================================================

/**
 * Taille du workgroup pour les compute shaders.
 * [x, y, z] où x * y * z = nombre total d'invocations par workgroup.
 */
typealias WorkgroupSize = List<UInt>

// ============================================================================
// TAILLE DE TABLEAU
// ============================================================================

/**
 * Taille d'un tableau.
 * Peut être constante, dynamique (expression), ou non spécifiée.
 */
@Serializable
sealed class ArraySize {
    /**
     * Taille constante.
     * Ex: array<f32, 10>
     */
    @Serializable
    data class Constant(val value: UInt) : ArraySize()
    
    /**
     * Taille dynamique (déterminée à l'exécution).
     * Ex: array<f32, N> où N est une variable
     */
    @Serializable
    data class Dynamic(val expression: Handle<dev.gfxrs.naga.ir.Expression>) : ArraySize()
    
    /**
     * Taille non spécifiée.
     * Ex: array<f32> (opaque array)
     */
    @Serializable
    object UN_SIZED : ArraySize()
}

// ============================================================================
// ACCÈS STORAGE
// ============================================================================

/**
 * Type d'accès pour le storage (SPIR-V).
 */
@Serializable
sealed class StorageAccess {
    /** Lecture seule */
    @Serializable
    object LOAD : StorageAccess()
    
    /** Écriture seule */
    @Serializable
    object STORE : StorageAccess()
    
    /** Lecture et écriture */
    @Serializable
    object LOAD_STORE : StorageAccess()
}

// ============================================================================
// CLASSE DE VARIABLE
// ============================================================================

/**
 * Classe de variable globale.
 */
@Serializable
enum class VariableClass {
    /** Uniform buffer */
    UNIFORM,
    
    /** Storage buffer */
    STORAGE,
    
    /** Workgroup memory */
    WORKGROUP,
    
    /** Private memory */
    PRIVATE
}

// ============================================================================
// TOPOLOGIE DE SORTIE MESH
// ============================================================================

/**
 * Topologie de sortie pour les mesh shaders.
 */
@Serializable
enum class MeshOutputTopology {
    /** Points */
    POINT,
    
    /** Lignes */
    LINE,
    
    /** Triangles */
    TRIANGLE
}

// ============================================================================
// COMPOSANTES DE SWIZZLE
// ============================================================================

/**
 * Composante de swizzle (pour les opérations de swizzle sur les vecteurs).
 */
@Serializable
enum class SwizzleComponent {
    X, Y, Z, W,
    R, G, B, A
}

// ============================================================================
// AXE DE DÉRIVÉE
// ============================================================================

/**
 * Axe pour les opérations de dérivée (dpdx, dpdy).
 */
@Serializable
enum class DerivativeAxis {
    X, Y
}

// ============================================================================
// NIVEAU D'ÉCHANTILLONNAGE
// ============================================================================

/**
 * Niveau de détail pour l'échantillonnage de textures.
 */
@Serializable
sealed class SampleLevel {
    /**
     * Niveau 0 (mipmap de base).
     */
    @Serializable
    object ZERO : SampleLevel()
    
    /**
     * Niveau de mipmap spécifique.
     */
    @Serializable
    data class MIPMAP(val level: Handle<dev.gfxrs.naga.ir.Expression>) : SampleLevel()
    
    /**
     * Niveau calculé à partir d'un gradient.
     */
    @Serializable
    data class GRADIENT(
        val x: Handle<dev.gfxrs.naga.ir.Expression>,
        val y: Handle<dev.gfxrs.naga.ir.Expression>
    ) : SampleLevel()
    
    /**
     * Niveau calculé automatiquement.
     */
    @Serializable
    data class AUTOMATIC(val level: Handle<dev.gfxrs.naga.ir.Expression>) : SampleLevel()
}

// ============================================================================
// FONCTIONS RELATIONNELLES
// ============================================================================

/**
 * Fonctions relationnelles (comparaison).
 */
@Serializable
enum class RelationalFunction {
    EQUAL,
    NOT_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL
}

// ============================================================================
// FONCTIONS MATHÉMATIQUES
// ============================================================================

/**
 * Fonctions mathématiques built-in.
 * 
 * Note: Certaines fonctions peuvent avoir des restrictions sur les types d'entrée.
 */
@Serializable
enum class MathFunction {
    // ===== Math Standard =====
    
    /** Valeur absolue */
    ABS,
    /** Arc cosinus */
    ACOS,
    /** Arc cosinus hyperbolique */
    ACOSH,
    /** Arc sinus */
    ASIN,
    /** Arc sinus hyperbolique */
    ASINH,
    /** Arc tangente */
    ATAN,
    /** Arc tangente à 2 arguments (atan2) */
    ATAN2,
    /** Arc tangente hyperbolique */
    ATANH,
    
    /** Arrondi vers le haut */
    CEIL,
    /** Clamp (min, valeur, max) */
    CLAMP,
    /** Cosinus */
    COS,
    /** Cosinus hyperbolique */
    COSH,
    /** Produit vectoriel (cross product) */
    CROSS,
    /** Convertit radians en degrés */
    DEGREES,
    /** Déterminant d'une matrice */
    DETERMINANT,
    
    /** Distance entre deux vecteurs */
    DISTANCE,
    /** Produit scalaire (dot product) */
    DOT,
    /** e^x */
    EXP,
    /** e^x - 1 */
    EXP2,
    
    /** Face forward (oriente un vecteur) */
    FACE_FORWARD,
    /** Arrondi vers le bas */
    FLOOR,
    /** Partie fractionnaire avec exposant biaisé */
    FRAC_EXP_BIAS,
    /** Frexp (mantisse et exposant) */
    FREXP,
    
    /** Longueur d'un vecteur */
    LENGTH,
    /** Interpolation linéaire */
    LERP,
    /** Logarithme naturel */
    LOG,
    /** Logarithme base 2 */
    LOG2,
    
    /** Maximum de deux valeurs */
    MAX,
    /** Minimum de deux valeurs */
    MIN,
    /** Interpolation linéaire (mix) */
    MIX,
    
    /** Modf (partie fractionnaire et entière) */
    MODF,
    /** Normalisation d'un vecteur */
    NORMALIZE,
    /** Puissance (x^y) */
    POW,
    /** Convertit degrés en radians */
    RADIANS,
    
    /** Réflexion d'un vecteur */
    REFLECT,
    /** Réfraction d'un vecteur */
    REFRACT,
    /** Arrondi */
    ROUND,
    
    /** Signe */
    SIGNOF,
    /** Sinus */
    SIN,
    /** Sinus hyperbolique */
    SINH,
    /** Step (étape) */
    STEP,
    
    /** Racine carrée */
    SQRT,
    /** Step smooth */
    SMOOTHSTEP,
    /** Tangente */
    TAN,
    /** Tangente hyperbolique */
    TANH,
    
    /** Transposée d'une matrice */
    TRANSPOSE,
    /** Troncature (vers zéro) */
    TRUNC,
    
    // ===== Math Counting (Compteurs de bits) =====
    
    /** Compte les zéros en tête (i32) */
    COUNT_LEADING_ZEROS_I32,
    /** Compte les zéros en tête (i64) */
    COUNT_LEADING_ZEROS_I64,
    
    /** Compte les uns (i32) */
    COUNT_ONE_BITS_I32,
    /** Compte les uns (i64) */
    COUNT_ONE_BITS_I64,
    
    /** Compte les zéros en queue (i32) */
    COUNT_TRAILING_ZEROS_I32,
    /** Compte les zéros en queue (i64) */
    COUNT_TRAILING_ZEROS_I64,
    
    /** Premier bit à un en tête (i32) */
    FIRST_LEADING_BIT_I32,
    /** Premier bit à un en tête (i64) */
    FIRST_LEADING_BIT_I64,
    
    /** Premier bit à un en queue (i32) */
    FIRST_TRAILING_BIT_I32,
    /** Premier bit à un en queue (i64) */
    FIRST_TRAILING_BIT_I64,
    
    // ===== Packing/Unpacking =====
    
    /** Pack deux f16 en u32 */
    PACK2X16_FLOAT,
    /** Pack deux f32 en u32 (normalisé) */
    PACK2X16_SNORM,
    /** Pack deux f32 en u32 (non normalisé) */
    PACK2X16_UNORM,
    
    /** Pack quatre f32 en u32 (normalisé) */
    PACK4X8_SNORM,
    /** Pack quatre f32 en u32 (non normalisé) */
    PACK4X8_UNORM,
    
    /** Unpack deux u32 en vec2<f32> */
    UNPACK2X16_FLOAT,
    /** Unpack deux u32 en vec2<f16> (normalisé) */
    UNPACK2X16_SNORM,
    /** Unpack deux u32 en vec2<f16> (non normalisé) */
    UNPACK2X16_UNORM,
    
    /** Unpack quatre u32 en vec4<f32> (normalisé) */
    UNPACK4X8_SNORM,
    /** Unpack quatre u32 en vec4<f32> (non normalisé) */
    UNPACK4X8_UNORM,
    
    // ===== Atomic Operations =====
    
    /** Addition atomique */
    ATOMIC_ADD,
    /** Compare Exchange atomique (faible) */
    ATOMIC_COMPARE_EXCHANGE_WEAK,
    /** Exchange atomique */
    ATOMIC_EXCHANGE,
    /** Load atomique */
    ATOMIC_LOAD,
    /** Max atomique */
    ATOMIC_MAX,
    /** Min atomique */
    ATOMIC_MIN,
    /** OR atomique */
    ATOMIC_OR,
    /** AND atomique */
    ATOMIC_AND,
    /** Store atomique */
    ATOMIC_STORE,
    /** XOR atomique */
    ATOMIC_XOR
}

// ============================================================================
// OPÉRATEURS UNAIRES
// ============================================================================

/**
 * Opérateurs unaires.
 */
@Serializable
enum class UnaryOperator {
    /** Négation numérique (-x) */
    NEGATE,
    
    /** Négation logique (!x) */
    NOT
}

// ============================================================================
// OPÉRATEURS BINAIRES
// ============================================================================

/**
 * Opérateurs binaires.
 */
@Serializable
enum class BinaryOperator {
    // ===== Arithmétiques =====
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    
    // ===== Comparaison =====
    EQUAL,
    NOT_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL,
    
    // ===== Logiques =====
    LOGICAL_AND,
    LOGICAL_OR,
    
    // ===== Bits =====
    BITWISE_AND,
    BITWISE_OR,
    BITWISE_XOR,
    SHIFT_LEFT,
    SHIFT_RIGHT
}

// ============================================================================
// FONCTIONS ATOMIQUES
// ============================================================================

/**
 * Fonctions atomiques pour les opérations sur les variables atomiques.
 */
@Serializable
enum class AtomicFunction {
    ADD,
    SUBTRACT,
    MIN,
    MAX,
    AND,
    OR,
    XOR,
    EXCHANGE,
    COMPARE_EXCHANGE_WEAK,
    LOAD,
    STORE
}

// ============================================================================
// FORWARD DECLARATION POUR Handle<Expression>
// ============================================================================

// Note: Ces types sont définis dans Expression.kt, mais nous en avons besoin ici
// pour les références dans ArraySize, SampleLevel, etc.
// Le compilateur Kotlin gère les forward references dans le même package.
```

---

## 📄 StorageFormat.kt (Formats de Stockage)

**Fichier** : `naga-core/src/main/kotlin/dev/gfxrs/naga/ir/StorageFormat.kt`

```kotlin
package dev.gfxrs.naga.ir

import kotlinx.serialization.Serializable

/**
 * Format de stockage pour les images.
 * Ces formats définissent comment les pixels sont stockés en mémoire.
 */
@Serializable
enum class StorageFormat {
    // ===== 8-bit formats =====
    
    /** 8-bit unsigned normalized (0-1) */
    R8_UNORM,
    /** 8-bit signed normalized (-1 to 1) */
    R8_SNORM,
    /** 8-bit unsigned integer */
    R8_UINT,
    /** 8-bit signed integer */
    R8_SINT,
    
    // ===== 16-bit formats =====
    
    /** 16-bit unsigned integer */
    R16_UINT,
    /** 16-bit signed integer */
    R16_SINT,
    /** 16-bit float */
    R16_FLOAT,
    
    /** 16-bit unsigned normalized (2 components) */
    RG8_UNORM,
    /** 16-bit signed normalized (2 components) */
    RG8_SNORM,
    /** 16-bit unsigned integer (2 components) */
    RG8_UINT,
    /** 16-bit signed integer (2 components) */
    RG8_SINT,
    
    // ===== 32-bit formats =====
    
    /** 32-bit unsigned integer */
    R32_UINT,
    /** 32-bit signed integer */
    R32_SINT,
    /** 32-bit float */
    R32_FLOAT,
    
    /** 32-bit unsigned integer (2 components) */
    RG16_UINT,
    /** 32-bit signed integer (2 components) */
    RG16_SINT,
    /** 32-bit float (2 components) */
    RG16_FLOAT,
    
    /** 32-bit unsigned normalized (4 components) */
    RGBA8_UNORM,
    /** 32-bit signed normalized (4 components) */
    RGBA8_SNORM,
    /** 32-bit unsigned integer (4 components) */
    RGBA8_UINT,
    /** 32-bit signed integer (4 components) */
    RGBA8_SINT,
    
    /** 32-bit BGRA unsigned normalized */
    BGRA8_UNORM,
    
    // ===== Packed formats =====
    
    /** 32-bit RGB10A2 unsigned integer */
    RGB10A2_UINT,
    /** 32-bit RGB10A2 unsigned normalized */
    RGB10A2_UNORM,
    
    /** 32-bit RG11B10 unsigned float */
    RG11B10_UFLOAT,
    
    // ===== 64-bit formats =====
    
    /** 64-bit unsigned integer */
    R64_UINT,
    
    /** 64-bit unsigned integer (2 components) */
    RG32_UINT,
    /** 64-bit signed integer (2 components) */
    RG32_SINT,
    /** 64-bit float (2 components) */
    RG32_FLOAT,
    
    // ===== 128-bit formats =====
    
    /** 128-bit unsigned integer (4 components) */
    RGBA32_UINT,
    /** 128-bit signed integer (4 components) */
    RGBA32_SINT,
    /** 128-bit float (4 components) */
    RGBA32_FLOAT,
    
    // ===== 16-bit per component (4 components) =====
    
    /** 64-bit unsigned normalized (4 components) */
    RGBA16_UNORM,
    /** 64-bit signed normalized (4 components) */
    RGBA16_SNORM,
    /** 64-bit unsigned integer (4 components) */
    RGBA16_UINT,
    /** 64-bit signed integer (4 components) */
    RGBA16_SINT,
    /** 64-bit float (4 components) */
    RGBA16_FLOAT
}
```

---

## ✅ CHECKLIST PHASE 1.2

### Types de Base
- [ ] `Bytes` (typealias UInt)
- [ ] `ScalarKind` (BOOL, SINT, UINT, FLOAT, ABSTRACT_INT)
- [ ] `VectorSize` (BI, TRI, QUAD)

### Espaces et Classes
- [ ] `AddressSpace` (FUNCTION, PRIVATE, WORKGROUP, UNIFORM, STORAGE, HANDLE, IMMEDIATE, TASK_PAYLOAD, RAY_PAYLOAD, INCOMING_RAY_PAYLOAD)
- [ ] `ImageClass` (DEPTH, COLOR, STORAGE)
- [ ] `ImageDimension` (ONE_D, TWO_D, THREE_D, CUBE, RECT, EXTERNAL, SUBPASS)
- [ ] `ImageAccess` (LOAD, STORE, LOAD_STORE)
- [ ] `VariableClass` (UNIFORM, STORAGE, WORKGROUP, PRIVATE)

### Binding et I/O
- [ ] `Binding` (BuiltIn, Location, Resource)
- [ ] `BuiltIn` (toutes les 50+ valeurs)

### Stages et Tests
- [ ] `ShaderStage` (VERTEX, TASK, MESH, FRAGMENT, COMPUTE, RAY_GENERATION, MISS, ANY_HIT, CLOSEST_HIT)
- [ ] `EarlyDepthTest` (FORCE, ALLOW)
- [ ] `ConservativeDepth` (GREATER_EQUAL, LESS_EQUAL, UNCHANGED)
- [ ] `MeshOutputTopology` (POINT, LINE, TRIANGLE)

### Opérateurs et Fonctions
- [ ] `UnaryOperator` (NEGATE, NOT)
- [ ] `BinaryOperator` (ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO, EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL, LOGICAL_AND, LOGICAL_OR, BITWISE_AND, BITWISE_OR, BITWISE_XOR, SHIFT_LEFT, SHIFT_RIGHT)
- [ ] `RelationalFunction` (EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL)
- [ ] `MathFunction` (toutes les 50+ fonctions)
- [ ] `AtomicFunction` (ADD, SUBTRACT, MIN, MAX, AND, OR, XOR, EXCHANGE, COMPARE_EXCHANGE_WEAK, LOAD, STORE)

### Autres Types
- [ ] `ArraySize` (Constant, Dynamic, UN_SIZED)
- [ ] `StorageAccess` (LOAD, STORE, LOAD_STORE)
- [ ] `SwizzleComponent` (X, Y, Z, W, R, G, B, A)
- [ ] `DerivativeAxis` (X, Y)
- [ ] `SampleLevel` (ZERO, MIPMAP, GRADIENT, AUTOMATIC)

### Storage Format
- [ ] `StorageFormat` (toutes les 50+ valeurs)

### Tests
- [ ] Tests pour tous les enums (valeurs correctes, sérialisation)
- [ ] Tests pour les conversions entre types
- [ ] Tests de sérialisation/désérialisation

### Documentation
- [ ] KDoc pour tous les enums
- [ ] KDoc pour toutes les valeurs des enums
- [ ] Documentation des correspondances avec WGSL/GLSL/HLSL/MSL

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Implémenter ScalarKind, VectorSize | 0.5 jour | Aucune | [ ] |
| Implémenter AddressSpace | 0.5 jour | Aucune | [ ] |
| Implémenter ImageClass, ImageDimension, ImageAccess | 1 jour | Aucune | [ ] |
| Implémenter Binding et BuiltIn | 2 jours | Aucune | [ ] |
| Implémenter ShaderStage | 0.5 jour | Aucune | [ ] |
| Implémenter EarlyDepthTest, ConservativeDepth | 0.5 jour | Aucune | [ ] |
| Implémenter UnaryOperator, BinaryOperator | 0.5 jour | Aucune | [ ] |
| Implémenter RelationalFunction | 0.5 jour | Aucune | [ ] |
| Implémenter MathFunction (50+ valeurs) | 1 jour | Aucune | [ ] |
| Implémenter AtomicFunction | 0.5 jour | Aucune | [ ] |
| Implémenter ArraySize, SampleLevel, SwizzleComponent, DerivativeAxis | 0.5 jour | Expression (forward ref) | [ ] |
| Implémenter StorageFormat (50+ valeurs) | 1 jour | Aucune | [ ] |
| Implémenter les autres enums | 0.5 jour | Aucune | [ ] |
| Écrire tests unitaires | 2 jours | Tout le code | [ ] |
| Ajouter documentation | 1 jour | Tout le code | [ ] |
| Validation manuelle | 0.5 jour | Tout le code | [ ] |

**Total estimé** : **1-2 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichier Kotlin** : `Types.kt` avec tous les enums
2. **Fichier Kotlin** : `StorageFormat.kt` avec tous les formats
3. **Tests unitaires** pour tous les types
4. **Couverture de test** : > 95%
5. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (lignes 536-816)
- **BuiltIn** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 403)
- **ShaderStage** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 323)
- **StorageFormat** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 754)
- **BinaryOperator** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 1278)
- **MathFunction** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 1362)

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Implémenter tous les enums dans `Types.kt`
2. [ ] Implémenter `StorageFormat` dans son propre fichier
3. [ ] Écrire les tests unitaires
4. [ ] Valider la sérialisation
5. [ ] Passer à `03_span-diagnostics.md`

**Fichier suivant** : `03_span-diagnostics.md`
