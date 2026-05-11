# 🏗️ Phase 1 : Structures IR - Core

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-core`  
**Phase** : 1 - Fondations  
**Durée** : 4-6 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

---

## 📋 OBJECTIFS DE LA PHASE

Implémenter **toutes les structures de données** de la Représentation Intermédiaire (IR) de Naga en Kotlin.

**Livrable principal** : Un module `naga-core` fonctionnel avec toutes les classes de base.

---

## 🎯 STRUCTURES À IMPLÉMENTER

### 1. **Module** (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` ligne 2844)

Le **Module** est la **racine** de la représentation d'un shader.

#### Rust (Original)
```rust
#[derive(Debug, Default, Clone, Serialize, Deserialize, Arbitrary)]
pub struct Module {
    pub types: UniqueArena<Type>,
    pub special_types: SpecialTypes,
    pub constants: Arena<Constant>,
    pub overrides: Arena<Override>,
    pub global_variables: Arena<GlobalVariable>,
    pub global_expressions: Arena<Expression>,
    pub functions: Arena<Function>,
    pub entry_points: Vec<EntryPoint>,
    pub diagnostic_filters: Arena<DiagnosticFilterNode>,
    pub diagnostic_filter_leaf: Option<Handle<DiagnosticFilterNode>>,
    pub doc_comments: Option<Box<DocComments>>,
}
```

#### Kotlin (À Implémenter)
```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Module.kt

@Serializable
data class Module(
    /** Arena de types (avec déduplication automatique) */
    val types: UniqueArena<Type> = UniqueArena(),
    
    /** Types spéciaux (vec2<f32>, mat4x4<f32>, etc.) */
    val specialTypes: SpecialTypes = SpecialTypes(),
    
    /** Constantes globales */
    val constants: Arena<Constant> = Arena(),
    
    /** Constantes overrideables (pipeline overrides) */
    val overrides: Arena<Override> = Arena(),
    
    /** Variables globales */
    val globalVariables: Arena<GlobalVariable> = Arena(),
    
    /** Expressions globales (constantes et overrides) */
    val globalExpressions: Arena<Expression> = Arena(),
    
    /** Fonctions */
    val functions: Arena<Function> = Arena(),
    
    /** Points d'entrée (vertex, fragment, compute, etc.) */
    val entryPoints: MutableList<EntryPoint> = mutableListOf(),
    
    /** Filtres de diagnostic (pour @diagnostic) */
    val diagnosticFilters: Arena<DiagnosticFilterNode> = Arena(),
    
    /** Racine de l'arbre des filtres de diagnostic */
    val diagnosticFilterLeaf: Handle<DiagnosticFilterNode>? = null,
    
    /** Commentaires de documentation */
    val docComments: DocComments? = null
) {
    // Builder pattern pour construction mutable
    class Builder {
        private val types = UniqueArena<Type>()
        private val specialTypes = SpecialTypes()
        private val constants = Arena<Constant>()
        private val overrides = Arena<Override>()
        private val globalVariables = Arena<GlobalVariable>()
        private val globalExpressions = Arena<Expression>()
        private val functions = Arena<Function>()
        private val entryPoints = mutableListOf<EntryPoint>()
        private val diagnosticFilters = Arena<DiagnosticFilterNode>()
        private var diagnosticFilterLeaf: Handle<DiagnosticFilterNode>? = null
        private var docComments: DocComments? = null
        
        // Ajouter un type (retourne un Handle)
        fun addType(type: Type): Handle<Type> = types.append(type)
        
        // Ajouter une constante
        fun addConstant(constant: Constant): Handle<Constant> = constants.append(constant)
        
        // Ajouter une variable globale
        fun addGlobalVariable(variable: GlobalVariable): Handle<GlobalVariable> = 
            globalVariables.append(variable)
        
        // Ajouter une expression globale
        fun addGlobalExpression(expression: Expression): Handle<Expression> = 
            globalExpressions.append(expression)
        
        // Ajouter une fonction
        fun addFunction(function: Function): Handle<Function> = functions.append(function)
        
        // Ajouter un point d'entrée
        fun addEntryPoint(entryPoint: EntryPoint) {
            entryPoints.add(entryPoint)
        }
        
        // Construire le module
        fun build(): Module = Module(
            types = types,
            specialTypes = specialTypes,
            constants = constants,
            overrides = overrides,
            globalVariables = globalVariables,
            globalExpressions = globalExpressions,
            functions = functions,
            entryPoints = entryPoints,
            diagnosticFilters = diagnosticFilters,
            diagnosticFilterLeaf = diagnosticFilterLeaf,
            docComments = docComments
        )
    }
    
    companion object {
        fun builder(): Builder = Builder()
    }
}
```

#### Points Clés
- [ ] **Immutable par défaut** : `data class` avec `val`
- [ ] **Builder pattern** : Pour construction mutable
- [ ] **@Serializable** : Pour sérialisation (kotlinx.serialization)
- [ ] **Default values** : Toutes les propriétés ont des valeurs par défaut

---

### 2. **Type et TypeInner** (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` ligne 846)

Les **types** sont au cœur du système IR.

#### Rust (Original)
```rust
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize, Arbitrary)]
pub struct Type {
    pub inner: TypeInner,
    pub span: Span,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize, Arbitrary)]
pub enum TypeInner {
    Scalar { kind: ScalarKind, width: Bytes },
    Vector { size: VectorSize, inner: Handle<Type> },
    Matrix { columns: VectorSize, rows: VectorSize, inner: Handle<Type> },
    Array { inner: Handle<Type>, size: ArraySize },
    Struct { members: Vec<StructMember>, span: Span },
    Pointer { base: Handle<Type>, space: AddressSpace },
    ValuePointer { size: Option<Bytes>, kind: ScalarKind, width: Bytes, space: AddressSpace },
    Atomic { inner: Handle<Type> },
    Image { class: ImageClass, dim: ImageDimension, format: Option<StorageFormat>, access: ImageAccess },
    Sampler { comparison: bool },
    AccelerationStructure,
    // ... autres variants
}
```

