# 🏷️ Phase 3.1 : Typifier

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:core`  
**Phase** : 3 - Processing  
**Sous-Phase** : 3.1 - Typifier  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Inférence et validation des types)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/typifier.rs` (~1500 lignes)

---

## 📋 OBJECTIFS

Implémenter le **typifier** qui assigne et infère les types pour toutes les expressions dans l'IR. Le typifier :
- Assigne les types explicites des déclarations
- Infère les types des expressions basés sur leurs opérandes
- Vérifie la compatibilité des types (type checking)
- Résout les types des littéraux
- Gère les conversions implicites

**Livrable principal** : Un module IR complètement typé où chaque expression a un type assigné.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce que le Typifier ?

Le typifier est responsable de **l'inférence de type** et du **type checking** dans l'IR. Contrairement au parser qui construit l'AST, le typifier :
- Travaille sur l'IR (après conversion de l'AST)
- Assigne les types aux expressions
- Vérifie que les opérations sont valides pour les types impliqués
- Infère les types quand ils ne sont pas explicites

### 2. Types d'inférence

| Type d'inférence | Exemple | Résultat |
|-----------------|---------|----------|
| Littéraux | `42` | `i32` (par défaut) |
| Binaire | `a + b` où `a: i32, b: i32` | `i32` |
| Unaire | `-x` où `x: f32` | `f32` |
| Appel de fonction | `myFunc(1, 2)` | Type de retour de `myFunc` |
| Accès membre | `v.x` où `v: vec4<f32>` | `f32` |
| Accès index | `arr[0]` où `arr: array<i32, 10>` | `i32` |

### 3. Rules de Type Checking WGSL

- **Compatibilité** : Les types doivent être compatibles pour les opérations
- **Promotion** : Pas de promotion automatique (ex: `i32 + f32` est une erreur)
- **Casts explicites** : Utiliser `as` pour convertir entre types
- **Constructeurs** : `vec3<f32>(1, 2, 3)` crée un `vec3<f32>`

### 4. Architecture

```
IR Module (non typé)
    ↓
Typifier.process()
    ↓
1. Assigner les types explicites
    ↓
2. Inférer les types des expressions
    ↓
3. Vérifier la compatibilité
    ↓
4. Résoudre les conversions implicites
    ↓
IR Module (complètement typé)
    ↓
Validation ou Erreur
```

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. Typifier.kt (Classe principale)

**Fichier** : `wgsl:core/src/main/kotlin/dev/gfxrs/naga/proc/Typifier.kt`

