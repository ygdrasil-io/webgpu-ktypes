# 🏗️ Phase 4.0 : Architecture des Backends

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core` / `wgsl:msl` / `wgsl:hlsl` / `wgsl:glsl` / `wgsl:wgsl`  
**Phase** : 4 - Backends  
**Sous-Phase** : 4.0 - Architecture Commune  
**Durée** : 1-2 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Fondations pour tous les backends)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/mod.rs` (~200 lignes)

---

## 📋 OBJECTIFS

Définir l'**architecture commune** à tous les backends de génération de code. Cela permet :
- De partager du code entre les différents backends (MSL, HLSL, GLSL, WGSL)
- De maintenir une cohérence dans la structure et le comportement
- De faciliter l'ajout de nouveaux backends
- D'optimiser la réutilisation du code

**Livrable principal** : Une architecture commune claire et bien documentée pour tous les backends de génération de code.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce qu'un Backend ?

Un **backend** est un module qui transforme l'IR (Intermediate Representation) en code source pour un langage cible spécifique. Chaque backend :
- Prend en entrée un `Module` IR valide
- Produit en sortie du code source dans le langage cible
- Doit gérer les spécificités du langage cible
- Doit respecter les règles sémantiques du langage cible

### 2. Architectures Possibles

| Approche | Avantages | Inconvénients |
|----------|-----------|---------------|
| **Backend unique par langage** | Simple, isolé | Duplication de code |
| **Backend générique + spécialisations** | Réutilisation, cohérence | Plus complexe, moins flexible |
| **Backend modulaire (notre choix)** | Équilibre parfait | Nécessite une bonne conception |

### 3. Pattern Visitor

Le pattern **Visitor** est utilisé pour parcourir l'IR et générer le code :
```
Module IR
    ↓
BackendWriter (implémente ExpressionVisitor<String>, StatementVisitor<String>)
    ↓
Pour chaque Expression/Statement :
    writer.visitExpression(expr) → String (code généré)
    ↓
Code source final
```

### 4. Composants Communs

Tous les backends partagent certains composants :
- **Writer** : Classe principale de génération
- **Namer** : Pour les noms uniques (déjà implémenté en Phase 3)
- **Layouter** : Pour les layouts de types (déjà implémenté en Phase 3)
- **Validator** : Pour la validation pré-génération (déjà implémenté en Phase 3)
- **PipelineConstants** : Pour les constantes de pipeline
- **BindingMap** : Pour le mapping des ressources
- **Options** : Configuration spécifique au backend

### 5. Architecture Modulaire

```
naga-backends/ (ou modules séparés)
├── wgsl:msl/       # Backend MSL
│   ├── src/main/kotlin/dev/gfxrs/naga/back/msl/
│   │   ├── Writer.kt          # Writer principal
│   │   ├── Options.kt         # Options MSL
│   │   ├── BindingMap.kt      # Mapping des bindings
│   │   ├── keywords.rs        # Mots-clés MSL
│   │   └── ...
│   └── build.gradle.kts
│
├── wgsl:hlsl/      # Backend HLSL
│   ├── src/main/kotlin/dev/gfxrs/naga/back/hlsl/
│   │   ├── Writer.kt
│   │   ├── Options.kt
│   │   └── ...
│   └── build.gradle.kts
│
├── wgsl:glsl/      # Backend GLSL
│   ├── src/main/kotlin/dev/gfxrs/naga/back/glsl/
│   │   ├── Writer.kt
│   │   └── ...
│   └── build.gradle.kts
│
└── wgsl:wgsl/      # Backend WGSL (output)
    ├── src/main/kotlin/dev/gfxrs/naga/back/wgsl/
    │   ├── Writer.kt
    │   └── ...
    └── build.gradle.kts
```

### 6. Pattern de Base pour un Writer