#### Kotlin (À Implémenter)
```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Type.kt

@Serializable
data class Type(
    /** Le type interne (Scalar, Vector, Matrix, etc.) */
    val inner: TypeInner,
    /** Position dans le code source */
    val span: Span = Span.default
)

@Serializable
sealed class TypeInner {
    // ===== Types de base =====
    
    @Serializable
    data class Scalar(
        val kind: ScalarKind,
        val width: Bytes
    ) : TypeInner()
    
    @Serializable
    data class Vector(
        val size: VectorSize,
        val inner: Handle<Type>
    ) : TypeInner()
    
    @Serializable
    data class Matrix(
        val columns: VectorSize,
        val rows: VectorSize,
        val inner: Handle<Type>
    ) : TypeInner()
    
    // ===== Types composés =====
    
    @Serializable
    data class Array(
        val inner: Handle<Type>,
        val size: ArraySize
    ) : TypeInner()
    
    @Serializable
    data class Struct(
        val members: List<StructMember>,
        val span: Span = Span.default
    ) : TypeInner()
    
    // ===== Types pointeurs =====
    
    @Serializable
    data class Pointer(
        val base: Handle<Type>,
        val space: AddressSpace
    ) : TypeInner()
    
    @Serializable
    data class ValuePointer(
        val size: Bytes?,
        val kind: ScalarKind,
        val width: Bytes,
        val space: AddressSpace
    ) : TypeInner()
    
    // ===== Types spéciaux =====
    
    @Serializable
    data class Atomic(
        val inner: Handle<Type>
    ) : TypeInner()
    
    @Serializable
    data class Image(
        val class: ImageClass,
        val dim: ImageDimension,
        val format: StorageFormat? = null,
        val access: ImageAccess = ImageAccess.LOAD
    ) : TypeInner()
    
    @Serializable
    data class Sampler(
        val comparison: Boolean = false
    ) : TypeInner()
    
    @Serializable
    object AccelerationStructure : TypeInner()
    
    @Serializable
    object BindgenGroup : TypeInner()
}

// Type alias pour simplifier
@Serializable
@JvmInline
value class Handle<T>(val index: Int)
```

#### Types Associés

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Types.kt

/** Taille en bytes (1, 2, 4, 8) */
typealias Bytes = UInt

/** Kind de scalaire */
@Serializable
enum class ScalarKind {
    BOOL,
    SINT,   // Signed integer
    UINT,   // Unsigned integer
    FLOAT,
    ABSTRACT_INT  // Abstract integer (pour compute shaders)
}

/** Taille de vecteur */
@Serializable
enum class VectorSize {
    BI,     // 2 components
    TRI,    // 3 components
    QUAD    // 4 components
}

/** Espace d'adressage */
@Serializable
enum class AddressSpace {
    FUNCTION,
    PRIVATE,
    WORKGROUP,
    UNIFORM,
    STORAGE,
    HANDLE,
    IMMEDIATE,
    TASK_PAYLOAD,
    RAY_PAYLOAD,
    INCOMING_RAY_PAYLOAD
}

/** Classe d'image */
@Serializable
enum class ImageClass {
    DEPTH,
    COLOR,
    STORAGE
}

/** Dimension d'image */
@Serializable
enum class ImageDimension {
    ONE_D,
    TWO_D,
    THREE_D,
    CUBE,
    RECT,
    EXTERNAL,
    SUBPASS
}

/** Accès image */
@Serializable
enum class ImageAccess {
    LOAD,
    STORE,
    LOAD_STORE
}

/** Format de stockage */
@Serializable
enum class StorageFormat {
    R8_UNORM, R8_SNORM, R8_UINT, R8_SINT,
    R16_UINT, R16_SINT, R16_FLOAT,
    RG8_UNORM, RG8_SNORM, RG8_UINT, RG8_SINT,
    R32_UINT, R32_SINT, R32_FLOAT,
    RG16_UINT, RG16_SINT, RG16_FLOAT,
    RGBA8_UNORM, RGBA8_SNORM, RGBA8_UINT, RGBA8_SINT,
    BGRA8_UNORM,
    RGB10A2_UINT, RGB10A2_UNORM,
    RG11B10_UFLOAT,
    // ... tous les formats
}

/** Taille de tableau */
@Serializable
sealed class ArraySize {
    @Serializable
    data class Constant(val value: UInt) : ArraySize()
    
    @Serializable
    data class Dynamic(val expression: Handle<Expression>) : ArraySize()
    
    @Serializable
    object Un-sized : ArraySize()
}

/** Membre de struct */
@Serializable
data class StructMember(
    val name: String? = null,
    val ty: Handle<Type>,
    val binding: Binding? = null,
    val offset: UInt = 0u
)

/** Binding pour I/O */
@Serializable
sealed class Binding {
    @Serializable
    data class BuiltIn(val builtin: BuiltIn) : Binding()
    
    @Serializable
    data class Location(val location: UInt) : Binding()
    
    @Serializable
    data class Resource(
        val group: UInt,
        val binding: UInt
    ) : Binding()
}

/** Built-in variables */
@Serializable
enum class BuiltIn {
    PRIMITIVE_INDEX,
    POSITION,
    VIEW_INDEX,
    BASE_INSTANCE,
    BASE_VERTEX,
    CLIP_DISTANCES,
    CULL_DISTANCE,
    INSTANCE_INDEX,
    POINT_SIZE,
    VERTEX_INDEX,
    DRAW_INDEX,
    FRAG_DEPTH,
    POINT_COORD,
    FRONT_FACING,
    BARYCENTRIC,
    SAMPLE_INDEX,
    SAMPLE_MASK,
    GLOBAL_INVOCATION_ID,
    LOCAL_INVOCATION_ID,
    LOCAL_INVOCATION_INDEX,
    WORKGROUP_ID,
    WORKGROUP_SIZE,
    NUM_WORKGROUPS,
    NUM_SUBGROUPS,
    SUBGROUP_ID,
    SUBGROUP_SIZE,
    SUBGROUP_INVOCATION_ID,
    MESH_TASK_SIZE,
    CULL_PRIMITIVE,
    POINT_INDEX,
    LINE_INDICES,
    TRIANGLE_INDICES,
    VERTEX_COUNT,
    VERTICES,
    PRIMITIVE_COUNT,
    PRIMITIVES,
    // ... ray tracing built-ins
}
```

---

### 3. **Expression** (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` ligne 1619)

Les **expressions** représentent des valeurs calculées.

