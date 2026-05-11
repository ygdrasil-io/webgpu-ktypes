# 🏗️ Phase 2.2 : Construction de l'AST

**Projet** : WebGPU-KTypes Shader Transpiler  
**Module** : `naga-wgsl`  
**Phase** : 2 - Parsing  
**Sous-Phase** : 2.2 - AST Building  
**Durée** : 2-3 semaines  
**Priorité** : ⭐⭐⭐⭐⭐ (Critique - Transformation Lexer→AST)  
**Statut** : [ ] Non commencé | [ ] En cours | [ ] Complété

> **Référence Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/mod.rs` (Parser::parse_*)

---

## 📋 OBJECTIFS

Transformer la sortie du lexer (séquence de tokens) en un **AST structuré** en utilisant le parser. Cette phase se concentre sur la **construction propre de l'AST** : création des nodes, gestion des arenas, association des spans, et maintenance de la structure hiérarchique.

**Livrable principal** : Un pipeline de parsing complet qui construit un AST valide à partir de code WGSL.

---

## 🎯 CONCEPTS CLÉS

### 1. Pipeline de Construction AST

```
Input (WGSL Source)
    ↓
Lexer → List<Token>
    ↓
Parser → TranslationUnit (AST)
    ↓
Type Resolver → AST with resolved types
    ↓
Output (Validated AST)
```

### 2. Responsabilités du Builder

Le builder est responsable de :
- **Création des nodes** : Instancier les bons types d'expressions, déclarations, etc.
- **Gestion des arenas** : Ajouter les nodes aux arenas appropriées et retourner les Handles
- **Tracking des spans** : Associer chaque node à son Span pour les erreurs et diagnostics
- **Construction hiérarchique** : Maintenir la relation parent-enfant dans l'AST

### 3. Gestion des Arenas

Chaque type de node a son propre Arena :
```kotlin
// Dans TranslationUnit
val declarations: Arena<GlobalDecl>    // Declarations globales
val expressions: Arena<Expression>      // Toutes les expressions
val types: Arena<TypeDeclaration>       // Types
val statements: Arena<Statement>       // Instructions (par fonction)
val locals: Arena<Local>               // Variables locales (par fonction)
```

**Règle importante** : Les Handles ne doivent JAMAIS être créés manuellement. Toujours utiliser `arena.append(value)` pour garantir l'unicité et la validité.

---

## 📦 IMPLÉMENTATION DÉTAILLÉE

### 1. AstBuilder.kt (Builder principal)

**Fichier** : `naga-wgsl/src/main/kotlin/dev/gfxrs/naga/front/wgsl/AstBuilder.kt`

```kotlin
package dev.gfxrs.naga.front.wgsl

import dev.gfxrs.naga.arena.Arena
import dev.gfxrs.naga.arena.Handle
import dev.gfxrs.naga.front.wgsl.ast.*
import dev.gfxrs.naga.front.wgsl.lexer.Token
import dev.gfxrs.naga.front.wgsl.lexer.TokenKind
import dev.gfxrs.naga.span.Span

/**
 * Builder pour construire l'AST à partir des tokens.
 * Centralise la création de tous les nodes AST.
 */
class AstBuilder(val input: String) {
    
    // Arenas
    val declarations: Arena<GlobalDecl> = Arena()
    val expressions: Arena<Expression> = Arena()
    val types: Arena<TypeDeclaration> = Arena()
    
    // State
    private var currentFunction: Handle<GlobalDecl.FunctionDecl>? = null
    private var currentBlock: Handle<Statement.Block>? = null
    
    // Stats pour debugging
    var expressionCount: Int = 0
    var statementCount: Int = 0
    var declarationCount: Int = 0
    
    /**
     * Réinitialise le builder pour un nouveau module.
     */
    fun reset() {
        declarations.clear()
        expressions.clear()
        types.clear()
        currentFunction = null
        currentBlock = null
        expressionCount = 0
        statementCount = 0
        declarationCount = 0
    }
    
    // ===== Déclarations =====
    
    /**
     * Crée une déclaration de fonction.
     */
    fun function(
        attributes: List<Attribute>,
        name: String,
        parameters: List<FunctionParameter>,
        returnType: Handle<TypeDeclaration>?,
        body: Handle<Statement.Block>,
        span: Span
    ): Handle<GlobalDecl> {
        val func = Function(
            attributes = attributes,
            name = name,
            parameters = parameters,
            returnType = returnType,
            body = body,
            span = span
        )
        val decl = GlobalDecl.FunctionDecl(func)
        val handle = declarations.append(decl)
        declarationCount++
        return handle
    }
    