```kotlin
package io.ygdrasil.wgsl.proc

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.ir.*

/**
 * Assigne et infère les types pour toutes les expressions dans un module.
 */
class Typifier(private val module: Module) {
    
    /**
     * Erreur de typage.
     */
    data class TypeError(
        val message: String,
        val span: Span
    )
    
    /**
     * Liste des erreurs de typage.
     */
    val errors: MutableList<TypeError> = mutableListOf()
    
    /**
     * Context pour le typage.
     */
    private data class Context(
        val function: Function? = null,
        val returnType: Handle<Type>? = null
    )
    
    /**
     * Traite tout le module et assigne les types.
     * Retourne true si tout est typé correctement.
     */
    fun process(): Boolean {
        // 1. Traiter les types explicites des déclarations
        processGlobalDeclarations()
        
        // 2. Traiter chaque fonction
        for (func in module.functions) {
            processFunction(func)
        }
        
        // 3. Traiter les expressions globales
        processGlobalExpressions()
        
        return errors.isEmpty()
    }
    
    /**
     * Traite les déclarations globales.
     */
    private fun processGlobalDeclarations() {
        for (var in module.globalVariables) {
            processGlobalVariable(var)
        }
    }
    
    /**
     * Traite une variable globale.
     */
    private fun processGlobalVariable(var: GlobalVariable) {
        // Le type est déjà assigné par le parser
        // Mais on doit inférer le type de l'initialisation
        if (var.init != null) {
            val context = Context()
            val initType = inferExpressionType(var.init, context)
            
            // Vérifier la compatibilité
            if (!areTypesCompatible(var.class.type, initType)) {
                error("Type mismatch: cannot initialize ${var.class.type} with ${initType}", var.span)
            }
            
            // Stocker le type inféré pour l'expression
            module.expressions[var.init].type = initType
        }
    }
    
    /**
     * Traite une fonction.
     */
    private fun processFunction(func: Function) {
        val context = Context(function = func, returnType = func.result.type)
        
        // Traiter les paramètres
        for (param in func.parameters) {
            // Le type est déjà assigné
        }
        
        // Traiter les variables locales
        for (local in func.locals) {
            processLocalVariable(local, context)
        }
        
        // Traiter le body
        processStatements(func.body, context)
    }
    
    /**
     * Traite une variable locale.
     */
    private fun processLocalVariable(local: LocalVariable, context: Context) {
        if (local.init != null) {
            val initType = inferExpressionType(local.init, context)
            
            if (local.type == Handle.INVALID) {
                // Inférer le type de l'initialisation
                local.type = initType
            } else {
                // Vérifier la compatibilité
                if (!areTypesCompatible(local.type, initType)) {
                    error("Type mismatch: cannot initialize ${local.type} with ${initType}", local.span)
                }
            }
            
            module.expressions[local.init].type = initType
        }
    }
    
    /**
     * Traite des instructions.
     */
    private fun processStatements(statements: List<Handle<Statement>>, context: Context) {
        for (stmtHandle in statements) {
            processStatement(stmtHandle, context)
        }
    }
    
    /**
     * Traite une instruction.
     */
    private fun processStatement(stmtHandle: Handle<Statement>, context: Context) {
        val stmt = module.statements[stmtHandle]
        
        when (stmt) {
            is Statement.Let -> {
                if (stmt.init != null) {
                    val initType = inferExpressionType(stmt.init, context)
                    
                    if (stmt.variable.type == Handle.INVALID) {
                        stmt.variable.type = initType
                    } else {
                        if (!areTypesCompatible(stmt.variable.type, initType)) {
                            error("Type mismatch in let declaration", stmt.span)
                        }
                    }
                    
                    module.expressions[stmt.init].type = initType
                }
            }
            is Statement.Assign -> {
                val lhsType = inferLValueType(stmt.lhs, context)
                val rhsType = inferExpressionType(stmt.value, context)
                
                if (!areTypesCompatible(lhsType, rhsType)) {
                    error("Type mismatch in assignment: cannot assign ${rhsType} to ${lhsType}", stmt.span)
                }
                
                module.expressions[stmt.value].type = rhsType
            }
            is Statement.Store -> {
                val ptrType = inferExpressionType(stmt.ptr, context)
                val valueType = inferExpressionType(stmt.value, context)
                
                if (ptrType is Type.Pointer) {
                    if (!areTypesCompatible(ptrType.element, valueType)) {
                        error("Type mismatch in store: cannot store ${valueType} to pointer of ${ptrType.element}", stmt.span)
                    }
                }
                
                module.expressions[stmt.value].type = valueType
            }
            is Statement.If -> {
                val condType = inferExpressionType(stmt.condition, context)
                if (!isBooleanType(condType)) {
                    error("If condition must be boolean, got ${module.types[condType].inner}", stmt.span)
                }
                
                processStatements(stmt.accept, context)
                if (stmt.reject != null) {
                    processStatements(stmt.reject, context)
                }
            }
            is Statement.Switch -> {
                val selectorType = inferExpressionType(stmt.selector, context)
                
                // Le type du selector doit être un type "switchable" (scalaire, vecteur, etc.)
                if (!isSwitchableType(selectorType)) {
                    error("Switch selector must be an integer, boolean, or enumeration, got ${module.types[selectorType].inner}", stmt.span)
                }
                
                for (case in stmt.body) {
                    for (value in case.values) {
                        val caseType = inferExpressionType(value, context)
                        if (!areTypesCompatible(selectorType, caseType)) {
                            error("Switch case type ${caseType} does not match selector type ${selectorType}", value.span)
                        }
                    }
                    processStatements(case.body, context)
                }
            }
            is Statement.Loop -> {
                processStatements(stmt.body, context)
            }
            is Statement.While -> {
                val condType = inferExpressionType(stmt.condition, context)
                if (!isBooleanType(condType)) {
                    error("While condition must be boolean", stmt.span)
                }
                processStatements(stmt.body, context)
            }
            is Statement.For -> {
                // Traiter l'init
                if (stmt.init != null) {
                    processStatement(stmt.init, context)
                }
                
                // Traiter la condition
                if (stmt.condition != null) {
                    val condType = inferExpressionType(stmt.condition, context)
                    if (!isBooleanType(condType)) {
                        error("For condition must be boolean", stmt.span)
                    }
                }
                
                // Traiter l'update
                if (stmt.update != null) {
                    processStatement(stmt.update, context)
                }
                
                processStatements(stmt.body, context)
            }
            is Statement.BreakIf -> {
                val condType = inferExpressionType(stmt.condition, context)
                if (!isBooleanType(condType)) {
                    error("BreakIf condition must be boolean", stmt.span)
                }
            }
            is Statement.ContinueIf -> {
                val condType = inferExpressionType(stmt.condition, context)
                if (!isBooleanType(condType)) {
                    error("ContinueIf condition must be boolean", stmt.span)
                }
            }
            is Statement.Return -> {
                if (stmt.value != null) {
                    val valueType = inferExpressionType(stmt.value, context)
                    
                    if (context.returnType != null && context.returnType != Handle.INVALID) {
                        if (!areTypesCompatible(context.returnType!!, valueType)) {
                            error("Return type ${valueType} does not match function return type ${context.returnType}", stmt.span)
                        }
                    }
                } else {
                    // return sans valeur
                    if (context.returnType != null && context.returnType != Handle.INVALID) {
                        val returnType = module.types[context.returnType!!]
                        if (returnType.inner != TypeInner.Void) {
                            error("Function returns ${returnType.inner}, but no value is returned", stmt.span)
                        }
                    }
                }
            }
            is Statement.Discard -> {
                // Pas de type à inférer
            }
            is Statement.Block -> {
                processStatements(stmt.children, context)
            }
            is Statement.Emit -> {
                // Traiter les expressions dans la range
                for (i in stmt.range.start..stmt.range.endInclusive) {
                    inferExpressionType(Handle(i), context)
                }
            }
        }
    }
    
    /**
     * Infère le type d'une expression.
     */
    fun inferExpressionType(exprHandle: Handle<Expression>, context: Context): Handle<Type> {
        val expr = module.expressions[exprHandle]
        
        // Si le type est déjà connu, le retourner
        if (expr.type != Handle.INVALID) {
            return expr.type
        }
        
        val type = when (expr) {
            is Expression.Literal -> inferLiteralType(expr)
            is Expression.Access -> inferAccessType(expr, context)
            is Expression.AccessIndex -> inferAccessIndexType(expr, context)
            is Expression.Splat -> inferSplatType(expr, context)
            is Expression.Unary -> inferUnaryType(expr, context)
            is Expression.Binary -> inferBinaryType(expr, context)
            is Expression.Select -> inferSelectType(expr, context)
            is Expression.ArrayLength -> inferArrayLengthType(expr, context)
            is Expression.As -> expr.type  // Type explicite dans As
            is Expression.GlobalVariable -> module.globalVariables[expr.variable].class.type
            is Expression.LocalVariable -> module.functions[context.function!!].locals[expr.variable].type
            is Expression.FunctionArgument -> {
                val func = context.function!!
                module.types[func.parameters[expr.argument].type]
            }
            is Expression.Const -> module.constants[expr.constant].specialization.type
            else -> {
                error("Cannot infer type for expression: ${expr::class.simpleName}", expr.span)
                module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
            }
        }
        
        // Stocker le type dans l'expression
        module.expressions[exprHandle].type = type
        
        return type
    }
    
    /**
     * Infère le type d'un littéral.
     */
    private fun inferLiteralType(literal: Expression.Literal): Handle<Type> {
        return when (literal.value) {
            is LiteralValue.Bool -> module.types.append(Type.Scalar(ScalarKind.Bool, literal.span))
            is LiteralValue.I32 -> module.types.append(Type.Scalar(ScalarKind.I32, literal.span))
            is LiteralValue.U32 -> module.types.append(Type.Scalar(ScalarKind.U32, literal.span))
            is LiteralValue.F32 -> module.types.append(Type.Scalar(ScalarKind.F32, literal.span))
            is LiteralValue.F16 -> module.types.append(Type.Scalar(ScalarKind.F16, literal.span))
            is LiteralValue.AbstractInt -> module.types.append(Type.Scalar(ScalarKind.AbstractInt, literal.span))
            is LiteralValue.AbstractFloat -> module.types.append(Type.Scalar(ScalarKind.AbstractFloat, literal.span))
        }
    }
    
    /**
     * Infère le type d'un accès (base[ index ]).
     */
    private fun inferAccessType(expr: Expression.Access, context: Context): Handle<Type> {
        val baseType = inferExpressionType(expr.base, context)
        val indexType = inferExpressionType(expr.index, context)
        
        return when (val base = module.types[baseType].inner) {
            is TypeInner.Vector -> {
                if (isIntegerType(indexType)) {
                    module.types.append(Type.Scalar(base.width.inner, expr.span))
                } else {
                    error("Vector index must be an integer", expr.span)
                    module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
                }
            }
            is TypeInner.Array -> {
                if (isIntegerType(indexType)) {
                    base.element
                } else {
                    error("Array index must be an integer", expr.span)
                    module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
                }
            }
            is TypeInner.Matrix -> {
                if (isIntegerType(indexType)) {
                    // Accès à une colonne (retourne un vecteur)
                    val colType = base.columns
                    module.types.append(Type.Vector(colType, base.width, expr.span))
                } else {
                    error("Matrix index must be an integer", expr.span)
                    module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
                }
            }
            else -> {
                error("Cannot index into type ${base}", expr.span)
                module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
            }
        }
    }
    
    /**
     * Infère le type d'un accès index constant (base[ 42 ]).
     */
    private fun inferAccessIndexType(expr: Expression.AccessIndex, context: Context): Handle<Type> {
        val baseType = inferExpressionType(expr.base, context)
        
        return when (val base = module.types[baseType].inner) {
            is TypeInner.Vector -> {
                module.types.append(Type.Scalar(base.width.inner, expr.span))
            }
            is TypeInner.Array -> {
                base.element
            }
            is TypeInner.Matrix -> {
                // Retourne un vecteur (une colonne)
                val colType = base.columns
                module.types.append(Type.Vector(colType, base.width, expr.span))
            }
            else -> {
                error("Cannot index into type ${base}", expr.span)
                module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
            }
        }
    }
    
    /**
     * Infère le type d'un splat (vec4( x )).
     */
    private fun inferSplatType(expr: Expression.Splat, context: Context): Handle<Type> {
        // Le type est déjà assigné par le parser
        expr.type
    }
    
    /**
     * Infère le type d'une expression unaire.
     */
    private fun inferUnaryType(expr: Expression.Unary, context: Context): Handle<Type> {
        val operandType = inferExpressionType(expr.operand, context)
        
        return when (expr.op) {
            UnaryOperator.Negate -> operandType
            UnaryOperator.Not -> {
                when (module.types[operandType].inner) {
                    is TypeInner.Scalar -> {
                        when ((module.types[operandType].inner as TypeInner.Scalar).kind) {
                            ScalarKind.Bool, ScalarKind.I32, ScalarKind.U32 -> operandType
                            else -> {
                                error("Cannot apply logical not to ${module.types[operandType].inner}", expr.span)
                                operandType
                            }
                        }
                    }
                    else -> {
                        error("Cannot apply logical not to non-scalar type", expr.span)
                        operandType
                    }
                }
            }
            UnaryOperator.BitNot -> operandType
            UnaryOperator.PreIncrement, UnaryOperator.PostIncrement -> operandType
            UnaryOperator.PreDecrement, UnaryOperator.PostDecrement -> operandType
        }
    }
    
    /**
     * Infère le type d'une expression binaire.
     */
    private fun inferBinaryType(expr: Expression.Binary, context: Context): Handle<Type> {
        val leftType = inferExpressionType(expr.left, context)
        val rightType = inferExpressionType(expr.right, context)
        
        // Vérifier la compatibilité
        if (!areTypesCompatible(leftType, rightType)) {
            error("Binary operator '${expr.op}' cannot be applied to types ${module.types[leftType].inner} and ${module.types[rightType].inner}", expr.span)
        }
        
        return when (expr.op) {
            // Arithmétiques
            BinaryOperator.Add,
            BinaryOperator.Subtract,
            BinaryOperator.Multiply,
            BinaryOperator.Divide,
            BinaryOperator.Modulo -> leftType
            
            // Comparaison (retourne bool)
            BinaryOperator.Equal,
            BinaryOperator.NotEqual,
            BinaryOperator.LessThan,
            BinaryOperator.LessThanEqual,
            BinaryOperator.GreaterThan,
            BinaryOperator.GreaterThanEqual -> {
                module.types.append(Type.Scalar(ScalarKind.Bool, expr.span))
            }
            
            // Logique (retourne bool)
            BinaryOperator.LogicalAnd,
            BinaryOperator.LogicalOr -> {
                module.types.append(Type.Scalar(ScalarKind.Bool, expr.span))
            }
            
            // Bits
            BinaryOperator.BitwiseAnd,
            BinaryOperator.BitwiseOr,
            BinaryOperator.BitwiseXor,
            BinaryOperator.ShiftLeft,
            BinaryOperator.ShiftRight -> leftType
        }
    }
    
    /**
     * Infère le type d'une expression Select.
     */
    private fun inferSelectType(expr: Expression.Select, context: Context): Handle<Type> {
        val conditionType = inferExpressionType(expr.condition, context)
        val acceptType = inferExpressionType(expr.accept, context)
        val rejectType = inferExpressionType(expr.reject, context)
        
        // Condition doit être bool ou vecteur de bool
        if (!isBooleanType(conditionType) && !isVectorOfBooleanType(conditionType)) {
            error("Select condition must be boolean or vector of boolean", expr.span)
        }
        
        // accept et reject doivent avoir le même type
        if (!areTypesCompatible(acceptType, rejectType)) {
            error("Select accept and reject types must match", expr.span)
        }
        
        return acceptType
    }
    
    /**
     * Infère le type d'une expression ArrayLength.
     */
    private fun inferArrayLengthType(expr: Expression.ArrayLength, context: Context): Handle<Type> {
        // Retourne toujours u32
        module.types.append(Type.Scalar(ScalarKind.U32, expr.span))
    }
    
    /**
     * Infère le type d'une LValue (pour l'assignment).
     */
    private fun inferLValueType(exprHandle: Handle<Expression>, context: Context): Handle<Type> {
        val expr = module.expressions[exprHandle]
        
        return when (expr) {
            is Expression.Access -> {
                val baseType = inferExpressionType(expr.base, context)
                when (val base = module.types[baseType].inner) {
                    is TypeInner.Vector -> {
                        module.types.append(Type.Scalar(base.width.inner, expr.span))
                    }
                    is TypeInner.Array -> {
                        base.element
                    }
                    is TypeInner.Matrix -> {
                        val colType = base.columns
                        module.types.append(Type.Vector(colType, base.width, expr.span))
                    }
                    else -> {
                        error("Cannot assign to index of type ${base}", expr.span)
                        module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
                    }
                }
            }
            is Expression.AccessIndex -> {
                val baseType = inferExpressionType(expr.base, context)
                when (val base = module.types[baseType].inner) {
                    is TypeInner.Vector -> {
                        module.types.append(Type.Scalar(base.width.inner, expr.span))
                    }
                    is TypeInner.Array -> {
                        base.element
                    }
                    is TypeInner.Matrix -> {
                        val colType = base.columns
                        module.types.append(Type.Vector(colType, base.width, expr.span))
                    }
                    else -> {
                        error("Cannot assign to index of type ${base}", expr.span)
                        module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
                    }
                }
            }
            is Expression.GlobalVariable -> {
                module.globalVariables[expr.variable].class.type
            }
            is Expression.LocalVariable -> {
                module.functions[context.function!!].locals[expr.variable].type
            }
            else -> {
                error("Invalid lvalue: ${expr::class.simpleName}", expr.span)
                module.types.append(Type.Scalar(ScalarKind.I32, expr.span))
            }
        }
    }
    
    /**
     * Vérifie si deux types sont compatibles.
     */
    private fun areTypesCompatible(type1: Handle<Type>, type2: Handle<Type>): Boolean {
        val t1 = module.types[type1]
        val t2 = module.types[type2]
        
        if (t1 == t2) return true
        
        return when {
            // Types scalaires
            t1.inner is TypeInner.Scalar && t2.inner is TypeInner.Scalar -> {
                val s1 = t1.inner as TypeInner.Scalar
                val s2 = t2.inner as TypeInner.Scalar
                // En WGSL, les types scalaires sont stricts (pas de conversion implicite)
                s1.kind == s2.kind
            }
            // Types vecteurs
            t1.inner is TypeInner.Vector && t2.inner is TypeInner.Vector -> {
                val v1 = t1.inner as TypeInner.Vector
                val v2 = t2.inner as TypeInner.Vector
                v1.size == v2.size && areTypesCompatible(v1.width, v2.width)
            }
            // Types matrices
            t1.inner is TypeInner.Matrix && t2.inner is TypeInner.Matrix -> {
                val m1 = t1.inner as TypeInner.Matrix
                val m2 = t2.inner as TypeInner.Matrix
                m1.rows == m2.rows && m1.cols == m2.cols && areTypesCompatible(m1.width, m2.width)
            }
            // Types tableaux
            t1.inner is TypeInner.Array && t2.inner is TypeInner.Array -> {
                val a1 = t1.inner as TypeInner.Array
                val a2 = t2.inner as TypeInner.Array
                a1.length == a2.length && areTypesCompatible(a1.element, a2.element)
            }
            else -> false
        }
    }
    
    /**
     * Vérifie si un type est booléen.
     */
    private fun isBooleanType(type: Handle<Type>): Boolean {
        val t = module.types[type].inner
        return t is TypeInner.Scalar && t.kind == ScalarKind.BOOL
    }
    
    /**
     * Vérifie si un type est un vecteur de booléens.
     */
    private fun isVectorOfBooleanType(type: Handle<Type>): Boolean {
        val t = module.types[type].inner
        return t is TypeInner.Vector && 
               (module.types[t.width].inner as TypeInner.Scalar).kind == ScalarKind.BOOL
    }
    
    /**
     * Vérifie si un type est un entier.
     */
    private fun isIntegerType(type: Handle<Type>): Boolean {
        val t = module.types[type].inner
        return t is TypeInner.Scalar && 
               (t.kind == ScalarKind.I32 || t.kind == ScalarKind.U32 || t.kind == ScalarKind.AbstractInt)
    }
    
    /**
     * Vérifie si un type est "switchable" (peut être utilisé dans un switch).
     */
    private fun isSwitchableType(type: Handle<Type>): Boolean {
        val t = module.types[type].inner
        return when {
            t is TypeInner.Scalar -> {
                t.kind == ScalarKind.I32 || t.kind == ScalarKind.U32 || t.kind == ScalarKind.Bool
            }
            else -> false
        }
    }
    
    /**
     * Signale une erreur.
     */
    private fun error(message: String, span: Span) {
        errors.add(TypeError(message, span))
    }
    
    /**
     * Traite les expressions globales (hors fonctions).
     */
    private fun processGlobalExpressions() {
        // Les expressions globales sont celles dans les initialisations
        // Déjà traitées dans processGlobalDeclarations
    }
}
```