```kotlin
class BackendWriter(
    private val output: StringBuilder,
    private val module: Module,
    private val moduleInfo: ModuleInfo,
    private val options: BackendOptions,
    private val namer: Namer,
    private val layouter: Layouter
) : ExpressionVisitor<String>, StatementVisitor<String> {
    
    private var indentLevel: Int = 0
    private var currentFunction: Handle<Function>? = null
    
    fun write(): String {
        // Parcourir le module et générer le code
        writeHeader()
        writeTypes()
        writeConstants()
        writeGlobalVariables()
        writeFunctions()
        writeEntryPoints()
        return output.toString()
    }
    
    override fun visitLiteral(expr: Expression.Literal): String {
        // Générer le code pour un littéral
    }
    
    override fun visitBinary(expr: Expression.Binary): String {
        // Générer le code pour une opération binaire
    }
    
    // ... autres méthodes du visitor
}
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. BackendOptions.kt (Options communes)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/BackendOptions.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.valid.Capabilities
import io.ygdrasil.wgsl.valid.ShaderStages
import io.ygdrasil.wgsl.valid.ValidationFlags

/**
 * Options communes à tous les backends.
 */
sealed class BackendOptions {
    /**
     * Flags de validation à appliquer avant la génération.
     */
    abstract val validationFlags: ValidationFlags
    
    /**
     * Capacités supportées par le backend.
     */
    abstract val capabilities: Capabilities
    
    /**
     * Étapes de shader supportées.
     */
    abstract val shaderStages: ShaderStages
    
    /**
     * Indentation à utiliser.
     */
    abstract val indent: String
    
    /**
     * Saut de ligne.
     */
    abstract val newline: String
    
    /**
     * Version du langage cible (ex: "450" pour GLSL, "2.3" pour MSL).
     */
    abstract val version: String?
    
    /**
     * Nom du langage cible.
     */
    abstract val languageName: String
    
    /**
     * Extension du fichier (ex: ".metal", ".hlsl", ".glsl", ".wgsl").
     */
    abstract val fileExtension: String
}

/**
 * Options spécifiques à MSL.
 */
data class MslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = MslCapabilities.DEFAULT,
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String? = "2.3",
    override val languageName: String = "MSL",
    override val fileExtension: String = ".metal",
    
    // Options spécifiques MSL
    val bindingMap: BindingMap = BindingMap(),
    val inlineSamplers: Boolean = false,
    val bufferSizeAlignment: Int = 16
) : BackendOptions()

/**
 * Options spécifiques à HLSL.
 */
data class HlslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = HlslCapabilities.DEFAULT,
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String? = "6.0",
    override val languageName: String = "HLSL",
    override val fileExtension: String = ".hlsl",
    
    // Options spécifiques HLSL
    val shaderModel: ShaderModel = ShaderModel.SM6_0,
    val externalTextureBindingMap: Map<ResourceBinding, u8> = emptyMap()
) : BackendOptions()

/**
 * Options spécifiques à GLSL.
 */
data class GlslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = GlslCapabilities.DEFAULT,
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String? = "450",
    override val languageName: String = "GLSL",
    override val fileExtension: String = ".glsl",
    
    // Options spécifiques GLSL
    val profile: GlslProfile = GlslProfile.CORE,
    val es: Boolean = false,
    val bindingMap: Map<ResourceBinding, u8> = emptyMap()
) : BackendOptions()

/**
 * Options spécifiques à WGSL.
 */
data class WgslOptions(
    override val validationFlags: ValidationFlags = ValidationFlags.ALL,
    override val capabilities: Capabilities = Capabilities.ALL,
    override val shaderStages: ShaderStages = ShaderStages.ALL,
    override val indent: String = "    ",
    override val newline: String = "\n",
    override val version: String? = null,
    override val languageName: String = "WGSL",
    override val fileExtension: String = ".wgsl",
    
    // Options spécifiques WGSL
    val debug: Boolean = false,
    val prettyPrint: Boolean = true
) : BackendOptions()

/**
 * Modèle de shader pour HLSL.
 */
enum class ShaderModel {
    SM5_0, SM5_1, SM6_0, SM6_1, SM6_2, SM6_3, SM6_4, SM6_5, SM6_6
}

/**
 * Profil GLSL.
 */
enum class GlslProfile {
    CORE, ES
}
```

### 2. WriterBase.kt (Classe de base pour tous les writers)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/WriterBase.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*
import io.ygdrasil.wgsl.proc.Layouter
import io.ygdrasil.wgsl.proc.Namer
import io.ygdrasil.wgsl.valid.ModuleInfo
import io.ygdrasil.wgsl.valid.Validator

/**
 * Classe de base pour tous les writers de backend.
 * 
 * Fournit des fonctionnalités communes :
 * - Gestion de l'indentation
 * - Gestion des noms
 * - Gestion des layouts
 * - Écriture des éléments de base (types, constantes, etc.)
 * 
 * @param T Options spécifiques au backend
 */
