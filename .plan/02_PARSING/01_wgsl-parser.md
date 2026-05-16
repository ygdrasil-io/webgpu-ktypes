# 📜 Phase 2.1 : Parser WGSL

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `wgsl:wgsl`  
**Phase** : 2 - Parsing  
**Sous-Phase** : 2.1 - WGSL Parser  
**Durée** : 3-4 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Deuxième étape du parsing)  
**Statut** : [x] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/mod.rs` (~2000+ lignes)
> **Référence AST** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/ast.rs` (~800 lignes)

---

## 📋 OBJECTIFS

Implémenter un **parser** pour le langage **WGSL** qui convertit la séquence de tokens produite par le lexer en un **AST (Abstract Syntax Tree)**.

Le parser est la deuxième étape du pipeline de compilation après le lexer. Il :
- Consomme les tokens produits par le lexer
- Construit une représentation structurée (AST) du code source
- Vérifie la syntaxe selon la grammaire WGSL
- Gère les erreurs de syntaxe avec récupération

**Livrable principal** : Un parser fonctionnel qui peut parser tout code WGSL valide et produire un AST complet.

---

## 🎯 CONCEPTS CLÉS

### 1. Qu'est-ce qu'un Parser ?

Un **parser** (ou analyseur syntaxique) est la deuxième étape du parsing. Il :
- Travaille au niveau **token** (contrairement au lexer qui travaille au niveau caractère)
- Construit un **arbre de syntaxe abstraite (AST)** qui représente la structure hiérarchique du code
- Vérifie que la séquence de tokens suit les règles de la **grammaire** WGSL
- Gère les erreurs de syntaxe avec des messages clairs

### 2. AST (Abstract Syntax Tree)

L'AST est une représentation arborescente du code source qui :
- Capture la **structure hiérarchique** (expressions, déclarations, blocs, etc.)
- Élimine les éléments non essentiels (commentaires, espaces, certains séparateurs)
- Facilite les étapes suivantes (validation, optimisation, génération de code)

Exemple : `let x: i32 = 42;` →
```
VariableDecl
├── name: "x"
├── type: ScalarType(i32)
└── init: Literal(42)
```

### 3. Grammaire WGSL