#### Rust (Original) - Extrait
```rust
#[derive(Debug, Clone, Serialize, Deserialize, Arbitrary)]
pub enum Expression {
    Literal(Literal),
    Constant(Handle<Constant>),
    ZeroValue(Handle<Type>),
    Compose { ty: Handle<Type>, components: Vec<Handle<Expression>> },
    Access { base: Handle<Expression>, index: Handle<Expression> },
    AccessIndex { base: Handle<Expression>, index: u32 },
    Splat { size: VectorSize, value: Handle<Expression> },
    Swizzle { size: VectorSize, vector: Handle<Expression>, pattern: [SwizzleComponent; 4] },
    FunctionArgument(u32),
    GlobalVariable(Handle<GlobalVariable>),
    LocalVariable(Handle<LocalVariable>),
    Load { pointer: Handle<Expression> },
    ImageSample { /* ... */ },
    ImageLoad { /* ... */ },
    Unary { op: UnaryOperator, expr: Handle<Expression> },
    Binary { op: BinaryOperator, left: Handle<Expression>, right: Handle<Expression> },
    Select { condition: Handle<Expression>, accept: Handle<Expression>, reject: Handle<Expression> },
    // ... 60+ variants
}
```

#### Kotlin (À Implémenter)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Expression.kt

@Serializable
sealed class Expression {
    // ===== Valeurs littérales et constantes =====
    
    @Serializable
    data class Literal(val value: Literal) : Expression()
    
    @Serializable
    data class Constant(val handle: Handle<Constant>) : Expression()
    
    @Serializable
    data class Override(val handle: Handle<Override>) : Expression()
    
    @Serializable
    data class ZeroValue(val ty: Handle<Type>) : Expression()
    
    // ===== Construction de valeurs =====
    
    @Serializable
    data class Compose(
        val ty: Handle<Type>,
        val components: List<Handle<Expression>>
    ) : Expression()
    
    // ===== Accès aux éléments =====
    
    @Serializable
    data class Access(
        val base: Handle<Expression>,
        val index: Handle<Expression>
    ) : Expression()
    
    @Serializable
    data class AccessIndex(
        val base: Handle<Expression>,
        val index: UInt
    ) : Expression()
    
    @Serializable
    data class Splat(
        val size: VectorSize,
        val value: Handle<Expression>
    ) : Expression()
    
    @Serializable
    data class Swizzle(
        val size: VectorSize,
        val vector: Handle<Expression>,
        val pattern: List<SwizzleComponent>  // [x, y, z, w] pour vec4
    ) : Expression()
    
    // ===== Références aux variables =====
    
    @Serializable
    data class FunctionArgument(val index: UInt) : Expression()
    
    @Serializable
    data class GlobalVariable(val handle: Handle<GlobalVariable>) : Expression()
    
    @Serializable
    data class LocalVariable(val handle: Handle<LocalVariable>) : Expression()
    
    // ===== Chargement de valeurs =====
    
    @Serializable
    data class Load(val pointer: Handle<Expression>) : Expression()
    
    // ===== Échantillonnage d'images =====
    
    @Serializable
    data class ImageSample(
        val image: Handle<Expression>,
        val sampler: Handle<Expression>,
        val coordinate: Handle<Expression>,
        val level: SampleLevel = SampleLevel.ZERO,
        val depthRef: Handle<Expression>? = null,
        val arrayIndex: Handle<Expression>? = null,
        val offset: Handle<Expression>? = null,
        val clampToEdge: Boolean = false,
        val gather: SwizzleComponent? = null
    ) : Expression()
    
    @Serializable
    data class ImageLoad(
        val image: Handle<Expression>,
        val coordinate: Handle<Expression>,
        val level: SampleLevel = SampleLevel.ZERO,
        val arrayIndex: Handle<Expression>? = null,
        val sample: Handle<Expression>? = null
    ) : Expression()
    
    // ===== Opérations unaires =====
    
    @Serializable
    data class Unary(
        val op: UnaryOperator,
        val expr: Handle<Expression>
    ) : Expression()
    
    // ===== Opérations binaires =====
    
    @Serializable
    data class Binary(
        val op: BinaryOperator,
        val left: Handle<Expression>,
        val right: Handle<Expression>
    ) : Expression()
    
    // ===== Sélection conditionnelle =====
    
    @Serializable
    data class Select(
        val condition: Handle<Expression>,
        val accept: Handle<Expression>,
        val reject: Handle<Expression>
    ) : Expression()
    
    // ===== Opérations relationnelles =====
    
    @Serializable
    data class Relational(
        val func: RelationalFunction,
        val left: Handle<Expression>,
        val right: Handle<Expression>
    ) : Expression()
    
    // ===== Fonctions mathématiques =====
    
    @Serializable
    data class Math(
        val func: MathFunction,
        val args: List<Handle<Expression>>
    ) : Expression()
    
    // ===== Conversion de types =====
    
    @Serializable
    data class As(
        val expr: Handle<Expression>,
        val target: Handle<Type>,
        val convert: Boolean  // true = cast, false = bitcast
    ) : Expression()
    
    // ===== Dérivées =====
    
    @Serializable
    data class Derivative(
        val axis: DerivativeAxis,
        val expr: Handle<Expression>
    ) : Expression()
    
    // ===== Résultat d'appel de fonction =====
    
    @Serializable
    data class CallResult(val function: Handle<Function>) : Expression()
    
    // ===== Atomic operations =====
    
    @Serializable
    data class AtomicResult(val function: AtomicFunction) : Expression()
    
    // ... autres variants selon besoins
    
    // ===== Visitor Pattern =====
    
    abstract fun <T> accept(visitor: ExpressionVisitor<T>): T
}

// Visitor interface pour pattern matching
interface ExpressionVisitor<T> {
    fun visitLiteral(expr: Expression.Literal): T
    fun visitConstant(expr: Expression.Constant): T
    fun visitOverride(expr: Expression.Override): T
    fun visitZeroValue(expr: Expression.ZeroValue): T
    fun visitCompose(expr: Expression.Compose): T
    fun visitAccess(expr: Expression.Access): T
    fun visitAccessIndex(expr: Expression.AccessIndex): T
    fun visitSplat(expr: Expression.Splat): T
    fun visitSwizzle(expr: Expression.Swizzle): T
    fun visitFunctionArgument(expr: Expression.FunctionArgument): T
    fun visitGlobalVariable(expr: Expression.GlobalVariable): T
    fun visitLocalVariable(expr: Expression.LocalVariable): T
    fun visitLoad(expr: Expression.Load): T
    fun visitImageSample(expr: Expression.ImageSample): T
    fun visitImageLoad(expr: Expression.ImageLoad): T
    fun visitUnary(expr: Expression.Unary): T
    fun visitBinary(expr: Expression.Binary): T
    fun visitSelect(expr: Expression.Select): T
    fun visitRelational(expr: Expression.Relational): T
    fun visitMath(expr: Expression.Math): T
    fun visitAs(expr: Expression.As): T
    fun visitDerivative(expr: Expression.Derivative): T
    fun visitCallResult(expr: Expression.CallResult): T
    fun visitAtomicResult(expr: Expression.AtomicResult): T
}