---

## 📝 UTILISATION

### Intégration avec le pipeline

```kotlin
// Après avoir converti l'AST en IR
val astToIr = AstToIrConverter(translationUnit)
val module = astToIr.convert()

// Typifier le module
val typifier = Typifier(module)
val success = typifier.process()

if (success) {
    // Module est complètement typé
    println("Module typé avec succès")
} else {
    // Il y a des erreurs de typage
    for (error in typifier.errors) {
        println("Erreur de typage: ${error.message} à ${error.span}")
    }
}
```

### Utilisation pour inférer un type spécifique

```kotlin
val typifier = Typifier(module)
val context = Typifier.Context(function)
val exprType = typifier.inferExpressionType(exprHandle, context)

println("Type de l'expression: ${module.types[exprType].inner}")
```

---

## ✅ CHECKLIST PHASE 3.1

### Structure Typifier
- [ ] `Typifier` class principale
- [ ] `TypeError` class pour les erreurs
- [ ] `Context` class pour le contexte de typage

### Méthodes principales
- [ ] `process()` - Traiter tout le module
- [ ] `processGlobalDeclarations()` - Déclarations globales
- [ ] `processFunction()` - Fonctions
- [ ] `processLocalVariable()` - Variables locales
- [ ] `processStatements()` - Instructions
- [ ] `processStatement()` - Instruction unique
- [ ] `inferExpressionType()` - Inférer le type d'une expression
- [ ] `inferLValueType()` - Inférer le type d'une LValue