La grammaire WGSL est définie dans la [spécification officielle](https://gpuweb.github.io/gpuweb/wgsl/).

Structure de base :
```
module ::= (top_level_decl)*

top_level_decl ::= 
    | function_decl
    | global_var_decl
    | struct_decl
    | type_alias_decl
    | const_assert
    | override_decl
```

### 4. Différence entre Parser et Lexer

| Lexer | Parser |
|-------|--------|
| Entrée : String | Entrée : List<Token> |
| Sortie : List<Token> | Sortie : AST |
| Niveau : Caractère | Niveau : Token |
| Vérifie : Syntaxe lexicale | Vérifie : Grammaire |

### 5. Stratégie de Parsing

Nous utilisons un **parser récursif descendant (Recursive Descent Parser)** avec :
- Une fonction de parsing par règle de grammaire
- Lookahead (peek) pour décider quelle règle appliquer
- Erreur avec récupération (error recovery) pour continuer après une erreur
- Précédence des opérateurs gérée par la structure des fonctions

---

## 📦 STRUCTURE DE L'AST

### 1. TranslationUnit (Unité de traduction)

**Fichier** : `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/ast/TranslationUnit.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl.ast

import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.span.Span

/**
 * Unité de traduction complète pour un module WGSL.
 * Contient toutes les déclarations au niveau module.
 */
data class TranslationUnit(
    /** Declarations globales (fonctions, variables, structs, etc.) */
    val declarations: Arena<GlobalDecl>,
    
    /** Expressions communes pour toute l'unité de traduction.
     * Toutes les fonctions, initialiseurs globaux, longueurs de tableaux, etc.
     * stockent leurs expressions ici.
     */
    val expressions: Arena<Expression>,
    
    /** Doc comments apparaissent en premier dans le fichier.
     * Sert de documentation pour toute l'unité de traduction.
     */
    val docComments: List<String>,
    
    /** Extensions activées (ex: @enable point_size_builtin) */
    val enableExtensions: EnableExtensions = EnableExtensions(),
    
    /** Filtres de diagnostic pour les directives diagnostic(…) */
    val diagnosticFilters: Arena<DiagnosticFilterNode> = Arena(),
    val diagnosticFilterLeaf: Handle<DiagnosticFilterNode>? = null
)
```

### 2. GlobalDecl (Déclaration globale)

```kotlin
/**
 * Déclaration au niveau module.
 */
sealed class GlobalDecl {
    /** Type de la déclaration */
    abstract val kind: GlobalDeclKind
    
    /** Noms de tous les objets module-scope ou prédéclarés utilisés par cette déclaration */
    abstract val dependencies: Set<Dependency>
}

/**
 * Types de déclarations globales.
 */
sealed class GlobalDeclKind {
    data class Function(val function: Function) : GlobalDeclKind()
    data class Variable(val variable: GlobalVariable) : GlobalDeclKind()
    data class Constant(val constant: Const) : GlobalDeclKind()
    data class Override(val override: Override) : GlobalDeclKind()
    data class Struct(val struct: Struct) : GlobalDeclKind()
    data class TypeAlias(val typeAlias: TypeAlias) : GlobalDeclKind()
    data class ConstAssert(val expression: Handle<Expression>) : GlobalDeclKind()
}
```

### 3. Function (Fonction)

```kotlin
/**
 * Déclaration de fonction.
 */
data class Function(
    /** Attributs de la fonction (ex: @compute, @fragment) */
    val attributes: List<Attribute>,
    
    /** Nom de la fonction */
    val name: String,
    
    /** Paramètres de la fonction */
    val parameters: List<FunctionParameter>,
    
    /** Type de retour (null si void) */
    val returnType: Handle<TypeDeclaration>?,
    
    /** Corps de la fonction (bloc de déclarations) */
    val body: Block,
    
    /** Span de la déclaration complète */
    val span: Span,
    
    /** Type de shader (compute, vertex, fragment, etc.) */
    val stage: ShaderStage? = null,
    
    /** Expressions arena spécifique à cette fonction */
    val expressions: Arena<Expression> = Arena(),
    
    /** Variables locales spécifiques à cette fonction */
    val locals: Arena<Local> = Arena()
)

/**
 * Paramètre de fonction.
 */
data class FunctionParameter(
    /** Attributs du paramètre */
    val attributes: List<Attribute>,
    
    /** Nom du paramètre */
    val name: String,
    
    /** Type du paramètre */
    val type: Handle<TypeDeclaration>,
    
    /** Expression par défaut (pour les paramètres optionnels) */
    val defaultValue: Handle<Expression>? = null,
    
    /** Mode de binding (in, out, inout) */
    val binding: ParameterBinding = ParameterBinding.IN,
    
    /** Mode de builtin (ex: @builtin(position)) */
    val builtin: BuiltIn? = null
)

enum class ParameterBinding {
    IN, OUT, INOUT
}
```

### 4. Expression (Expression)

```kotlin
/**
 * Expression WGSL.
 * Base class pour toutes les expressions.
 */
sealed class Expression {
    /** Type de l'expression (calculé pendant la résolution de type) */
    var resolvedType: Handle<Type>? = null
    
    /** Span de l'expression */
    abstract val span: Span
    
    // Types d'expressions
    data class Literal(val value: LiteralValue, override val span: Span) : Expression()
    data class Ident(val ident: IdentExpr, override val span: Span) : Expression()
    data class Call(val call: CallPhrase, override val span: Span) : Expression()
    data class Binary(val left: Handle<Expression>, val op: BinaryOperator, val right: Handle<Expression>, override val span: Span) : Expression()
    data class Unary(val expr: Handle<Expression>, val op: UnaryOperator, override val span: Span) : Expression()
    data class Ternary(val condition: Handle<Expression>, val accept: Handle<Expression>, val reject: Handle<Expression>, override val span: Span) : Expression()
    data class Access(val expr: Handle<Expression>, val index: Handle<Expression>, override val span: Span) : Expression()
    data class AccessIndex(val expr: Handle<Expression>, val index: Int, override val span: Span) : Expression()
    data class MemberAccess(val expr: Handle<Expression>, val member: String, override val span: Span) : Expression()
    data class Splat(val expr: Handle<Expression>, override val span: Span) : Expression()
    data class ArrayLength(val expr: Handle<Expression>, override val span: Span) : Expression()
    data class As(val expr: Handle<Expression>, val type: Handle<TypeDeclaration>, override val span: Span) : Expression()
    data class Compose(val type: Handle<TypeDeclaration>, val arguments: List<Handle<Expression>>, override val span: Span) : Expression()
    data class Select(val condition: Handle<Expression>, val accept: Handle<Expression>, val reject: Handle<Expression>, override val span: Span) : Expression()
}

/**
 * Valeur littérale.
 */
sealed class LiteralValue {
    data class Bool(val value: Boolean) : LiteralValue()
    data class I32(val value: Int) : LiteralValue()
    data class U32(val value: Long) : LiteralValue()  // ULong pour éviter sign extension
    data class F32(val value: Float) : LiteralValue()
    data class F16(val value: UShort) : LiteralValue()  // Encodé comme bits u16
}

/**
 * Identifiant avec expression de template optionnelle.
 */
data class TemplateElaboratedIdent(
    /** Expression d'identifiant (résolue ou non) */
    val ident: IdentExpr,
    
    /** Span de l'identifiant */
    val identSpan: Span,
    
    /** Liste des paramètres de template (si présente) */
    val templateList: List<Handle<Expression>>,
    
    /** Span de la template list */
    val templateListSpan: Span
)

/**
 * Expression d'identifiant.
 */
sealed class IdentExpr {
    /** Identifiant non résolu (sera résolu plus tard) */
    data class Unresolved(val name: String) : IdentExpr()
    
    /** Identifiant local (variable ou paramètre de fonction) */
    data class Local(val handle: Handle<Local>) : IdentExpr()
}

/**
 * Phrase d'appel (fonction ou constructeur).
 */
data class CallPhrase(
    /** Fonction/constructeur appelé */
    val function: TemplateElaboratedIdent,
    
    /** Arguments de l'appel */
    val arguments: List<Handle<Expression>>
)

/**
 * Opérateurs binaires.
 */
enum class BinaryOperator {
    // Arithmétiques
    ADD, SUB, MUL, DIV, MOD,
    
    // Comparaison
    EQ, NE, LT, LE, GT, GE,
    
    // Logiques
    AND, OR,
    
    // Bits
    BIT_AND, BIT_OR, BIT_XOR, SHL, SHR,
    
    // Assignment
    ASSIGN, ADD_ASSIGN, SUB_ASSIGN, MUL_ASSIGN, DIV_ASSIGN, MOD_ASSIGN,
    AND_ASSIGN, OR_ASSIGN, XOR_ASSIGN, SHL_ASSIGN, SHR_ASSIGN
}

/**
 * Opérateurs unaires.
 */
enum class UnaryOperator {
    NEG, POS, NOT, BIT_NOT, PRE_INCREMENT, POST_INCREMENT, PRE_DECREMENT, POST_DECREMENT
}
```

### 5. Statement (Instruction)

```kotlin
/**
 * Instruction WGSL.
 */
sealed class Statement {
    abstract val span: Span
    
    // Types d'instructions
    data class Block(val statements: List<Handle<Statement>>, override val span: Span) : Statement()
    data class If(val condition: Handle<Expression>, val accept: Handle<Statement>, val reject: Handle<Statement>?, override val span: Span) : Statement()
    data class Switch(val selector: Handle<Expression>, val body: SwitchBody, override val span: Span) : Statement()
    data class Loop(val body: Handle<Statement>, val continuing: LoopContinuing? = null, override val span: Span) : Statement()
    data class While(val condition: Handle<Expression>, val body: Handle<Statement>, val continuing: LoopContinuing? = null, override val span: Span) : Statement()
    data class For(val init: Handle<Statement>?, val condition: Handle<Expression>?, val update: Handle<Statement>?, val body: Handle<Statement>, val continuing: LoopContinuing? = null, override val span: Span) : Statement()
    data class Break(override val span: Span) : Statement()
    data class Continue(override val span: Span) : Statement()
    data class Return(val expr: Handle<Expression>?, override val span: Span) : Statement()
    data class Discard(override val span: Span) : Statement()
    
    // Déclarations
    data class Emit(val range: Range<Expression>) : Statement()
    data class VariableDecl(val name: String, val type: Handle<TypeDeclaration>?, val init: Handle<Expression>?, override val span: Span) : Statement()
    data class ConstDecl(val name: String, val type: Handle<TypeDeclaration>, val init: Handle<Expression>, override val span: Span) : Statement()
    data class LetDecl(val name: String, val type: Handle<TypeDeclaration>?, val init: Handle<Expression>, override val span: Span) : Statement()
    data class Assignment(val lhs: Handle<Expression>, val rhs: Handle<Expression>, override val span: Span) : Statement()
    data class IncrementDecrement(val expr: Handle<Expression>, val op: IncrementDecrementOperator, override val span: Span) : Statement()
}

enum class IncrementDecrementOperator {
    INCREMENT, DECREMENT
}

/**
 * Corps d'un switch.
 */
data class SwitchBody(
    val cases: List<SwitchCase>
)

/**
 * Cas d'un switch.
 */
data class SwitchCase(
    val selectors: List<Handle<Expression>>,
    val body: Handle<Statement>
)

/**
 * Clause continuing pour les boucles.
 */
data class LoopContinuing(
    val body: Handle<Statement>
)
```

### 6. TypeDeclaration (Déclaration de type)

```kotlin
/**
 * Déclaration de type WGSL.
 */
sealed class TypeDeclaration {
    abstract val span: Span
    
    // Types scalaires
    data class Scalar(val kind: ScalarKind, override val span: Span) : TypeDeclaration()
    
    // Types vecteurs
    data class Vector(val size: VectorSize, val scalar: Handle<TypeDeclaration>, override val span: Span) : TypeDeclaration()
    
    // Types matrices
    data class Matrix(val rows: VectorSize, val cols: VectorSize, val scalar: Handle<TypeDeclaration>, override val span: Span) : TypeDeclaration()
    
    // Types tableaux
    data class Array(val element: Handle<TypeDeclaration>, val length: Handle<Expression>?, override val span: Span) : TypeDeclaration()
    
    // Types struct
    data class Struct(val name: String?, val members: List<StructMember>, override val span: Span) : TypeDeclaration()
    
    // Référence à un type nommé
    data class Named(val name: String, val templateList: List<Handle<Expression>>, override val span: Span) : TypeDeclaration()
    
    // Type pointeur
    data class Pointer(val base: Handle<TypeDeclaration>, val access: StorageAccess, override val span: Span) : TypeDeclaration()
    
    // Type reference
    data class Reference(val base: Handle<TypeDeclaration>, val access: StorageAccess, override val span: Span) : TypeDeclaration()
    
    // Type template
    data class Template(val name: String, val templateList: List<Handle<TypeDeclaration>>, override val span: Span) : TypeDeclaration()
}

/**
 * Membre d'une struct.
 */
data class StructMember(
    val attributes: List<Attribute>,
    val name: String,
    val type: Handle<TypeDeclaration>,
    val binding: StructMemberBinding? = null,
    val builtin: BuiltIn? = null,
    val span: Span
)

data class StructMemberBinding(
    val location: Int? = null,
    val interpolation: InterpolationType? = null,
    val sampling: SamplingType? = null
)

enum class ScalarKind {
    BOOL, I8, U8, I16, U16, I32, U32, I64, U64, F16, F32, F64, ABSTRACT_INT, ABSTRACT_FLOAT
}

enum class VectorSize {
    BI, TRI, QUAD
}

enum class StorageAccess {
    READ, WRITE, READ_WRITE
}

enum class InterpolationType {
    PERSPECTIVE, LINEAR, FLAT
}

enum class SamplingType {
    CENTER, CENTROID, SAMPLE
}
```

### 7. Attribute (Attribut)

```kotlin
/**
 * Attribut WGSL (ex: @compute, @location(0), @builtin(position)).
 */
data class Attribute(
    val name: String,
    val arguments: List<Handle<Expression>>,
    val span: Span
)
```

---

## 🎯 IMPLÉMENTATION DU PARSER

### 1. Parser.kt

**Fichier** : `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/Parser.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl

import io.ygdrasil.wgsl.arena.Arena
import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.front.wgsl.ast.*
import io.ygdrasil.wgsl.front.wgsl.lexer.Lexer
import io.ygdrasil.wgsl.front.wgsl.lexer.Token
import io.ygdrasil.wgsl.front.wgsl.lexer.TokenKind
import io.ygdrasil.wgsl.span.Span

/**
 * Parser WGSL principal.
 * Construit un AST à partir d'une séquence de tokens.
 */
class Parser(
    private val input: String,
    private val lexer: Lexer = Lexer(input)
) {
    private var currentToken: Token? = null
    private var lookahead: List<Token> = mutableListOf()
    private val lookaheadSize: Int = 2
    
    /** Arena pour les déclarations globales */
    val declarations: Arena<GlobalDecl> = Arena()
    
    /** Arena pour les expressions globales */
    val expressions: Arena<Expression> = Arena()
    
    /** Arena pour les types */
    val types: Arena<TypeDeclaration> = Arena()
    
    /** Liste des erreurs accumulées */
    val errors: MutableList<ParseError> = mutableListOf()
    
    /** Doc comments pour le module */
    val docComments: MutableList<String> = mutableListOf()

    /**
     * Parse l'intégralité du module et retourne l'AST.
     */
    fun parse(): TranslationUnit {
        advance() // Charge le premier token
        skipTrivia()
        
        val startSpan = currentSpan()
        val decls = mutableListOf<Handle<GlobalDecl>>()
        
        // Collecter les doc comments au début
        while (match(TokenKind.ModuleDocComment)) {
            docComments.add(currentToken!!.value as String)
            advance()
            skipTrivia()
        }
        
        // Parser toutes les déclarations top-level
        while (!match(TokenKind.EOF)) {
            val decl = parseTopLevelDecl()
            if (decl != null) {
                decls.add(declarations.append(decl))
            }
            skipTrivia()
        }
        
        return TranslationUnit(
            declarations = declarations,
            expressions = expressions,
            docComments = docComments,
            enableExtensions = EnableExtensions()
        )
    }

    /**
     * Parse une déclaration top-level.
     */
    private fun parseTopLevelDecl(): GlobalDecl? {
        return when {
            match(TokenKind.FN) -> parseFunctionDecl()
            match(TokenKind.LET) -> parseGlobalVariableDecl(isConst = false)
            match(TokenKind.CONST) -> parseGlobalVariableDecl(isConst = true)
            match(TokenKind.VAR) -> parseGlobalVariableDecl(isConst = false, isVar = true)
            match(TokenKind.STRUCT) -> parseStructDecl()
            match(TokenKind.TYPE) -> parseTypeAliasDecl()
            match(TokenKind.OVERRIDE) -> parseOverrideDecl()
            match(TokenKind.CONST_ASSERT) -> parseConstAssertDecl()
            match(TokenKind.ENABLE) -> parseEnableDirective()
            match(TokenKind.REQUIRES) -> parseRequiresDirective()
            else -> {
                error("Expected top-level declaration")
                null
            }
        }
    }

    // ===== Parsing des déclarations =====

    private fun parseFunctionDecl(): GlobalDecl {
        expect(TokenKind.FN)
        val startSpan = previousSpan()
        
        val attributes = parseAttributes()
        val name = expectIdentifier()
        
        expect(TokenKind.PAREN_OPEN)
        val parameters = parseFunctionParameters()
        expect(TokenKind.PAREN_CLOSE)
        
        val returnType = if (match(TokenKind.COLON)) {
            parseTypeDecl()
        } else {
            null
        }
        
        val body = parseBlock()
        
        return GlobalDecl.FunctionDecl(
            Function(
                attributes = attributes,
                name = name,
                parameters = parameters,
                returnType = returnType,
                body = body,
                span = spanFrom(startSpan)
            )
        )
    }

    private fun parseFunctionParameters(): List<FunctionParameter> {
        val params = mutableListOf<FunctionParameter>()
        
        while (!match(TokenKind.PAREN_CLOSE)) {
            if (!params.isEmpty()) {
                expect(TokenKind.COMMA)
            }
            
            val attributes = parseAttributes()
            val name = expectIdentifier()
            expect(TokenKind.COLON)
            val type = parseTypeDecl()
            
            params.add(FunctionParameter(
                attributes = attributes,
                name = name,
                type = type
            ))
        }
        
        return params
    }

    private fun parseStructDecl(): GlobalDecl {
        expect(TokenKind.STRUCT)
        val startSpan = previousSpan()
        
        val name = expectIdentifier()
        expect(TokenKind.BRACE_OPEN)
        
        val members = mutableListOf<StructMember>()
        while (!match(TokenKind.BRACE_CLOSE)) {
            if (!members.isEmpty()) {
                expect(TokenKind.COMMA)
                skipTrivia()
            }
            
            val attributes = parseAttributes()
            val memberName = expectIdentifier()
            expect(TokenKind.COLON)
            val type = parseTypeDecl()
            
            members.add(StructMember(
                attributes = attributes,
                name = memberName,
                type = type,
                span = currentSpan()
            ))
        }
        
        return GlobalDecl.StructDecl(
            Struct(
                name = name,
                members = members,
                span = spanFrom(startSpan)
            )
        )
    }

    private fun parseTypeAliasDecl(): GlobalDecl {
        expect(TokenKind.TYPE)
        val startSpan = previousSpan()
        
        val name = expectIdentifier()
        expect(TokenKind.ASSIGN)
        val type = parseTypeDecl()
        expect(TokenKind.SEMICOLON)
        
        return GlobalDecl.TypeAliasDecl(
            TypeAlias(
                name = name,
                type = type,
                span = spanFrom(startSpan)
            )
        )
    }

    private fun parseGlobalVariableDecl(isConst: Boolean, isVar: Boolean = false): GlobalDecl {
        val startSpan = previousSpan()
        
        val attributes = parseAttributes()
        val name = expectIdentifier()
        expect(TokenKind.COLON)
        val type = parseTypeDecl()
        
        val init = if (match(TokenKind.ASSIGN)) {
            parseExpression()
        } else {
            null
        }
        
        expect(TokenKind.SEMICOLON)
        
        val decl = if (isConst) {
            GlobalDecl.ConstDecl(Const(name, type, init!!, spanFrom(startSpan)))
        } else if (isVar) {
            GlobalDecl.VarDecl(GlobalVariable(name, type, init, attributes, spanFrom(startSpan)))
        } else {
            GlobalDecl.LetDecl(Let(name, type, init!!, spanFrom(startSpan)))
        }
        
        return decl
    }

    // ===== Parsing des types =====

    private fun parseTypeDecl(): Handle<TypeDeclaration> {
        val startSpan = currentSpan()
        
        when {
            // Type scalaire
            matchScalarType() -> {
                val kind = currentScalarKind()
                advance()
                return types.append(TypeDeclaration.Scalar(kind, spanFrom(startSpan)))
            }
            
            // Type vecteur
            matchVectorType() -> {
                val kind = parseVectorType()
                return types.append(kind)
            }
            
            // Type matrice
            matchMatrixType() -> {
                val kind = parseMatrixType()
                return types.append(kind)
            }
            
            // Type tableau
            match(TokenKind.BRACKET_OPEN) -> {
                return parseArrayType()
            }
            
            // Type struct
            match(TokenKind.STRUCT) -> {
                return parseStructType()
            }
            
            // Type nommé ou template
            match(TokenKind.WORD) -> {
                val name = currentToken!!.value as String
                advance()
                
                if (match(TokenKind.TEMPLATE_ARGS_START)) {
                    val templateList = parseTemplateList()
                    expect(TokenKind.TEMPLATE_ARGS_END)
                    return types.append(TypeDeclaration.Template(name, templateList, spanFrom(startSpan)))
                } else {
                    return types.append(TypeDeclaration.Named(name, emptyList(), spanFrom(startSpan)))
                }
            }
            
            else -> {
                error("Expected type declaration")
                return types.append(TypeDeclaration.Scalar(ScalarKind.I32, spanFrom(startSpan)))
            }
        }
    }

    private fun parseVectorType(): TypeDeclaration.Vector {
        val name = currentToken!!.value as String
        val size = when (name) {
            "vec2" -> VectorSize.BI
            "vec3" -> VectorSize.TRI
            "vec4" -> VectorSize.QUAD
            else -> error("Invalid vector type: $name"); VectorSize.QUAD
        }
        advance()
        
        expect(TokenKind.TEMPLATE_ARGS_START)
        val scalar = parseTypeDecl()
        expect(TokenKind.TEMPLATE_ARGS_END)
        
        return TypeDeclaration.Vector(size, scalar, spanFrom(previousSpan()))
    }

    // ===== Parsing des expressions =====

    private fun parseExpression(): Handle<Expression> {
        return parseTernaryExpression()
    }

    private fun parseTernaryExpression(): Handle<Expression> {
        val condition = parseLogicalOrExpression()
        
        if (match(TokenKind.QUESTION)) {
            val accept = parseExpression()
            expect(TokenKind.COLON)
            val reject = parseExpression()
            return expressions.append(Expression.Ternary(condition, accept, reject, spanFrom(condition)))
        }
        
        return condition
    }

    private fun parseLogicalOrExpression(): Handle<Expression> {
        var left = parseLogicalAndExpression()
        
        while (match(TokenKind.LOGICAL_OR)) {
            val opSpan = previousSpan()
            val right = parseLogicalAndExpression()
            left = expressions.append(Expression.Binary(left, BinaryOperator.OR, right, spanFrom(opSpan)))
        }
        
        return left
    }

    private fun parseLogicalAndExpression(): Handle<Expression> {
        var left = parseBitwiseOrExpression()
        
        while (match(TokenKind.LOGICAL_AND)) {
            val opSpan = previousSpan()
            val right = parseBitwiseOrExpression()
            left = expressions.append(Expression.Binary(left, BinaryOperator.AND, right, spanFrom(opSpan)))
        }
        
        return left
    }

    // Continuer avec les autres niveaux de précédence...
    // (BitwiseOr, BitwiseXor, BitwiseAnd, Equality, Relational, Shift, Additive, Multiplicative, Unary, Postfix, Primary)

    private fun parsePrimaryExpression(): Handle<Expression> {
        return when {
            match(TokenKind.NUMBER) -> {
                val value = parseNumberLiteral()
                val expr = expressions.append(Expression.Literal(value, previousSpan()))
                
                // Gérer les suffixes de type
                // Ex: 42u, 3.14f, etc.
                expr
            }
            
            match(TokenKind.WORD) -> {
                val name = currentToken!!.value as String
                advance()
                
                // Vérifier si c'est un mot-clé vrai (true, false) ou un identifiant
                when (name) {
                    "true" -> expressions.append(Expression.Literal(LiteralValue.Bool(true), previousSpan()))
                    "false" -> expressions.append(Expression.Literal(LiteralValue.Bool(false), previousSpan()))
                    else -> parseIdentExpression(name)
                }
            }
            
            match(TokenKind.PAREN_OPEN) -> {
                advance()
                val expr = parseExpression()
                expect(TokenKind.PAREN_CLOSE)
                expr
            }
            
            else -> {
                error("Expected primary expression")
                expressions.append(Expression.Literal(LiteralValue.I32(0), currentSpan()))
            }
        }
    }

    private fun parseIdentExpression(name: String): Handle<Expression> {
        val startSpan = previousSpan()
        
        // Vérifier si c'est suivi d'une template list (appel de constructeur ou fonction générique)
        if (match(TokenKind.TEMPLATE_ARGS_START)) {
            val templateList = parseTemplateList()
            expect(TokenKind.TEMPLATE_ARGS_END)
            
            // Vérifier si c'est un appel de fonction/constructeur
            if (match(TokenKind.PAREN_OPEN)) {
                val arguments = parseCallArguments()
                return expressions.append(Expression.Call(
                    CallPhrase(
                        function = TemplateElaboratedIdent(
                            ident = IdentExpr.Unresolved(name),
                            identSpan = startSpan,
                            templateList = templateList,
                            templateListSpan = previousSpan()
                        ),
                        arguments = arguments
                    ),
                    spanFrom(startSpan)
                ))
            } else {
                // C'est une référence à un type avec template parameters
                return expressions.append(Expression.Ident(
                    IdentExpr.Unresolved(name),
                    spanFrom(startSpan)
                ))
            }
        } 
        // Vérifier si c'est un appel de fonction
        else if (match(TokenKind.PAREN_OPEN)) {
            val arguments = parseCallArguments()
            return expressions.append(Expression.Call(
                CallPhrase(
                    function = TemplateElaboratedIdent(
                        ident = IdentExpr.Unresolved(name),
                        identSpan = startSpan,
                        templateList = emptyList(),
                        templateListSpan = Span.INVALID
                    ),
                    arguments = arguments
                ),
                spanFrom(startSpan)
            ))
        }
        
        // Sinon, c'est juste un identifiant
        return expressions.append(Expression.Ident(
            IdentExpr.Unresolved(name),
            spanFrom(startSpan)
        ))
    }

    private fun parseCallArguments(): List<Handle<Expression>> {
        val args = mutableListOf<Handle<Expression>>()
        
        while (!match(TokenKind.PAREN_CLOSE)) {
            if (!args.isEmpty()) {
                expect(TokenKind.COMMA)
            }
            args.add(parseExpression())
        }
        
        return args
    }

    private fun parseTemplateList(): List<Handle<Expression>> {
        val args = mutableListOf<Handle<Expression>>()
        
        while (!match(TokenKind.TEMPLATE_ARGS_END) && !match(TokenKind.GT)) {
            if (!args.isEmpty()) {
                expect(TokenKind.COMMA)
            }
            args.add(parseExpression())
        }
        
        return args
    }

    // ===== Parsing des instructions =====

    private fun parseBlock(): Block {
        expect(TokenKind.BRACE_OPEN)
        val startSpan = previousSpan()
        
        val statements = mutableListOf<Handle<Statement>>()
        while (!match(TokenKind.BRACE_CLOSE)) {
            val stmt = parseStatement()
            if (stmt != null) {
                statements.add(statementsArena.append(stmt))
            }
            skipTrivia()
        }
        
        return Block(statements, spanFrom(startSpan))
    }

    private fun parseStatement(): Statement? {
        return when {
            match(TokenKind.BRACE_OPEN) -> {
                // Bloc vide ou avec contenu
                lookahead { 
                    if (peek().kind == TokenKind.BRACE_CLOSE) {
                        // Bloc vide
                        advance()
                        Statement.Block(emptyList(), previousSpan())
                    } else {
                        parseBlock()
                    }
                }
            }
            
            match(TokenKind.IF) -> parseIfStatement()
            match(TokenKind.SWITCH) -> parseSwitchStatement()
            match(TokenKind.FOR) -> parseForStatement()
            match(TokenKind.WHILE) -> parseWhileStatement()
            match(TokenKind.LOOP) -> parseLoopStatement()
            match(TokenKind.BREAK) -> parseBreakStatement()
            match(TokenKind.CONTINUE) -> parseContinueStatement()
            match(TokenKind.RETURN) -> parseReturnStatement()
            match(TokenKind.DISCARD) -> parseDiscardStatement()
            match(TokenKind.LET) -> parseLetStatement()
            match(TokenKind.CONST) -> parseConstStatement()
            match(TokenKind.VAR) -> parseVarStatement()
            
            // Peut être une expression statement
            else -> parseExpressionStatement()
        }
    }

    private fun parseIfStatement(): Statement {
        expect(TokenKind.IF)
        val startSpan = previousSpan()
        
        val condition = parseExpression()
        val accept = parseBlock()
        
        val reject = if (match(TokenKind.ELSE)) {
            if (match(TokenKind.IF)) {
                // else if
                parseIfStatement()
            } else {
                parseBlock()
            }
        } else {
            null
        }
        
        return Statement.If(condition, accept, reject, spanFrom(startSpan))
    }

    private fun parseLetStatement(): Statement {
        expect(TokenKind.LET)
        val startSpan = previousSpan()
        
        val name = expectIdentifier()
        val type = if (match(TokenKind.COLON)) {
            parseTypeDecl()
        } else {
            null
        }
        
        val init = if (match(TokenKind.ASSIGN)) {
            parseExpression()
        } else {
            error("Let declaration requires initializer")
            null
        }
        
        expect(TokenKind.SEMICOLON)
        
        return Statement.LetDecl(name, type, init!!, spanFrom(startSpan))
    }

    private fun parseExpressionStatement(): Statement? {
        val startSpan = currentSpan()
        val expr = parseExpression()
        
        // Vérifier si c'est une assignment ou increment/decrement
        if (match(TokenKind.ASSIGN)) {
            val rhs = parseExpression()
            expect(TokenKind.SEMICOLON)
            return Statement.Assignment(expr, rhs, spanFrom(startSpan))
        } else if (match(TokenKind.INCREMENT)) {
            expect(TokenKind.SEMICOLON)
            return Statement.IncrementDecrement(expr, IncrementDecrementOperator.INCREMENT, spanFrom(startSpan))
        } else if (match(TokenKind.DECREMENT)) {
            expect(TokenKind.SEMICOLON)
            return Statement.IncrementDecrement(expr, IncrementDecrementOperator.DECREMENT, spanFrom(startSpan))
        } else {
            expect(TokenKind.SEMICOLON)
            return Statement.Emit(Range.from(expr.index..expr.index))
        }
    }

    // ===== Helper methods =====

    private fun advance() {
        currentToken = if (lookahead.isNotEmpty()) {
            lookahead.removeAt(0)
        } else {
            lexer.nextToken()
        }
    }

    private fun peek(ahead: Int = 0): Token? {
        while (lookahead.size <= ahead) {
            lookahead.add(lexer.nextToken())
        }
        return lookahead.getOrNull(ahead)
    }

    private fun match(kind: TokenKind): Boolean {
        return currentToken?.kind == kind
    }

    private fun expect(kind: TokenKind): Token {
        if (match(kind)) {
            val token = currentToken!!
            advance()
            return token
        } else {
            error("Expected $kind, got ${currentToken?.kind}")
            return Token(kind, "", currentSpan())
        }
    }

    private fun expectIdentifier(): String {
        if (match(TokenKind.WORD)) {
            val name = currentToken!!.value as String
            advance()
            return name
        } else {
            error("Expected identifier")
            return "_error_"
        }
    }

    private fun error(message: String) {
        val span = currentToken?.span ?: Span.INVALID
        errors.add(ParseError(message, span))
    }

    private fun skipTrivia() {
        while (match(TokenKind.TRIVIA) || match(TokenKind.DOC_COMMENT) || match(TokenKind.MODULE_DOC_COMMENT)) {
            advance()
        }
    }

    private fun currentSpan(): Span = currentToken?.span ?: Span.INVALID
    private fun previousSpan(): Span = lookahead.getOrNull(lookahead.size - 1)?.span ?: Span.INVALID
    private fun spanFrom(start: Span): Span = start.merge(currentSpan())

    private inline fun lookahead(block: () -> Statement): Statement {
        val saved = currentToken
        val result = block()
        currentToken = saved
        return result
    }
}

/**
 * Erreur de parsing.
 */
data class ParseError(
    val message: String,
    val span: Span
)
```

### 2. TypeResolver.kt (Résolution de types optionnelle pendant le parsing)

**Fichier** : `wgsl:wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/TypeResolver.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl

import io.ygdrasil.wgsl.arena.Handle
import io.ygdrasil.wgsl.front.wgsl.ast.*

/**
 * Résout les références de types non résolues dans l'AST.
 * Peut être fait pendant ou après le parsing.
 */
class TypeResolver(private val translationUnit: TranslationUnit) {
    
    private val typeMap: MutableMap<String, Handle<TypeDeclaration>> = mutableMapOf()
    private val structMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    private val functionMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    private val constMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()
    private val varMap: MutableMap<String, Handle<GlobalDecl>> = mutableMapOf()

    fun resolve() {
        // D'abord, indexer toutes les déclarations
        indexDeclarations()
        
        // Puis résoudre toutes les références
        resolveInTranslationUnit()
    }

    private fun indexDeclarations() {
        for (declHandle in translationUnit.declarations) {
            val decl = translationUnit.declarations[declHandle]
            when (decl.kind) {
                is GlobalDeclKind.Struct -> {
                    structMap[decl.kind.struct.name!!] = declHandle
                }
                is GlobalDeclKind.TypeAlias -> {
                    typeMap[decl.kind.typeAlias.name] = decl.kind.typeAlias.type
                }
                is GlobalDeclKind.Function -> {
                    functionMap[decl.kind.function.name] = declHandle
                }
                is GlobalDeclKind.Const -> {
                    constMap[decl.kind.const.name] = declHandle
                }
                is GlobalDeclKind.Var -> {
                    varMap[decl.kind.var.name] = declHandle
                }
                else -> {}
            }
        }
    }

    private fun resolveInTranslationUnit() {
        // Résoudre dans toutes les expressions
        for (exprHandle in translationUnit.expressions) {
            resolveExpression(exprHandle)
        }
        
        // Résoudre dans toutes les déclarations
        for (declHandle in translationUnit.declarations) {
            resolveInDecl(declHandle)
        }
    }

    private fun resolveExpression(exprHandle: Handle<Expression>) {
        val expr = translationUnit.expressions[exprHandle]
        when (expr) {
            is Expression.Ident -> {
                if (expr.ident is IdentExpr.Unresolved) {
                    // Essayer de résoudre
                }
            }
            is Expression.Call -> {
                // Résoudre la fonction
                // Résoudre les arguments
            }
            // ... autres cas
        }
    }
}
```

---

## 🧪 TESTS UNITAIRES

### ParserTest.kt

**Fichier** : `wgsl:wgsl/src/test/kotlin/dev/gfxrs/naga/front/wgsl/ParserTest.kt`

```kotlin
package io.ygdrasil.wgsl.front.wgsl

import io.ygdrasil.wgsl.front.wgsl.ast.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParserTest {
    
    @Test
    fun `test empty module`() {
        val input = ""
        val parser = Parser(input)
        val unit = parser.parse()
        
        assertThat(unit.declarations.size).isEqualTo(0)
    }
    
    @Test
    fun `test single function`() {
        val input = """
            fn main() {}
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        assertThat(unit.declarations.size).isEqualTo(1)
        val decl = unit.declarations[0]
        assertThat(decl).isInstanceOf(GlobalDecl.FunctionDecl::class.java)
        val func = (decl as GlobalDecl.FunctionDecl).function
        assertThat(func.name).isEqualTo("main")
        assertThat(func.parameters).isEmpty()
    }
    
    @Test
    fun `test function with parameters`() {
        val input = """
            fn add(a: i32, b: i32) -> i32 {
                return a + b;
            }
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        val decl = unit.declarations[0] as GlobalDecl.FunctionDecl
        val func = decl.function
        
        assertThat(func.parameters.size).isEqualTo(2)
        assertThat(func.parameters[0].name).isEqualTo("a")
        assertThat(func.parameters[1].name).isEqualTo("b")
        assertThat(func.returnType).isNotNull()
    }
    
    @Test
    fun `test struct declaration`() {
        val input = """
            struct Vertex {
                @location(0) position: vec4<f32>,
                @location(1) uv: vec2<f32>
            }
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        val decl = unit.declarations[0] as GlobalDecl.StructDecl
        val struct = decl.struct
        
        assertThat(struct.name).isEqualTo("Vertex")
        assertThat(struct.members.size).isEqualTo(2)
    }
    
    @Test
    fun `test let statement`() {
        val input = """
            fn main() {
                let x: i32 = 42;
            }
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        val funcDecl = unit.declarations[0] as GlobalDecl.FunctionDecl
        val block = funcDecl.function.body
        
        assertThat(block.statements.size).isEqualTo(1)
        val stmt = block.statements[0]
        assertThat(stmt).isInstanceOf(Statement.LetDecl::class.java)
    }
    
    @Test
    fun `test if statement`() {
        val input = """
            fn main() {
                let x = 5;
                if (x > 0) {
                    return;
                } else {
                    let y = -x;
                }
            }
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        val funcDecl = unit.declarations[0] as GlobalDecl.FunctionDecl
        val block = funcDecl.function.body
        
        assertThat(block.statements.size).isEqualTo(2)
        val ifStmt = block.statements[1] as Statement.If
        assertThat(ifStmt.reject).isNotNull()
    }
    
    @Test
    fun `test for loop`() {
        val input = """
            fn main() {
                for (var i: i32 = 0; i < 10; i = i + 1) {
                    let x = i * 2;
                }
            }
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        val funcDecl = unit.declarations[0] as GlobalDecl.FunctionDecl
        val block = funcDecl.function.body
        
        val forStmt = block.statements[0] as Statement.For
        assertThat(forStmt.init).isNotNull()
        assertThat(forStmt.condition).isNotNull()
        assertThat(forStmt.update).isNotNull()
    }
    
    @Test
    fun `test while loop`() {
        val input = """
            fn main() {
                var i: i32 = 0;
                while (i < 10) {
                    i = i + 1;
                }
            }
        """.trimIndent()
        
        val parser = Parser(input)
        val unit = parser.parse()
        
        val funcDecl = unit.declarations[0] as GlobalDecl.FunctionDecl
        val block = funcDecl.function.body
        
        assertThat(block.statements[1] is Statement.While).isTrue()
    }
    
    @Test
    fun `test expression parsing`() {
        val input = "42 + 3 * 2"
        val parser = Parser(input)
        val expr = parser.parseExpression()
        
        // Vérifier que la précédence est correcte : 42 + (3 * 2)
        val binary = expr as Expression.Binary
        assertThat(binary.op).isEqualTo(BinaryOperator.ADD)
        
        val right = binary.right as Expression.Binary
        assertThat(right.op).isEqualTo(BinaryOperator.MUL)
    }
    
    @Test
    fun `test function call`() {
        val input = "myFunc(a, b, c)"
        val parser = Parser(input)
        val expr = parser.parseExpression()
        
        val call = expr as Expression.Call
        assertThat(call.function.function.ident).isInstanceOf(IdentExpr.Unresolved::class.java)
        assertThat((call.function.function.ident as IdentExpr.Unresolved).name).isEqualTo("myFunc")
        assertThat(call.function.arguments.size).isEqualTo(3)
    }
    
    @Test
    fun `test template type`() {
        val input = "vec3<f32>"
        val parser = Parser(input)
        val typeDecl = parser.parseTypeDecl()
        
        val vecType = typeDecl as TypeDeclaration.Vector
        assertThat(vecType.size).isEqualTo(VectorSize.TRI)
    }
    
    @Test
    fun `test array type`() {
        val input = "array<i32, 10>"
        val parser = Parser(input)
        val typeDecl = parser.parseTypeDecl()
        
        val arrayType = typeDecl as TypeDeclaration.Array
        // Vérifier que l'élément est i32 et la longueur est 10
    }
    
    @Test
    fun `test const assertion`() {
        val input = "@const_assert(42 == 42)"
        val parser = Parser(input)
        val unit = parser.parse()
        
        assertThat(unit.declarations.size).isEqualTo(1)
        val decl = unit.declarations[0] as GlobalDecl.ConstAssertDecl
        assertThat(decl).isNotNull()
    }
}
```

---

## ✅ CHECKLIST PHASE 2.1

### Parser WGSL
- [ ] Implémenter `Parser` class avec gestion des tokens
- [ ] Implémenter `TranslationUnit` et tous les nodes AST
- [ ] Implémenter le parsing des déclarations top-level
  - [ ] Fonctions (`fn`)
  - [ ] Variables globales (`let`, `const`, `var`)
  - [ ] Structs (`struct`)
  - [ ] Type aliases (`type`)
  - [ ] Override declarations (`@override`)
  - [ ] Const assertions (`@const_assert`)
- [ ] Implémenter le parsing des types
  - [ ] Types scalaires (`i32`, `f32`, `bool`, etc.)
  - [ ] Types vecteurs (`vec2<T>`, `vec3<T>`, `vec4<T>`)
  - [ ] Types matrices (`mat2x2<T>`, `mat3x3<T>`, etc.)
  - [ ] Types tableaux (`array<T>`, `array<T, N>`)
  - [ ] Types struct
  - [ ] Types pointeurs (`ptr<function, T, read>`)
  - [ ] Types templates
- [ ] Implémenter le parsing des expressions avec bonne précédence
  - [ ] Littéraux (nombres, booléens, strings)
  - [ ] Identifiants et appels de fonction
  - [ ] Opérateurs unaires (`-`, `!`, `~`, `++`, `--`)
  - [ ] Opérateurs binaires (arithmétiques, logiques, bits, comparaisons)
  - [ ] Opérateur ternaire (`?:`)
  - [ ] Accès aux membres (`.x`, `.y`, `[index]`)
  - [ ] Constructeurs de vecteurs/matrices
  - [ ] Casts (`T(expr)`, `as`)
  - [ ] Expressions de sélection (`select`)
- [ ] Implémenter le parsing des instructions
  - [ ] Bloc (`{ ... }`)
  - [ ] `if`/`else if`/`else`
  - [ ] `switch`/`case`/`default`
  - [ ] `for` loop
  - [ ] `while` loop
  - [ ] `loop`/`continuing`
  - [ ] `break`/`continue`
  - [ ] `return`
  - [ ] `discard`
  - [ ] Déclarations locales (`let`, `const`, `var`)
  - [ ] Assignations (`=`, `+=`, `-=`, etc.)
  - [ ] Incrément/Décrément (`++`, `--`)
- [ ] Implémenter le type resolver optionnel
- [ ] Implémenter la gestion des erreurs avec récupération

### Tests
- [ ] ParserTest (`test empty module`, `test single function`, etc.)
- [ ] Tests pour chaque type de déclaration
- [ ] Tests pour chaque type d'expression
- [ ] Tests pour chaque type d'instruction
- [ ] Tests pour la précédence des opérateurs
- [ ] Tests pour la récupération d'erreurs

### Documentation
- [ ] KDoc pour toutes les classes publiques
- [ ] KDoc pour toutes les méthodes publiques
- [ ] Diagrammes de la structure AST
- [ ] Exemples d'utilisation

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Finaliser la conception de l'AST | 2 jours | Lexer | [ ] |
| Implémenter TranslationUnit et GlobalDecl | 2 jours | AST Design | [ ] |
| Implémenter Expression et tous les types | 3 jours | AST Design | [ ] |
| Implémenter Statement et tous les types | 3 jours | AST Design | [ ] |
| Implémenter TypeDeclaration | 2 jours | AST Design | [ ] |
| Implémenter le parser principal | 5 jours | AST complet | [ ] |
| Implémenter le parsing des déclarations | 3 jours | Parser principal | [ ] |
| Implémenter le parsing des types | 3 jours | Parser principal | [ ] |
| Implémenter le parsing des expressions | 5 jours | Parser principal | [ ] |
| Implémenter le parsing des instructions | 3 jours | Parser principal | [ ] |
| Implémenter le type resolver | 2 jours | Parser complet | [ ] |
| Écrire les tests unitaires | 4 jours | Parser complet | [ ] |
| Documentation complète | 2 jours | Tout | [ ] |
| Validation manuelle | 2 jours | Tout | [ ] |

**Total estimé** : **3-4 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `Parser.kt`
   - `TranslationUnit.kt`
   - `GlobalDecl.kt`
   - `Expression.kt`
   - `Statement.kt`
   - `TypeDeclaration.kt`
   - `Attribute.kt`
   - `TypeResolver.kt`

2. **Tests unitaires** :
   - `ParserTest.kt`

3. **Couverture de test** : > 95%

4. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/mod.rs`
- **AST Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/ast.rs`
- **Lexer Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/lexer.rs`
- **Spécification WGSL** : https://gpuweb.github.io/gpuweb/wgsl/
- **Fichier précédent** : `00_wgsl-lexer.md`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Finaliser la conception de l'AST
2. [ ] Implémenter les structures de données AST
3. [ ] Implémenter le parser principal
4. [ ] Implémenter le parsing de chaque construct
5. [ ] Écrire tous les tests unitaires
6. [ ] Valider avec des tests manuels
7. [ ] Passer à `02_ast-building.md` (construction de l'AST)

**Fichier suivant** : `02_ast-building.md`