// Implémentation du visitor pour Expression
fun Expression.accept(visitor: ExpressionVisitor<Nothing>): Nothing {
    return when (this) {
        is Expression.Literal -> visitor.visitLiteral(this)
        is Expression.Constant -> visitor.visitConstant(this)
        is Expression.Override -> visitor.visitOverride(this)
        is Expression.ZeroValue -> visitor.visitZeroValue(this)
        is Expression.Compose -> visitor.visitCompose(this)
        is Expression.Access -> visitor.visitAccess(this)
        is Expression.AccessIndex -> visitor.visitAccessIndex(this)
        is Expression.Splat -> visitor.visitSplat(this)
        is Expression.Swizzle -> visitor.visitSwizzle(this)
        is Expression.FunctionArgument -> visitor.visitFunctionArgument(this)
        is Expression.GlobalVariable -> visitor.visitGlobalVariable(this)
        is Expression.LocalVariable -> visitor.visitLocalVariable(this)
        is Expression.Load -> visitor.visitLoad(this)
        is Expression.ImageSample -> visitor.visitImageSample(this)
        is Expression.ImageLoad -> visitor.visitImageLoad(this)
        is Expression.Unary -> visitor.visitUnary(this)
        is Expression.Binary -> visitor.visitBinary(this)
        is Expression.Select -> visitor.visitSelect(this)
        is Expression.Relational -> visitor.visitRelational(this)
        is Expression.Math -> visitor.visitMath(this)
        is Expression.As -> visitor.visitAs(this)
        is Expression.Derivative -> visitor.visitDerivative(this)
        is Expression.CallResult -> visitor.visitCallResult(this)
        is Expression.AtomicResult -> visitor.visitAtomicResult(this)
    }
}
```

#### Types Associés à Expression

```kotlin
// Opérateurs unaires
@Serializable
enum class UnaryOperator {
    NEGATE,       // - (négation numérique)
    NOT           // ! (négation logique)
}

// Opérateurs binaires
@Serializable
enum class BinaryOperator {
    ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO,
    EQUAL, NOT_EQUAL,
    LESS, LESS_EQUAL, GREATER, GREATER_EQUAL,
    LOGICAL_AND, LOGICAL_OR,
    BITWISE_AND, BITWISE_OR, BITWISE_XOR,
    SHIFT_LEFT, SHIFT_RIGHT
}

// Fonctions relationnelles
@Serializable
enum class RelationalFunction {
    EQUAL,
    NOT_EQUAL,
    LESS,
    LESS_EQUAL,
    GREATER,
    GREATER_EQUAL
}

// Fonctions mathématiques
@Serializable
enum class MathFunction {
    // Math européens
    ABS, ACOS, ACOSH, ASIN, ASINH, ATAN, ATAN2, ATANH,
    CEIL, CLAMP, COS, COSH, CROSS, DEGREES, DETERMINANT,
    DISTANCE, DOT, EXP, EXP2, FACE_FORWARD, FLOOR, FRAC_EXP_BIAS,
    FREXP, LENGTH, LERP, LOG, LOG2, MAX, MIN, MIX,
    MODF, NORMALIZE, POW, RADIANS, REFLECT, REFRACT, ROUND,
    SIGNOF, SIN, SINH, SMOOTHSTEP, SQRT, STEP, TAN, TANH, TRANSPOSE,
    TRUNC,
    
    // Atomics
    ATOMIC_ADD, ATOMIC_COMPARE_EXCHANGE_WEAK, ATOMIC_EXCHANGE,
    ATOMIC_LOAD, ATOMIC_MAX, ATOMIC_MIN, ATOMIC_OR, ATOMIC_AND,
    ATOMIC_STORE, ATOMIC_XOR,
    
    // Counting
    COUNT_LEADING_ZEROS_I32, COUNT_LEADING_ZEROS_I64,
    COUNT_ONE_BITS_I32, COUNT_ONE_BITS_I64,
    COUNT_TRAILING_ZEROS_I32, COUNT_TRAILING_ZEROS_I64,
    FIRST_LEADING_BIT_I32, FIRST_LEADING_BIT_I64,
    FIRST_TRAILING_BIT_I32, FIRST_TRAILING_BIT_I64,
    
    // Packing
    PACK2X16_FLOAT, PACK2X16_SNORM, PACK2X16_UNORM,
    PACK4X8_SNORM, PACK4X8_UNORM,
    UNPACK2X16_FLOAT, UNPACK2X16_SNORM, UNPACK2X16_UNORM,
    UNPACK4X8_SNORM, UNPACK4X8_UNORM
}

// Composantes de swizzle
@Serializable
enum class SwizzleComponent {
    X, Y, Z, W, R, G, B, A
}

// Niveau d'échantillonnage
@Serializable
sealed class SampleLevel {
    @Serializable
    object ZERO : SampleLevel()
    
    @Serializable
    data class Mipmap(val level: Handle<Expression>) : SampleLevel()
    
    @Serializable
    data class Gradient(
        val x: Handle<Expression>,
        val y: Handle<Expression>
    ) : SampleLevel()
    
    @Serializable
    data class Automatic(val level: Handle<Expression>) : SampleLevel()
}

// Axe de dérivée
@Serializable
enum class DerivativeAxis {
    X, Y
}

// Fonctions atomiques
@Serializable
enum class AtomicFunction {
    ADD, SUBTRACT, MIN, MAX, AND, OR, XOR, EXCHANGE, COMPARE_EXCHANGE_WEAK, LOAD, STORE
}
```

---

### 4. **Statement** (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` ligne 2052)

Les **statements** représentent des instructions exécutables.