### Inférence par type d'expression
- [ ] Literal
- [ ] Access
- [ ] AccessIndex
- [ ] Splat
- [ ] Unary
- [ ] Binary
- [ ] Select
- [ ] ArrayLength
- [ ] As
- [ ] GlobalVariable
- [ ] LocalVariable
- [ ] FunctionArgument
- [ ] Const

### Inférence par type d'instruction
- [ ] Let
- [ ] Assign
- [ ] Store
- [ ] If
- [ ] Switch
- [ ] Loop
- [ ] While
- [ ] For
- [ ] BreakIf
- [ ] ContinueIf
- [ ] Return
- [ ] Discard
- [ ] Block
- [ ] Emit

### Vérifications de types
- [ ] Compatibilité des types binaires
- [ ] Types booléens pour les conditions
- [ ] Types compatibles pour les assignments
- [ ] Types compatibles pour les stores
- [ ] Types switchables pour switch
- [ ] Types de retour compatibles

### Helpers
- [ ] `areTypesCompatible()`
- [ ] `isBooleanType()`
- [ ] `isVectorOfBooleanType()`
- [ ] `isIntegerType()`
- [ ] `isSwitchableType()`

### Tests
- [ ] `TypifierTest.kt`
  - [ ] Inférence de types littéraux
  - [ ] Inférence de types binaires
  - [ ] Inférence de types unaires
  - [ ] Inférence de types vectoriels
  - [ ] Inférence de types Select
  - [ ] Vérification de compatibilité
  - [ ] Erreurs de typage
  - [ ] Functions complètes
  - [ ] Modules complets