abstract class WriterBase<T : BackendOptions>(
    protected val output: StringBuilder,
    protected val module: Module,
    protected val moduleInfo: ModuleInfo,
    protected val options: T,
    protected val namer: Namer,
    protected val layouter: Layouter
) {
    
    protected var indentLevel: Int = 0
    protected var currentFunction: Handle<Function>? = null
    protected var currentEntryPoint: Int? = null
    
    /**
     * Génère le code complet pour le module.
     */
    fun write(): String {
        output.clear()
        indentLevel = 0
        
        writeHeader()
        writePreamble()
        writeTypes()
        writeConstants()
        writeGlobalVariables()
        writeFunctions()
        writeEntryPoints()
        
        return output.toString()
    }
    
    /**
     * Écrit l'en-tête du fichier (version, includes, etc.).
     */
    protected abstract fun writeHeader()
    
    /**
     * Écrit le préambule (déclarations globales, defines, etc.).
     */
    protected open fun writePreamble() {
        // Par défaut, rien
    }
    
    /**
     * Écrit les déclarations de types.
     */
    protected fun writeTypes() {
        for ((typeHandle, type) in module.types.withIndex()) {
            val handle = Handle<Type>(typeHandle)
            val name = namer[NameKey.Type(handle)]
            
            when (val inner = type.inner) {
                is TypeInner.Struct -> writeStructType(handle, inner, name)
                else -> {
                    // Les autres types n'ont pas besoin de déclaration
                    // (ils sont référencés par leur nom built-in)
                }
            }
        }
    }
    
    /**
     * Écrit une déclaration de struct.
     */
    protected abstract fun writeStructType(
        handle: Handle<Type>,
        structInner: TypeInner.Struct,
        name: String
    )
    
    /**
     * Écrit les constantes globales.
     */
    protected fun writeConstants() {
        for ((constHandle, constant) in module.constants.withIndex()) {
            val handle = Handle<Constant>(constHandle)
            val name = namer[NameKey.Constant(handle)]
            val type = module.types[constant.ty.index]
            val typeName = getTypeName(constant.ty)
            val init = writeExpression(constant.init)
            
            writeLine("const $typeName $name = $init;")
        }
    }
    
    /**
     * Écrit les variables globales.
     */
    protected fun writeGlobalVariables() {
        for ((varHandle, variable) in module.globalVariables.withIndex()) {
            val handle = Handle<GlobalVariable>(varHandle)
            val name = namer[NameKey.GlobalVariable(handle)]
            val type = module.types[variable.ty.index]
            val typeName = getTypeName(variable.ty)
            
            // Gérer le binding si présent
            val binding = variable.binding?.let { binding ->
                writeBinding(binding)
            } ?: ""
            
            val init = variable.init?.let { init ->
                " = ${writeExpression(init)}"
            } ?: ""
            
            writeLine("$binding$typeName $name;$init")
        }
    }
    
    /**
     * Écrit les fonctions.
     */
    protected fun writeFunctions() {
        for ((funcHandle, func) in module.functions.withIndex()) {
            val handle = Handle<Function>(funcHandle)
            currentFunction = handle
            writeFunction(func, handle)
        }
    }
    
    /**
     * Écrit une fonction.
     */
    protected fun writeFunction(func: Function, handle: Handle<Function>) {
        val name = namer[NameKey.Function(handle)]
        
        writeLine()
        writeFunctionSignature(func, name)
        writeLine(" {")
        indent {
            // Déclarations des variables locales
            for ((varHandle, variable) in func.localVariables.withIndex()) {
                val varHandleObj = Handle<LocalVariable>(varHandle)
                val varName = namer[NameKey.FunctionLocal(handle, varHandleObj)]
                val typeName = getTypeName(variable.ty)
                val init = variable.init?.let { init ->
                    " = ${writeExpression(init)}"
                } ?: ""
                writeLine("$typeName $varName;$init")
            }
            
            writeLine()
            
            // Corps de la fonction
            writeStatement(func.body)
        }
        writeLine("}")
        
        currentFunction = null
    }
    
    /**
     * Écrit la signature d'une fonction.
     */
    protected abstract fun writeFunctionSignature(func: Function, name: String)
    
    /**
     * Écrit les entry points.
     */
    protected fun writeEntryPoints() {
        for ((epIndex, ep) in module.entryPoints.withIndex()) {
            currentEntryPoint = epIndex
            writeEntryPoint(ep, epIndex)
        }
    }
    
    /**
     * Écrit un entry point.
     */
    protected abstract fun writeEntryPoint(ep: EntryPoint, index: Int)
    
    /**
     * Écrit une déclaration de binding.
     */
    protected abstract fun writeBinding(binding: ResourceBinding): String
    
    /**
     * Écrit un statement.
     */
    protected fun writeStatement(stmt: Handle<Statement>): String {
        val statement = module.statements[stmt.index]
        return when (statement) {
            is Statement.Block -> writeBlock(statement)
            is Statement.If -> writeIf(statement)
            is Statement.Switch -> writeSwitch(statement)
            is Statement.Loop -> writeLoop(statement)
            is Statement.While -> writeWhile(statement)
            is Statement.For -> writeFor(statement)
            is Statement.Break -> writeBreak()
            is Statement.Continue -> writeContinue()
            is Statement.Return -> writeReturn(statement)
            is Statement.Discard -> writeDiscard()
            is Statement.Emit -> writeEmit(statement)
            is Statement.Call -> writeCallStatement(statement)
            is Statement.Store -> writeStore(statement)
            is Statement.Atomic -> writeAtomic(statement)
            is Statement.RayQuery -> writeRayQuery(statement)
            is Statement.MemoryBarrier -> writeMemoryBarrier(statement)
            is Statement.ControlBarrier -> writeControlBarrier(statement)
            else -> throw UnsupportedOperationException("Statement not yet supported: ${statement::class.simpleName}")
        }
    }
    
    /**
     * Écrit un bloc de statements.
     */
    protected fun writeBlock(block: Statement.Block): String {
        val result = StringBuilder()
        result.append("{").append(options.newline)
        indent {
            for (stmt in block.statements) {
                result.append(writeStatement(stmt)).append(options.newline)
            }
        }
        result.append("}")
        return result.toString()
    }
    
    /**
     * Écrit une expression.
     */
    protected fun writeExpression(expr: Handle<Expression>): String {
        val expression = module.expressions[expr.index]
        return when (expression) {
            is Expression.Literal -> writeLiteral(expression)
            is Expression.Ident -> writeIdent(expression)
            is Expression.Unary -> writeUnary(expression)
            is Expression.Binary -> writeBinary(expression)
            is Expression.Select -> writeSelect(expression)
            is Expression.Compose -> writeCompose(expression)
            is Expression.Splat -> writeSplat(expression)
            is Expression.Swizzle -> writeSwizzle(expression)
            is Expression.Access -> writeAccess(expression)
            is Expression.AccessIndex -> writeAccessIndex(expression)
            is Expression.As -> writeAs(expression)
            is Expression.Call -> writeCall(expression)
            is Expression.CallResult -> writeCallResult(expression)
            is Expression.Load -> writeLoad(expression)
            is Expression.Store -> writeStoreExpr(expression)
            is Expression.ImageSample -> writeImageSample(expression)
            is Expression.ImageLoad -> writeImageLoad(expression)
            is Expression.ImageQuery -> writeImageQuery(expression)
            is Expression.Derivative -> writeDerivative(expression)
            is Expression.Math -> writeMath(expression)
            is Expression.AtomicResult -> writeAtomicResult(expression)
            is Expression.RayQueryProceedResult -> writeRayQueryProceedResult(expression)
            else -> throw UnsupportedOperationException("Expression not yet supported: ${expression::class.simpleName}")
        }
    }
    
    // Méthodes abstraites à implémenter par chaque backend
    
    protected abstract fun writeLiteral(expr: Expression.Literal): String
    protected abstract fun writeIdent(expr: Expression.Ident): String
    protected abstract fun writeUnary(expr: Expression.Unary): String
    protected abstract fun writeBinary(expr: Expression.Binary): String
    protected abstract fun writeSelect(expr: Expression.Select): String
    protected abstract fun writeCompose(expr: Expression.Compose): String
    protected abstract fun writeSplat(expr: Expression.Splat): String
    protected abstract fun writeSwizzle(expr: Expression.Swizzle): String
    protected abstract fun writeAccess(expr: Expression.Access): String
    protected abstract fun writeAccessIndex(expr: Expression.AccessIndex): String
    protected abstract fun writeAs(expr: Expression.As): String
    protected abstract fun writeCall(expr: Expression.Call): String
    protected abstract fun writeCallResult(expr: Expression.CallResult): String
    protected abstract fun writeLoad(expr: Expression.Load): String
    protected abstract fun writeStoreExpr(expr: Expression.Store): String
    protected abstract fun writeImageSample(expr: Expression.ImageSample): String
    protected abstract fun writeImageLoad(expr: Expression.ImageLoad): String
    protected abstract fun writeImageQuery(expr: Expression.ImageQuery): String
    protected abstract fun writeDerivative(expr: Expression.Derivative): String
    protected abstract fun writeMath(expr: Expression.Math): String
    protected abstract fun writeAtomicResult(expr: Expression.AtomicResult): String
    protected abstract fun writeRayQueryProceedResult(expr: Expression.RayQueryProceedResult): String
    
    protected abstract fun writeIf(stmt: Statement.If): String
    protected abstract fun writeSwitch(stmt: Statement.Switch): String
    protected abstract fun writeLoop(stmt: Statement.Loop): String
    protected abstract fun writeWhile(stmt: Statement.While): String
    protected abstract fun writeFor(stmt: Statement.For): String
    protected abstract fun writeBreak(): String
    protected abstract fun writeContinue(): String
    protected abstract fun writeReturn(stmt: Statement.Return): String
    protected abstract fun writeDiscard(): String
    protected abstract fun writeEmit(stmt: Statement.Emit): String
    protected abstract fun writeCallStatement(stmt: Statement.Call): String
    protected abstract fun writeStore(stmt: Statement.Store): String
    protected abstract fun writeAtomic(stmt: Statement.Atomic): String
    protected abstract fun writeRayQuery(stmt: Statement.RayQuery): String
    protected abstract fun writeMemoryBarrier(stmt: Statement.MemoryBarrier): String
    protected abstract fun writeControlBarrier(stmt: Statement.ControlBarrier): String
    
    /**
     * Récupère le nom d'un type.
     */
    protected fun getTypeName(handle: Handle<Type>): String {
        val type = module.types[handle.index]
        
        // Si le type a un nom, l'utiliser
        if (type.name != null) {
            return namer[NameKey.Type(handle)]
        }
        
        // Sinon, générer le nom basé sur le TypeInner
        return when (val inner = type.inner) {
            is TypeInner.Scalar -> getScalarTypeName(inner.scalar)
            is TypeInner.Vector -> getVectorTypeName(inner)
            is TypeInner.Matrix -> getMatrixTypeName(inner)
            is TypeInner.Array -> "${getTypeName(inner.base)}[${inner.size}]"
            is TypeInner.Struct -> namer[NameKey.Type(handle)]
            is TypeInner.Pointer -> "ptr<${getAddressSpaceName(inner.addressSpace)}, ${getTypeName(inner.base)}>"
            is TypeInner.ValuePointer -> "ptr<${getAddressSpaceName(inner.addressSpace)}, ${getTypeName(inner.base)}>"
            is TypeInner.Image -> getImageTypeName(inner)
            is TypeInner.Sampler -> getSamplerTypeName(inner)
            is TypeInner.AccelerationStructure -> "acceleration_structure"
            is TypeInner.RayQuery -> "ray_query"
            is TypeInner.BindingArray -> "binding_array"
            is TypeInner.Atomic -> "atomic<${getScalarTypeName(inner.scalar)}>"
            is TypeInner.CooperativeMatrix -> getCooperativeMatrixTypeName(inner)
        }
    }
    
    private fun getScalarTypeName(scalar: Scalar): String {
        return when (scalar.kind) {
            ScalarKind.BOOL -> "bool"
            ScalarKind.SINT -> when (scalar.width) {
                1 -> "i8"
                2 -> "i16"
                4 -> "i32"
                8 -> "i64"
                else -> throw IllegalArgumentException("Invalid width: ${scalar.width}")
            }
            ScalarKind.UINT -> when (scalar.width) {
                1 -> "u8"
                2 -> "u16"
                4 -> "u32"
                8 -> "u64"
                else -> throw IllegalArgumentException("Invalid width: ${scalar.width}")
            }
            ScalarKind.FLOAT -> when (scalar.width) {
                2 -> "f16"
                4 -> "f32"
                8 -> "f64"
                else -> throw IllegalArgumentException("Invalid width: ${scalar.width}")
            }
            ScalarKind.ABSTRACT_INT -> "AbstractInt"
        }
    }
    
    private fun getVectorTypeName(vector: TypeInner.Vector): String {
        val scalarName = getScalarTypeName(vector.scalar)
        val sizeName = when (vector.size) {
            VectorSize.BI -> "2"
            VectorSize.TRI -> "3"
            VectorSize.QUAD -> "4"
        }
        return "vec${sizeName}<${scalarName}>"
    }
    
    private fun getMatrixTypeName(matrix: TypeInner.Matrix): String {
        val scalarName = getScalarTypeName(matrix.scalar)
        val rowsName = when (matrix.rows) {
            VectorSize.BI -> "2"
            VectorSize.TRI -> "3"
            VectorSize.QUAD -> "4"
        }
        val colsName = when (matrix.columns) {
            VectorSize.BI -> "2"
            VectorSize.TRI -> "3"
            VectorSize.QUAD -> "4"
        }
        return "mat${rowsName}x${colsName}<${scalarName}>"
    }
    
    // Autres méthodes utilitaires...
    
    /**
     * Indente le code.
     */
    protected fun indent(block: () -> Unit) {
        indentLevel++
        block()
        indentLevel--
    }
    
    /**
     * Écrit une ligne avec indentation.
     */
    protected fun writeLine(line: String = "") {
        repeat(indentLevel) { output.append(options.indent) }
        output.append(line).append(options.newline)
    }
    
    /**
     * Écrit du texte sans saut de ligne.
     */
    protected fun write(text: String) {
        output.append(text)
    }
}
```

### 3. BackendWriter.kt (Interface commune)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/BackendWriter.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo

/**
 * Interface commune pour tous les writers de backend.
 */
interface BackendWriter<T : BackendOptions> {
    
    /**
     * Génère le code pour un module.
     * 
     * @param module Le module IR à transformer
     * @param moduleInfo Informations calculées sur le module
     * @return Le code source généré
     * @throws BackendError Si une erreur survient pendant la génération
     */
    fun write(module: Module, moduleInfo: ModuleInfo): String
    
    /**
     * Crée un nouveau writer avec les options spécifiées.
     */
    fun withOptions(options: T): BackendWriter<T>
    
    /**
     * Valide que le module peut être transformé par ce backend.
     * 
     * @param module Le module à valider
     * @param moduleInfo Informations calculées
     * @return true si le module est valide pour ce backend
     */
    fun canHandle(module: Module, moduleInfo: ModuleInfo): Boolean
}
```