    /**
     * Crée une déclaration de struct.
     */
    fun struct(
        name: String,
        members: List<StructMember>,
        span: Span
    ): Handle<GlobalDecl> {
        val s = Struct(name = name, members = members, span = span)
        val decl = GlobalDecl.StructDecl(s)
        val handle = declarations.append(decl)
        declarationCount++
        return handle
    }
    
    /**
     * Crée une déclaration de variable globale let.
     */
    fun letDecl(
        name: String,
        type: Handle<TypeDeclaration>?,
        init: Handle<Expression>,
        span: Span
    ): Handle<GlobalDecl> {
        val let = Let(name = name, type = type, init = init, span = span)
        val decl = GlobalDecl.LetDecl(let)
        val handle = declarations.append(decl)
        declarationCount++
        return handle
    }
    
    /**
     * Crée une déclaration de constante globale.
     */
    fun constDecl(
        name: String,
        type: Handle<TypeDeclaration>,
        init: Handle<Expression>,
        span: Span
    ): Handle<GlobalDecl> {
        val c = Const(name = name, type = type, init = init, span = span)
        val decl = GlobalDecl.ConstDecl(c)
        val handle = declarations.append(decl)
        declarationCount++
        return handle
    }
    
    /**
     * Crée une déclaration de variable globale var.
     */
    fun varDecl(
        name: String,
        type: Handle<TypeDeclaration>,
        init: Handle<Expression>?,
        attributes: List<Attribute>,
        span: Span
    ): Handle<GlobalDecl> {
        val v = GlobalVariable(name = name, type = type, init = init, attributes = attributes, span = span)
        val decl = GlobalDecl.VarDecl(v)
        val handle = declarations.append(decl)
        declarationCount++
        return handle
    }
    
    /**
     * Crée un type alias.
     */
    fun typeAlias(
        name: String,
        type: Handle<TypeDeclaration>,
        span: Span
    ): Handle<GlobalDecl> {
        val alias = TypeAlias(name = name, type = type, span = span)
        val decl = GlobalDecl.TypeAliasDecl(alias)
        val handle = declarations.append(decl)
        declarationCount++
        return handle
    }
    
    // ===== Types =====
    