#### Rust (Original) - Extrait
```rust
#[derive(Debug, Clone, Serialize, Deserialize, Arbitrary)]
pub enum Statement {
    Emit(Range<Expression>),
    Block(Block),
    If { condition: Handle<Expression>, accept: Block, reject: Block },
    Switch { selector: Handle<Expression>, cases: Vec<SwitchCase> },
    Loop { body: Block, continuing: Block, break_if: Option<Handle<Expression>> },
    Break,
    Continue,
    Return { value: Option<Handle<Expression>> },
    Kill,
    Store { pointer: Handle<Expression>, value: Handle<Expression> },
    Call { function: Handle<Function>, arguments: Vec<Handle<Expression>>, result: Option<Handle<Expression>> },
    Atomic { pointer: Handle<Expression>, func: AtomicFunction, operands: Vec<Handle<Expression>>, result: Handle<Expression> },
    // ... autres variants
}
```

#### Kotlin (À Implémenter)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Statement.kt

@Serializable
sealed class Statement {
    // ===== Émission d'expressions =====
    
    @Serializable
    data class Emit(val range: Range<Expression>) : Statement()
    
    // ===== Blocs de code =====
    
    @Serializable
    data class Block(val body: List<Statement>) : Statement()
    
    // ===== Contrôle conditionnel =====
    
    @Serializable
    data class If(
        val condition: Handle<Expression>,
        val accept: Block,
        val reject: Block
    ) : Statement()
    
    @Serializable
    data class Switch(
        val selector: Handle<Expression>,
        val cases: List<SwitchCase>
    ) : Statement()
    
    // ===== Boucles =====
    
    @Serializable
    data class Loop(
        val body: Block,
        val continuing: Block = Block(emptyList()),
        val breakIf: Handle<Expression>? = null
    ) : Statement()
    
    // ===== Contrôle de boucle =====
    
    @Serializable
    object Break : Statement()
    
    @Serializable
    object Continue : Statement()
    
    // ===== Retour de fonction =====
    
    @Serializable
    data class Return(val value: Handle<Expression>? = null) : Statement()
    
    // ===== Terminaison de shader =====
    
    @Serializable
    object Kill : Statement()
    
    // ===== Stockage en mémoire =====
    
    @Serializable
    data class Store(
        val pointer: Handle<Expression>,
        val value: Handle<Expression>
    ) : Statement()
    
    // ===== Appel de fonction =====
    
    @Serializable
    data class Call(
        val function: Handle<Function>,
        val arguments: List<Handle<Expression>>,
        val result: Handle<Expression>? = null
    ) : Statement()
    
    // ===== Opérations atomiques =====
    
    @Serializable
    data class Atomic(
        val pointer: Handle<Expression>,
        val func: AtomicFunction,
        val operands: List<Handle<Expression>>,
        val result: Handle<Expression>
    ) : Statement()
    
    // ===== Ray Query (Ray Tracing) =====
    
    @Serializable
    data class RayQuery(
        val query: Handle<Expression>,
        val proceed: Proceed
    ) : Statement()
    
    // ===== Barrières =====
    
    @Serializable
    data class ControlBarrier(val barrier: Barrier) : Statement()
    
    @Serializable
    data class MemoryBarrier(val barrier: Barrier) : Statement()
    
    // ===== Visitor Pattern =====
    
    abstract fun <T> accept(visitor: StatementVisitor<T>): T
}

// Visitor interface
interface StatementVisitor<T> {
    fun visitEmit(stmt: Statement.Emit): T
    fun visitBlock(stmt: Statement.Block): T
    fun visitIf(stmt: Statement.If): T
    fun visitSwitch(stmt: Statement.Switch): T
    fun visitLoop(stmt: Statement.Loop): T
    fun visitBreak(stmt: Statement.Break): T
    fun visitContinue(stmt: Statement.Continue): T
    fun visitReturn(stmt: Statement.Return): T
    fun visitKill(stmt: Statement.Kill): T
    fun visitStore(stmt: Statement.Store): T
    fun visitCall(stmt: Statement.Call): T
    fun visitAtomic(stmt: Statement.Atomic): T
    fun visitRayQuery(stmt: Statement.RayQuery): T
    fun visitControlBarrier(stmt: Statement.ControlBarrier): T
    fun visitMemoryBarrier(stmt: Statement.MemoryBarrier): T
}

// Implémentation du visitor
fun Statement.accept(visitor: StatementVisitor<Nothing>): Nothing {
    return when (this) {
        is Statement.Emit -> visitor.visitEmit(this)
        is Statement.Block -> visitor.visitBlock(this)
        is Statement.If -> visitor.visitIf(this)
        is Statement.Switch -> visitor.visitSwitch(this)
        is Statement.Loop -> visitor.visitLoop(this)
        is Statement.Break -> visitor.visitBreak(this)
        is Statement.Continue -> visitor.visitContinue(this)
        is Statement.Return -> visitor.visitReturn(this)
        is Statement.Kill -> visitor.visitKill(this)
        is Statement.Store -> visitor.visitStore(this)
        is Statement.Call -> visitor.visitCall(this)
        is Statement.Atomic -> visitor.visitAtomic(this)
        is Statement.RayQuery -> visitor.visitRayQuery(this)
        is Statement.ControlBarrier -> visitor.visitControlBarrier(this)
        is Statement.MemoryBarrier -> visitor.visitMemoryBarrier(this)
    }
}
```

#### Types Associés à Statement

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/ControlFlow.kt

@Serializable
sealed class SwitchValue {
    @Serializable
    data class Default(val value: Int) : SwitchValue()
    
    @Serializable
    data class Literal(val value: Int) : SwitchValue()
}

@Serializable
data class SwitchCase(
    val value: SwitchValue,
    val body: Block
)

@Serializable
data class Block(
    val statements: List<Statement> = emptyList()
)

// Pour Ray Query
@Serializable
sealed class Proceed {
    @Serializable
    object Continue : Proceed()
    
    @Serializable
    data class Result(val value: Handle<Expression>) : Proceed()
}

// Barrières
bitflags::bitflags! {
    // Rust utilise bitflags, en Kotlin on utilise des flags
}

// En Kotlin, on peut utiliser un Int avec des bits
@Serializable
data class Barrier(
    val flags: Int = 0  // Bitmask de MemoryAccess
)

// Memory Access flags
object MemoryAccess {
    const val NONE = 0
    const val WORKGROUP_MEMORY = 1 shl 0
    const val STORAGE_MEMORY = 1 shl 1
    const val UNIFORM_MEMORY = 1 shl 2
}
```

