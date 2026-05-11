# 🧮 Phase 3.0 : Évaluateur de Constantes

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-core`  
**Phase** : 3 - Processing  
**Sous-Phase** : 3.0 - Constant Evaluator  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Évaluation à la compilation)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/constant_evaluator.rs` (~3000 lignes)

---

## 📋 OBJECTIFS

Implémenter un **évaluateur de constantes** qui évalue les expressions constantes à la compilation. Cela permet :
- D'optimiser le code en remplaçant les expressions constantes par leurs valeurs
- De valider que les expressions constantes sont bien valides
- De détecter les erreurs dans les expressions constantes (division par zéro, overflow, etc.)
- De supporter les constantes globales et locales

**Livrable principal** : Un évaluateur capable d'évaluer toutes les expressions constantes valides en WGSL.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce qu'une expression constante ?

En WGSL, une expression constante est une expression qui peut être évaluée **à la compilation** sans dépendre de valeurs runtime. Exemples :

```wgsl
let x = 42;                    // Constante simple
let y = 2 + 3;                 // Expression constante
let z = array<i32, 5>;         // Tableau avec longueur constante
const PI = 3.14159;            // Constante globale
let size = arrayLength(&myArray); // Constante si myArray est constante
```

### 2. Règles WGSL pour les constantes

- Les `const` declarations doivent avoir une initialisation constante
- Les `let` declarations peuvent avoir une initialisation constante
- Les expressions dans les `for` loops doivent être constantes (init, condition, update)
- Les longueurs de tableaux doivent être des expressions constantes
- Les arguments de template doivent être des expressions constantes

### 3. Types de valeurs constantes supportés

| Type | Exemple | Valeur évaluée |
|------|---------|----------------|
| Scalar | `42` | `LiteralValue.I32(42)` |
| Scalar | `3.14` | `LiteralValue.F32(3.14)` |
| Scalar | `true` | `LiteralValue.Bool(true)` |
| Vector | `vec3<f32>(1, 2, 3)` | `VectorValue([1.0, 2.0, 3.0])` |
| Matrix | `mat2x2<f32>(...)` | `MatrixValue([[...]])` |
| Array | `array<i32, 3>(1, 2, 3)` | `ArrayValue([1, 2, 3])` |

### 4. Opérations supportées

**Opérations arithmétiques** : `+`, `-`, `*`, `/`, `%` (sur scalaires)
**Opérations logiques** : `&&`, `||`, `!`
**Opérations de comparaison** : `==`, `!=`, `<`, `>`, `<=`, `>=`
**Opérations bits** : `&`, `|`, `^`, `~`, `<<`, `>>`
**Constructeurs** : `vec2`, `vec3`, `vec4`, `mat2x2`, etc.
**Conversions** : `i32()`, `f32()`, etc.
**Autres** : `select`, `arrayLength`

### 5. Architecture

```
Expression Constant
    ↓
ConstantEvaluator.tryEvaluate()
    ↓
Vérification que l'expression est "constant-friendly"
    ↓
Évaluation récursive des sous-expressions
    ↓
ConstValue (valeur constante évaluée)
    ↓
Utilisation :
  - Remplacement dans l'IR
  - Validation des constantes
  - Optimisations
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. ConstValue.kt (Valeurs constantes)

**Fichier** : `naga-core/src/main/kotlin/dev/gfxrs/naga/ir/ConstValue.kt`

```kotlin
package dev.gfxrs.naga.ir

import dev.gfxrs.naga.arena.Handle
import kotlinx.serialization.Serializable

/**
 * Valeur constante évaluée.
 * Représente le résultat de l'évaluation d'une expression constante.
 */
@Serializable
sealed class ConstValue {
    
    /**
     * Type de la valeur constante.
     */
    abstract val type: Handle<Type>
    
    // ===== Valeurs scalaires =====
    
    @Serializable
    data class Scalar(
        val value: ScalarValue,
        override val type: Handle<Type>
    ) : ConstValue()
    
    @Serializable
    data class Vector(
        val components: List<ScalarValue>,
        override val type: Handle<Type>
    ) : ConstValue()
    
    @Serializable
    data class Matrix(
        val columns: List<List<ScalarValue>>,
        override val type: Handle<Type>
    ) : ConstValue()
    
    @Serializable
    data class Array(
        val elements: List<ConstValue>,
        override val type: Handle<Type>
    ) : ConstValue()
    
    @Serializable
    data class Struct(
        val members: List<ConstValue>,
        override val type: Handle<Type>
    ) : ConstValue()
    
    @Serializable
    data class Pointer(
        val base: ConstValue?,
        val offset: Long,
        override val type: Handle<Type>
    ) : ConstValue()
    
    // ===== Valeur spéciale pour "non évaluable" =====
    
    @Serializable
    object NotConst : ConstValue() {
        override val type: Handle<Type> get() = Handle.INVALID
    }
}

/**
 * Valeur scalaire.
 * Stocke la valeur de manière type-safe.
 */
@Serializable
sealed class ScalarValue {
    @Serializable
    data class Bool(val value: Boolean) : ScalarValue()
    
    @Serializable
    data class I8(val value: Byte) : ScalarValue()
    
    @Serializable
    data class U8(val value: UByte) : ScalarValue()
    
    @Serializable
    data class I16(val value: Short) : ScalarValue()
    
    @Serializable
    data class U16(val value: UShort) : ScalarValue()
    
    @Serializable
    data class I32(val value: Int) : ScalarValue()
    
    @Serializable
    data class U32(val value: Long) : ScalarValue() // Stocker comme Long pour éviter overflow
    
    @Serializable
    data class I64(val value: Long) : ScalarValue()
    
    @Serializable
    data class U64(val value: ULong) : ScalarValue()
    