### Documentation
- [ ] KDoc complet
- [ ] Exemples d'utilisation
- [ ] Documentation des règles de typage

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Concevoir Typifier | 1 jour | IR Structures | [ ] |
| Implémenter Typifier class | 2 jours | Conception | [ ] |
| Implémenter processModule | 2 jours | Typifier | [ ] |
| Implémenter inferExpressionType | 5 jours | Typifier | [ ] |
| Implémenter toutes les inférences | 5 jours | inferExpressionType | [ ] |
| Implémenter les vérifications | 2 jours | Typifier | [ ] |
| Intégrer avec le pipeline | 1 jour | Tout | [ ] |
| Écrire les tests | 3 jours | Tout | [ ] |
| Documentation | 1 jour | Tout | [ ] |
| Validation manuelle | 2 jours | Tout | [ ] |

**Total estimé** : **2-3 semaines**

---

## 🎯 LIVRABLES

1. **Fichier Kotlin** : `Typifier.kt`
2. **Tests unitaires** : `TypifierTest.kt`
3. **Couverture de test** : > 95%
4. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/proc/typifier.rs`
- **Fichier précédent** : `00_constant-evaluator.md`
- **Fichier suivant** : `02_layouter.md`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Finaliser la conception
2. [ ] Implémenter Typifier
3. [ ] Implémenter toutes les méthodes d'inférence
4. [ ] Écrire les tests
5. [ ] Valider avec des tests manuels
6. [ ] Passer à `02_layouter.md`