### 4. BackendError.kt (Erreurs de backend)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/BackendError.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Erreurs générées par les backends.
 */
sealed class BackendError {
    abstract val message: String
    
    // Erreurs communes
    data class UnsupportedFeature(val feature: String) : BackendError() {
        override val message: String get() = "Feature not supported: $feature"
    }
    
    data class UnsupportedType(val type: Handle<Type>) : BackendError() {
        override val message: String get() = "Type not supported: $type"
    }
    
    data class UnsupportedExpression(val expr: Handle<Expression>) : BackendError() {
        override val message: String get() = "Expression not supported: $expr"
    }
    
    data class UnsupportedStatement(val stmt: Handle<Statement>) : BackendError() {
        override val message: String get() = "Statement not supported: $stmt"
    }
    
    data class TypeMismatch(val expected: String, val actual: String) : BackendError() {
        override val message: String get() = "Type mismatch: expected $expected, got $actual"
    }
    
    data class InternalError(val cause: Throwable) : BackendError() {
        override val message: String get() = "Internal error: ${cause.message}"
    }
    
    // Erreurs spécifiques à certains backends
    data class MslError(val message: String) : BackendError()
    data class HlslError(val message: String) : BackendError()
    data class GlslError(val message: String) : BackendError()
    data class WgslError(val message: String) : BackendError()
}
```

### 5. BindingMap.kt (Mapping des ressources)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/BindingMap.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.ResourceBinding

/**
 * Mapping entre les ResourceBinding et les cibles de binding spécifiques au backend.
 * 
 * Chaque backend a sa propre façon de gérer les bindings (group, binding, etc.).
 * Ce mapping permet de traduire les bindings génériques de l'IR vers les bindings spécifiques du backend.
 */
class BindingMap {
    
    private val map: MutableMap<ResourceBinding, BindTarget> = mutableMapOf()
    
    /**
     * Cible de binding pour un backend spécifique.
     */
    data class BindTarget(
        val buffer: Int? = null,
        val texture: Int? = null,
        val sampler: Int? = null,
        val mutable: Boolean = false
    )
    
    /**
     * Ajoute un mapping.
     */
    fun insert(binding: ResourceBinding, target: BindTarget) {
        map[binding] = target
    }
    
    /**
     * Récupère la cible pour un binding.
     */
    operator fun get(binding: ResourceBinding): BindTarget? {
        return map[binding]
    }
    
    /**
     * Vérifie si un binding a un mapping.
     */
    fun contains(binding: ResourceBinding): Boolean {
        return map.containsKey(binding)
    }
    
    /**
     * Efface tous les mappings.
     */
    fun clear() {
        map.clear()
    }
    
    /**
     * Crée un BindingMap par défaut pour MSL.
     */
    companion object {
        fun defaultMsl(): BindingMap {
            val map = BindingMap()
            // Ajouter des mappings par défaut si nécessaire
            return map
        }
        
        fun defaultHlsl(): BindingMap {
            val map = BindingMap()
            return map
        }
        
        fun defaultGlsl(): BindingMap {
            val map = BindingMap()
            return map
        }
    }
}
```