---

### 5. **Function et EntryPoint**

#### Function (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` ligne 2444)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Function.kt

@Serializable
data class Function(
    val name: String? = null,
    val arguments: List<FunctionArgument> = emptyList(),
    val result: FunctionResult? = null,
    val localVariables: Arena<LocalVariable> = Arena(),
    val expressions: Arena<Expression> = Arena(),
    val namedExpressions: Map<Handle<Expression>, String> = emptyMap(),
    val body: Block = Block(emptyList()),
    val diagnosticFilterLeaf: Handle<DiagnosticFilterNode>? = null
)

@Serializable
data class FunctionArgument(
    val name: String? = null,
    val ty: Handle<Type>,
    val binding: Binding? = null
)

@Serializable
data class FunctionResult(
    val ty: Handle<Type>,
    val binding: Binding? = null
)

@Serializable
data class LocalVariable(
    val name: String? = null,
    val ty: Handle<Type>,
    val init: Handle<Expression>? = null
)
```

#### EntryPoint (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` ligne 2534)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/EntryPoint.kt

@Serializable
data class EntryPoint(
    val name: String,
    val stage: ShaderStage,
    val earlyDepthTest: EarlyDepthTest? = null,
    val workgroupSize: List<UInt> = listOf(1u, 1u, 1u),  // [x, y, z]
    val workgroupSizeOverrides: List<Handle<Expression>?> = listOf(null, null, null),
    val function: Function,
    val meshInfo: MeshStageInfo? = null,
    val taskPayload: Handle<GlobalVariable>? = null,
    val incomingRayPayload: Handle<GlobalVariable>? = null
)

@Serializable
sealed class EarlyDepthTest {
    @Serializable
    object Force : EarlyDepthTest()
    
    @Serializable
    data class Allow(val conservative: ConservativeDepth) : EarlyDepthTest()
}

@Serializable
enum class ConservativeDepth {
    GREATER_EQUAL,
    LESS_EQUAL,
    UNCHANGED
}

@Serializable
data class MeshStageInfo(
    val maxVertices: UInt,
    val maxPrimitives: UInt,
    val outputTopology: MeshOutputTopology
)

@Serializable
enum class MeshOutputTopology {
    POINT,
    LINE,
    TRIANGLE
}
```

---

### 6. **Constant et Override**

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Constant.kt

@Serializable
data class Constant(
    val name: String? = null,
    val specialization: UInt = 0u,
    val value: Handle<Expression>,
    val span: Span = Span.default
)

@Serializable
data class Override(
    val name: String? = null,
    val value: Handle<Expression>,
    val span: Span = Span.default
)
```

---

### 7. **GlobalVariable**

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/GlobalVariable.kt

@Serializable
sealed class ResourceBinding {
    @Serializable
    data class Buffer(
        val index: UInt,
        val binding: Binding
    ) : ResourceBinding()
    
    @Serializable
    data class Texture(
        val index: UInt,
        val binding: Binding
    ) : ResourceBinding()
    
    @Serializable
    data class Sampler(
        val index: UInt,
        val binding: Binding
    ) : ResourceBinding()
}

@Serializable
sealed class StorageAccess {
    @Serializable
    object LOAD : StorageAccess()
    
    @Serializable
    object STORE : StorageAccess()
    
    @Serializable
    object LOAD_STORE : StorageAccess()
}

@Serializable
data class GlobalVariable(
    val name: String? = null,
    val class: VariableClass,
    val binding: ResourceBinding? = null,
    val ty: Handle<Type>,
    val init: Handle<Expression>? = null,
    val span: Span = Span.default
)

@Serializable
enum class VariableClass {
    UNIFORM,
    STORAGE,
    WORKGROUP,
    PRIVATE
}
```

---

### 8. **Literal** (Valeurs littérales)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Literal.kt

@Serializable
sealed class Literal {
    // Bool
    @Serializable
    data class Bool(val value: Boolean) : Literal()
    
    // Entiers
    @Serializable
    data class Sint(val value: Int) : Literal()
    
    @Serializable
    data class Uint(val value: UInt) : Literal()
    
    // Flottants
    @Serializable
    data class Float(val value: Float) : Literal()
    
    // Abstract integers (pour compute shaders)
    @Serializable
    data class AbstractInt(val value: Long) : Literal()
    
    // Vecteurs (pour optimisation)
    @Serializable
    data class Vector(
        val components: List<Literal>
    ) : Literal()
}
```

---

## 📦 ARNA SYSTEM (Gestion Mémoire)

### Arena (Référence : `/Users/chaos/RustroverProjects/wgpu/naga/src/arena/mod.rs`)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/arena/Arena.kt

/**
 * Arena pour stocker des éléments de manière efficace.
 * Les éléments sont stockés dans une liste et référencés par un Handle (index).
 */
class Arena<T> : Iterable<T> {
    private val data: MutableList<T> = mutableListOf()
    
    /**
     * Ajoute un élément à l'arena et retourne son Handle.
     */
    fun append(value: T): Handle<T> {
        val index = data.size
        data.add(value)
        return Handle(index)
    }
    
    /**
     * Récupère un élément par son Handle.
     */
    operator fun get(handle: Handle<T>): T {
        return data[handle.index]
    }
    
    /**
     * Nombre d'éléments dans l'arena.
     */
    val size: Int get() = data.size
    
    /**
     * Vérifie si l'arena est vide.
     */
    fun isEmpty(): Boolean = data.isEmpty()
    
    /**
     * Retourne un iterateur sur tous les éléments.
     */
    override fun iterator(): Iterator<T> = data.iterator()
    
    /**
     * Applique une fonction à chaque élément avec son Handle.
     */
    inline fun forEachWithHandle(action: (Handle<T>, T) -> Unit) {
        data.forEachIndexed { index, value ->
            action(Handle(index), value)
        }
    }
    
    /**
     * Filtre les éléments.
     */
    fun filter(predicate: (T) -> Boolean): List<T> = data.filter(predicate)
    
    /**
     * Trouve le premier élément correspondant.
     */
    fun find(predicate: (T) -> Boolean): T? = data.find(predicate)
    