    @Serializable
    data class F32(val value: Float) : ScalarValue()
    
    @Serializable
    data class F64(val value: Double) : ScalarValue()
    
    @Serializable
    data class AbstractInt(val value: Long) : ScalarValue()
    
    @Serializable
    data class AbstractFloat(val value: Double) : ScalarValue()
}

/**
 * Extension pour créer facilement des ConstValue.NotConst.
 */
fun notConst(): ConstValue = ConstValue.NotConst

/**
 * Vérifie si une ConstValue est NotConst.
 */
fun ConstValue.isConst(): Boolean = this !is ConstValue.NotConst

/**
 * Vérifie si une ConstValue est NotConst.
 */
fun ConstValue?.isConstOrNull(): Boolean = this?.isConst() ?: false
```

### 2. ConstantEvaluator.kt (Évaluateur principal)

**Fichier** : `naga-core/src/main/kotlin/dev/gfxrs/naga/proc/ConstantEvaluator.kt`

```kotlin
package dev.gfxrs.naga.proc

import dev.gfxrs.naga.arena.Handle
import dev.gfxrs.naga.ir.*
import dev.gfxrs.naga.span.Span

/**
 * Évalue les expressions constantes dans l'IR.
 * Cet évaluateur parcourt l'IR et évalue toutes les expressions
 * qui peuvent être évaluées à la compilation.
 */