### 6. PipelineConstants.kt (Constantes de pipeline)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/PipelineConstants.kt`

```kotlin
package io.ygdrasil.wgsl.back

/**
 * Spécifie les valeurs des constantes de pipeline dans le module shader.
 * 
 * Si un attribut `@id` a été spécifié sur la déclaration, la clé doit être l'ID de la constante
 * de pipeline sous forme de chaîne décimale. Sinon, la clé doit être le nom de l'identifiant
 * de la constante.
 * 
 * La valeur peut représenter n'importe quel type scalaire concret de WGSL.
 */
typealias PipelineConstants = Map<String, Double>

/**
 * Utilitaires pour les constantes de pipeline.
 */
object PipelineConstantsUtils {
    
    /**
     * Crée une map vide.
     */
    fun empty(): PipelineConstants = emptyMap()
    
    /**
     * Crée une map avec une seule constante.
     */
    fun single(id: String, value: Double): PipelineConstants = mapOf(id to value)
    
    /**
     * Crée une map avec plusieurs constantes.
     */
    fun of(vararg pairs: Pair<String, Double>): PipelineConstants = mapOf(*pairs)
    
    /**
     * Convertit une PipelineConstants en chaîne de caractères pour le backend.
     */
    fun toBackendString(constants: PipelineConstants, backend: BackendType): String {
        return when (backend) {
            BackendType.MSL -> constants.entries.joinToString(", ") { (id, value) ->
                "$id = $value"
            }
            BackendType.HLSL -> constants.entries.joinToString(", ") { (id, value) ->
                "$id = $value"
            }
            BackendType.GLSL -> constants.entries.joinToString(", ") { (id, value) ->
                "$id = $value"
            }
            BackendType.WGSL -> constants.entries.joinToString(", ") { (id, value) ->
                "$id = $value"
            }
        }
    }
}

/**
 * Type de backend.
 */
enum class BackendType {
    MSL, HLSL, GLSL, WGSL
}
```