    /**
     * Trouve le Handle du premier élément correspondant.
     */
    fun findHandle(predicate: (T) -> Boolean): Handle<T>? {
        data.forEachIndexed { index, value ->
            if (predicate(value)) return Handle(index)
        }
        return null
    }
}
```

### UniqueArena (Avec déduplication)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/arena/UniqueArena.kt

/**
 * Arena qui garantit l'unicité des éléments (dédoublonnage automatique).
 * Utilisé pour les types (Type) où plusieurs définitions identiques doivent
 * partager le même Handle.
 */
class UniqueArena<T> where T : Equatable {
    private val data: MutableList<T> = mutableListOf()
    private val indexMap: MutableMap<T, Int> = mutableMapOf()
    
    /**
     * Ajoute un élément à l'arena. Si l'élément existe déjà, retourne
     * le Handle existant.
     */
    fun append(value: T): Handle<T> {
        return indexMap.getOrPut(value) {
            val index = data.size
            data.add(value)
            index
        }.let { Handle(it) }
    }
    
    /**
     * Récupère un élément par son Handle.
     */
    operator fun get(handle: Handle<T>): T {
        return data[handle.index]
    }
    
    /**
     * Vérifie si un élément existe déjà.
     */
    fun contains(value: T): Boolean = value in indexMap
    
    /**
     * Trouve le Handle d'un élément existant.
     */
    fun findHandle(value: T): Handle<T>? {
        return indexMap[value]?.let { Handle(it) }
    }
    
    /**
     * Nombre d'éléments uniques dans l'arena.
     */
    val size: Int get() = data.size
    
    /**
     * Vérifie si l'arena est vide.
     */
    fun isEmpty(): Boolean = data.isEmpty()
}

// Interface pour les types qui peuvent être comparés
interface Equatable {
    fun isEquivalentTo(other: Any): Boolean
}

// Implémentation pour Type
fun Type.isEquivalentTo(other: Any): Boolean {
    if (other !is Type) return false
    return this.inner == other.inner
}
```

### Handle (Type-safe wrapper)

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/arena/Handle.kt

/**
 * Handle est un wrapper type-safe autour d'un Int (index dans une Arena).
 * Utilise @JvmInline pour éviter l'allocation d'objet.
 */
@Serializable
@JvmInline
value class Handle<T>(val index: Int) {
    override fun toString(): String = "Handle(${index})"
    
    companion object {
        // Handle invalide (utilisé comme valeur par défaut)
        val INVALID = Handle<Nothing>(-1)
    }
    
    /**
     * Vérifie si le Handle est valide.
     */
    fun isValid(): Boolean = index >= 0
    
    /**
     * Compare deux Handles (même index, peu importe le type générique).
     */
    inline fun <reified U> equalsIgnoreType(other: Handle<U>): Boolean {
        return this.index == other.index
    }
}
```

---

## 📍 SPAN ET DIAGNOSTICS

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/Span.kt

@Serializable
data class Span(
    /**
     * Position de début (ligne, colonne).
     */
    val start: SourceLocation,
    /**
     * Position de fin (ligne, colonne).
     */
    val end: SourceLocation
) {
    companion object {
        val default = Span(
            start = SourceLocation(0, 0),
            end = SourceLocation(0, 0)
        )
    }
}

@Serializable
data class SourceLocation(
    val line: Int,
    val column: Int
) {
    companion object {
        val default = SourceLocation(0, 0)
    }
}

/**
 * Interface pour les éléments qui ont une position source.
 */
interface WithSpan {
    val span: Span
}
```

---

## 📝 SPECIAL TYPES

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/SpecialTypes.kt

/**
 * Types spéciaux qui sont générés automatiquement et stockés ici pour accès rapide.
 */
@Serializable
data class SpecialTypes(
    // Types scalaires
    var bool: Handle<Type>? = null,
    var i8: Handle<Type>? = null,
    var u8: Handle<Type>? = null,
    var i16: Handle<Type>? = null,
    var u16: Handle<Type>? = null,
    var i32: Handle<Type>? = null,
    var u32: Handle<Type>? = null,
    var i64: Handle<Type>? = null,
    var u64: Handle<Type>? = null,
    var f16: Handle<Type>? = null,
    var f32: Handle<Type>? = null,
    var f64: Handle<Type>? = null,
    
    // Types vecteurs (2, 3, 4 components)
    var vec2F32: Handle<Type>? = null,
    var vec3F32: Handle<Type>? = null,
    var vec4F32: Handle<Type>? = null,
    var vec2I32: Handle<Type>? = null,
    var vec3I32: Handle<Type>? = null,
    var vec4I32: Handle<Type>? = null,
    var vec2U32: Handle<Type>? = null,
    var vec3U32: Handle<Type>? = null,
    var vec4U32: Handle<Type>? = null,
    
    // Types matrices
    var mat2x2F32: Handle<Type>? = null,
    var mat2x3F32: Handle<Type>? = null,
    var mat2x4F32: Handle<Type>? = null,
    var mat3x2F32: Handle<Type>? = null,
    var mat3x3F32: Handle<Type>? = null,
    var mat3x4F32: Handle<Type>? = null,
    var mat4x2F32: Handle<Type>? = null,
    var mat4x3F32: Handle<Type>? = null,
    var mat4x4F32: Handle<Type>? = null,
    
    // Types spéciaux pour built-in functions
    var predeclaredTypes: Map<String, Handle<Type>> = emptyMap()
)
```

---

## 📊 DOC COMMENTS

```kotlin
// Fichier: naga-core/src/main/kotlin/dev/gfxrs/naga/ir/DocComments.kt

/**
 * Arbre des commentaires de documentation.
 */
@Serializable
data class DocComments(
    val entries: List<DocCommentEntry> = emptyList()
)

@Serializable
sealed class DocCommentEntry {
    @Serializable
    data class Function(
        val handle: Handle<Function>,
        val comment: String
    ) : DocCommentEntry()
    
    @Serializable
    data class Type(
        val handle: Handle<Type>,
        val comment: String
    ) : DocCommentEntry()
    
    @Serializable
    data class Constant(
        val handle: Handle<Constant>,
        val comment: String
    ) : DocCommentEntry()
    