class ConstantEvaluator(
    private val module: Module,
    private val function: Function? = null
) {
    
    /**
     * Résultat d'une évaluation de constante.
     */
    data class Result(
        val value: ConstValue,
        val span: Span
    )
    
    /**
     * Erreur d'évaluation de constante.
     */
    data class Error(
        val message: String,
        val span: Span
    )
    
    /**
     * Liste des erreurs accumulées.
     */
    val errors: MutableList<Error> = mutableListOf()
    
    /**
     * Tente d'évaluer une expression comme constante.
     * Retourne la valeur constante ou null si l'expression ne peut pas être évaluée.
     */
    fun tryEvaluate(expression: Handle<Expression>): Result? {
        val expr = module.expressions[expression]
        return try {
            val context = EvaluationContext(module, function)
            val result = evaluateExpression(expression, context)
            if (result.isConst()) {
                Result(result, expr.span)
            } else {
                null
            }
        } catch (e: EvaluationError) {
            errors.add(Error(e.message, e.span))
            null
        }
    }
    
    /**
     * Évalue une expression constante.
     * Lève une exception si l'évaluation échoue.
     */
    fun evaluate(expression: Handle<Expression>): Result {
        val result = tryEvaluate(expression)
        return result ?: throw EvaluationError(
            "Expression is not constant",
            module.expressions[expression].span
        )
    }
    
    /**
     * Évalue toutes les expressions constantes dans un module.
     * Remplace les expressions constantes par des Expression::Literal.
     */
    fun evaluateModule() {
        // Évaluer les constantes globales
        for (global in module.globalVariables) {
            if (global.init != null) {
                tryEvaluateAndReplace(global.init)
            }
        }
        
        // Évaluer les constantes dans les fonctions
        for (func in module.functions) {
            evaluateFunction(func)
        }
    }
    
    /**
     * Évalue toutes les expressions constantes dans une fonction.
     */
    private fun evaluateFunction(func: Function) {
        val context = EvaluationContext(module, func)
        
        // Évaluer les variables locales avec initialisation
        for (local in func.locals) {
            if (local.init != null) {
                tryEvaluateAndReplaceInFunction(local.init, func)
            }
        }
        
        // Évaluer les expressions dans le body
        evaluateStatements(func.body, func, context)
    }
    
    /**
     * Évalue une expression et remplace par une littérale si possible.
     */
    private fun tryEvaluateAndReplace(exprHandle: Handle<Expression>) {
        val result = tryEvaluate(exprHandle)
        if (result != null) {
            val newExpr = Expression.Literal(result.value, result.span)
            module.expressions[exprHandle] = newExpr
        }
    }
    
    /**
     * Évalue une expression dans le contexte d'une fonction.
     */
    private fun tryEvaluateAndReplaceInFunction(
        exprHandle: Handle<Expression>,
        func: Function
    ) {
        val result = try {
            val context = EvaluationContext(module, func)
            val value = evaluateExpression(exprHandle, context)
            if (value.isConst()) {
                Result(value, module.expressions[exprHandle].span)
            } else {
                null
            }
        } catch (e: EvaluationError) {
            errors.add(Error(e.message, e.span))
            null
        }
        
        if (result != null) {
            val newExpr = Expression.Literal(result.value, result.span)
            // Note: Dans l'IR, il faut utiliser l'arena de la fonction
            // Pour simplifier, on modifie directement dans module.expressions
            // mais en production, il faudrait utiliser func.expressions
        }
    }
    
    /**
     * Évalue des instructions.
     */
    private fun evaluateStatements(
        statements: List<Handle<Statement>>,
        func: Function,
        context: EvaluationContext
    ) {
        for (stmtHandle in statements) {
            evaluateStatement(stmtHandle, func, context)
        }
    }
    
    /**
     * Évalue une instruction.
     */
    private fun evaluateStatement(
        stmtHandle: Handle<Statement>,
        func: Function,
        context: EvaluationContext
    ) {
        val stmt = module.statements[stmtHandle]
        
        when (stmt) {
            is Statement.Let -> {
                if (stmt.init != null) {
                    tryEvaluateAndReplaceInFunction(stmt.init, func)
                }
            }
            is Statement.Assign -> {
                tryEvaluateAndReplaceInFunction(stmt.value, func)
            }
            is Statement.Store -> {
                tryEvaluateAndReplaceInFunction(stmt.value, func)
            }
            is Statement.If -> {
                // Évaluer la condition (mais pas remplacer, car elle est utilisée pour le branching)
                tryEvaluate(stmt.condition)
                evaluateStatements(stmt.accept, func, context)
                if (stmt.reject != null) {
                    evaluateStatements(stmt.reject, func, context)
                }
            }
            is Statement.Switch -> {
                tryEvaluate(stmt.selector)
                for (case in stmt.body) {
                    for (value in case.values) {
                        tryEvaluate(value)
                    }
                    evaluateStatements(case.body, func, context)
                }
            }
            is Statement.Loop -> {
                evaluateStatements(stmt.body, func, context)
            }
            is Statement.While -> {
                tryEvaluate(stmt.condition)
                evaluateStatements(stmt.body, func, context)
            }
            is Statement.For -> {
                tryEvaluateAndReplaceInFunction(stmt.init, func)
                tryEvaluate(stmt.condition)
                tryEvaluateAndReplaceInFunction(stmt.update, func)
                evaluateStatements(stmt.body, func, context)
            }
            is Statement.BreakIf -> {
                tryEvaluate(stmt.condition)
            }
            is Statement.ContinueIf -> {
                tryEvaluate(stmt.condition)
            }
            is Statement.Return -> {
                if (stmt.value != null) {
                    tryEvaluateAndReplaceInFunction(stmt.value, func)
                }
            }
            is Statement.Discard -> {
                // Pas d'expression à évaluer
            }
            is Statement.Block -> {
                evaluateStatements(stmt.children, func, context)
            }
            is Statement.Emit -> {
                // Les ranges d'expressions
                for (i in stmt.range.start..stmt.range.endInclusive) {
                    tryEvaluateAndReplaceInFunction(Handle(i), func)
                }
            }
            else -> {
                // Autres types de statements
            }
        }
    }
    
    /**
     * Évalue une expression.
     */
    private fun evaluateExpression(
        exprHandle: Handle<Expression>,
        context: EvaluationContext
    ): ConstValue {
        val expr = module.expressions[exprHandle]
        
        return when (expr) {
            is Expression.Literal -> {
                convertLiteralToConstValue(expr)
            }
            is Expression.Access -> {
                evaluateAccessExpression(expr, context)
            }
            is Expression.AccessIndex -> {
                evaluateAccessIndexExpression(expr, context)
            }
            is Expression.Splat -> {
                evaluateSplatExpression(expr, context)
            }
            is Expression.Unary -> {
                evaluateUnaryExpression(expr, context)
            }
            is Expression.Binary -> {
                evaluateBinaryExpression(expr, context)
            }
            is Expression.Select -> {
                evaluateSelectExpression(expr, context)
            }
            is Expression.ArrayLength -> {
                evaluateArrayLengthExpression(expr, context)
            }
            is Expression.As -> {
                evaluateAsExpression(expr, context)
            }
            is Expression.GlobalVariable -> {
                evaluateGlobalVariableExpression(expr, context)
            }
            is Expression.LocalVariable -> {
                evaluateLocalVariableExpression(expr, context)
            }
            is Expression.FunctionArgument -> {
                evaluateFunctionArgumentExpression(expr, context)
            }
            is Expression.Const -> {
                // Déjà une constante
                expr.value
            }
            else -> {
                ConstValue.NotConst
            }
        }
    }
    
    /**
     * Convertit un littéral en ConstValue.
     */
    private fun convertLiteralToConstValue(literal: Expression.Literal): ConstValue {
        return when (val value = literal.value) {
            is LiteralValue.Bool -> ConstValue.Scalar(ScalarValue.Bool(value.value), literal.type)
            is LiteralValue.I32 -> ConstValue.Scalar(ScalarValue.I32(value.value), literal.type)
            is LiteralValue.U32 -> ConstValue.Scalar(ScalarValue.U32(value.value.toLong()), literal.type)
            is LiteralValue.F32 -> ConstValue.Scalar(ScalarValue.F32(value.value), literal.type)
            is LiteralValue.F16 -> {
                // Convertir f16 en f32 pour l'évaluation
                ConstValue.Scalar(ScalarValue.F32(value.value.toFloat()), literal.type)
            }
            is LiteralValue.AbstractInt -> ConstValue.Scalar(ScalarValue.AbstractInt(value.value), literal.type)
            is LiteralValue.AbstractFloat -> ConstValue.Scalar(ScalarValue.AbstractFloat(value.value), literal.type)
        }
    }
    
    /**
     * Évalue une expression Access (tableau[ index ]).
     */
    private fun evaluateAccessExpression(
        expr: Expression.Access,
        context: EvaluationContext
    ): ConstValue {
        val base = evaluateExpression(expr.base, context)
        val index = evaluateExpression(expr.index, context)
        
        return when (base) {
            is ConstValue.Array -> {
                if (index is ConstValue.Scalar) {
                    val idx = index.toI32()
                    if (idx >= 0 && idx < base.elements.size) {
                        base.elements[idx]
                    } else {
                        throw EvaluationError("Array index out of bounds", expr.span)
                    }
                } else {
                    ConstValue.NotConst
                }
            }
            is ConstValue.Vector -> {
                if (index is ConstValue.Scalar) {
                    val idx = index.toI32()
                    if (idx >= 0 && idx < base.components.size) {
                        ConstValue.Scalar(base.components[idx], base.type)
                    } else {
                        throw EvaluationError("Vector index out of bounds", expr.span)
                    }
                } else {
                    ConstValue.NotConst
                }
            }
            is ConstValue.Matrix -> {
                if (index is ConstValue.Scalar) {
                    val idx = index.toI32()
                    if (idx >= 0 && idx < base.columns.size) {
                        // Retourner une colonne comme vecteur
                        ConstValue.Vector(base.columns[idx], base.type)
                    } else {
                        throw EvaluationError("Matrix column index out of bounds", expr.span)
                    }
                } else {
                    ConstValue.NotConst
                }
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une expression AccessIndex (tableau[ 42 ]).
     */
    private fun evaluateAccessIndexExpression(
        expr: Expression.AccessIndex,
        context: EvaluationContext
    ): ConstValue {
        val base = evaluateExpression(expr.base, context)
        
        return when (base) {
            is ConstValue.Array -> {
                if (expr.index >= 0 && expr.index < base.elements.size) {
                    base.elements[expr.index]
                } else {
                    throw EvaluationError("Array index out of bounds", expr.span)
                }
            }
            is ConstValue.Vector -> {
                if (expr.index >= 0 && expr.index < base.components.size) {
                    ConstValue.Scalar(base.components[expr.index], base.type)
                } else {
                    throw EvaluationError("Vector index out of bounds", expr.span)
                }
            }
            is ConstValue.Matrix -> {
                if (expr.index >= 0 && expr.index < base.columns.size) {
                    ConstValue.Vector(base.columns[expr.index], base.type)
                } else {
                    throw EvaluationError("Matrix column index out of bounds", expr.span)
                }
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une expression Splat (vec4( x )).
     */
    private fun evaluateSplatExpression(
        expr: Expression.Splat,
        context: EvaluationContext
    ): ConstValue {
        val value = evaluateExpression(expr.value, context)
        
        return when (value) {
            is ConstValue.Scalar -> {
                val type = module.types[expr.type]
                if (type is Type.Vector) {
                    val components = List(type.size.toInt()) { value }
                    ConstValue.Vector(components, expr.type)
                } else {
                    ConstValue.NotConst
                }
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une expression unaire.
     */
    private fun evaluateUnaryExpression(
        expr: Expression.Unary,
        context: EvaluationContext
    ): ConstValue {
        val operand = evaluateExpression(expr.operand, context)
        
        return when (operand) {
            is ConstValue.Scalar -> {
                evaluateUnaryOp(expr.op, operand, expr.span)
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une opération unaire sur une valeur scalaire.
     */
    private fun evaluateUnaryOp(
        op: UnaryOperator,
        operand: ConstValue.Scalar,
        span: Span
    ): ConstValue.Scalar {
        return when (op) {
            UnaryOperator.Negate -> {
                when (operand.value) {
                    is ScalarValue.I32 -> ConstValue.Scalar(ScalarValue.I32(-operand.value.value), operand.type)
                    is ScalarValue.U32 -> {
                        // Négation d'un unsigned est invalide en WGSL
                        throw EvaluationError("Cannot negate unsigned integer", span)
                    }
                    is ScalarValue.F32 -> ConstValue.Scalar(ScalarValue.F32(-operand.value.value), operand.type)
                    is ScalarValue.F64 -> ConstValue.Scalar(ScalarValue.F64(-operand.value.value), operand.type)
                    else -> throw EvaluationError("Cannot negate type ${module.types[operand.type].inner}", span)
                }
            }
            UnaryOperator.Not -> {
                when (operand.value) {
                    is ScalarValue.Bool -> ConstValue.Scalar(ScalarValue.Bool(!operand.value.value), operand.type)
                    is ScalarValue.I32 -> ConstValue.Scalar(ScalarValue.I32(if (operand.value.value == 0) 1 else 0), operand.type)
                    is ScalarValue.U32 -> ConstValue.Scalar(ScalarValue.U32(if (operand.value.value == 0L) 1L else 0L), operand.type)
                    else -> throw EvaluationError("Cannot apply logical not to type ${module.types[operand.type].inner}", span)
                }
            }
            UnaryOperator.BitNot -> {
                when (operand.value) {
                    is ScalarValue.I32 -> ConstValue.Scalar(ScalarValue.I32(operand.value.value.inv()), operand.type)
                    is ScalarValue.U32 -> ConstValue.Scalar(ScalarValue.U32(operand.value.value.inv().toLong() and 0xFFFFFFFFL), operand.type)
                    else -> throw EvaluationError("Cannot apply bitwise not to type ${module.types[operand.type].inner}", span)
                }
            }
            UnaryOperator.PreIncrement, UnaryOperator.PostIncrement -> {
                // Les incréments ne sont pas constants (ils modifient la variable)
                ConstValue.NotConst
            }
            UnaryOperator.PreDecrement, UnaryOperator.PostDecrement -> {
                // Les décréments ne sont pas constants
                ConstValue.NotConst
            }
        }
    }
    
    /**
     * Évalue une expression binaire.
     */
    private fun evaluateBinaryExpression(
        expr: Expression.Binary,
        context: EvaluationContext
    ): ConstValue {
        val left = evaluateExpression(expr.left, context)
        val right = evaluateExpression(expr.right, context)
        
        return when {
            left is ConstValue.Scalar && right is ConstValue.Scalar -> {
                evaluateBinaryOp(expr.op, left, right, expr.span)
            }
            left is ConstValue.Vector && right is ConstValue.Vector -> {
                evaluateBinaryOpVector(expr.op, left, right, expr.span)
            }
            left is ConstValue.Scalar && right is ConstValue.Vector -> {
                evaluateBinaryOpScalarVector(expr.op, left, right, expr.span)
            }
            left is ConstValue.Vector && right is ConstValue.Scalar -> {
                evaluateBinaryOpVectorScalar(expr.op, left, right, expr.span)
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une opération binaire sur des scalaires.
     */
    private fun evaluateBinaryOp(
        op: BinaryOperator,
        left: ConstValue.Scalar,
        right: ConstValue.Scalar,
        span: Span
    ): ConstValue.Scalar {
        val leftType = module.types[left.type]
        val rightType = module.types[right.type]
        
        // Vérifier la compatibilité des types
        if (leftType != rightType) {
            throw EvaluationError("Binary operator '$op' cannot be applied to types ${leftType.inner} and ${rightType.inner}", span)
        }
        
        return when (left.value) {
            is ScalarValue.I32 -> {
                val rightVal = (right.value as ScalarValue.I32).value
                val result = when (op) {
                    BinaryOperator.Add -> left.value.value + rightVal
                    BinaryOperator.Subtract -> left.value.value - rightVal
                    BinaryOperator.Multiply -> left.value.value * rightVal
                    BinaryOperator.Divide -> {
                        if (rightVal == 0) throw EvaluationError("Division by zero", span)
                        left.value.value / rightVal
                    }
                    BinaryOperator.Modulo -> {
                        if (rightVal == 0) throw EvaluationError("Modulo by zero", span)
                        left.value.value % rightVal
                    }
                    BinaryOperator.Equal -> (left.value.value == rightVal).toI32()
                    BinaryOperator.NotEqual -> (left.value.value != rightVal).toI32()
                    BinaryOperator.LessThan -> (left.value.value < rightVal).toI32()
                    BinaryOperator.LessThanEqual -> (left.value.value <= rightVal).toI32()
                    BinaryOperator.GreaterThan -> (left.value.value > rightVal).toI32()
                    BinaryOperator.GreaterThanEqual -> (left.value.value >= rightVal).toI32()
                    BinaryOperator.LogicalAnd -> (left.value.value != 0 && rightVal != 0).toI32()
                    BinaryOperator.LogicalOr -> (left.value.value != 0 || rightVal != 0).toI32()
                    BinaryOperator.BitwiseAnd -> left.value.value and rightVal
                    BinaryOperator.BitwiseOr -> left.value.value or rightVal
                    BinaryOperator.BitwiseXor -> left.value.value xor rightVal
                    BinaryOperator.ShiftLeft -> left.value.value shl rightVal
                    BinaryOperator.ShiftRight -> left.value.value shr rightVal
                    else -> throw EvaluationError("Unsupported binary operator '$op' for i32", span)
                }
                ConstValue.Scalar(ScalarValue.I32(result), left.type)
            }
            is ScalarValue.U32 -> {
                val rightVal = (right.value as ScalarValue.U32).value
                val result = when (op) {
                    BinaryOperator.Add -> left.value.value + rightVal
                    BinaryOperator.Subtract -> left.value.value - rightVal
                    BinaryOperator.Multiply -> left.value.value * rightVal
                    BinaryOperator.Divide -> {
                        if (rightVal == 0L) throw EvaluationError("Division by zero", span)
                        left.value.value / rightVal
                    }
                    BinaryOperator.Modulo -> {
                        if (rightVal == 0L) throw EvaluationError("Modulo by zero", span)
                        left.value.value % rightVal
                    }
                    BinaryOperator.Equal -> (left.value.value == rightVal).toI32()
                    BinaryOperator.NotEqual -> (left.value.value != rightVal).toI32()
                    BinaryOperator.LessThan -> (left.value.value < rightVal).toI32()
                    BinaryOperator.LessThanEqual -> (left.value.value <= rightVal).toI32()
                    BinaryOperator.GreaterThan -> (left.value.value > rightVal).toI32()
                    BinaryOperator.GreaterThanEqual -> (left.value.value >= rightVal).toI32()
                    BinaryOperator.LogicalAnd -> (left.value.value != 0L && rightVal != 0L).toI32()
                    BinaryOperator.LogicalOr -> (left.value.value != 0L || rightVal != 0L).toI32()
                    BinaryOperator.BitwiseAnd -> left.value.value and rightVal
                    BinaryOperator.BitwiseOr -> left.value.value or rightVal
                    BinaryOperator.BitwiseXor -> left.value.value xor rightVal
                    BinaryOperator.ShiftLeft -> left.value.value shl rightVal.toInt()
                    BinaryOperator.ShiftRight -> left.value.value shr rightVal.toInt()
                    else -> throw EvaluationError("Unsupported binary operator '$op' for u32", span)
                }
                ConstValue.Scalar(ScalarValue.U32(result), left.type)
            }
            is ScalarValue.F32 -> {
                val rightVal = (right.value as ScalarValue.F32).value
                val result = when (op) {
                    BinaryOperator.Add -> left.value.value + rightVal
                    BinaryOperator.Subtract -> left.value.value - rightVal
                    BinaryOperator.Multiply -> left.value.value * rightVal
                    BinaryOperator.Divide -> {
                        if (rightVal == 0f) throw EvaluationError("Division by zero", span)
                        left.value.value / rightVal
                    }
                    BinaryOperator.Modulo -> {
                        if (rightVal == 0f) throw EvaluationError("Modulo by zero", span)
                        left.value.value % rightVal
                    }
                    BinaryOperator.Equal -> (left.value.value == rightVal).toI32()
                    BinaryOperator.NotEqual -> (left.value.value != rightVal).toI32()
                    BinaryOperator.LessThan -> (left.value.value < rightVal).toI32()
                    BinaryOperator.LessThanEqual -> (left.value.value <= rightVal).toI32()
                    BinaryOperator.GreaterThan -> (left.value.value > rightVal).toI32()
                    BinaryOperator.GreaterThanEqual -> (left.value.value >= rightVal).toI32()
                    BinaryOperator.LogicalAnd -> (left.value.value != 0f && rightVal != 0f).toI32()
                    BinaryOperator.LogicalOr -> (left.value.value != 0f || rightVal != 0f).toI32()
                    else -> throw EvaluationError("Unsupported binary operator '$op' for f32", span)
                }
                ConstValue.Scalar(ScalarValue.F32(result), left.type)
            }
            is ScalarValue.Bool -> {
                val rightVal = (right.value as ScalarValue.Bool).value
                val result = when (op) {
                    BinaryOperator.LogicalAnd -> left.value.value && rightVal
                    BinaryOperator.LogicalOr -> left.value.value || rightVal
                    BinaryOperator.Equal -> left.value.value == rightVal
                    BinaryOperator.NotEqual -> left.value.value != rightVal
                    else -> throw EvaluationError("Unsupported binary operator '$op' for bool", span)
                }
                ConstValue.Scalar(ScalarValue.Bool(result), left.type)
            }
            else -> throw EvaluationError("Unsupported scalar type for binary operator", span)
        }
    }
    
    /**
     * Évalue une opération binaire sur des vecteurs.
     */
    private fun evaluateBinaryOpVector(
        op: BinaryOperator,
        left: ConstValue.Vector,
        right: ConstValue.Vector,
        span: Span
    ): ConstValue.Vector {
        if (left.components.size != right.components.size) {
            throw EvaluationError("Vector size mismatch", span)
        }
        
        val result = mutableListOf<ScalarValue>()
        for (i in left.components.indices) {
            val l = ConstValue.Scalar(left.components[i], left.type)
            val r = ConstValue.Scalar(right.components[i], right.type)
            val componentResult = evaluateBinaryOp(op, l, r, span)
            result.add(componentResult.value)
        }
        
        return ConstValue.Vector(result, left.type)
    }
    
    /**
     * Évalue une opération binaire scalaire + vecteur.
     */
    private fun evaluateBinaryOpScalarVector(
        op: BinaryOperator,
        left: ConstValue.Scalar,
        right: ConstValue.Vector,
        span: Span
    ): ConstValue.Vector {
        val result = mutableListOf<ScalarValue>()
        for (component in right.components) {
            val r = ConstValue.Scalar(component, right.type)
            val componentResult = evaluateBinaryOp(op, left, r, span)
            result.add(componentResult.value)
        }
        return ConstValue.Vector(result, right.type)
    }
    
    /**
     * Évalue une opération binaire vecteur + scalaire.
     */
    private fun evaluateBinaryOpVectorScalar(
        op: BinaryOperator,
        left: ConstValue.Vector,
        right: ConstValue.Scalar,
        span: Span
    ): ConstValue.Vector {
        // C'est la même chose que Scalaire + Vecteur
        return evaluateBinaryOpScalarVector(op, right, left, span)
    }
    
    /**
     * Évalue une expression Select.
     */
    private fun evaluateSelectExpression(
        expr: Expression.Select,
        context: EvaluationContext
    ): ConstValue {
        val condition = evaluateExpression(expr.condition, context)
        val accept = evaluateExpression(expr.accept, context)
        val reject = evaluateExpression(expr.reject, context)
        
        return when (condition) {
            is ConstValue.Scalar -> {
                val condValue = condition.toBool()
                if (condValue) accept else reject
            }
            is ConstValue.Vector -> {
                // select avec condition vectorielle
                // Retourne un vecteur avec les composantes sélectionnées
                if (accept is ConstValue.Vector && reject is ConstValue.Vector) {
                    val result = mutableListOf<ScalarValue>()
                    for (i in condition.components.indices) {
                        val selected = if (condition.components[i].toBool()) {
                            accept.components[i]
                        } else {
                            reject.components[i]
                        }
                        result.add(selected)
                    }
                    ConstValue.Vector(result, accept.type)
                } else {
                    ConstValue.NotConst
                }
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une expression ArrayLength.
     */
    private fun evaluateArrayLengthExpression(
        expr: Expression.ArrayLength,
        context: EvaluationContext
    ): ConstValue {
        val array = evaluateExpression(expr.array, context)
        
        return when (array) {
            is ConstValue.Array -> {
                ConstValue.Scalar(ScalarValue.U32(array.elements.size.toLong()), expr.type)
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Évalue une expression As (cast).
     */
    private fun evaluateAsExpression(
        expr: Expression.As,
        context: EvaluationContext
    ): ConstValue {
        val value = evaluateExpression(expr.value, context)
        
        return when (value) {
            is ConstValue.Scalar -> {
                val fromType = module.types[value.type]
                val toType = module.types[expr.type]
                
                if (fromType == toType) {
                    return value // Pas besoin de cast
                }
                
                // Cast entre types scalaires
                if (fromType is Type.Scalar && toType is Type.Scalar) {
                    castScalar(value, toType.inner, expr.span)
                } else {
                    ConstValue.NotConst
                }
            }
            is ConstValue.Vector -> {
                // Cast de vecteur
                val fromType = module.types[value.type]
                val toType = module.types[expr.type]
                
                if (fromType is Type.Vector && toType is Type.Vector) {
                    if (fromType.size == toType.size) {
                        val newComponents = value.components.map { component ->
                            val scalarValue = ConstValue.Scalar(component, value.type)
                            val scalarType = module.types[toType.width]
                            castScalar(scalarValue, scalarType.inner, expr.span).value
                        }
                        ConstValue.Vector(newComponents, expr.type)
                    } else {
                        throw EvaluationError("Cannot cast vector of size ${fromType.size} to ${toType.size}", expr.span)
                    }
                } else {
                    ConstValue.NotConst
                }
            }
            else -> ConstValue.NotConst
        }
    }
    
    /**
     * Cast une valeur scalaire vers un autre type.
     */
    private fun castScalar(
        value: ConstValue.Scalar,
        targetKind: ScalarKind,
        span: Span
    ): ConstValue.Scalar {
        return when (value.value) {
            is ScalarValue.I32 -> {
                when (targetKind) {
                    ScalarKind.I32 -> value
                    ScalarKind.U32 -> ConstValue.Scalar(ScalarValue.U32(value.value.value.toLong() and 0xFFFFFFFFL), value.type)
                    ScalarKind.F32 -> ConstValue.Scalar(ScalarValue.F32(value.value.value.toFloat()), value.type)
                    ScalarKind.F64 -> ConstValue.Scalar(ScalarValue.F64(value.value.value.toDouble()), value.type)
                    else -> throw EvaluationError("Cannot cast i32 to $targetKind", span)
                }
            }
            is ScalarValue.U32 -> {
                when (targetKind) {
                    ScalarKind.I32 -> ConstValue.Scalar(ScalarValue.I32(value.value.value.toInt()), value.type)
                    ScalarKind.U32 -> value
                    ScalarKind.F32 -> ConstValue.Scalar(ScalarValue.F32(value.value.value.toFloat()), value.type)
                    ScalarKind.F64 -> ConstValue.Scalar(ScalarValue.F64(value.value.value.toDouble()), value.type)
                    else -> throw EvaluationError("Cannot cast u32 to $targetKind", span)
                }
            }
            is ScalarValue.F32 -> {
                when (targetKind) {
                    ScalarKind.I32 -> ConstValue.Scalar(ScalarValue.I32(value.value.value.toInt()), value.type)
                    ScalarKind.U32 -> ConstValue.Scalar(ScalarValue.U32(value.value.value.toLong() and 0xFFFFFFFFL), value.type)
                    ScalarKind.F32 -> value
                    ScalarKind.F64 -> ConstValue.Scalar(ScalarValue.F64(value.value.value.toDouble()), value.type)
                    else -> throw EvaluationError("Cannot cast f32 to $targetKind", span)
                }
            }
            is ScalarValue.F64 -> {
                when (targetKind) {
                    ScalarKind.I32 -> ConstValue.Scalar(ScalarValue.I32(value.value.value.toInt()), value.type)
                    ScalarKind.U32 -> ConstValue.Scalar(ScalarValue.U32(value.value.value.toLong() and 0xFFFFFFFFL), value.type)
                    ScalarKind.F32 -> ConstValue.Scalar(ScalarValue.F32(value.value.value.toFloat()), value.type)
                    ScalarKind.F64 -> value
                    else -> throw EvaluationError("Cannot cast f64 to $targetKind", span)
                }
            }
            else -> throw EvaluationError("Cannot cast from ${value.value::class.simpleName} to $targetKind", span)
        }
    }
    
    /**
     * Évalue une référence à une variable globale.
     */
    private fun evaluateGlobalVariableExpression(
        expr: Expression.GlobalVariable,
        context: EvaluationContext
    ): ConstValue {
        val varHandle = expr.variable
        val globalVar = module.globalVariables[varHandle]
        
        // Seules les constantes globales (const) ont une valeur constante
        if (globalVar.class.isConst) {
            if (globalVar.init != null) {
                return evaluateExpression(globalVar.init, context)
            }
        }
        
        return ConstValue.NotConst
    }
    
    /**
     * Évalue une référence à une variable locale.
     */
    private fun evaluateLocalVariableExpression(
        expr: Expression.LocalVariable,
        context: EvaluationContext
    ): ConstValue {
        val varHandle = expr.variable
        
        // Vérifier si c'est une constante locale
        val local = context.function?.locals?.get(varHandle)
        if (local != null && local.init != null) {
            return evaluateExpression(local.init, context)
        }
        
        return ConstValue.NotConst
    }
    
    /**
     * Évalue une référence à un argument de fonction.
     */
    private fun evaluateFunctionArgumentExpression(
        expr: Expression.FunctionArgument,
        context: EvaluationContext
    ): ConstValue {
        // Les arguments de fonction ne sont pas constants (sauf si le paramètre est marqué const)
        return ConstValue.NotConst
    }
}

/**
 * Contexte d'évaluation.
 */
private data class EvaluationContext(
    val module: Module,
    val function: Function?
)

/**
 * Erreur d'évaluation.
 */
class EvaluationError(message: String, val span: Span) : RuntimeException(message)

/**
 * Extensions utilitaires.
 */

/**
 * Convertit une ScalarValue en Int pour les indices.
 */
fun ScalarValue.toI32(): Int = when (this) {
    is ScalarValue.I32 -> value
    is ScalarValue.U32 -> value.toInt()
    is ScalarValue.Bool -> if (value) 1 else 0
    else -> throw IllegalArgumentException("Cannot convert ${this::class.simpleName} to Int")
}

/**
 * Convertit une ScalarValue en Boolean.
 */
fun ScalarValue.toBool(): Boolean = when (this) {
    is ScalarValue.Bool -> value
    is ScalarValue.I32 -> value != 0
    is ScalarValue.U32 -> value != 0L
    is ScalarValue.F32 -> value != 0f
    is ScalarValue.F64 -> value != 0.0
    else -> false
}

/**
 * Convertit une ConstValue.Scalar en Int.
 */
fun ConstValue.Scalar.toI32(): Int = value.toI32()

/**
 * Convertit une ConstValue.Scalar en Boolean.
 */
fun ConstValue.Scalar.toBool(): Boolean = value.toBool()

/**
 * Crée une ScalarValue.I32 à partir d'un Boolean.
 */
fun Boolean.toI32(): Int = if (this) 1 else 0
```

---

## 📝 UTILISATION

### Intégration avec le pipeline de compilation

```kotlin
// 1. Parser le code WGSL en AST
val parser = Parser(wgslSource)
val translationUnit = parser.parse()

// 2. Résoudre les types
val typeResolver = TypeResolver(translationUnit)
typeResolver.resolve()

// 3. Convertir AST en IR
val astToIr = AstToIrConverter(translationUnit)
val module = astToIr.convert()

// 4. Évaluer les constantes
val constantEvaluator = ConstantEvaluator(module)
constantEvaluator.evaluateModule()

// 5. Le module contient maintenant les expressions constantes évaluées
```

### Utilisation directe

```kotlin
val module = ... // Module IR
val evaluator = ConstantEvaluator(module)

// Évaluer une expression spécifique
val exprHandle = ...
val result = evaluator.tryEvaluate(exprHandle)

if (result != null) {
    println("Expression évaluée à: ${result.value}")
} else {
    println("Expression non constante")
}

// Vérifier les erreurs
if (evaluator.errors.isNotEmpty()) {
    for (error in evaluator.errors) {
        println("Erreur: ${error.message} à ${error.span}")
    }
}
```

---

## ✅ CHECKLIST PHASE 3.0

### Structure ConstValue
- [ ] `ConstValue` sealed class
- [ ] `Scalar` avec tous les types scalaires
- [ ] `Vector` avec composantes
- [ ] `Matrix` avec colonnes
- [ ] `Array` avec éléments
- [ ] `Struct` avec membres
- [ ] `Pointer` (optionnel)
- [ ] `NotConst` sentinelle
- [ ] `ScalarValue` sealed class
- [ ] Toutes les variantes de ScalarValue

### ConstantEvaluator
- [ ] Classe principale `ConstantEvaluator`
- [ ] `EvaluationContext`
- [ ] `tryEvaluate()` - Évaluation avec retour optionnel
- [ ] `evaluate()` - Évaluation avec exception
- [ ] `evaluateModule()` - Évaluation complète d'un module
- [ ] `evaluateFunction()` - Évaluation d'une fonction
- [ ] `evaluateStatement()` - Évaluation d'une instruction
- [ ] `evaluateExpression()` - Évaluation d'une expression

### Évaluation des Expressions
- [ ] Literal
- [ ] Access
- [ ] AccessIndex
- [ ] Splat
- [ ] Unary (tous les opérateurs)
- [ ] Binary (tous les opérateurs, scalaires et vecteurs)
- [ ] Select
- [ ] ArrayLength
- [ ] As (cast)
- [ ] GlobalVariable
- [ ] LocalVariable
- [ ] FunctionArgument

### Opérations supportées
- [ ] Opérations arithmétiques (+, -, *, /, %)
- [ ] Opérations logiques (&&, ||, !)
- [ ] Opérations de comparaison (==, !=, <, >, <=, >=)
- [ ] Opérations bits (&, |, ^, ~, <<, >>)
- [ ] Casts entre types scalaires
- [ ] Casts entre types vecteurs
- [ ] Select (scalaire et vectoriel)

### Gestion des erreurs
- [ ] Détection des divisions par zéro
- [ ] Détection des modulo par zéro
- [ ] Détection des index out of bounds
- [ ] Détection des casts invalides
- [ ] Messages d'erreur clairs avec spans

### Tests
- [ ] `ConstantEvaluatorTest.kt`
  - [ ] Littéraux simples
  - [ ] Expressions arithmétiques
  - [ ] Expressions logiques
  - [ ] Expressions bits
  - [ ] Comparaisons
  - [ ] Casts
  - [ ] Vecteurs
  - [ ] Matrices
  - [ ] Tableaux
  - [ ] Select
  - [ ] ArrayLength
  - [ ] Splat
  - [ ] Erreurs (division par zéro, etc.)
  - [ ] Expressions non constantes

### Documentation
- [ ] KDoc complet pour toutes les classes
- [ ] KDoc complet pour toutes les méthodes
- [ ] Exemples d'utilisation
- [ ] Documentation des limitations

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Concevoir ConstValue | 1 jour | IR Structures | [ ] |
| Implémenter ConstValue et ScalarValue | 2 jours | Conception | [ ] |
| Implémenter ConstantEvaluator class | 2 jours | ConstValue | [ ] |
| Implémenter evaluateExpression | 5 jours | Evaluator | [ ] |
| Implémenter les opérateurs arithmétiques | 2 jours | evaluateExpression | [ ] |
| Implémenter les opérateurs logiques | 1 jour | evaluateExpression | [ ] |
| Implémenter les opérateurs bits | 1 jour | evaluateExpression | [ ] |
| Implémenter les opérateurs de comparaison | 1 jour | evaluateExpression | [ ] |
| Implémenter les casts | 2 jours | evaluateExpression | [ ] |
| Implémenter les opérations vecteurs | 3 jours | evaluateExpression | [ ] |
| Implémenter Select | 1 jour | evaluateExpression | [ ] |
| Implémenter ArrayLength | 0.5 jour | evaluateExpression | [ ] |
| Implémenter la gestion des erreurs | 1 jour | Tout | [ ] |
| Intégrer avec le pipeline | 1 jour | Tout | [ ] |
| Écrire les tests unitaires | 3 jours | Tout | [ ] |
| Documentation complète | 1 jour | Tout | [ ] |
| Validation manuelle | 2 jours | Tout | [ ] |

**Total estimé** : **2-3 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `ConstValue.kt`
   - `ConstantEvaluator.kt`

2. **Fichiers modifiés** :
   - Aucun (nouveaux fichiers)

3. **Tests unitaires** :
   - `ConstantEvaluatorTest.kt`

4. **Couverture de test** : > 95%

5. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/constant_evaluator.rs`
- **IR Structures** : `/Users/chaos/RustroverProjects/wgpu/naga/src/ir/mod.rs`
- **Fichier précédent** : `04_error-handling.md` (Phase 2)
- **Fichier suivant** : `01_typifier.md`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Finaliser la conception de ConstValue
2. [ ] Implémenter ConstValue et ScalarValue
3. [ ] Implémenter ConstantEvaluator
4. [ ] Implémenter toutes les opérations
5. [ ] Écrire les tests
6. [ ] Valider avec des tests manuels
7. [ ] Passer à `01_typifier.md` (typage)