### 7. BackendRegistry.kt (Registre des backends)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/BackendRegistry.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo

/**
 * registre de tous les backends disponibles.
 * 
 * Permet de :
 * - Obtenir un backend par nom
 * - Lister tous les backends disponibles
 * - Vérifier si un backend est disponible
 */
class BackendRegistry {
    
    private val backends: MutableMap<String, BackendFactory> = mutableMapOf()
    
    /**
     * Usine de création de backend.
     */
    fun interface BackendFactory {
        fun create(): BackendWriter<*>
        fun createWithOptions(options: BackendOptions): BackendWriter<*>
    }
    
    init {
        // Enregistrer les backends par défaut
        register("msl", MslBackendFactory)
        register("hlsl", HlslBackendFactory)
        register("glsl", GlslBackendFactory)
        register("wgsl", WgslBackendFactory)
    }
    
    /**
     * Enregistre un nouveau backend.
     */
    fun register(name: String, factory: BackendFactory) {
        backends[name.lowercase()] = factory
    }
    
    /**
     * Obtient un backend par nom.
     */
    fun get(name: String): BackendWriter<*>? {
        return backends[name.lowercase()]?.create()
    }
    
    /**
     * Obtient un backend par nom avec des options.
     */
    fun getWithOptions(name: String, options: BackendOptions): BackendWriter<*>? {
        return backends[name.lowercase()]?.createWithOptions(options)
    }
    
    /**
     * Liste tous les noms de backends disponibles.
     */
    fun listBackendNames(): List<String> {
        return backends.keys.toList()
    }
    
    /**
     * Vérifie si un backend est disponible.
     */
    fun hasBackend(name: String): Boolean {
        return backends.containsKey(name.lowercase())
    }
    
    /**
     * Génère le code pour un module avec un backend spécifique.
     */
    fun write(module: Module, moduleInfo: ModuleInfo, backendName: String): String {
        val backend = get(backendName)
            ?: throw IllegalArgumentException("Backend '$backendName' not found")
        return backend.write(module, moduleInfo)
    }
    