    @Serializable
    data class GlobalVariable(
        val handle: Handle<GlobalVariable>,
        val comment: String
    ) : DocCommentEntry()
}
```

---

## 📁 STRUCTURE DES FICHIERS KOTLIN

```
naga-core/
├── src/main/kotlin/dev/gfxrs/naga/
│   ├── ir/                      # Représentation Intermédiaire
│   │   ├── Module.kt            # Module (racine)
│   │   ├── Type.kt             # Type, TypeInner
│   │   ├── Types.kt            # Types utilitaires (ScalarKind, VectorSize, etc.)
│   │   ├── Expression.kt        # Expression (sealed class + variants)
│   │   ├── ExpressionTypes.kt   # Types associés à Expression
│   │   ├── Statement.kt         # Statement (sealed class + variants)
│   │   ├── StatementTypes.kt   # Types associés à Statement
│   │   ├── Function.kt          # Function, FunctionArgument, FunctionResult
│   │   ├── EntryPoint.kt        # EntryPoint, EarlyDepthTest, ConservativeDepth
│   │   ├── Constant.kt          # Constant, Override
│   │   ├── GlobalVariable.kt    # GlobalVariable, ResourceBinding, StorageAccess
│   │   ├── Literal.kt          # Literal (toutes les valeurs littérales)
│   │   └── SpecialTypes.kt      # SpecialTypes
│   │
│   ├── arena/                  # Système Arena/Handle
│   │   ├── Arena.kt            # Arena<T>
│   │   ├── UniqueArena.kt      # UniqueArena<T>
│   │   └── Handle.kt           # Handle<T>
│   │
│   └── Span.kt                # Span, SourceLocation, WithSpan
│
└── src/test/kotlin/dev/gfxrs/naga/ir/
    ├── ModuleTest.kt         # Tests du Module
    ├── TypeTest.kt           # Tests des types
    ├── ExpressionTest.kt      # Tests des expressions
    ├── ArenaTest.kt          # Tests du système Arena
    └── SpanTest.kt           # Tests des spans
```

---

## ✅ CHECKLIST DE LA PHASE 1

### Structures de Base
- [ ] `Module` (data class + Builder)
- [ ] `Type` et `TypeInner` (sealed class)
- [ ] Tous les types enum (ScalarKind, VectorSize, AddressSpace, etc.)
- [ ] `Expression` (sealed class + 60+ variants)
- [ ] `Statement` (sealed class + 25+ variants)
- [ ] `Function` et `FunctionArgument`, `FunctionResult`
- [ ] `EntryPoint` et types associés
- [ ] `Constant` et `Override`
- [ ] `GlobalVariable` et `ResourceBinding`
- [ ] `Literal` (toutes les valeurs littérales)

### Système Arena
- [ ] `Handle<T>` (value class inline)
- [ ] `Arena<T>` (MutableList wrapper)
- [ ] `UniqueArena<T>` (avec déduplication)
- [ ] Extensions utilitaires (forEachWithHandle, findHandle, etc.)

### Types Utilitaires
- [ ] `Span` et `SourceLocation`
- [ ] `SpecialTypes`
- [ ] `DocComments`

### Visitor Pattern
- [ ] `ExpressionVisitor<T>` (interface)
- [ ] `StatementVisitor<T>` (interface)
- [ ] Implémentation de `accept()` pour Expression et Statement

### Tests
- [ ] Tests unitaires pour `Module.Builder`
- [ ] Tests unitaires pour `Arena<T>`
- [ ] Tests unitaires pour `UniqueArena<T>`
- [ ] Tests unitaires pour `Handle<T>`
- [ ] Tests de sérialisation (kotlinx.serialization)

### Documentation
- [ ] KDoc pour toutes les classes publiques
- [ ] KDoc pour toutes les propriétés
- [ ] Exemples d'utilisation dans la documentation

---

## 📅 PLANNING DÉTAILLÉ

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Lire ir/mod.rs | 2-3 jours | Aucune | [ ] |
| Implémenter Handle<T> | 1 jour | Aucune | [ ] |
| Implémenter Arena<T> | 1 jour | Handle<T> | [ ] |
| Implémenter UniqueArena<T> | 2 jours | Arena<T> | [ ] |
| Implémenter Span/SourceLocation | 1 jour | Aucune | [ ] |
| Implémenter tous les enums (ScalarKind, VectorSize, etc.) | 3 jours | Aucune | [ ] |
| Implémenter Type/TypeInner | 5 jours | Enums | [ ] |
| Implémenter Literal | 2 jours | Type | [ ] |
| Implémenter Expression (50% des variants) | 5 jours | Type, Literal | [ ] |
| Implémenter Expression (50% restants) | 5 jours | Expression partie 1 | [ ] |
| Implémenter Statement | 5 jours | Expression | [ ] |
| Implémenter Function/EntryPoint | 3 jours | Type, Expression, Statement | [ ] |
| Implémenter Constant/Override/GlobalVariable | 3 jours | Type, Expression | [ ] |
| Implémenter SpecialTypes | 2 jours | Type | [ ] |
| Implémenter Visitor Pattern | 3 jours | Expression, Statement | [ ] |
| Écrire tests unitaires | 5 jours | Tout le code | [ ] |
| Validation manuelle | 2 jours | Tout le code | [ ] |

**Total estimé** : **4-6 semaines** (1 senior developer)

---

## 🎯 LIVRABLES

1. **Module `naga-core`** compilable et testable
2. **Toutes les structures IR** implémentées en Kotlin
3. **Système Arena/Handle** fonctionnel
4. **Visitor Pattern** pour Expression et Statement
5. **Tests unitaires** avec couverture > 90%
6. **Documentation** complète (KDoc)

---

## 🔗 RÉFÉRENCES PRINCIPALES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs`
- **Système Arena** : `/Users/chaos/RustroverProjects/wgpu/naga/src/arena/mod.rs`
- **Types** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 846)
- **Expression** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 1619)
- **Statement** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs` (ligne 2052)

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Lire attentivement `ir/mod.rs` et `arena/mod.rs`
2. [ ] Commencer par implémenter `Handle<T>` et `Arena<T>`
3. [ ] Implémenter les enums de base (ScalarKind, VectorSize, etc.)
4. [ ] Implémenter `Type` et `TypeInner`
5. [ ] Continuer avec `Expression` et `Statement`
6. [ ] Implémenter le Visitor Pattern
7. [ ] Écrire les tests unitaires
8. [ ] Passer à la Phase 2 (Parser WGSL)

**Fichier suivant** : `01_arena-system.md` ou `02_primitive-types.md`