    /**
     * Crée un type scalaire.
     */
    fun scalarType(kind: ScalarKind, span: Span): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Scalar(kind, span)
        val handle = types.append(type)
        return handle
    }
    
    /**
     * Crée un type vecteur.
     */
    fun vectorType(
        size: VectorSize,
        scalar: Handle<TypeDeclaration>,
        span: Span
    ): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Vector(size, scalar, span)
        val handle = types.append(type)
        return handle
    }
    
    /**
     * Crée un type matrice.
     */
    fun matrixType(
        rows: VectorSize,
        cols: VectorSize,
        scalar: Handle<TypeDeclaration>,
        span: Span
    ): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Matrix(rows, cols, scalar, span)
        val handle = types.append(type)
        return handle
    }
    
    /**
     * Crée un type tableau.
     */
    fun arrayType(
        element: Handle<TypeDeclaration>,
        length: Handle<Expression>?,
        span: Span
    ): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Array(element, length, span)
        val handle = types.append(type)
        return handle
    }
    
    /**
     * Crée un type struct.
     */
    fun structType(
        name: String?,
        members: List<StructMember>,
        span: Span
    ): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Struct(name, members, span)
        val handle = types.append(type)
        return handle
    }
    
    /**
     * Crée une référence à un type nommé (avec template parameters).
     */
    fun namedType(
        name: String,
        templateList: List<Handle<Expression>>,
        span: Span
    ): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Named(name, templateList, span)
        val handle = types.append(type)
        return handle
    }
    
    /**
     * Crée un type pointeur.
     */
    fun pointerType(
        base: Handle<TypeDeclaration>,
        access: StorageAccess,
        span: Span
    ): Handle<TypeDeclaration> {
        val type = TypeDeclaration.Pointer(base, access, span)
        val handle = types.append(type)
        return handle
    }
    
    // ===== Expressions =====
    
    /**
     * Crée une expression littérale.
     */
    fun literal(value: LiteralValue, span: Span): Handle<Expression> {
        val expr = Expression.Literal(value, span)
        val handle = expressions.append(expr)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression identifiant.
     */
    fun ident(name: String, span: Span): Handle<Expression> {
        val expr = Expression.Ident(IdentExpr.Unresolved(name), span)
        val handle = expressions.append(expr)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression d'appel (fonction ou constructeur).
     */
    fun call(
        function: TemplateElaboratedIdent,
        arguments: List<Handle<Expression>>,
        span: Span
    ): Handle<Expression> {
        val phrase = CallPhrase(function, arguments)
        val expr = Expression.Call(phrase, span)
        val handle = expressions.append(expr)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression binaire.
     */
    fun binary(
        left: Handle<Expression>,
        op: BinaryOperator,
        right: Handle<Expression>,
        span: Span
    ): Handle<Expression> {
        val expr = Expression.Binary(left, op, right, span)
        val handle = expressions.append(expr)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression unaire.
     */
    fun unary(
        expr: Handle<Expression>,
        op: UnaryOperator,
        span: Span
    ): Handle<Expression> {
        val e = Expression.Unary(expr, op, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression ternaire (condition ? accept : reject).
     */
    fun ternary(
        condition: Handle<Expression>,
        accept: Handle<Expression>,
        reject: Handle<Expression>,
        span: Span
    ): Handle<Expression> {
        val expr = Expression.Ternary(condition, accept, reject, span)
        val handle = expressions.append(expr)
        expressionCount++
        return handle
    }
    
    /**
     * Crée un accès à un membre (expr.member).
     */
    fun memberAccess(
        expr: Handle<Expression>,
        member: String,
        span: Span
    ): Handle<Expression> {
        val e = Expression.MemberAccess(expr, member, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée un accès par index (expr[index]).
     */
    fun access(
        expr: Handle<Expression>,
        index: Handle<Expression>,
        span: Span
    ): Handle<Expression> {
        val e = Expression.Access(expr, index, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée un accès par index constant (expr[42]).
     */
    fun accessIndex(
        expr: Handle<Expression>,
        index: Int,
        span: Span
    ): Handle<Expression> {
        val e = Expression.AccessIndex(expr, index, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression splat (vec4(x)).
     */
    fun splat(
        expr: Handle<Expression>,
        span: Span
    ): Handle<Expression> {
        val e = Expression.Splat(expr, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression as (cast).
     */
    fun asExpr(
        expr: Handle<Expression>,
        type: Handle<TypeDeclaration>,
        span: Span
    ): Handle<Expression> {
        val e = Expression.As(expr, type, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression compose (construction de vecteur/matrice).
     */
    fun compose(
        type: Handle<TypeDeclaration>,
        arguments: List<Handle<Expression>>,
        span: Span
    ): Handle<Expression> {
        val e = Expression.Compose(type, arguments, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression select.
     */
    fun select(
        condition: Handle<Expression>,
        accept: Handle<Expression>,
        reject: Handle<Expression>,
        span: Span
    ): Handle<Expression> {
        val e = Expression.Select(condition, accept, reject, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    /**
     * Crée une expression arrayLength.
     */
    fun arrayLength(
        expr: Handle<Expression>,
        span: Span
    ): Handle<Expression> {
        val e = Expression.ArrayLength(expr, span)
        val handle = expressions.append(e)
        expressionCount++
        return handle
    }
    
    // ===== Instructions =====
    
    /**
     * Crée un bloc d'instructions.
     */
    fun block(
        statements: List<Handle<Statement>>,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.Block(statements, span)
        // Note: Les blocs ne sont pas stockés dans une arena séparée
        // Ils sont stockés directement dans le body de leur parent
        statementCount++
        // Pour les blocs, on retourne directement l'objet (pas de Handle)
        // car les blocs sont des containers, pas des feuilles
        return Handle.fromIndex(statementCount - 1)
    }
    
    /**
     * Crée une instruction if.
     */
    fun ifStatement(
        condition: Handle<Expression>,
        accept: Handle<Statement>,
        reject: Handle<Statement>?,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.If(condition, accept, reject, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction switch.
     */
    fun switchStatement(
        selector: Handle<Expression>,
        body: SwitchBody,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.Switch(selector, body, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction loop.
     */
    fun loopStatement(
        body: Handle<Statement>,
        continuing: LoopContinuing?,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.Loop(body, continuing, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction while.
     */
    fun whileStatement(
        condition: Handle<Expression>,
        body: Handle<Statement>,
        continuing: LoopContinuing?,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.While(condition, body, continuing, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction for.
     */
    fun forStatement(
        init: Handle<Statement>?,
        condition: Handle<Expression>?,
        update: Handle<Statement>?,
        body: Handle<Statement>,
        continuing: LoopContinuing?,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.For(init, condition, update, body, continuing, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction break.
     */
    fun breakStatement(span: Span): Handle<Statement> {
        val stmt = Statement.Break(span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction continue.
     */
    fun continueStatement(span: Span): Handle<Statement> {
        val stmt = Statement.Continue(span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction return.
     */
    fun returnStatement(
        expr: Handle<Expression>?,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.Return(expr, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction discard.
     */
    fun discardStatement(span: Span): Handle<Statement> {
        val stmt = Statement.Discard(span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction emit.
     */
    fun emitStatement(
        range: Range<Expression>,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.Emit(range)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une déclaration let.
     */
    fun letStatement(
        name: String,
        type: Handle<TypeDeclaration>?,
        init: Handle<Expression>,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.LetDecl(name, type, init, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une déclaration const.
     */
    fun constStatement(
        name: String,
        type: Handle<TypeDeclaration>,
        init: Handle<Expression>,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.ConstDecl(name, type, init, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une déclaration var.
     */
    fun varStatement(
        name: String,
        type: Handle<TypeDeclaration>,
        init: Handle<Expression>?,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.VariableDecl(name, type, init, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction d'assignment.
     */
    fun assignmentStatement(
        lhs: Handle<Expression>,
        rhs: Handle<Expression>,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.Assignment(lhs, rhs, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    /**
     * Crée une instruction increment/decrement.
     */
    fun incrementDecrementStatement(
        expr: Handle<Expression>,
        op: IncrementDecrementOperator,
        span: Span
    ): Handle<Statement> {
        val stmt = Statement.IncrementDecrement(expr, op, span)
        val handle = statements.append(stmt)
        statementCount++
        return handle
    }
    
    // ===== Helpers pour TemplateElaboratedIdent =====
    
    /**
     * Crée un TemplateElaboratedIdent.
     */
    fun templateElaboratedIdent(
        name: String,
        templateList: List<Handle<Expression>>,
        identSpan: Span,
        templateListSpan: Span
    ): TemplateElaboratedIdent {
        return TemplateElaboratedIdent(
            ident = IdentExpr.Unresolved(name),
            identSpan = identSpan,
            templateList = templateList,
            templateListSpan = templateListSpan
        )
    }
    
    // ===== Helpers pour CallPhrase =====
    
    /**
     * Crée un CallPhrase.
     */
    fun callPhrase(
        function: TemplateElaboratedIdent,
        arguments: List<Handle<Expression>>
    ): CallPhrase {
        return CallPhrase(function, arguments)
    }
    
    // ===== Helpers pour Switch =====
    
    /**
     * Crée un SwitchBody.
     */
    fun switchBody(cases: List<SwitchCase>): SwitchBody {
        return SwitchBody(cases)
    }
    
    /**
     * Crée un SwitchCase.
     */
    fun switchCase(
        selectors: List<Handle<Expression>>,
        body: Handle<Statement>
    ): SwitchCase {
        return SwitchCase(selectors, body)
    }
    
    // ===== Helpers pour LoopContinuing =====
    
    /**
     * Crée un LoopContinuing.
     */
    fun loopContinuing(body: Handle<Statement>): LoopContinuing {
        return LoopContinuing(body)
    }
}
```

---

## 📝 UTILISATION AVEC LE PARSER

### Intégration Parser + Builder

```kotlin
class Parser(private val input: String) {
    private val lexer: Lexer = Lexer(input)
    private val builder: AstBuilder = AstBuilder(input)
    
    fun parse(): TranslationUnit {
        // Initialiser le lexer
        advance()
        skipTrivia()
        
        val decls = mutableListOf<Handle<GlobalDecl>>()
        
        while (!match(TokenKind.EOF)) {
            val decl = parseTopLevelDeclWithBuilder()
            if (decl != null) {
                decls.add(decl)
            }
            skipTrivia()
        }
        
        return TranslationUnit(
            declarations = builder.declarations,
            expressions = builder.expressions,
            types = builder.types,
            docComments = builder.docComments
        )
    }
    
    private fun parseFunctionDeclWithBuilder(): Handle<GlobalDecl>? {
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
        
        return builder.function(attributes, name, parameters, returnType, body, spanFrom(startSpan))
    }
    
    private fun parseTypeDecl(): Handle<TypeDeclaration> {
        val startSpan = currentSpan()
        
        when {
            matchScalarType() -> {
                val kind = currentScalarKind()
                advance()
                return builder.scalarType(kind, spanFrom(startSpan))
            }
            matchVectorType() -> {
                return parseVectorTypeWithBuilder()
            }
            // ... autres types
        }
    }
    
    private fun parseExpression(): Handle<Expression> {
        return parseTernaryExpression()
    }
    
    private fun parseBinaryExpression(
        left: Handle<Expression>,
        op: BinaryOperator,
        right: Handle<Expression>
    ): Handle<Expression> {
        return builder.binary(left, op, right, spanFrom(left))
    }
    
    private fun parseCallExpression(
        function: TemplateElaboratedIdent,
        arguments: List<Handle<Expression>>
    ): Handle<Expression> {
        return builder.call(function, arguments, spanFrom(function.identSpan))
    }
    
    // ... autres méthodes
}
```

---

## ✅ CHECKLIST PHASE 2.2

### Builder AST
- [ ] Implémenter `AstBuilder` class
- [ ] Implémenter les méthodes de création pour toutes les déclarations
- [ ] Implémenter les méthodes de création pour tous les types
- [ ] Implémenter les méthodes de création pour toutes les expressions
- [ ] Implémenter les méthodes de création pour toutes les instructions
- [ ] Implémenter les helpers (TemplateElaboratedIdent, CallPhrase, etc.)

### Intégration
- [ ] Intégrer AstBuilder dans Parser
- [ ] Remplacer la création directe de nodes par les méthodes du builder
- [ ] Vérifier que tous les nodes ont des spans correctes
- [ ] Vérifier que toutes les arenas sont correctement utilisées

### Tests
- [ ] BuilderTest (`test scalar type creation`, `test vector type creation`, etc.)
- [ ] Tests d'intégration parser+builder
- [ ] Tests de validation des spans
- [ ] Tests de validation des arenas

### Documentation
- [ ] KDoc pour toutes les méthodes du builder
- [ ] Exemples d'utilisation
- [ ] Documentation des patterns de design

---

## 📅 PLANNING

| Tâche | Durée | Dépendances | Statut |
|-------|-------|-------------|--------|
| Concevoir l'API du builder | 1 jour | AST design | [ ] |
| Implémenter AstBuilder class | 2 jours | AST complet | [ ] |
| Implémenter les méthodes de création (déclarations) | 2 jours | Builder class | [ ] |
| Implémenter les méthodes de création (types) | 1 jour | Builder class | [ ] |
| Implémenter les méthodes de création (expressions) | 2 jours | Builder class | [ ] |
| Implémenter les méthodes de création (statements) | 2 jours | Builder class | [ ] |
| Intégrer avec Parser | 2 jours | Builder complet | [ ] |
| Écrire les tests unitaires | 2 jours | Intégration | [ ] |
| Documentation complète | 1 jour | Tout | [ ] |
| Validation manuelle | 1 jour | Tout | [ ] |

**Total estimé** : **2-3 semaines** (1 developer)

---

## 🎯 LIVRABLES

1. **Fichiers Kotlin** :
   - `AstBuilder.kt`

2. **Fichiers modifiés** :
   - `Parser.kt` (intégration avec AstBuilder)

3. **Tests unitaires** :
   - `BuilderTest.kt`

4. **Couverture de test** : > 95%

5. **Documentation** : KDoc complet

---

## 🔗 RÉFÉRENCES

- **Fichier Rust principal** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/mod.rs`
- **AST Rust** : `/Users/chaos/RustroverProjects/wgpu/naga/src/front/wgsl/parse/ast.rs`
- **Fichier précédent** : `01_wgsl-parser.md`
- **Fichier suivant** : `03_type-resolution.md`

---

## 🔄 PROCHAINES ÉTAPES

1. [ ] Finaliser la conception de AstBuilder
2. [ ] Implémenter AstBuilder class
3. [ ] Implémenter toutes les méthodes de création
4. [ ] Intégrer avec Parser
5. [ ] Écrire les tests unitaires
6. [ ] Valider avec des tests manuels
7. [ ] Passer à `03_type-resolution.md` (résolution de types)