    companion object {
        val DEFAULT = BackendRegistry()
    }
}

// Implémentations des factories
private object MslBackendFactory : BackendRegistry.BackendFactory {
    override fun create(): BackendWriter<*> {
        return MslWriter(StringBuilder(), MslOptions()) // Simplifié
    }
    
    override fun createWithOptions(options: BackendOptions): BackendWriter<*> {
        @Suppress("UNCHECKED_CAST")
        return MslWriter(StringBuilder(), options as MslOptions)
    }
}

private object HlslBackendFactory : BackendRegistry.BackendFactory {
    override fun create(): BackendWriter<*> {
        return HlslWriter(StringBuilder(), HlslOptions())
    }
    
    override fun createWithOptions(options: BackendOptions): BackendWriter<*> {
        @Suppress("UNCHECKED_CAST")
        return HlslWriter(StringBuilder(), options as HlslOptions)
    }
}

private object GlslBackendFactory : BackendRegistry.BackendFactory {
    override fun create(): BackendWriter<*> {
        return GlslWriter(StringBuilder(), GlslOptions())
    }
    
    override fun createWithOptions(options: BackendOptions): BackendWriter<*> {
        @Suppress("UNCHECKED_CAST")
        return GlslWriter(StringBuilder(), options as GlslOptions)
    }
}

private object WgslBackendFactory : BackendRegistry.BackendFactory {
    override fun create(): BackendWriter<*> {
        return WgslWriter(StringBuilder(), WgslOptions())
    }
    
    override fun createWithOptions(options: BackendOptions): BackendWriter<*> {
        @Suppress("UNCHECKED_CAST")
        return WgslWriter(StringBuilder(), options as WgslOptions)
    }
}
```

---

## 📁 STRUCTURE DES FICHIERS

```
wgsl:core/src/main/kotlin/dev/gfxrs/naga/back/
├── BackendOptions.kt       # Options communes et spécifiques
├── BackendError.kt        # Erreurs de backend
├── BindingMap.kt          # Mapping des ressources
├── PipelineConstants.kt   # Constantes de pipeline
├── WriterBase.kt          # Classe de base pour les writers
├── BackendWriter.kt       # Interface commune
└── BackendRegistry.kt     # Registre des backends
```

---

## 🧪 TESTS

### 1. BackendOptionsTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/back/BackendOptionsTest.kt`

```kotlin
package io.ygdrasil.wgsl.back

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BackendOptionsTest {
    
    @Test
    fun `test MslOptions defaults`() {
        val options = MslOptions()
        assertThat(options.indent).isEqualTo("    ")
        assertThat(options.newline).isEqualTo("\n")
        assertThat(options.version).isEqualTo("2.3")
        assertThat(options.languageName).isEqualTo("MSL")
        assertThat(options.fileExtension).isEqualTo(".metal")
    }
    
    @Test
    fun `test HlslOptions defaults`() {
        val options = HlslOptions()
        assertThat(options.indent).isEqualTo("    ")
        assertThat(options.version).isEqualTo("6.0")
        assertThat(options.languageName).isEqualTo("HLSL")
    }
    
    @Test
    fun `test GlslOptions defaults`() {
        val options = GlslOptions()
        assertThat(options.indent).isEqualTo("    ")
        assertThat(options.version).isEqualTo("450")
        assertThat(options.profile).isEqualTo(GlslProfile.CORE)
    }
    
    @Test
    fun `test WgslOptions defaults`() {
        val options = WgslOptions()
        assertThat(options.indent).isEqualTo("    ")
        assertThat(options.languageName).isEqualTo("WGSL")
    }
}
```

### 2. BackendRegistryTest.kt

**Fichier** : `wgsl:core/src/test/kotlin/dev/gfxrs/naga/back/BackendRegistryTest.kt`

```kotlin
package io.ygdrasil.wgsl.back

import io.ygdrasil.wgsl.ir.Module
import io.ygdrasil.wgsl.valid.ModuleInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BackendRegistryTest {
    
    @Test
    fun `test list backend names`() {
        val registry = BackendRegistry()
        val names = registry.listBackendNames()
        
        assertThat(names).contains("msl")
        assertThat(names).contains("hlsl")
        assertThat(names).contains("glsl")
        assertThat(names).contains("wgsl")
    }
    
    @Test
    fun `test has backend`() {
        val registry = BackendRegistry()
        
        assertThat(registry.hasBackend("msl")).isTrue()
        assertThat(registry.hasBackend("MSL")).isTrue()
        assertThat(registry.hasBackend("hlsl")).isTrue()
        assertThat(registry.hasBackend("nonexistent")).isFalse()
    }
    
    @Test
    fun `test get backend`() {
        val registry = BackendRegistry()
        
        val mslBackend = registry.get("msl")
        assertThat(mslBackend).isNotNull()
        
        val hlslBackend = registry.get("hlsl")
        assertThat(hlslBackend).isNotNull()
    }
    
    @Test
    fun `test write empty module`() {
        val registry = BackendRegistry()
        val module = Module()
        val moduleInfo = ModuleInfo.empty()
        
        val mslCode = registry.write(module, moduleInfo, "msl")
        assertThat(mslCode).isNotEmpty()
        
        val hlslCode = registry.write(module, moduleInfo, "hlsl")
        assertThat(hlslCode).isNotEmpty()
    }
}
```

---

## ✅ CHECKLIST D'IMPLÉMENTATION

### Structure des Fichiers
- [ ] `BackendOptions.kt` - Options communes et spécifiques à chaque backend
- [ ] `BackendError.kt` - Toutes les classes d'erreur
- [ ] `BindingMap.kt` - Mapping des ressources
- [ ] `PipelineConstants.kt` - Constantes de pipeline
- [ ] `WriterBase.kt` - Classe de base pour les writers
- [ ] `BackendWriter.kt` - Interface commune
- [ ] `BackendRegistry.kt` - Registre des backends

### Fonctionnalités BackendOptions
- [ ] ValidationFlags
- [ ] Capabilities
- [ ] ShaderStages
- [ ] indent, newline
- [ ] version, languageName, fileExtension
- [ ] MslOptions
- [ ] HlslOptions
- [ ] GlslOptions
- [ ] WgslOptions

### Fonctionnalités WriterBase
- [ ] write()
- [ ] writeHeader() (abstrait)
- [ ] writePreamble()
- [ ] writeTypes()
- [ ] writeConstants()
- [ ] writeGlobalVariables()
- [ ] writeFunctions()
- [ ] writeFunction()
- [ ] writeFunctionSignature() (abstrait)
- [ ] writeEntryPoints()
- [ ] writeEntryPoint() (abstrait)
- [ ] writeStatement()
- [ ] writeBlock()
- [ ] writeExpression()
- [ ] Toutes les méthodes writeXxx() (abstraites)
- [ ] getTypeName()
- [ ] indent(), writeLine(), write()

### Tests
- [ ] Tests pour BackendOptions
- [ ] Tests pour BackendRegistry
- [ ] Tests pour BindingMap
- [ ] Tests pour PipelineConstants

### Intégration
- [ ] Utiliser WriterBase dans tous les backends
- [ ] Utiliser BackendRegistry pour l'API publique
- [ ] Documenter l'API publique

---

## 📖 RÉFÉRENCES

1. **Rust Reference** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/mod.rs`
2. **Backend MSL** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/msl/mod.rs`
3. **Backend HLSL** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/hlsl/mod.rs`
4. **Backend GLSL** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/glsl/mod.rs`
5. **Backend WGSL** : `/Users/chaos/RustroverProjects/wgpu/naga/src/back/wgsl/mod.rs`

---

## 🎯 PLANNING

| Tâche | Durée | Dépendances | Priorité |
|-------|-------|-------------|----------|
| Implémenter BackendOptions.kt | 4-6h | ValidationFlags, Capabilities | ⭐⭐⭐⭐⭐ |
| Implémenter BackendError.kt | 2h | Aucune | ⭐⭐⭐⭐ |
| Implémenter BindingMap.kt | 2-4h | ResourceBinding | ⭐⭐⭐⭐⭐ |
| Implémenter PipelineConstants.kt | 2h | Aucune | ⭐⭐⭐ |
| Implémenter BackendWriter.kt | 2h | BackendOptions | ⭐⭐⭐⭐ |
| Implémenter WriterBase.kt | 8-12h | BackendOptions, BackendWriter | ⭐⭐⭐⭐⭐ |
| Implémenter BackendRegistry.kt | 4h | WriterBase | ⭐⭐⭐⭐ |
| Tests unitaires | 4-8h | Tout | ⭐⭐⭐⭐ |
| Intégration | 2h | Tout | ⭐⭐⭐ |
| **Total** | **32-54h (1-1.5 semaine)** | | |

---

## 🔄 DÉPENDANCES

### Dépendances Internes
- `wgsl:core` : Module IR (Module, Type, Expression, Statement, etc.)
- `naga-proc` : Layouter, Namer
- `naga-valid` : ModuleInfo, Validator, ValidationFlags, Capabilities, ShaderStages
- `io.ygdrasil.wgsl.arena.Handle`
- `io.ygdrasil.wgsl.ir.*`

### Dépendances Externes
- Aucune (kotlin-stdlib uniquement)

---

## 📝 NOTES

1. **Pattern Visitor** : L'utilisation du pattern Visitor pour parcourir l'IR est essentielle pour maintenir une structure cohérente entre les différents backends.

2. **Réutilisation de code** : WriterBase fournit une implémentation par défaut pour de nombreuses méthodes, ce qui réduit la duplication de code.

3. **Flexibilité** : Chaque backend peut étendre ou remplacer les méthodes de WriterBase selon ses besoins spécifiques.

4. **Performance** : La génération de code utilise StringBuilder pour éviter les allocations inutiles.

5. **Validation** : Chaque backend peut spécifier ses propres ValidationFlags et Capabilities pour s'assurer que seul le code supporté est généré.

6. **Backends multiples** : Un même module IR peut être transformé en plusieurs langages cibles différents.

7. **Options par défaut** : Chaque backend a des options par défaut sensées, mais elles peuvent être personnalisées.
